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

  private MirrorService service;

  private final Repository repository = createHeartOfGold("git");

  @BeforeEach
  void createService() {
    service = new MirrorService(manager, configurationStore, mirrorWorker);
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

