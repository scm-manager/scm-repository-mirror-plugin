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
import sonia.scm.EagerSingleton;
import sonia.scm.event.ScmEventBus;
import sonia.scm.plugin.Extension;

import jakarta.inject.Inject;

@EagerSingleton
@Extension
public class MirrorStatusChangedHook {

  private final MirrorStatusStore mirrorStatusStore;
  private final ScmEventBus scmEventBus;

  @Inject
  public MirrorStatusChangedHook(MirrorStatusStore mirrorStatusStore, ScmEventBus scmEventBus) {
    this.mirrorStatusStore = mirrorStatusStore;
    this.scmEventBus = scmEventBus;
  }


  @Subscribe(async = false)
  public void handleEvent(MirrorSyncEvent event) {
    MirrorStatus status = mirrorStatusStore.getStatus(event.getRepository());
    MirrorStatus.Result previousResult = status.getResult();
    MirrorStatus.Result newResult = event.getStatus().getResult();
    if (previousResult != newResult) {
      scmEventBus.post(new MirrorStatusChangedEvent(event.getRepository(), previousResult, newResult));
    }
  }
}
