/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cloudogu.scm.mirror;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.Signature;
import sonia.scm.repository.SignatureStatus;
import sonia.scm.repository.Tag;
import sonia.scm.repository.api.MirrorFilter;
import sonia.scm.security.PublicKey;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.SignatureStatus.INVALID;
import static sonia.scm.repository.SignatureStatus.NOT_FOUND;
import static sonia.scm.repository.SignatureStatus.VERIFIED;

class FilterBuilderTest {

  private final FilterBuilder filterBuilder = new FilterBuilder();

  @Test
  void shouldReturnNonFilteringFilterWhenNothingConfigured() {
    MirrorConfiguration configuration = new MirrorConfiguration();
    configuration.setGpgVerificationType(MirrorGpgVerificationType.NONE);

    MirrorFilter mirrorFilter = filterBuilder.createFilter(configuration, emptyList());

    MirrorFilter.Filter filter = mirrorFilter.getFilter(null);
    assertThat(filter.acceptBranch(mock(MirrorFilter.BranchUpdate.class))).isTrue();
    assertThat(filter.acceptTag(mock(MirrorFilter.TagUpdate.class))).isTrue();
  }

  @Nested
  class WhenConfiguredForAnySignature {

    private final MirrorConfiguration configuration = new MirrorConfiguration();
    private MirrorFilter mirrorFilter;

    @BeforeEach
    void setUpConfiguration() {
      configuration.setGpgVerificationType(MirrorGpgVerificationType.SIGNATURE);
      mirrorFilter = filterBuilder.createFilter(configuration, emptyList());
    }

    @Test
    void shouldRejectBranchesWithoutSignature() {
      Changeset changeset = new Changeset();
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isFalse();
    }

    @Test
    void shouldRejectTagsWithoutSignature() {
      Tag tag = new Tag("nice", "abc");
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isFalse();
    }

    @Test
    void shouldRejectBranchesInvalidSignature() {
      Changeset changeset = createSignedChangeset("key-1", INVALID);
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isFalse();
    }

    @Test
    void shouldRejectTagsInvalidSignature() {
      Tag tag = createSignedTag("nice", "key-2", INVALID);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isFalse();
    }

    @Test
    void shouldAcceptBranchesWithValidSignatureForUnknownUser() {
      Changeset changeset = createSignedChangeset("key-1", NOT_FOUND);
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isTrue();
    }

    @Test
    void shouldAcceptTagsWithValidSignatureForUnknownUser() {
      Tag tag = createSignedTag("nice", "key-2", NOT_FOUND);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isTrue();
    }

    @Test
    void shouldAcceptBranchesWithValidSignatureForKnownUser() {
      Changeset changeset = createSignedChangeset("key-1", VERIFIED);
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isTrue();
    }

    @Test
    void shouldAcceptTagsWithValidSignatureForKnownUser() {
      Tag tag = createSignedTag("nice", "key-2", VERIFIED);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isTrue();
    }
  }

  @Nested
  class WhenConfiguredForScmUserSignature {

    private final MirrorConfiguration configuration = new MirrorConfiguration();
    private MirrorFilter mirrorFilter;

    @BeforeEach
    void setUpConfiguration() {
      configuration.setGpgVerificationType(MirrorGpgVerificationType.SCM_USER_SIGNATURE);
      mirrorFilter = filterBuilder.createFilter(configuration, emptyList());
    }

    @Test
    void shouldRejectBranchesWithoutSignature() {
      Changeset changeset = new Changeset();
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isFalse();
    }

    @Test
    void shouldRejectTagsWithoutSignature() {
      Tag tag = new Tag("nice", "abc");
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isFalse();
    }

    @Test
    void shouldRejectBranchesInvalidSignature() {
      Changeset changeset = createSignedChangeset("key-1", INVALID);
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isFalse();
    }

    @Test
    void shouldRejectTagsInvalidSignature() {
      Tag tag = createSignedTag("nice", "key-2", INVALID);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isFalse();
    }

    @Test
    void shouldRejectBranchesWithValidSignatureForUnknownUser() {
      Changeset changeset = createSignedChangeset("key-1", NOT_FOUND);
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isFalse();
    }

    @Test
    void shouldRejectTagsWithValidSignatureForUnknownUser() {
      Tag tag = createSignedTag("nice", "key-2", NOT_FOUND);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isFalse();
    }

    @Test
    void shouldAcceptBranchesWithValidSignatureForKnownUser() {
      Changeset changeset = createSignedChangeset("key-1", VERIFIED);
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isTrue();
    }

    @Test
    void shouldAcceptTagsWithValidSignatureForKnownUser() {
      Tag tag = createSignedTag("nice", "key-2", VERIFIED);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isTrue();
    }
  }

  @Nested
  class WhenConfiguredForSignatureFromKeyList {

    private final MirrorConfiguration configuration = new MirrorConfiguration();
    private MirrorFilter mirrorFilter;

