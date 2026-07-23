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

import lombok.extern.slf4j.Slf4j;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.MirrorCommandBuilder;

import java.time.Instant;


@Slf4j
class MirrorProgressTracker implements MirrorCommandBuilder.LogCallback {

  private final Instant started = Instant.now();
  private final Repository repository;

  private String step;
  private int totalWork;
  private int worked;
  private boolean stepFinished;
  private Instant updated = started;

  MirrorProgressTracker(Repository repository) {
    this.repository = repository;
  }

  synchronized MirrorProgress getProgress() {
    return new MirrorProgress(true, step, totalWork, worked, stepFinished, started, updated);
  }

  @Override
  public synchronized void stepStarted(String step, int totalWork) {
    log.trace("step '{}' started with a total work of {} in repository {}", step, totalWork, repository);
    this.step = step;
    this.totalWork = totalWork;
    this.worked = 0;
    this.stepFinished = false;
    this.updated = Instant.now();
  }

  @Override
  public synchronized void currentStepProgressed(int completedWork) {
    this.worked = completedWork;
    this.stepFinished = false;
    this.updated = Instant.now();
  }

  @Override
  public synchronized void currentStepFinished() {
    log.trace("step '{}' finished in repository {}", step, repository);
    if (totalWork > 0 && worked < totalWork) {
      worked = totalWork;
    }
    this.stepFinished = true;
    this.updated = Instant.now();
  }
}
