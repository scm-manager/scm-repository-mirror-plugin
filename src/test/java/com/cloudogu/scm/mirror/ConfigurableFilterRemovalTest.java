/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.mirror;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.Tag;
import sonia.scm.repository.api.MirrorFilter;
import sonia.scm.repository.api.MirrorFilter.Result;
import sonia.scm.repository.api.MirrorFilter.UpdateType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurableFilterRemovalTest {

  private final MirrorConfiguration configuration = new MirrorConfiguration();
  private final ConfigurableFilter mirrorFilter = new ConfigurableFilter(configuration, emptyList());
  private final MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

  @ParameterizedTest
  @MethodSource("updateTypes")
  void shouldRemoveExcludedRefs(Optional<UpdateType> updateType) {
    configuration.setBranchesAndTagsPatterns(List.of("main"));

    Result branchResult = filter.acceptBranch(branch("feature/local", updateType));
    Result tagResult = filter.acceptTag(tag("beta/1.0", updateType));

    assertThat(branchResult.isRemovalRequested()).isTrue();
    assertThat(tagResult.isRemovalRequested()).isTrue();
    assertThat(branchResult.getRemoveReason()).contains("does not match configured patterns");
    assertThat(tagResult.getRemoveReason()).contains("does not match configured patterns");
    assertThat(mirrorFilter.hadIssues()).isFalse();
  }

  @ParameterizedTest
  @MethodSource("updateTypes")
  void shouldPreserveExcludedRefsWhenUserBranchesAreAllowed(Optional<UpdateType> updateType) {
    configuration.setAllBranchesProtected(false);
    configuration.setBranchesAndTagsPatterns(List.of("main"));

    Result branchResult = filter.acceptBranch(branch("feature/local", updateType));
    Result tagResult = filter.acceptTag(tag("beta/1.0", updateType));

    assertPreserved(branchResult, "does not match configured patterns");
    assertPreserved(tagResult, "does not match configured patterns");
    assertThat(mirrorFilter.hadIssues()).isFalse();
  }

  @Test
  void shouldRemoveUnchangedRefsAfterPatternsChange() {
    assertThat(filter.acceptBranch(branch("feature/local", of(UpdateType.CREATE))).isAccepted()).isTrue();
    assertThat(filter.acceptTag(tag("beta/1.0", of(UpdateType.CREATE))).isAccepted()).isTrue();

    configuration.setBranchesAndTagsPatterns(List.of("main"));

    assertThat(filter.acceptBranch(branch("feature/local", empty())).isRemovalRequested()).isTrue();
    assertThat(filter.acceptTag(tag("beta/1.0", empty())).isRemovalRequested()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldAcceptMatchingRefsInBothProtectionModes(boolean allBranchesProtected) {
    configuration.setAllBranchesProtected(allBranchesProtected);
    configuration.setBranchesAndTagsPatterns(List.of("main", "release/*"));

    assertThat(filter.acceptBranch(branch("main", of(UpdateType.UPDATE))).isAccepted()).isTrue();
    assertThat(filter.acceptTag(tag("release/1.0", of(UpdateType.UPDATE))).isAccepted()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldAcceptAllNamesWhenPatternsAreEmpty(boolean allBranchesProtected) {
    configuration.setAllBranchesProtected(allBranchesProtected);

    assertThat(filter.acceptBranch(branch("feature/local", empty())).isAccepted()).isTrue();
    assertThat(filter.acceptTag(tag("beta/1.0", empty())).isAccepted()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldPreserveMatchingBranchesRejectedByFastForwardCheck(boolean allBranchesProtected) {
    configuration.setAllBranchesProtected(allBranchesProtected);
    configuration.setBranchesAndTagsPatterns(List.of("main"));
    configuration.setFastForwardOnly(true);
    MirrorFilter.BranchUpdate branch = branch("main", of(UpdateType.UPDATE));
    when(branch.isForcedUpdate()).thenReturn(true);

    assertPreserved(filter.acceptBranch(branch), "no fast forward");
    assertThat(mirrorFilter.hadIssues()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldPreserveMatchingRefsRejectedBySignatureCheck(boolean allBranchesProtected) {
    configuration.setAllBranchesProtected(allBranchesProtected);
    configuration.setBranchesAndTagsPatterns(List.of("main", "release/*"));
    configuration.setGpgVerificationType(MirrorGpgVerificationType.SIGNATURE);
    MirrorFilter.BranchUpdate branch = branch("main", of(UpdateType.UPDATE));
    when(branch.getChangeset()).thenReturn(of(new Changeset()));
    MirrorFilter.TagUpdate tag = tag("release/1.0", of(UpdateType.UPDATE));
    when(tag.getTag()).thenReturn(of(new Tag("release/1.0", "revision")));

    assertPreserved(filter.acceptBranch(branch), "no valid signature");
    assertPreserved(filter.acceptTag(tag), "no valid signature");
    assertThat(mirrorFilter.hadIssues()).isTrue();
  }

  @Test
  void shouldNotReportUpdateIssuesForUnchangedMatchingRefs() {
    configuration.setBranchesAndTagsPatterns(List.of("main", "release/*"));
    configuration.setFastForwardOnly(true);
    configuration.setGpgVerificationType(MirrorGpgVerificationType.SIGNATURE);
    MirrorFilter.BranchUpdate branch = branch("main", empty());
    when(branch.getChangeset()).thenReturn(of(new Changeset()));
    MirrorFilter.TagUpdate tag = tag("release/1.0", empty());
    when(tag.getTag()).thenReturn(of(new Tag("release/1.0", "revision")));

    assertThat(filter.acceptBranch(branch).isAccepted()).isTrue();
    assertThat(filter.acceptTag(tag).isAccepted()).isTrue();
    assertThat(mirrorFilter.hadIssues()).isFalse();
  }

  private void assertPreserved(Result result, String reason) {
    assertThat(result.isAccepted()).isFalse();
    assertThat(result.isRemovalRequested()).isFalse();
    assertThat(result.getRemoveReason()).isEmpty();
    assertThat(result.getRejectReason()).contains(reason);
  }

  private MirrorFilter.BranchUpdate branch(String name, Optional<UpdateType> updateType) {
    MirrorFilter.BranchUpdate branch = mock(MirrorFilter.BranchUpdate.class);
    when(branch.getBranchName()).thenReturn(name);
    when(branch.getUpdateType()).thenReturn(updateType);
    return branch;
  }

  private MirrorFilter.TagUpdate tag(String name, Optional<UpdateType> updateType) {
    MirrorFilter.TagUpdate tag = mock(MirrorFilter.TagUpdate.class);
    when(tag.getTagName()).thenReturn(name);
    when(tag.getUpdateType()).thenReturn(updateType);
    return tag;
  }

  private static Stream<Optional<UpdateType>> updateTypes() {
    return Stream.of(empty(), of(UpdateType.CREATE), of(UpdateType.UPDATE), of(UpdateType.DELETE));
  }
}
