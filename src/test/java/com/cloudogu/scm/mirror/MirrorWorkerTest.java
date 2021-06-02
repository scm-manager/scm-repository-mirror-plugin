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

import com.cloudogu.scm.mirror.MirrorConfiguration.CertificateCredential;
import com.cloudogu.scm.mirror.MirrorConfiguration.UsernamePasswordCredential;
import com.google.common.collect.ImmutableList;
import com.google.inject.util.Providers;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
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
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

@ExtendWith(MockitoExtension.class)
class MirrorWorkerTest {

  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;
  @Mock
  private MirrorStatusStore statusStore;

  @Mock
  private RepositoryService repositoryService;
  @Mock(answer = Answers.RETURNS_SELF)
  private MirrorCommandBuilder mirrorCommandBuilder;
  @Mock
  private PrivilegedMirrorRunner privilegedMirrorRunner;
  @Mock
  private NotificationSender notificationSender;
  @Mock
  private ScheduledExecutorService executor;
  @Mock
  private ScmEventBus eventBus;
  @Mock
  @SuppressWarnings("rawtypes")
  private ScheduledFuture cancelableSchedule;


  private MirrorWorker worker;

  private final Repository repository = RepositoryTestData.createHeartOfGold();

  @BeforeEach
  void createService() {
    lenient().doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    }).when(executor).submit(any(Runnable.class));
    lenient().doAnswer(
      invocationOnMock -> {
        invocationOnMock.getArgument(0, Runnable.class).run();
        return null;
      }
    ).when(privilegedMirrorRunner).exceptedFromReadOnly(any());
    worker = new MirrorWorker(repositoryServiceFactory, new SimpleMeterRegistry(), executor, statusStore, of(privilegedMirrorRunner), notificationSender, eventBus);
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

    @BeforeEach
    void supportMirrorCommand() {
      when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
      when(repositoryService.getMirrorCommand()).thenReturn(mirrorCommandBuilder);
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
            assertThat(n.getLink()).isEqualTo("/repo/" + repository.getNamespaceAndName() + "/settings/general");
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
              assertThat(n.getLink()).isEqualTo("/repo/" + repository.getNamespaceAndName() + "/settings/general");
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
              assertThat(syncEvent.getResult().getResult()).isEqualTo(FAILED);
              assertThat(syncEvent.getRepository()).isSameAs(repository);
              return true;
            }));
        }

        @Test
        void shouldSetUrlInCommand() {
          MirrorConfiguration configuration = createMirrorConfig();

          worker.startUpdate(repository, configuration);

          verify(mirrorCommandBuilder).setSourceUrl("https://hog/");
        }

        @Test
        void shouldSetUsernamePasswordCredentialsInCommand() {
          MirrorConfiguration configuration = createMirrorConfig(new UsernamePasswordCredential("dent", "hog"));

          worker.startUpdate(repository, configuration);

          verify(mirrorCommandBuilder).setCredentials(argThat(
            credentials -> {
              assertThat(credentials).extracting("username").containsExactly("dent");
              assertThat(credentials).extracting("password").containsExactly("hog");
              return true;
            }
          ));
        }

        @Test
        void shouldSetKeyCredentialsInCommand() {
          byte[] key = {};
          MirrorConfiguration configuration = createMirrorConfig(new CertificateCredential(key, "hog"));

          worker.startUpdate(repository, configuration);

          verify(mirrorCommandBuilder).setCredentials(argThat(
            credentials -> {
              assertThat(credentials).extracting("certificate").containsExactly(key);
              assertThat(credentials).extracting("password").containsExactly((Object) "hog".toCharArray());
              return true;
            }
          ));
        }

        @Test
        void shouldRunOnlyOneUpdateAtATime() throws InterruptedException {
          byte[] key = {};
          MirrorConfiguration configuration = createMirrorConfig(new CertificateCredential(key, "hog"));
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
        mockLastStatus(MirrorStatus.Result.REJECTED_UPDATES);

        MirrorConfiguration configuration = createMirrorConfig(ImmutableList.of("trillian"), null, null);

        worker.startUpdate(repository, configuration);

        verify(statusStore).setStatus(
          eq(repository),
          argThat(status -> status.getResult().equals(MirrorStatus.Result.REJECTED_UPDATES)));
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

  private MirrorConfiguration createMirrorConfig(CertificateCredential cc) {
    return createMirrorConfig(emptyList(), null, cc);
  }

  private MirrorConfiguration createMirrorConfig(UsernamePasswordCredential upc) {
    return createMirrorConfig(emptyList(), upc, null);
  }

  private MirrorConfiguration createMirrorConfig(List<String> managingUsers, UsernamePasswordCredential upc, CertificateCredential cc) {
    return new MirrorConfiguration("https://hog/", 42, managingUsers, upc, cc);
  }
}
