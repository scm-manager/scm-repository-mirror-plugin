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

import org.apache.shiro.SecurityUtils;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

public final class MirrorPermissions {

  public static final String PERMISSION = "configureMirror";
  public static final String READ_REPO_MIRROR_CONFIG_PERMISSION = "readMirrorConfig";
  public static final String WRITE_REPO_MIRROR_CONFIG_PERMISSION = "writeMirrorConfig";

  private MirrorPermissions() {
  }

  static void checkMirrorWritePermission(Repository repository) {
    RepositoryPermissions.custom(WRITE_REPO_MIRROR_CONFIG_PERMISSION, repository).check();
  }

  public static boolean hasMirrorWritePermission(Repository repository) {
    return RepositoryPermissions.custom(WRITE_REPO_MIRROR_CONFIG_PERMISSION, repository).isPermitted();
  }

  public static boolean hasMirrorReadPermission(Repository repository) {
    return RepositoryPermissions.custom(READ_REPO_MIRROR_CONFIG_PERMISSION, repository).isPermitted();
  }

  public static boolean hasGlobalMirrorWritePermission() {
    return SecurityUtils.getSubject().isPermitted(PERMISSION + ":write");
  }

  public static boolean hasGlobalMirrorReadPermission() {
    return SecurityUtils.getSubject().isPermitted(PERMISSION + ":read");
  }
}
