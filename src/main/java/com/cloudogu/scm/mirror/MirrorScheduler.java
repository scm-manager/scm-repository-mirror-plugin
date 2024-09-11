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

import jakarta.inject.Singleton;
import sonia.scm.repository.Repository;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
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
