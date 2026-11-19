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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import sonia.scm.repository.Feature;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

@Singleton
public class MirrorSourceUrlSupport {

  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  MirrorSourceUrlSupport(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  public boolean isUrlModifiable(Repository repository) {
    try (RepositoryService service = repositoryServiceFactory.create(repository)) {
      return service.isSupported(Feature.MIRROR_SOURCE_URL_CHANGE);
    }
  }
}
