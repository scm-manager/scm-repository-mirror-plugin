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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.MirrorCommandResult;

import java.time.Duration;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MirrorStatusChangedHookTest {

  private final Repository repository = RepositoryTestData.create42Puzzle();

  @Mock
  private MirrorStatusStore store;
  @Mock
  private ScmEventBus scmEventBus;

  @InjectMocks
  private MirrorStatusChangedHook hook;

  @Test
  void shouldNotSendEvent() {
    when(store.getStatus(repository)).thenReturn(new MirrorStatus(MirrorStatus.Result.SUCCESS));

    hook.handleEvent(new MirrorSyncEvent(repository, null, new MirrorStatus(MirrorStatus.Result.SUCCESS)));

    verify(scmEventBus, never()).post(any(MirrorStatusChangedEvent.class));
  }

  @Test
  void shouldSendEventForSuccessfulMirror() {
    when(store.getStatus(repository)).thenReturn(new MirrorStatus(MirrorStatus.Result.FAILED));

    hook.handleEvent(new MirrorSyncEvent(repository, null, new MirrorStatus(MirrorStatus.Result.SUCCESS)));

    verify(scmEventBus).post(any(MirrorStatusChangedEvent.class));
  }

  @Test
  void shouldSendEventForFailedMirror() {
    when(store.getStatus(repository)).thenReturn(new MirrorStatus(MirrorStatus.Result.SUCCESS));

    hook.handleEvent(new MirrorSyncEvent(repository, null, new MirrorStatus(MirrorStatus.Result.FAILED)));

    verify(scmEventBus).post(any(MirrorStatusChangedEvent.class));
  }
}
