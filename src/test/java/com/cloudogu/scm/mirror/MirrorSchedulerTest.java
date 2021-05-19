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

import com.cloudogu.scm.mirror.MirrorWorker.CancelableSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.web.security.AdministrationContext;
import sonia.scm.web.security.PrivilegedAction;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MirrorSchedulerTest {

  @Mock
  private MirrorWorker worker;
  @Mock
  private MirrorConfigurationStore configurationStore;
  @Mock
  private RepositoryManager repositoryManager;
  @Mock
  private AdministrationContext administrationContext;

  @InjectMocks
  private MirrorScheduler scheduler;

  private Set<String> cancelledSchedules = new HashSet<>();

  @BeforeEach
  void mockWorkerCancellation() {
    doAnswer(
      invocation -> (CancelableSchedule) (() -> cancelledSchedules.add(invocation.getArgument(0, Repository.class).getId()))
    ).when(worker).scheduleUpdate(any(), anyInt());
  }

  @Test
  void shouldScheduleRepositoriesAtStartup() {
    doAnswer(
      invocation -> {
        invocation.getArgument(0, PrivilegedAction.class).run();
        return null;
      }
    ).when(administrationContext).runAsAdmin(any(PrivilegedAction.class));
    Repository normalRepository = RepositoryTestData.createHeartOfGold();
    Repository mirrorRepository = RepositoryTestData.create42Puzzle();
    when(repositoryManager.getAll())
      .thenReturn(asList(normalRepository, mirrorRepository));
    when(configurationStore.hasConfiguration(normalRepository)).thenReturn(false);
    when(configurationStore.hasConfiguration(mirrorRepository)).thenReturn(true);

    scheduler.init(null);

    verify(worker).scheduleUpdate(mirrorRepository, 5);
    verify(worker, never()).scheduleUpdate(normalRepository, 5);
  }

  @Test
  void shouldSchedule() {
    Repository repository = RepositoryTestData.create42Puzzle();

    scheduler.schedule(repository);

    verify(worker).scheduleUpdate(repository, 0);
  }

  @Test
  void shouldCancelExistingSchedule() {
    Repository repository = RepositoryTestData.create42Puzzle();

    scheduler.schedule(repository);
    scheduler.schedule(repository);

    verify(worker, times(2)).scheduleUpdate(repository, 0);
    assertThat(cancelledSchedules).contains(repository.getId());
  }
}
