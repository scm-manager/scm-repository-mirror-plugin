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

import org.apache.shiro.authz.UnauthorizedException;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.InMemoryConfigurationStoreFactory;
import sonia.scm.web.security.AdministrationContext;
import sonia.scm.web.security.PrivilegedAction;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ShiroExtension.class)
@ExtendWith(MockitoExtension.class)
@SubjectAware(
  value = "trillian"
)
class MirrorConfigurationStoreTest {

  public static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock
  private MirrorScheduler scheduler;

  @Mock
  private RepositoryManager repositoryManager;
  @Mock
  private AdministrationContext administrationContext;

  private MirrorConfigurationStore store;
  private InMemoryConfigurationStoreFactory storeFactory;

  @BeforeEach
  void createService() {
    storeFactory = new InMemoryConfigurationStoreFactory();
    store = new MirrorConfigurationStore(storeFactory, scheduler, repositoryManager, administrationContext);
  }

  @BeforeAll
  static void setRepositoryId() {
    REPOSITORY.setId("42");
  }

  @Test
  void shouldFailToGetConfigurationWithoutPermission() {
    assertThrows(UnauthorizedException.class,
      () -> store.getConfiguration(REPOSITORY));
  }

  @Test
  void shouldFailToSetConfigurationWithoutPermission() {
    MirrorConfiguration configuration = new MirrorConfiguration();
    assertThrows(UnauthorizedException.class,
      () -> store.setConfiguration(REPOSITORY, configuration));
  }

  @Nested
  @SubjectAware(
    value = "trillian",
    permissions = "repository:configureMirror:42"
  )
  class WithPermission {

    @Test
    void shouldThrowExceptionForUnknownRepository() {
      assertThrows(
        MirrorConfigurationStore.NotConfiguredForMirrorException.class,
        () -> store.getConfiguration(REPOSITORY)
      );
    }

    @Test
    void shouldGetConfiguration() {
      MirrorConfiguration existingConfiguration = new MirrorConfiguration();
      mockExistingConfiguration(existingConfiguration);

      MirrorConfiguration configuration = store.getConfiguration(REPOSITORY);

      assertThat(configuration).isSameAs(existingConfiguration);
    }

    @Test
    void shouldSetConfiguration() {
      MirrorConfiguration newConfiguration = new MirrorConfiguration();

      store.setConfiguration(REPOSITORY, newConfiguration);

      Object configurationFromStore = storeFactory.get("mirror", REPOSITORY.getId()).get();
      assertThat(configurationFromStore).isSameAs(newConfiguration);
      verify(scheduler).schedule(REPOSITORY, newConfiguration);
    }
  }

  @Test
  @SubjectAware(
    value = "trillian",
    permissions = "*" // admin context
  )
  void shouldScheduleRepositoriesAtStartup() {
    doAnswer(
      invocation -> {
        invocation.getArgument(0, PrivilegedAction.class).run();
        return null;
      }
    ).when(administrationContext).runAsAdmin(any(PrivilegedAction.class));
    Repository normalRepository = RepositoryTestData.createHeartOfGold();
    Repository mirrorRepository = RepositoryTestData.create42Puzzle();
    when(repositoryManager.getAll())
      .thenReturn(asList(normalRepository, mirrorRepository));
    MirrorConfiguration configuration = mock(MirrorConfiguration.class);
    mockExistingConfiguration(configuration, mirrorRepository);

    store.init(null);

    verify(scheduler).scheduleNow(mirrorRepository, configuration);
    verify(scheduler, never()).scheduleNow(eq(normalRepository), any());
  }

  @SuppressWarnings("unchecked")
  private void mockExistingConfiguration(MirrorConfiguration existingConfiguration) {
    mockExistingConfiguration(existingConfiguration, REPOSITORY);
  }

  private void mockExistingConfiguration(MirrorConfiguration existingConfiguration, Repository repository) {
    storeFactory.get("mirror", repository.getId()).set(existingConfiguration);
  }
}
