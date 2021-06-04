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

import com.github.legman.EventBus;
import com.google.common.collect.Iterables;
import org.apache.shiro.authz.AuthorizationException;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.MirrorCommandResult;
import sonia.scm.repository.api.MirrorCommandResult.ResultType;
import sonia.scm.store.InMemoryByteDataStoreFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(ShiroExtension.class)
@SuppressWarnings("UnstableApiUsage")
class LogStoreTest {

  private LogStore logStore;
  private EventBus eventBus;

  private Repository heartOfGold;

  @BeforeEach
  void setup() {
    eventBus = EventBus.builder().build();
    logStore = new LogStore(new InMemoryByteDataStoreFactory());
    eventBus.register(logStore);

    heartOfGold = RepositoryTestData.createHeartOfGold();
    heartOfGold.setId("42");
  }

  @AfterEach
  void tearDown() {
    eventBus.shutdown();
  }

  @Test
  @SubjectAware(value = "trillian", permissions = "repository:mirror:42")
  void shouldStoreLogs() {
    MirrorSyncEvent event = event(MirrorStatus.Result.SUCCESS, 42L, "awesome sync");

    eventBus.post(event);

    List<LogEntry> entries = logStore.get(heartOfGold);
    assertThat(entries).hasSize(1);

    LogEntry entry = entries.get(0);
    assertThat(entry.getStarted()).isNotNull();
    assertThat(entry.getEnded()).isNotNull();
    assertThat(entry.getLog()).containsOnly("awesome sync");
  }

  private MirrorSyncEvent event(MirrorStatus.Result result, long duration, String... logs) {
    MirrorCommandResult commandResult = result(ResultType.OK, duration, logs);
    MirrorStatus status = MirrorStatus.create(result, Instant.now().minusMillis(duration));
    return new MirrorSyncEvent(heartOfGold, commandResult, status);
  }

  @Test
  @SubjectAware("trillian")
  void shouldFailToReadLogsWithoutPermission() {
    Repository heartOfGold = RepositoryTestData.createHeartOfGold();
    heartOfGold.setId("42");

    assertThrows(AuthorizationException.class, () -> logStore.get(heartOfGold));
  }

  @Test
  @SubjectAware(value = "trillian", permissions = "repository:mirror:42")
  void shouldLimitStoreEntries() {
    for (int i=0; i<LogStore.ENTRY_LIMIT + 5; i++) {
      eventBus.post(event(MirrorStatus.Result.FAILED, 21L, "sync " + i));
    }

    List<LogEntry> entries = logStore.get(heartOfGold);
    assertThat(entries).hasSize(LogStore.ENTRY_LIMIT);
    LogEntry first = entries.get(0);
    assertThat(first.getLog()).containsOnly("sync 24");
    LogEntry last = Iterables.getLast(entries);
    assertThat(last.getLog()).containsOnly("sync 5");
  }

  private MirrorCommandResult result(ResultType result, long duration, String... logs) {
    return new MirrorCommandResult(result, Arrays.asList(logs), Duration.ofMillis(duration));
  }

}
