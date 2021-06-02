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
