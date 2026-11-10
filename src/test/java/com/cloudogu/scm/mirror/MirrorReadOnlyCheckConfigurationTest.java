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

import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.ReadOnlyException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryHookEvent;
import sonia.scm.repository.RepositoryHookType;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.Tag;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookFeature;
import sonia.scm.repository.api.HookTagProvider;
import sonia.scm.store.InMemoryConfigurationStoreFactory;
import sonia.scm.web.security.AdministrationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware(value = "trillian", permissions = "repository:read:42")
class MirrorReadOnlyCheckConfigurationTest {

  @Mock
  private HookContext hookContext;
  @Mock
  private HookBranchProvider branchProvider;
  @Mock
  private HookTagProvider tagProvider;

  private final Repository repository = RepositoryTestData.createHeartOfGold();
  private final InMemoryConfigurationStoreFactory storeFactory = new InMemoryConfigurationStoreFactory();
  private MirrorConfigurationStore configurationStore;
  private MirrorReadOnlyCheck check;
  private PreReceiveRepositoryHookEvent event;

  @BeforeEach
  void setUp() {
    repository.setId("42");
    configurationStore = new MirrorConfigurationStore(
      storeFactory, mock(MirrorScheduler.class), mock(RepositoryManager.class), mock(AdministrationContext.class)
    );
    check = new MirrorReadOnlyCheck(configurationStore);
    event = new PreReceiveRepositoryHookEvent(
      new RepositoryHookEvent(hookContext, repository, RepositoryHookType.PRE_RECEIVE)
    );
  }

  @ParameterizedTest
  @CsvSource({
    "false, false, false, false", "false, false, true, false",
    "true, false, false, false", "true, false, true, false",
    "true, true, false, false", "true, true, true, false",
    "false, true, false, false", "false, true, true, false",
    "false, false, false, true", "false, false, true, true",
    "true, false, false, true", "true, false, true, true",
    "true, true, false, true", "true, true, true, true",
    "false, true, false, true", "false, true, true, true"
  })
  void shouldProtectRefsUsingEffectivePatterns(boolean disableLocalFilters, boolean overwriteGlobalFilters, boolean deletion, boolean tag) {
    GlobalMirrorConfiguration globalConfiguration = new GlobalMirrorConfiguration();
    globalConfiguration.setDisableRepositoryFilterOverwrite(disableLocalFilters);
    globalConfiguration.setBranchesAndTagsPatterns(List.of("main", "release/*"));
    configurationStore.setGlobalConfiguration(globalConfiguration);

    MirrorConfiguration localConfiguration = new MirrorConfiguration();
    localConfiguration.setAllBranchesProtected(false);
    localConfiguration.setOverwriteGlobalConfiguration(overwriteGlobalFilters);
    localConfiguration.setBranchesAndTagsPatterns(List.of("feature/*"));
    storeFactory.get("mirror", repository.getId()).set(localConfiguration);

    boolean usesGlobalFilters = disableLocalFilters || !overwriteGlobalFilters;
    String protectedBranch = usesGlobalFilters ? "release/1.0" : "feature/local";
    String writableBranch = usesGlobalFilters ? "feature/local" : "release/1.0";

    setChangedRef(protectedBranch, deletion, tag);
    assertThatExceptionOfType(ReadOnlyException.class).isThrownBy(() -> check.checkForReadOnlyRefs(event));

    setChangedRef(writableBranch, deletion, tag);
    assertThatCode(() -> check.checkForReadOnlyRefs(event)).doesNotThrowAnyException();
  }

  @Test
  void shouldProtectAllBranchesWhenEffectivePatternsAreEmpty() {
    MirrorConfiguration localConfiguration = new MirrorConfiguration();
    localConfiguration.setAllBranchesProtected(false);
    localConfiguration.setBranchesAndTagsPatterns(List.of("feature/*"));
    storeFactory.get("mirror", repository.getId()).set(localConfiguration);
    when(hookContext.isFeatureSupported(HookFeature.BRANCH_PROVIDER)).thenReturn(true);

    assertThatExceptionOfType(ReadOnlyException.class).isThrownBy(() -> check.checkForReadOnlyRefs(event));
  }

