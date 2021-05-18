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
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.InMemoryConfigurationStoreFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(ShiroExtension.class)
@SubjectAware(
  value = "trillian"
)
class MirrorConfigurationServiceTest {

  public static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  private MirrorConfigurationService service;
  private InMemoryConfigurationStoreFactory storeFactory;

  @BeforeEach
  void createService() {
    storeFactory = new InMemoryConfigurationStoreFactory();
    service = new MirrorConfigurationService(storeFactory);
  }

  @BeforeAll
  static void setRepositoryId() {
    REPOSITORY.setId("42");
  }

  @Test
  void shouldFailToGetConfigurationWithoutPermission() {
    assertThrows(UnauthorizedException.class,
      () -> service.getConfiguration(REPOSITORY));
  }

  @Test
  void shouldFailToSetConfigurationWithoutPermission() {
    MirrorConfiguration configuration = new MirrorConfiguration();
    assertThrows(UnauthorizedException.class,
      () -> service.setConfiguration(REPOSITORY, configuration));
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
        MirrorConfigurationService.NotConfiguredForMirrorException.class,
        () -> service.getConfiguration(REPOSITORY)
      );
    }

    @Test
    void shouldGetConfiguration() {
      MirrorConfiguration existingConfiguration = new MirrorConfiguration();
      mockExistingConfiguration(existingConfiguration);

      MirrorConfiguration configuration = service.getConfiguration(REPOSITORY);

      assertThat(configuration).isSameAs(existingConfiguration);
    }

    @Test
    void shouldSetConfiguration() {
      MirrorConfiguration newConfiguration = new MirrorConfiguration();

      service.setConfiguration(REPOSITORY, newConfiguration);

      Object configurationFromStore = storeFactory.get("mirror", REPOSITORY.getId()).get();
      assertThat(configurationFromStore).isSameAs(newConfiguration);
    }
  }

  @SuppressWarnings("unchecked")
  private void mockExistingConfiguration(MirrorConfiguration existingConfiguration) {
    storeFactory.get("mirror", REPOSITORY.getId()).set(existingConfiguration);
  }
}
