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

import org.junit.jupiter.api.Test;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.InMemoryByteDataStoreFactory;

import static com.cloudogu.scm.mirror.MirrorStatus.Result.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

class MirrorStatusStoreTest {

  private InMemoryByteDataStoreFactory storeFactory = new InMemoryByteDataStoreFactory();
  private MirrorStatusStore statusStore = new MirrorStatusStore(storeFactory);

  @Test
  void shouldStoreStatus() {
    Repository repository = RepositoryTestData.createHeartOfGold();
    statusStore.setStatus(repository, new MirrorStatus(SUCCESS));

    assertThat(statusStore.getStatus(repository).getResult()).isEqualTo(SUCCESS);
  }
}
