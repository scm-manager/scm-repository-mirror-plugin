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

import com.github.legman.Subscribe;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import sonia.scm.collect.EvictingQueue;
import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Queue;

@Singleton
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
    entries.add(new LogEntry(event.getResult()));
    store.put(STORE_ENTRY, entries);
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
