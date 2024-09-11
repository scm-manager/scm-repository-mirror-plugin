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

import sonia.scm.config.ConfigurationPermissions;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

public final class MirrorPermissions {

  public static final String PERMISSION = "mirror";

  private MirrorPermissions() {
  }

  static void checkRepositoryMirrorPermission(Repository repository) {
    RepositoryPermissions.custom(PERMISSION, repository).check();
  }

  public static boolean hasRepositoryMirrorPermission(Repository repository) {
    return RepositoryPermissions.custom(PERMISSION, repository).isPermitted();
  }

  public static boolean hasGlobalMirrorPermission() {
    return ConfigurationPermissions.write(PERMISSION).isPermitted();
  }
}
