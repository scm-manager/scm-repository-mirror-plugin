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

import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.ReadOnlyException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryHookEvent;
import sonia.scm.repository.RepositoryHookType;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookFeature;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ShiroExtension.class)
class MirrorReadOnlyCheckTest {

  @Mock
  private MirrorConfigurationStore configurationStore;

  @InjectMocks
  private MirrorReadOnlyCheck check;

  @Test
  void shouldNotBeReadOnlyForNormalRepository() {
    boolean readOnly = check.isReadOnly("other");

    assertThat(readOnly).isFalse();
  }

  @Test
  void shouldBeReadOnlyForRepositoryWithConfiguration() {
    when(configurationStore.isReadOnly("mirror")).thenReturn(true);

    boolean readOnly = check.isReadOnly("push", "mirror");

    assertThat(readOnly).isTrue();
  }

  @Test
  void shouldNotBeReadOnlyForRepositoryWithConfigurationButWithException() {
    lenient().when(configurationStore.isReadOnly("mirror")).thenReturn(true);

    boolean readOnly = check.isReadOnly("push");

    assertThat(readOnly).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"delete", "permissionWrite", "pull", "modify"})
  void shouldHaveNormalPermissionsEvenWithConfiguration(String permission) {
    lenient().when(configurationStore.isReadOnly("mirror")).thenReturn(true);

    boolean readOnly = check.isReadOnly(permission, "mirror");

    assertThat(readOnly).isFalse();
  }

  @Nested
  @SubjectAware("trillian")
  class ChecksForEvents {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HookContext hookContext;
    private final Repository repository = RepositoryTestData.createHeartOfGold();
    private PreReceiveRepositoryHookEvent event;

    @BeforeEach
    void initBranchProvider() {
      event = new PreReceiveRepositoryHookEvent(
        new RepositoryHookEvent(hookContext, repository, RepositoryHookType.PRE_RECEIVE)
      );
      lenient().when(hookContext.isFeatureSupported(HookFeature.BRANCH_PROVIDER))
        .thenReturn(true);
    }

    @Test
    void shouldDoNothingWithoutConfiguration() {
      when(configurationStore.getConfiguration(repository)).thenReturn(empty());
      check.checkForReadOnlyBranch(event);

      // no exception should be thrown
    }

    @Test
    void shouldThrowExceptionWhenNoPatternsAreConfigured() {
      MirrorConfiguration configuration = new MirrorConfiguration();
      configuration.setBranchesAndTagsPatterns(emptyList());
      when(configurationStore.getConfiguration(repository)).thenReturn(of(configuration));

      Assert.assertThrows(
        ReadOnlyException.class,
        () -> check.checkForReadOnlyBranch(event)
      );
    }

    @Test
    void shouldThrowExceptionWhenModifiedBranchMatchesPatternDirectly() {
      MirrorConfiguration configuration = new MirrorConfiguration();
      configuration.setBranchesAndTagsPatterns(List.of("main"));
      when(hookContext.getBranchProvider().getCreatedOrModified()).thenReturn(List.of("main"));
      when(configurationStore.getConfiguration(repository)).thenReturn(of(configuration));

      Assert.assertThrows(
        ReadOnlyException.class,
        () -> check.checkForReadOnlyBranch(event)
      );
    }

    @Test
    void shouldThrowExceptionWhenDeletedBranchMatchesPatternDirectly() {
      MirrorConfiguration configuration = new MirrorConfiguration();
      configuration.setBranchesAndTagsPatterns(List.of("main"));
      when(hookContext.getBranchProvider().getDeletedOrClosed()).thenReturn(List.of("main"));
      when(configurationStore.getConfiguration(repository)).thenReturn(of(configuration));

      Assert.assertThrows(
        ReadOnlyException.class,
        () -> check.checkForReadOnlyBranch(event)
      );
    }

    @Test
    void shouldThrowExceptionWhenModifiedBranchMatchesPattern() {
      MirrorConfiguration configuration = new MirrorConfiguration();
      configuration.setBranchesAndTagsPatterns(List.of("feature/*"));
      when(hookContext.getBranchProvider().getCreatedOrModified()).thenReturn(List.of("feature/nice"));
      when(configurationStore.getConfiguration(repository)).thenReturn(of(configuration));

      Assert.assertThrows(
        ReadOnlyException.class,
        () -> check.checkForReadOnlyBranch(event)
      );
    }

    @Test
    void shouldNotThrowExceptionWhenModifiedBranchDoesNotMatchPattern() {
      MirrorConfiguration configuration = new MirrorConfiguration();
      configuration.setBranchesAndTagsPatterns(List.of("feature/*"));
      when(hookContext.getBranchProvider().getCreatedOrModified()).thenReturn(List.of("develop"));
      when(configurationStore.getConfiguration(repository)).thenReturn(of(configuration));

      check.checkForReadOnlyBranch(event);

      // no exception should be thrown
    }

    @Test
    @SubjectAware("_scmsystem")
    void shouldNotThrowExceptionModifiedBySystemUser() {
      MirrorConfiguration configuration = new MirrorConfiguration();
      configuration.setBranchesAndTagsPatterns(List.of("main"));
      when(hookContext.getBranchProvider().getCreatedOrModified()).thenReturn(List.of("main"));

      check.checkForReadOnlyBranch(event);

      // no exception should be thrown
    }
  }
}
