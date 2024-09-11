/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.mirror;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import sonia.scm.event.ScmEventBus;
import sonia.scm.notifications.NotificationSender;
import sonia.scm.notifications.Type;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.MirrorCommandBuilder;
import sonia.scm.repository.api.MirrorCommandResult;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.cloudogu.scm.mirror.MirrorStatus.Result.SUCCESS;
import static com.google.inject.util.Providers.of;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.api.MirrorCommandResult.ResultType.FAILED;
import static sonia.scm.repository.api.MirrorCommandResult.ResultType.OK;
import static sonia.scm.repository.api.MirrorCommandResult.ResultType.REJECTED_UPDATES;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class MirrorWorkerTest {

  @Mock
  private MirrorStatusStore statusStore;

  @Mock
  private MirrorCommandBuilder mirrorCommandBuilder;
  @Mock
  private TaskDecoratorFactory taskDecoratorFactory;
  @Mock
  private NotificationSender notificationSender;
  @Mock
  private ScheduledExecutorService executor;
  @Mock
  private ScmEventBus eventBus;
  @Mock
  @SuppressWarnings("rawtypes")
  private ScheduledFuture cancelableSchedule;
  @Mock
  private MirrorCommandCaller mirrorCommandCaller;

  private MirrorWorker worker;

  private final Repository repository = RepositoryTestData.createHeartOfGold();

  @BeforeEach
  void createService() {
    lenient().doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    }).when(executor).submit(any(Runnable.class));
    lenient().when(taskDecoratorFactory.decorate(any()))
      .thenAnswer(invocation -> invocation.getArgument(0));
    worker = new MirrorWorker(new SimpleMeterRegistry(), executor, statusStore, notificationSender, eventBus, mirrorCommandCaller, taskDecoratorFactory);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldCancelUpdates() {
    MirrorConfiguration configuration = createMirrorConfig();
    when(executor.scheduleAtFixedRate(any(), anyLong(), anyLong(), any()))
      .thenReturn(cancelableSchedule);

    worker.scheduleUpdate(repository, configuration, 23)
      .cancel();

    verify(cancelableSchedule).cancel(false);
  }

  @Nested
  class ForRepository {

    @Mock
    private ConfigurableFilter appliedFilter;


    @BeforeEach
    void supportMirrorCommand() {
      when(mirrorCommandCaller.call(eq(repository), any(), any()))
        .thenAnswer(invocation -> new MirrorCommandCaller.CallResult(invocation.getArgument(2, Function.class).apply(mirrorCommandBuilder), appliedFilter));
    }

    @Test
    void shouldSetSuccessStatus() {
      MirrorConfiguration configuration = createMirrorConfig();
      mockResultFor(mirrorCommandBuilder.initialCall(), OK);

      worker.startInitialSync(repository, configuration);

      verify(statusStore).setStatus(
        eq(repository),
        argThat(status -> status.getResult().equals(MirrorStatus.Result.NOT_YET_RUN)));
      verify(statusStore).setStatus(
        eq(repository),
        argThat(status -> status.getResult().equals(SUCCESS)));
    }

    @Nested
    class ForSuccessfulUpdate {

      @BeforeEach
      void mockUpdate() {
        mockLastStatus(MirrorStatus.Result.FAILED);
        mockResultFor(mirrorCommandBuilder.update(), OK);
      }

      @Test
      void shouldSetSuccessStatusAndSendNotificationToManagingUsers() {
        MirrorConfiguration configuration = createMirrorConfig(ImmutableList.of("trillian"), null, null);

        worker.startUpdate(repository, configuration);

        verify(statusStore).setStatus(
          eq(repository),
          argThat(status -> status.getResult().equals(SUCCESS)));
        verify(notificationSender).send(argThat(n -> {
            assertThat(n.getMessage()).isEqualTo("mirrorSuccess");
            assertThat(n.getType()).isEqualTo(Type.SUCCESS);
            assertThat(n.getLink()).isEqualTo("/repo/" + repository.getNamespaceAndName() + "/mirror-logs");
            return true;
          }),
          eq("trillian")
        );
      }
    }

    @Nested
    class ForFailedUpdate {

      @Nested
      class AfterSuccess {

        @BeforeEach
        void mockSuccessAfterFailure() {
          mockResultFor(mirrorCommandBuilder.update(), FAILED);
          mockLastStatus(SUCCESS);
        }

        @Test
        void shouldSetFailedStatusAndSendNotificationToManagingUsers() {
          MirrorConfiguration configuration = createMirrorConfig(ImmutableList.of("trillian"), null, null);

          worker.startUpdate(repository, configuration);

          verify(statusStore).setStatus(
            eq(repository),
            argThat(status -> status.getResult().equals(MirrorStatus.Result.FAILED)));
          verify(notificationSender).send(argThat(n -> {
              assertThat(n.getMessage()).isEqualTo("mirrorFailed");
              assertThat(n.getType()).isEqualTo(Type.ERROR);
              assertThat(n.getLink()).isEqualTo("/repo/" + repository.getNamespaceAndName() + "/mirror-logs");
              return true;
            }),
            eq("trillian")
          );
        }

        @Test
        void shouldPostSyncEvent() {
          MirrorConfiguration configuration = createMirrorConfig(ImmutableList.of("trillian"), null, null);

          worker.startUpdate(repository, configuration);

          verify(eventBus).post(
            argThat(event -> {
              MirrorSyncEvent syncEvent = (MirrorSyncEvent) event;
              assertThat(syncEvent.getStatus().getResult()).isEqualTo(MirrorStatus.Result.FAILED);
              assertThat(syncEvent.getRepository()).isSameAs(repository);
              return true;
            }));
        }

        @Test
        void shouldRunOnlyOneUpdateAtATime() throws InterruptedException {
          MirrorConfiguration configuration = createMirrorConfig();
          CountDownLatch startedLatch = new CountDownLatch(1);
          when(mirrorCommandBuilder.update())
            .thenAnswer(invocation -> {
              startedLatch.await();
              return new MirrorCommandResult(FAILED, emptyList(), Duration.ZERO);
            });

          CountDownLatch doneLatch = new CountDownLatch(2);
          // start two updates in parallel
          for (int i = 0; i < 2; ++i) {
            new Thread(() -> {
              worker.startUpdate(repository, configuration);
              startedLatch.countDown();
              doneLatch.countDown();
            }).start();
          }

          // make sure both updates have been triggered
          doneLatch.await();

          // make sure both updates have been run
          verify(mirrorCommandBuilder).update();
        }

        @Test
        void shouldScheduleUpdates() {
          ArgumentCaptor<Runnable> runnableArgumentCaptor = forClass(Runnable.class);
          MirrorConfiguration configuration = createMirrorConfig();
          configuration.setSynchronizationPeriod(42);

          worker.scheduleUpdate(repository, configuration, 23);

          verify(executor).scheduleAtFixedRate(
            runnableArgumentCaptor.capture(),
            eq(23L),
            eq(42L),
            eq(TimeUnit.MINUTES)
          );

          runnableArgumentCaptor.getValue().run();

          verify(mirrorCommandBuilder).update();
        }
      }

      @Test
      void shouldSetFailedStatusButNotSendNotificationToManagingUsers() {
        mockResultFor(mirrorCommandBuilder.update(), FAILED);
        mockLastStatus(MirrorStatus.Result.FAILED);

        MirrorConfiguration configuration = createMirrorConfig(ImmutableList.of("trillian"), null, null);

        worker.startUpdate(repository, configuration);

        verify(statusStore).setStatus(
          eq(repository),
          argThat(status -> status.getResult().equals(MirrorStatus.Result.FAILED)));
        verify(notificationSender, never()).send(any(), any());
      }

      @Test
      void shouldSetRejectedStatusButNotSendNotificationToManagingUsers() {
        mockResultFor(mirrorCommandBuilder.update(), REJECTED_UPDATES);
        mockLastStatus(MirrorStatus.Result.FAILED_UPDATES);
        when(appliedFilter.hadIssues()).thenReturn(true);

        MirrorConfiguration configuration = createMirrorConfig(ImmutableList.of("trillian"), null, null);

        worker.startUpdate(repository, configuration);

        verify(statusStore).setStatus(
          eq(repository),
          argThat(status -> status.getResult().equals(MirrorStatus.Result.FAILED_UPDATES)));
        verify(notificationSender, never()).send(any(), any());
      }

      @Test
      void shouldSetOkStatusButNotSendNotificationToManagingUsers() {
        mockResultFor(mirrorCommandBuilder.update(), OK);
        mockLastStatus(SUCCESS);

        MirrorConfiguration configuration = createMirrorConfig(ImmutableList.of("trillian"), null, null);

        worker.startUpdate(repository, configuration);

        verify(statusStore).setStatus(
          eq(repository),
          argThat(status -> status.getResult().equals(SUCCESS)));
        verify(notificationSender, never()).send(any(), any());
      }

      @Test
      void shouldSetFailStatusOnException() {
        when(mirrorCommandBuilder.update()).thenThrow(RuntimeException.class);
        mockLastStatus(SUCCESS);

        MirrorConfiguration configuration = createMirrorConfig(ImmutableList.of("trillian"), null, null);

        worker.startUpdate(repository, configuration);

        verify(statusStore).setStatus(
          eq(repository),
          argThat(status -> status.getResult().equals(MirrorStatus.Result.FAILED)));
      }
    }
  }

  private OngoingStubbing<MirrorCommandResult> mockResultFor(MirrorCommandResult command, MirrorCommandResult.ResultType result) {
    return when(command).thenReturn(new MirrorCommandResult(result, emptyList(), Duration.ZERO));
  }

  private OngoingStubbing<MirrorStatus> mockLastStatus(MirrorStatus.Result lastStatus) {
    return when(statusStore.getStatus(repository)).thenReturn(new MirrorStatus(lastStatus));
  }

  private MirrorConfiguration createMirrorConfig() {
    return createMirrorConfig(emptyList(), null, null);
  }

  private MirrorConfiguration createMirrorConfig(List<String> managingUsers, MirrorAccessConfiguration.UsernamePasswordCredential upc, MirrorAccessConfiguration.CertificateCredential cc) {
    return new MirrorConfiguration("https://hog/", 42, managingUsers, upc, cc, null);
  }
}
