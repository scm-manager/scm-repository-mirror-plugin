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

import sonia.scm.BadRequestException;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import javax.inject.Inject;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

class MirrorConfigurationService {

  private static final String STORE_NAME = "mirror";

  private final RepositoryManager repositoryManager;
  private final ConfigurationStoreFactory storeFactory;

  @Inject
  MirrorConfigurationService(RepositoryManager repositoryManager, ConfigurationStoreFactory storeFactory) {
    this.repositoryManager = repositoryManager;
    this.storeFactory = storeFactory;
  }

  MirrorConfiguration getConfiguration(String namespace, String name) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    Repository repository = repositoryManager.get(namespaceAndName);
    if (repository == null) {
      throw new NotFoundException(Repository.class, namespaceAndName.toString());
    }
    return getConfiguration(repository);
  }

  MirrorConfiguration getConfiguration(Repository repository) {
    return createConfigurationStore(repository)
      .getOptional()
      .orElseThrow(() -> new NotConfiguredForMirrorException(repository));
  }

  void setConfiguration(Repository repository, MirrorConfiguration configuration) {
    createConfigurationStore(repository).set(configuration);
  }

  boolean hasConfiguration(Repository repository) {
    return createConfigurationStore(repository)
      .getOptional()
      .isPresent();
  }

  private ConfigurationStore<MirrorConfiguration> createConfigurationStore(Repository repository) {
    return storeFactory
      .withType(MirrorConfiguration.class)
      .withName(STORE_NAME)
      .forRepository(repository).build();
  }

  static class NotConfiguredForMirrorException extends BadRequestException {

    public NotConfiguredForMirrorException(Repository repository) {
      super(entity(repository).build(), "repository not configured as a mirror");
    }

    @Override
    public String getCode() {
      return "9vSXNxLT01";
    }
  }
}
