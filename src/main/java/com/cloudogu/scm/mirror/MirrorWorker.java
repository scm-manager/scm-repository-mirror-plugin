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
import sonia.scm.notifications.Notification;
import sonia.scm.notifications.NotificationSender;
import sonia.scm.notifications.Type;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

class MirrorWorker {

  private final RepositoryServiceFactory repositoryServiceFactory;
  private final ScheduledExecutorService executor;
  private final MirrorStatusStore statusStore;
  private final PrivilegedMirrorRunner privilegedMirrorRunner;
  private final NotificationSender notificationSender;

  private final Set<String> runningSynchronizations = Collections.synchronizedSet(new HashSet<>());

  @Inject
  MirrorWorker(RepositoryServiceFactory repositoryServiceFactory,
               MeterRegistry registry,
               MirrorStatusStore statusStore,
               PrivilegedMirrorRunner privilegedMirrorRunner,
               NotificationSender notificationSender) {
    this(repositoryServiceFactory, registry, Executors.newScheduledThreadPool(4), statusStore, privilegedMirrorRunner, notificationSender);
  }

  @VisibleForTesting
  MirrorWorker(RepositoryServiceFactory repositoryServiceFactory,
               MeterRegistry registry,
               ScheduledExecutorService executor,
               MirrorStatusStore statusStore,
               PrivilegedMirrorRunner privilegedMirrorRunner,
               NotificationSender notificationSender) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.executor = executor;
    this.statusStore = statusStore;
    this.privilegedMirrorRunner = privilegedMirrorRunner;
    this.notificationSender = notificationSender;
    Metrics.executor(registry, executor, "mirror", "fixed");
  }

  void startInitialSync(Repository repository, MirrorConfiguration configuration) {
    privilegedMirrorRunner.exceptedFromReadOnly(() -> statusStore.setStatus(repository, MirrorStatus.initialStatus()));
    startAsynchronously(repository, configuration, MirrorCommandBuilder::initialCall);
  }

  void startUpdate(Repository repository, MirrorConfiguration configuration) {
    startAsynchronously(repository, configuration, MirrorCommandBuilder::update);
  }

  CancelableSchedule scheduleUpdate(Repository repository, MirrorConfiguration configuration, int delay) {
    ScheduledFuture<?> scheduledFuture =
      executor.scheduleAtFixedRate(
        () -> privilegedMirrorRunner.exceptedFromReadOnly(() -> startSynchronously(repository, configuration, MirrorCommandBuilder::update)),
        delay,
        configuration.getSynchronizationPeriod(),
        TimeUnit.MINUTES);
    return () -> scheduledFuture.cancel(false);
  }

  private void startAsynchronously(Repository repository, MirrorConfiguration configuration, Function<MirrorCommandBuilder, MirrorCommandResult> callback) {
    // TODO Shiro context should either be set to admin or inherit the current subject
    executor.submit(
      () -> privilegedMirrorRunner.exceptedFromReadOnly(() -> startSynchronously(repository, configuration, callback))
    );
  }

  // TODO event listener when repository is deleted -> remove from schedules

  private void startSynchronously(Repository repository, MirrorConfiguration configuration, Function<MirrorCommandBuilder, MirrorCommandResult> callback) {
    if (runningSynchronizations.add(repository.getId())) {
      Instant startTime = Instant.now();
      try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
        MirrorCommandBuilder mirrorCommand = repositoryService.getMirrorCommand().setSourceUrl(configuration.getUrl());
        setCredentials(configuration, mirrorCommand);
        MirrorCommandResult commandResult = callback.apply(mirrorCommand);
        if (commandResult.isSuccess()) {
          statusStore.setStatus(repository, MirrorStatus.success(startTime));
        } else {
          shouldSendFailureNotification(repository, configuration);
          statusStore.setStatus(repository, MirrorStatus.failed(startTime));
        }
      } finally {
        runningSynchronizations.remove(repository.getId());
      }
    }
  }

  private void shouldSendFailureNotification(Repository repository, MirrorConfiguration configuration) {
    if (statusStore.getStatus(repository).getResult() == MirrorStatus.Result.SUCCESS) {
      for (String user : configuration.getManagingUsers()) {
        notificationSender.send(new Notification(Type.ERROR, "/repo/" + repository.getNamespaceAndName() + "/settings/general", "mirrorFailed"), user);
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
