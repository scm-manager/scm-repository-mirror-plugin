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

import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import jakarta.inject.Inject;

public class MirrorStatusStore {

  private static final String STORE_ID = "status";

  private final DataStoreFactory storeFactory;

  @Inject
  MirrorStatusStore(DataStoreFactory storeFactory) {
    this.storeFactory = storeFactory;
  }

  void setStatus(Repository repository, MirrorStatus status) {
    createStore(repository).put(STORE_ID, status);
  }

  public MirrorStatus getStatus(Repository repository) {
    return createStore(repository).get(STORE_ID);
  }

  private DataStore<MirrorStatus> createStore(Repository repository) {
    return storeFactory.withType(MirrorStatus.class).withName("mirrorStatus").forRepository(repository).build();
  }
}
