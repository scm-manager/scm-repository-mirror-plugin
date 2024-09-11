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

import com.github.legman.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.HandlerEventType;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.RepositoryEvent;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

@Extension
@EagerSingleton
public class MirrorCleanupHook {

  private static final Logger LOG = LoggerFactory.getLogger(MirrorCleanupHook.class);

  private final Provider<MirrorScheduler> scheduler;

  @Inject
  MirrorCleanupHook(Provider<MirrorScheduler> scheduler) {
    this.scheduler = scheduler;
  }

  @Subscribe
  public void cleanupSchedules(RepositoryEvent event) {
    if (event.getEventType() == HandlerEventType.DELETE) {
      LOG.debug("cleanup schedule for repository {} due to deletion", event.getItem());
      scheduler.get().cancel(event.getItem());
    }
  }
}
