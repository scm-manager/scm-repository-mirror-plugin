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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.InMemoryConfigurationStoreFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MirrorConfigurationServiceTest {

  public static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock
  private RepositoryManager repositoryManager;

  private MirrorConfigurationService service;
  private InMemoryConfigurationStoreFactory storeFactory;

  @BeforeEach
  void createService() {
    storeFactory = new InMemoryConfigurationStoreFactory();
    service = new MirrorConfigurationService(repositoryManager, storeFactory);
  }

  @Test
  void shouldThrowExceptionForUnknownRepository() {
    Assertions.assertThrows(
      MirrorConfigurationService.NotConfiguredForMirrorException.class,
      () -> service.getConfiguration(REPOSITORY)
    );
  }

  @Test
  void shouldReturnExistingConfiguration() {
    MirrorConfiguration existingConfiguration = new MirrorConfiguration();
    storeFactory.get("mirror", REPOSITORY.getId()).set(existingConfiguration);

    MirrorConfiguration configuration = service.getConfiguration(REPOSITORY);

    assertThat(configuration).isSameAs(existingConfiguration);
  }

  @Test
  void shouldStoreConfiguration() {
    Repository repository = RepositoryTestData.createHeartOfGold();
    MirrorConfiguration newConfiguration = new MirrorConfiguration();

    service.setConfiguration(repository, newConfiguration);

    Object configurationFromStore = storeFactory.get("mirror", repository.getId()).get();
    assertThat(configurationFromStore).isSameAs(newConfiguration);
  }
}