    @BeforeEach
    void setUpConfiguration() {
      configuration.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
      PublicKey publicKey = mock(PublicKey.class);
      when(publicKey.getId()).thenReturn("accepted");
      mirrorFilter = filterBuilder.createFilter(configuration, singletonList(publicKey));
    }

    @Test
    void shouldRejectBranchesWithoutSignature() {
      Changeset changeset = new Changeset();
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isFalse();
    }

    @Test
    void shouldRejectTagsWithoutSignature() {
      Tag tag = new Tag("nice", "abc");
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isFalse();
    }

    @Test
    void shouldRejectBranchesInvalidSignature() {
      Changeset changeset = createSignedChangeset("key-1", INVALID);
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isFalse();
    }

    @Test
    void shouldRejectTagsInvalidSignature() {
      Tag tag = createSignedTag("nice", "key-2", INVALID);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isFalse();
    }

    @Test
    void shouldRejectBranchesWithValidSignatureForUnknownUser() {
      Changeset changeset = createSignedChangeset("key-1", NOT_FOUND);
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isFalse();
    }

    @Test
    void shouldRejectTagsWithValidSignatureForUnknownUser() {
      Tag tag = createSignedTag("nice", "key-2", NOT_FOUND);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isFalse();
    }

    @Test
    void shouldRejectBranchesWithValidSignatureWithKeyNotFromKeyList() {
      Changeset changeset = createSignedChangeset("key-1", VERIFIED);
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isFalse();
    }

    @Test
    void shouldRejectTagsWithValidSignatureForKeyNotFromKeyList() {
      Tag tag = createSignedTag("nice", "key-2", VERIFIED);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isFalse();
    }

    @Test
    void shouldAcceptBranchesWithValidSignatureWithKeyFromKeyList() {
      Changeset changeset = createSignedChangeset("accepted", VERIFIED);
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isTrue();
    }

    @Test
    void shouldAcceptTagsWithValidSignatureWithKeyFromKeyList() {
      Tag tag = createSignedTag("nice", "accepted", VERIFIED);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isTrue();
    }
  }

  @Nested
  class WhenConfiguredWithBranchTagFilter {

    private final MirrorConfiguration configuration = new MirrorConfiguration();
    private MirrorFilter mirrorFilter;

    @BeforeEach
    void setUpConfiguration() {
      configuration.setGpgVerificationType(MirrorGpgVerificationType.NONE);
      configuration.setBranchesAndTagsPatterns(asList("main", "feature/*", "release/*"));
      mirrorFilter = filterBuilder.createFilter(configuration, emptyList());
    }

    @Test
    void shouldAcceptBranchesWithMatchingName() {
      Changeset changeset = new Changeset();
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "main");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isTrue();
    }

    @Test
    void shouldAcceptTagsWithMatchingName() {
      Tag tag = createSignedTag("release/42.0", "accepted", VERIFIED);
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isTrue();
    }

    @Test
    void shouldRejectBranchesWithOtherName() {
      Changeset changeset = new Changeset();
      MirrorFilter.BranchUpdate branchUpdate = mockBranchUpdate(changeset, "testing/something");

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptBranch(branchUpdate)).isFalse();
    }

    @Test
    void shouldRejectTagsWithOtherName() {
      Tag tag = new Tag("beta/0.0.0.1", "abc");
      MirrorFilter.TagUpdate tagUpdate = mockTagUpdate(tag);

      MirrorFilter.Filter filter = mirrorFilter.getFilter(null);

      assertThat(filter.acceptTag(tagUpdate)).isFalse();
    }
  }

  private MirrorFilter.BranchUpdate mockBranchUpdate(Changeset changeset, String name) {
    MirrorFilter.BranchUpdate branchUpdate = mock(MirrorFilter.BranchUpdate.class);
    when(branchUpdate.getChangeset()).thenReturn(changeset);
    when(branchUpdate.getBranchName()).thenReturn(name);
    return branchUpdate;
  }

  private MirrorFilter.TagUpdate mockTagUpdate(Tag tag) {
    String tagName = tag.getName();
    MirrorFilter.TagUpdate tagUpdate = mock(MirrorFilter.TagUpdate.class);
    when(tagUpdate.getTag()).thenReturn(tag);
    when(tagUpdate.getTagName()).thenReturn(tagName);
    return tagUpdate;
  }

  private Changeset createSignedChangeset(String keyId, SignatureStatus status) {
    Changeset changeset = new Changeset();
    changeset.setSignatures(singletonList(createSignature(keyId, status)));
    return changeset;
  }

  private Tag createSignedTag(String name, String keyId, SignatureStatus status) {
    Tag tag = new Tag(name, "42");
    tag.addSignature(createSignature(keyId, status));
    return tag;
  }

  private Signature createSignature(String keyId, SignatureStatus status) {
    return new Signature(keyId, "gpg", status, "dent", emptySet());
  }
}
