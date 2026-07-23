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

import lombok.Getter;

import java.time.Instant;

@Getter
public class MirrorProgress {

  private static final MirrorProgress IDLE = new MirrorProgress(false, null, 0, 0, false, null, null);

  private final boolean running;
  private final String step;
  private final int totalWork;
  private final int worked;
  private final boolean stepFinished;
  private final Instant started;
  private final Instant updated;

  public MirrorProgress(boolean running, String step, int totalWork, int worked, boolean stepFinished, Instant started, Instant updated) {
    this.running = running;
    this.step = step;
    this.totalWork = totalWork;
    this.worked = worked;
    this.stepFinished = stepFinished;
    this.started = started;
    this.updated = updated;
  }

  public static MirrorProgress idle() {
    return IDLE;
  }
}
