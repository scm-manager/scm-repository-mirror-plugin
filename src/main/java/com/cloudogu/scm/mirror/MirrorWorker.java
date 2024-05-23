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
import sonia.scm.repository.api.MirrorCommandBuilder;
import sonia.scm.repository.api.MirrorCommandResult;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.cloudogu.scm.mirror.MirrorStatus.Result.FAILED;
import static com.cloudogu.scm.mirror.MirrorStatus.Result.FAILED_UPDATES;
import static com.cloudogu.scm.mirror.MirrorStatus.Result.SUCCESS;
import static java.util.Collections.singletonList;

@Singleton
@SuppressWarnings("UnstableApiUsage")
class MirrorWorker {

  private static final Logger LOG = LoggerFactory.getLogger(MirrorWorker.class);

  private final ScheduledExecutorService executor;
  private final MirrorStatusStore statusStore;
  private final NotificationSender notificationSender;
  private final ScmEventBus eventBus;
  private final MirrorCommandCaller mirrorCommandCaller;
  private final TaskDecoratorFactory taskDecoratorFactory;

  private final Set<String> runningSynchronizations = Collections.synchronizedSet(new HashSet<>());

  @Inject
  MirrorWorker(MeterRegistry registry,
               MirrorStatusStore statusStore,
               NotificationSender notificationSender,
               ScmEventBus eventBus,
               MirrorCommandCaller mirrorCommandCaller,
               TaskDecoratorFactory taskDecoratorFactory) {
    this(registry, Executors.newScheduledThreadPool(4), statusStore, notificationSender, eventBus, mirrorCommandCaller, taskDecoratorFactory);
  }

  @VisibleForTesting
  MirrorWorker(MeterRegistry registry,
               ScheduledExecutorService executor,
               MirrorStatusStore statusStore,
               NotificationSender notificationSender,
               ScmEventBus eventBus,
               MirrorCommandCaller mirrorCommandCaller,
               TaskDecoratorFactory taskDecoratorFactory) {
    this.executor = executor;
    this.statusStore = statusStore;
    this.notificationSender = notificationSender;
    this.eventBus = eventBus;
    this.mirrorCommandCaller = mirrorCommandCaller;
    this.taskDecoratorFactory = taskDecoratorFactory;
    Metrics.executor(registry, executor, "mirror", "fixed");
  }

  void startInitialSync(Repository repository, MirrorConfiguration configuration) {
    taskDecoratorFactory.decorate(() -> statusStore.setStatus(repository, MirrorStatus.initialStatus())).run();
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
        () -> {
          try {
            taskDecoratorFactory.decorate(() -> startSynchronously(repository, configuration, MirrorCommandBuilder::update)).run();
          } catch (Exception e) {
            LOG.error("got exception running scheduled mirror call", e);
          }
        },
        delay,
        configuration.getSynchronizationPeriod(),
        TimeUnit.MINUTES);
    return () -> {
      LOG.info("cancelling schedule for repository {}", repository);
      scheduledFuture.cancel(false);
    };
  }

  private void startAsynchronously(Repository repository, MirrorConfiguration configuration, Function<MirrorCommandBuilder, MirrorCommandResult> callback) {
    executor.submit(
      () -> {
        try {
          taskDecoratorFactory.decorate(() -> startSynchronously(repository, configuration, callback)).run();
        } catch (Exception e) {
          LOG.error("got exception running asynchronous mirror call", e);
        }
      }
    );
  }

  private void startSynchronously(Repository repository, MirrorConfiguration configuration, Function<MirrorCommandBuilder, MirrorCommandResult> callback) {
    LOG.debug("running sync for mirror {}", repository);
    if (runningSynchronizations.add(repository.getId())) {
      Instant startTime = Instant.now();
      try {
        MirrorCommandCaller.CallResult<MirrorCommandResult> callResult = mirrorCommandCaller.call(repository, configuration, callback);
        MirrorCommandResult commandResult = callResult.getResultFromCallback();
        ConfigurableFilter appliedFilter = callResult.getAppliedFilter();
        LOG.debug("got result {} for sync of {}", commandResult.getResult(), repository);
        handleResult(repository, configuration, startTime, commandResult, appliedFilter);
      } catch (Exception e) {
        LOG.error("got exception while syncing {}", repository, e);
        MirrorCommandResult errorResult = new MirrorCommandResult(MirrorCommandResult.ResultType.FAILED, singletonList(e.getMessage()), Duration.ZERO);
        handleResult(repository, configuration, startTime, errorResult, null);
      } finally {
        runningSynchronizations.remove(repository.getId());
      }
    } else {
      LOG.info("skipping sync for mirror {}; other sync still running", repository);
    }
  }

  private void handleResult(Repository repository, MirrorConfiguration configuration, Instant startTime, MirrorCommandResult result, ConfigurableFilter appliedFilter) {
    MirrorStatus status = MirrorStatus.create(getFor(result.getResult(), appliedFilter), startTime);
    eventBus.post(new MirrorSyncEvent(repository, result, status));
    sendNotificationWhenChanged(repository, configuration, status.getResult());
    statusStore.setStatus(repository, status);
  }

  static MirrorStatus.Result getFor(MirrorCommandResult.ResultType type, ConfigurableFilter appliedFilter) {
    switch (type) {
      case OK:
        return SUCCESS;
      case FAILED:
        return FAILED;
      case REJECTED_UPDATES:
        return checkFilter(appliedFilter);
      default:
        LOG.warn("got unknown return type: {}", type);
        // If we do not know the result, we better prepare for the worst
        return FAILED;
    }
  }

  private static MirrorStatus.Result checkFilter(ConfigurableFilter appliedFilter) {
    if (appliedFilter.hadIssues()) {
      return FAILED_UPDATES;
    } else {
      return SUCCESS;
    }
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
    return new Notification(error, "/repo/" + repository.getNamespaceAndName() + "/mirror-logs", mirrorFailed);
  }

  private MirrorStatus.Result getLatestStatus(Repository repository) {
    MirrorStatus status = statusStore.getStatus(repository);
    if (status == null) {
      return null;
    } else {
      return status.getResult();
    }
  }


  interface CancelableSchedule {
    void cancel();
  }
}
