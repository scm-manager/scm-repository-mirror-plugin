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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  private MirrorWorker worker;

  private final Repository repository = RepositoryTestData.createHeartOfGold();

  @BeforeEach
  void createService() {
    ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    lenient().doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    }).when(executor).submit(any(Runnable.class));
    doAnswer(
      invocationOnMock -> {
        invocationOnMock.getArgument(0, Runnable.class).run();
        return null;
      }
    ).when(privilegedMirrorRunner).exceptedFromReadOnly(any());
    worker = new MirrorWorker(repositoryServiceFactory, new SimpleMeterRegistry(), executor, statusStore, privilegedMirrorRunner, notificationSender);
  }

  @BeforeEach
  void supportMirrorCommand() {
    when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
    when(repositoryService.getMirrorCommand()).thenReturn(mirrorCommandBuilder);
  }

  @Test
  void shouldSetSuccessStatus() {
    MirrorConfiguration configuration = createMirrorConfig();
    when(mirrorCommandBuilder.initialCall()).thenReturn(new MirrorCommandResult(true, emptyList(), Duration.ZERO));

    worker.startInitialSync(repository, configuration);

    verify(statusStore).setStatus(
      eq(repository),
      argThat(status -> status.getResult().equals(MirrorStatus.Result.NOT_YET_RUN)));
    verify(statusStore).setStatus(
      eq(repository),
      argThat(status -> status.getResult().equals(MirrorStatus.Result.SUCCESS)));
  }

  @Nested
  class ForUpdate {

    @BeforeEach
    void mockUpdate() {
      when(statusStore.getStatus(repository)).thenReturn(new MirrorStatus(MirrorStatus.Result.SUCCESS));
      when(mirrorCommandBuilder.update()).thenReturn(new MirrorCommandResult(false, emptyList(), Duration.ZERO));
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
    void shouldSetFailedStatusAndButNotSendNotificationToManagingUsers() {
      when(statusStore.getStatus(repository)).thenReturn(new MirrorStatus(MirrorStatus.Result.FAILED));

      MirrorConfiguration configuration = createMirrorConfig(ImmutableList.of("trillian"), null, null);

      worker.startUpdate(repository, configuration);

      verify(statusStore).setStatus(
        eq(repository),
        argThat(status -> status.getResult().equals(MirrorStatus.Result.FAILED)));
      verify(notificationSender, never()).send(any(), any());
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
          return new MirrorCommandResult(false, emptyList(), Duration.ZERO);
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
