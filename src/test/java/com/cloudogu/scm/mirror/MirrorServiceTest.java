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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.shiro.authz.AuthorizationException;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryHandler;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryPermission;
import sonia.scm.repository.RepositoryType;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.MirrorCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.spi.MirrorCommand;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ShiroExtension.class)
class MirrorServiceTest {

  @Mock
  private RepositoryManager manager;
  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;
  @Mock
  private MirrorConfigurationStore configurationService;

  private MirrorWorker mirrorWorker;

  private MirrorService service;

  private final Repository repository = createHeartOfGold("git");

  @BeforeEach
  void createService() {
    ExecutorService executor = mock(ExecutorService.class);
    lenient().doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    }).when(executor).submit(any(Runnable.class));
    mirrorWorker = new MirrorWorker(repositoryServiceFactory, new SimpleMeterRegistry(), executor);
    service = new MirrorService(manager, configurationService, mirrorWorker);
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

  @Nested
  @SubjectAware(
    value = "trillian",
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

      @Mock
      private RepositoryService repositoryService;
      @Mock
      private MirrorCommand mirrorCommand;
      @InjectMocks
      private MirrorCommandBuilder mirrorCommandBuilder;

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
        when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
        when(repositoryService.getMirrorCommand()).thenReturn(mirrorCommandBuilder);
      }

      @Test
      void shouldSetOwnerPermissionForCurrentUser() {
        MirrorConfiguration configuration = createConfiguration();

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

        service.createMirror(configuration, repository);

        verify(mirrorCommand).mirror(argThat(mirrorCommandRequest -> {
          assertThat(mirrorCommandRequest.getSourceUrl()).isEqualTo("http://hog/");
          return true;
        }));
      }

      @Test
      void shouldSetCredentialsForMirrorCall() {
        MirrorConfiguration configuration = createConfiguration();
        MirrorConfiguration.UsernamePasswordCredential usernamePasswordCredential = new MirrorConfiguration.UsernamePasswordCredential();
        usernamePasswordCredential.setUsername("dent");
        usernamePasswordCredential.setPassword("hg2g");
        configuration.setUsernamePasswordCredential(usernamePasswordCredential);

        service.createMirror(configuration, repository);

        verify(mirrorCommand).mirror(argThat(mirrorCommandRequest -> {
          assertThat(mirrorCommandRequest.getCredentials()).hasSize(1);
          assertThat(mirrorCommandRequest.getCredentials()).extracting("username").containsExactly("dent");
          assertThat(mirrorCommandRequest.getCredentials()).extracting("password").containsExactly("hg2g");
          return true;
        }));
      }
    }
  }

  private MirrorConfiguration createConfiguration() {
    MirrorConfiguration request = new MirrorConfiguration();
    request.setUrl("http://hog/");
    return request;
  }
}

