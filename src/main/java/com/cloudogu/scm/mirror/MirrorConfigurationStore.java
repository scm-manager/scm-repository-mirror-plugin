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
import sonia.scm.Initable;
import sonia.scm.SCMContextProvider;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.web.security.AdministrationContext;

import javax.inject.Inject;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class MirrorConfigurationStore implements Initable {

  private static final String STORE_NAME = "mirror";

  private final ConfigurationStoreFactory storeFactory;
  private final MirrorScheduler scheduler;
  private final RepositoryManager repositoryManager;
  private final AdministrationContext administrationContext;

  @Inject
  MirrorConfigurationStore(ConfigurationStoreFactory storeFactory, MirrorScheduler scheduler, RepositoryManager repositoryManager, AdministrationContext administrationContext) {
    this.storeFactory = storeFactory;
    this.scheduler = scheduler;
    this.repositoryManager = repositoryManager;
    this.administrationContext = administrationContext;
  }

  public MirrorConfiguration getConfiguration(Repository repository) {
    MirrorPermissions.checkMirrorPermission(repository);
    return createConfigurationStore(repository)
      .getOptional()
      .orElseThrow(() -> new NotConfiguredForMirrorException(repository));
  }

  public void setConfiguration(Repository repository, MirrorConfiguration configuration) {
    MirrorPermissions.checkMirrorPermission(repository);
    createConfigurationStore(repository).set(configuration);
    scheduler.schedule(repository, configuration);
  }

  public boolean hasConfiguration(Repository repository) {
    return hasConfiguration(repository.getId());
  }

  public boolean hasConfiguration(String repositoryId) {
    return createConfigurationStore(repositoryId)
      .getOptional()
      .isPresent();
  }

  @Override
  public void init(SCMContextProvider context) {
    administrationContext.runAsAdmin(() -> repositoryManager.getAll().forEach(this::init));
  }

  private void init(Repository repository) {
    if (hasConfiguration(repository)) {
      MirrorConfiguration configuration = getConfiguration(repository);
      scheduler.scheduleNow(repository, configuration);
    }
  }

  private ConfigurationStore<MirrorConfiguration> createConfigurationStore(Repository repository) {
    return createConfigurationStore(repository.getId());
  }

  private ConfigurationStore<MirrorConfiguration> createConfigurationStore(String repositoryId) {
    return storeFactory
      .withType(MirrorConfiguration.class)
      .withName(STORE_NAME)
      .forRepository(repositoryId).build();
  }

  @SuppressWarnings("java:S110") // We accept deep hierarchy for exceptions
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