  @Test
  void shouldProtectAllTagsWhenEffectivePatternsAreEmpty() {
    MirrorConfiguration localConfiguration = new MirrorConfiguration();
    localConfiguration.setAllBranchesProtected(false);
    localConfiguration.setBranchesAndTagsPatterns(List.of("feature/*"));
    storeFactory.get("mirror", repository.getId()).set(localConfiguration);
    when(hookContext.isFeatureSupported(HookFeature.BRANCH_PROVIDER)).thenReturn(false);
    when(hookContext.isFeatureSupported(HookFeature.TAG_PROVIDER)).thenReturn(true);

    assertThatExceptionOfType(ReadOnlyException.class).isThrownBy(() -> check.checkForReadOnlyRefs(event));
  }

  @Test
  void shouldProtectModifiedTags() {
    GlobalMirrorConfiguration globalConfiguration = new GlobalMirrorConfiguration();
    globalConfiguration.setBranchesAndTagsPatterns(List.of("release/*"));
    configurationStore.setGlobalConfiguration(globalConfiguration);
    MirrorConfiguration localConfiguration = new MirrorConfiguration();
    localConfiguration.setAllBranchesProtected(false);
    storeFactory.get("mirror", repository.getId()).set(localConfiguration);
    setChangedRef("release/1.0", false, true);
    when(tagProvider.getDeletedTags()).thenReturn(List.of(new Tag("release/1.0", "old-revision")));

    assertThatExceptionOfType(ReadOnlyException.class).isThrownBy(() -> check.checkForReadOnlyRefs(event));
  }

  @Test
  @SubjectAware("_scmsystem")
  void shouldAllowSynchronizationBySystemUser() {
    storeFactory.get("mirror", repository.getId()).set(new MirrorConfiguration());

    assertThatCode(() -> check.checkForReadOnlyRefs(event)).doesNotThrowAnyException();
  }

  @Test
  void shouldAllowHooksWithoutBranchOrTagProvider() {
    storeFactory.get("mirror", repository.getId()).set(new MirrorConfiguration());

    assertThatCode(() -> check.checkForReadOnlyRefs(event)).doesNotThrowAnyException();
  }

  @Test
  void shouldNotProtectNormalRepositoriesWithGlobalPatterns() {
    GlobalMirrorConfiguration globalConfiguration = new GlobalMirrorConfiguration();
    globalConfiguration.setBranchesAndTagsPatterns(List.of("main"));
    configurationStore.setGlobalConfiguration(globalConfiguration);

    assertThatCode(() -> check.checkForReadOnlyRefs(event)).doesNotThrowAnyException();
  }

  private void setChangedRef(String name, boolean deletion, boolean tag) {
    if (tag) {
      when(hookContext.isFeatureSupported(HookFeature.BRANCH_PROVIDER)).thenReturn(false);
      when(hookContext.isFeatureSupported(HookFeature.TAG_PROVIDER)).thenReturn(true);
      when(hookContext.getTagProvider()).thenReturn(tagProvider);
      if (deletion) {
        when(tagProvider.getDeletedTags()).thenReturn(List.of(new Tag(name, "revision")));
      } else {
        when(tagProvider.getCreatedTags()).thenReturn(List.of(new Tag(name, "revision")));
      }
    } else {
      setChangedBranch(name, deletion);
    }
  }

  private void setChangedBranch(String branch, boolean deletion) {
    when(hookContext.isFeatureSupported(HookFeature.BRANCH_PROVIDER)).thenReturn(true);
    when(hookContext.getBranchProvider()).thenReturn(branchProvider);
    if (deletion) {
      when(branchProvider.getDeletedOrClosed()).thenReturn(List.of(branch));
    } else {
      when(branchProvider.getCreatedOrModified()).thenReturn(List.of(branch));
    }
  }
}
