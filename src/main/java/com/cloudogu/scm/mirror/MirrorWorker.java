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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.event.ScmEventBus;
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

  private static final Logger LOG = LoggerFactory.getLogger(MirrorWorker.class);

  private final RepositoryServiceFactory repositoryServiceFactory;
  private final ScheduledExecutorService executor;
  private final MirrorStatusStore statusStore;
  private final PrivilegedMirrorRunner privilegedMirrorRunner;
  private final NotificationSender notificationSender;
  private final ScmEventBus eventBus;

  private final Set<String> runningSynchronizations = Collections.synchronizedSet(new HashSet<>());

  @Inject
  MirrorWorker(RepositoryServiceFactory repositoryServiceFactory,
               MeterRegistry registry,
               MirrorStatusStore statusStore,
               PrivilegedMirrorRunner privilegedMirrorRunner,
               NotificationSender notificationSender, ScmEventBus eventBus) {
    this(repositoryServiceFactory, registry, Executors.newScheduledThreadPool(4), statusStore, privilegedMirrorRunner, notificationSender, eventBus);
  }

  @VisibleForTesting
  MirrorWorker(RepositoryServiceFactory repositoryServiceFactory,
               MeterRegistry registry,
               ScheduledExecutorService executor,
               MirrorStatusStore statusStore,
               PrivilegedMirrorRunner privilegedMirrorRunner,
               NotificationSender notificationSender, ScmEventBus eventBus) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.executor = executor;
    this.statusStore = statusStore;
    this.privilegedMirrorRunner = privilegedMirrorRunner;
    this.notificationSender = notificationSender;
    this.eventBus = eventBus;
    Metrics.executor(registry, executor, "mirror", "fixed");
  }

  void startInitialSync(Repository repository, MirrorConfiguration configuration) {
    privilegedMirrorRunner.exceptedFromReadOnly(() -> statusStore.setStatus(repository, MirrorStatus.initialStatus()));
    LOG.info("enqueuing initial sync for mirror {} from url {}", repository, configuration.getUrl());
    startAsynchronously(repository, configuration, MirrorCommandBuilder::initialCall);
  }

  void startUpdate(Repository repository, MirrorConfiguration configuration) {
    LOG.info("enqueuing update for mirror {} from url {}", repository, configuration.getUrl());
    startAsynchronously(repository, configuration, MirrorCommandBuilder::update);
  }

  CancelableSchedule scheduleUpdate(Repository repository, MirrorConfiguration configuration, int delay) {
    LOG.info("scheduling update for mirror {} from url {} in {} minutes every {} minutes", repository, configuration.getUrl(), delay, configuration.getSynchronizationPeriod());
    ScheduledFuture<?> scheduledFuture =
      executor.scheduleAtFixedRate(
        () -> privilegedMirrorRunner.exceptedFromReadOnly(() -> startSynchronously(repository, configuration, MirrorCommandBuilder::update)),
        delay,
        configuration.getSynchronizationPeriod(),
        TimeUnit.MINUTES);
    return () -> {
      LOG.info("cancelling schedule for repository {}", repository);
      scheduledFuture.cancel(false);
    };
  }

  private void startAsynchronously(Repository repository, MirrorConfiguration configuration, Function<MirrorCommandBuilder, MirrorCommandResult> callback) {
    // TODO Shiro context should either be set to admin or inherit the current subject
    executor.submit(
      () -> privilegedMirrorRunner.exceptedFromReadOnly(() -> startSynchronously(repository, configuration, callback))
    );
  }

  private void startSynchronously(Repository repository, MirrorConfiguration configuration, Function<MirrorCommandBuilder, MirrorCommandResult> callback) {
    LOG.debug("running sync for mirror {}", repository);
    if (runningSynchronizations.add(repository.getId())) {
      Instant startTime = Instant.now();
      try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
        LOG.debug("using url {}", configuration.getUrl());
        MirrorCommandBuilder mirrorCommand =
          repositoryService.getMirrorCommand()
            .setSourceUrl(configuration.getUrl());
        setCredentials(configuration, mirrorCommand);
        MirrorCommandResult commandResult = callback.apply(mirrorCommand);
        LOG.debug("got result {} for sync of {}", commandResult.getResult(), repository);
        handleResult(repository, configuration, startTime, commandResult.getResult());
        eventBus.post(new MirrorSyncEvent(repository, commandResult));
      } catch (Exception e) {
        LOG.error("got exception while syncing {}", repository, e);
        handleResult(repository, configuration, startTime, MirrorCommandResult.ResultType.FAILED);
      } finally {
        runningSynchronizations.remove(repository.getId());
      }
    } else {
      LOG.info("skipping sync for mirror {}; other sync still running", repository);
    }
  }

  private void handleResult(Repository repository, MirrorConfiguration configuration, Instant startTime, MirrorCommandResult.ResultType result) {
    MirrorStatus status = MirrorStatus.create(result, startTime);
    statusStore.setStatus(repository, status);
    sendNotificationWhenChanged(repository, configuration, status.getResult());
  }

  private void sendNotificationWhenChanged(Repository repository, MirrorConfiguration configuration, MirrorStatus.Result status) {
    if (getLatestStatus(repository) != status) {
      sendNotifications(repository, configuration, status.getNotificationType(), status.getNotificationKey());
    }
  }

  private void sendNotifications(Repository repository, MirrorConfiguration configuration, Type type, String mirrorUpdatesRejected) {
    for (String user : configuration.getManagingUsers()) {
      notificationSender.send(buildNotification(repository, type, mirrorUpdatesRejected), user);
    }
  }

  private Notification buildNotification(Repository repository, Type error, String mirrorFailed) {
    return new Notification(error, "/repo/" + repository.getNamespaceAndName() + "/settings/general", mirrorFailed);
  }

  private MirrorStatus.Result getLatestStatus(Repository repository) {
    MirrorStatus status = statusStore.getStatus(repository);
    if (status == null) {
      return null;
    } else {
      return status.getResult();
    }
  }

  private void setCredentials(MirrorConfiguration configuration, MirrorCommandBuilder mirrorCommand) {
    Collection<Credential> credentials = new ArrayList<>();
    if (configuration.getUsernamePasswordCredential() != null) {
      LOG.debug("using username/password credential for sync");
      credentials.add(configuration.getUsernamePasswordCredential());
    }
    if (configuration.getCertificateCredential() != null) {
      LOG.debug("using certificate credential for sync");
      credentials.add(new Pkcs12ClientCertificateCredential(configuration.getCertificateCredential().getCertificate(), configuration.getCertificateCredential().getPassword().toCharArray()));
    }
    mirrorCommand.setCredentials(credentials);
  }

  interface CancelableSchedule {
    void cancel();
  }
}
