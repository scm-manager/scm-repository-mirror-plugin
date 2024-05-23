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

import sonia.scm.repository.Repository;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class MirrorScheduler  {

  private final MirrorWorker worker;

  private final Map<String, MirrorWorker.CancelableSchedule> schedules = new ConcurrentHashMap<>();

  @Inject
  MirrorScheduler(MirrorWorker worker) {
    this.worker = worker;
  }

  void scheduleNow(Repository repository, MirrorConfiguration configuration) {
    schedule(repository, configuration, 5);
  }

  void schedule(Repository repository, MirrorConfiguration configuration) {
    schedule(repository, configuration, configuration.getSynchronizationPeriod());
  }

  void cancel(Repository repository) {
    schedules.getOrDefault(repository.getId(), () -> {}).cancel();
  }

  private void schedule(Repository repository, MirrorConfiguration configuration, int delay) {
    cancel(repository);
    schedules.put(repository.getId(), worker.scheduleUpdate(repository, configuration, delay));
  }
}
