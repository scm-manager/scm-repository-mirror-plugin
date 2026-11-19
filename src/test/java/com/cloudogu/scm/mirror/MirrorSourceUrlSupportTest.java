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
import sonia.scm.repository.Feature;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class MirrorSourceUrlSupportTest {

  @Test
  void shouldUseCapabilityRegardlessOfRepositoryTypeAndCloseService() {
    RepositoryServiceFactory factory = mock(RepositoryServiceFactory.class);
    RepositoryService service = mock(RepositoryService.class);
    Repository repository = RepositoryTestData.createHeartOfGold();
    repository.setType("custom-vcs");
    when(factory.create(repository)).thenReturn(service);
    MirrorSourceUrlSupport support = new MirrorSourceUrlSupport(factory);

    assertThat(support.isUrlModifiable(repository)).isFalse();
    when(service.isSupported(Feature.MIRROR_SOURCE_URL_CHANGE)).thenReturn(true);
    assertThat(support.isUrlModifiable(repository)).isTrue();

    verify(service, times(2)).close();
  }
}
