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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import sonia.scm.EagerSingleton;
import sonia.scm.collect.EvictingQueue;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import jakarta.inject.Inject;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Queue;

@EagerSingleton
@Extension
public class LogStore {

  @VisibleForTesting
  static final int ENTRY_LIMIT = 20;

  private static final String STORE_NAME = "mirrorSyncLog";
  private static final String STORE_ENTRY = "main";

  private final DataStoreFactory storeFactory;

  @Inject
  public LogStore(DataStoreFactory storeFactory) {
    this.storeFactory = storeFactory;
  }

  @Subscribe(async = false)
  public void handle(MirrorSyncEvent event) {
    DataStore<LogEntries> store = store(event.getRepository());
    LogEntries entries = entries(store);
    entries.add(entry(event));
    store.put(STORE_ENTRY, entries);
  }

  @SuppressWarnings("UnstableApiUsage")
  private LogEntry entry(MirrorSyncEvent event) {
    return new LogEntry(event.getStatus(), event.getResult().getLog());
  }

  public List<LogEntry> get(Repository repository) {
    MirrorPermissions.checkRepositoryMirrorPermission(repository);
    return ImmutableList.copyOf(entries(store(repository)).getEntries()).reverse();
  }

  private DataStore<LogEntries> store(Repository repository) {
    return storeFactory.withType(LogEntries.class).withName(STORE_NAME).forRepository(repository).build();
  }

  private LogEntries entries(DataStore<LogEntries> store) {
    return store.getOptional(STORE_ENTRY).orElseGet(LogEntries::new);
  }

  @Getter
  @XmlRootElement
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class LogEntries {

    private Queue<LogEntry> entries = EvictingQueue.create(ENTRY_LIMIT);

    LogEntries() {
    }

    void add(LogEntry entry) {
      entries.add(entry);
    }
  }

}
