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

import com.cloudogu.scm.mirror.MirrorWorker.CancelableSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MirrorSchedulerTest {

  @Mock
  private MirrorWorker worker;

  @InjectMocks
  private MirrorScheduler scheduler;

  private final Set<String> cancelledSchedules = new HashSet<>();

  @BeforeEach
  void mockWorkerCancellation() {
    doAnswer(
      invocation -> (CancelableSchedule) (() -> cancelledSchedules.add(invocation.getArgument(0, Repository.class).getId()))
    ).when(worker).scheduleUpdate(any(), any(), anyInt());
  }

  @Test
  void shouldSchedule() {
    Repository repository = RepositoryTestData.create42Puzzle();
    MirrorConfiguration configuration = mockConfiguration(repository, 42);

    scheduler.schedule(repository, configuration);

    verify(worker).scheduleUpdate(repository, configuration, 42);
  }

  @Test
  void shouldCancelExistingSchedule() {
    Repository repository = RepositoryTestData.create42Puzzle();
    scheduler.schedule(repository, mockConfiguration(repository, 42));

    MirrorConfiguration configuration = mockConfiguration(repository, 23);
    scheduler.schedule(repository, configuration);

    verify(worker).scheduleUpdate(eq(repository), any(), eq(42));
    assertThat(cancelledSchedules).contains(repository.getId());
    verify(worker).scheduleUpdate(repository, configuration, 23);
  }

  private MirrorConfiguration mockConfiguration(Repository repository, int i) {
    return new MirrorConfiguration(null, i, emptyList(), null, null, null);
  }
}
