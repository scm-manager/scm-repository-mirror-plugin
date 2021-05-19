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

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.MeterRegistry;
import sonia.scm.metrics.Metrics;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Credential;
import sonia.scm.repository.api.MirrorCommandBuilder;
import sonia.scm.repository.api.MirrorCommandResult;
import sonia.scm.repository.api.Pkcs12ClientCertificateCredential;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

class MirrorWorker {

  private final RepositoryServiceFactory repositoryServiceFactory;
  private final ScheduledExecutorService executor;
  private final MirrorStatusStore statusStore;
  private final MirrorConfigurationStore configurationStore;
  private final MirrorReadOnlyCheck readOnlyCheck;

  @Inject
  MirrorWorker(RepositoryServiceFactory repositoryServiceFactory,
               MeterRegistry registry,
               MirrorStatusStore statusStore,
               MirrorConfigurationStore configurationStore,
               MirrorReadOnlyCheck readOnlyCheck) {
    this(repositoryServiceFactory, registry, Executors.newScheduledThreadPool(4), statusStore, configurationStore, readOnlyCheck);
  }

  @VisibleForTesting
  MirrorWorker(RepositoryServiceFactory repositoryServiceFactory,
               MeterRegistry registry,
               ScheduledExecutorService executor,
               MirrorStatusStore statusStore,
               MirrorConfigurationStore configurationStore,
               MirrorReadOnlyCheck readOnlyCheck) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.executor = executor;
    this.statusStore = statusStore;
    this.configurationStore = configurationStore;
    this.readOnlyCheck = readOnlyCheck;
    Metrics.executor(registry, executor, "mirror", "fixed");
  }

  void startInitialSync(Repository repository) {
    readOnlyCheck.exceptedFromReadOnly(() -> statusStore.setStatus(repository, MirrorStatus.initialStatus()));
    startAsynchronously(repository, MirrorCommandBuilder::initialCall);
  }

  void startUpdate(Repository repository) {
    startAsynchronously(repository, MirrorCommandBuilder::update);
  }

  CancelableSchedule scheduleUpdate(Repository repository, int delay) {
    ScheduledFuture<?> scheduledFuture =
      executor.scheduleAtFixedRate(
        () -> readOnlyCheck.exceptedFromReadOnly(() -> startSynchronously(repository, MirrorCommandBuilder::update)),
        delay,
        10,
        TimeUnit.MINUTES);
    return () -> scheduledFuture.cancel(false);
  }

  private void startAsynchronously(Repository repository, Function<MirrorCommandBuilder, MirrorCommandResult> callback) {
    // TODO Shiro context should either be set to admin or inherit the current subject
    executor.submit(
      () -> readOnlyCheck.exceptedFromReadOnly(() -> startSynchronously(repository, callback))
    );
  }

  // TODO event listener when repository is deleted -> remove from schedules

  private void startSynchronously(Repository repository, Function<MirrorCommandBuilder, MirrorCommandResult> callback) {
    // TODO Abort if already running
    Instant startTime = Instant.now();
    MirrorConfiguration configuration = configurationStore.getConfiguration(repository);
    try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
      MirrorCommandBuilder mirrorCommand = repositoryService.getMirrorCommand().setSourceUrl(configuration.getUrl());
      setCredentials(configuration, mirrorCommand);
      MirrorCommandResult commandResult = callback.apply(mirrorCommand);
      if (commandResult.isSuccess()) {
        statusStore.setStatus(repository, MirrorStatus.success(startTime));
      } else {
        statusStore.setStatus(repository, MirrorStatus.failed(startTime));
      }
    }
  }

  private void setCredentials(MirrorConfiguration configuration, MirrorCommandBuilder mirrorCommand) {
    Collection<Credential> credentials = new ArrayList<>();
    if (configuration.getUsernamePasswordCredential() != null) {
      credentials.add(configuration.getUsernamePasswordCredential());
    }
    if (configuration.getCertificateCredential() != null) {
      credentials.add(new Pkcs12ClientCertificateCredential(configuration.getCertificateCredential().getCertificate(), configuration.getCertificateCredential().getPassword().toCharArray()));
    }
    mirrorCommand.setCredentials(credentials);
  }

  interface CancelableSchedule {
    void cancel();
  }
}
