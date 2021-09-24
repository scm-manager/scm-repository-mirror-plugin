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

import org.apache.shiro.authz.AuthorizationException;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryHandler;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryPermission;
import sonia.scm.repository.RepositoryType;
import sonia.scm.repository.api.Command;

import java.util.Collection;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith({ShiroExtension.class, MockitoExtension.class})
@SubjectAware(
  value = "trillian"
)
class MirrorServiceTest {

  @Mock
  private RepositoryManager manager;
  @Mock
  private MirrorConfigurationStore configurationStore;
  @Mock
  private MirrorWorker mirrorWorker;
  @Mock
  private ScmEventBus eventBus;

  private MirrorService service;

  private final Repository repository = createHeartOfGold("git");

  @BeforeEach
  void createService() {
    service = new MirrorService(manager, configurationStore, mirrorWorker, eventBus);
  }

  @Test
  void shouldFailToCreateWithoutPermission() {
    MirrorConfiguration configuration = createConfiguration();

    assertThrows(AuthorizationException.class, () -> service.createMirror(configuration, repository));
  }

  @Test
  void shouldFailToSyncMirrorWithoutPermission() {
    assertThrows(AuthorizationException.class, () -> service.updateMirror(repository));
  }

  @Test
  @SubjectAware(
    permissions = "repository:mirror:42"
  )
  void shouldCallUpdateCommand() {
    repository.setId("42");
    MirrorConfiguration configuration = mock(MirrorConfiguration.class);
    when(configurationStore.getApplicableConfiguration(repository)).thenReturn(of(configuration));

    service.updateMirror(repository);

    verify(configurationStore).getApplicableConfiguration(repository);
    verify(mirrorWorker).startUpdate(repository, configuration);
  }

  @Test
  @SubjectAware(
    permissions = "repository:mirror:42"
  )
  void shouldUnmirrorRepository() {
    service.unmirror(repository);

    verify(configurationStore).setConfiguration(repository, null);
    verify(eventBus).post(any(RepositoryUnmirrorEvent.class));
  }

  @Test
  @SubjectAware(
    permissions = "repository:mirror:42"
  )
  void shouldFailUpdateCommandWhenNotConfiguredAsMirror() {
    repository.setId("42");
    when(configurationStore.getApplicableConfiguration(repository)).thenReturn(empty());

    assertThrows(NotConfiguredForMirrorException.class, () -> service.updateMirror(repository));
  }

  @Nested
  @SubjectAware(
    permissions = "repository:create"
  )
  class WithCreatePermission {

    @Mock
    RepositoryHandler handler;

    @BeforeEach
    void initManagerAndService() {
      when(manager.getHandler("git")).thenReturn(handler);
      when(handler.getType()).thenReturn(new RepositoryType("git", "Git", emptySet()));
    }

    @Test
    void shouldFailForUnsupportedType() {
      MirrorConfiguration configuration = createConfiguration();

      assertThrows(ScmConstraintViolationException.class, () -> service.createMirror(configuration, repository));
    }

    @Nested
    class WithSupportingType {

      @BeforeEach
      @SuppressWarnings("unchecked")
      void supportMirrorCommand() {
        when(handler.getType()).thenReturn(new RepositoryType("git", "Git", singleton(Command.MIRROR)));
        when(manager.create(any(), any())).thenAnswer(
          invocation -> {
            invocation.getArgument(1, Consumer.class).accept(invocation.getArgument(0));
            return invocation.getArgument(0);
          }
        );
      }

      @Test
      void shouldSetOwnerPermissionForCurrentUser() {
        MirrorConfiguration configuration = createConfiguration();
        when(configurationStore.getApplicableConfiguration(repository)).thenReturn(of(configuration));

        Repository createdRepository = service.createMirror(configuration, repository);

        Collection<RepositoryPermission> permissions = createdRepository.getPermissions();
        assertThat(permissions)
          .hasSize(1)
          .extracting("role")
          .containsExactly("OWNER");
        assertThat(permissions)
          .extracting("name")
          .containsExactly("trillian");
      }

      @Test
      void shouldCallMirrorCommand() {
        MirrorConfiguration configuration = createConfiguration();
        when(configurationStore.getApplicableConfiguration(repository)).thenReturn(of(configuration));

        service.createMirror(configuration, repository);

        verify(configurationStore).setConfiguration(repository, configuration);
        verify(mirrorWorker).startInitialSync(repository, configuration);
        verify(configurationStore).getApplicableConfiguration(repository);
      }
    }
  }

  private MirrorConfiguration createConfiguration() {
    MirrorConfiguration request = new MirrorConfiguration();
    request.setUrl("http://hog/");
    return request;
  }
}

