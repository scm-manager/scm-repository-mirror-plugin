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

import sonia.scm.Initable;
import sonia.scm.SCMContextProvider;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.web.security.AdministrationContext;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class MirrorScheduler implements Initable {

  private final MirrorWorker worker;
  private final MirrorConfigurationStore configurationStore;
  private final RepositoryManager repositoryManager;
  private final AdministrationContext administrationContext;

  private final Map<String, MirrorWorker.CancelableSchedule> schedules = new ConcurrentHashMap<>();

  @Inject
  MirrorScheduler(MirrorWorker worker,
                  MirrorConfigurationStore configurationStore,
                  RepositoryManager repositoryManager,
                  AdministrationContext administrationContext) {
    this.worker = worker;
    this.configurationStore = configurationStore;
    this.repositoryManager = repositoryManager;
    this.administrationContext = administrationContext;
  }

  @Override
  public void init(SCMContextProvider context) {
    administrationContext.runAsAdmin(() -> repositoryManager.getAll().forEach(this::init));
  }

  private void init(Repository repository) {
    if (configurationStore.hasConfiguration(repository)) {
      schedules.put(repository.getId(), worker.scheduleUpdate(repository, 5));
    }
  }

  public void schedule(Repository repository) {
      schedules.getOrDefault(repository.getId(), () -> {}).cancel();
      schedules.put(repository.getId(), worker.scheduleUpdate(repository, 0));
  }
}
