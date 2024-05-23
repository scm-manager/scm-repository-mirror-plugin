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

import sonia.scm.plugin.Extension;
import sonia.scm.repository.ReadOnlyCheck;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;

import static java.util.Arrays.asList;

@Extension
@Singleton
class MirrorReadOnlyCheck implements ReadOnlyCheck {

  private static final Collection<String> FORBIDDEN_PERMISSIONS = asList("push", "archive", "createPullRequest");

  private final MirrorConfigurationStore configurationStore;

  @Inject
  MirrorReadOnlyCheck(MirrorConfigurationStore configurationStore) {
    this.configurationStore = configurationStore;
  }

  @Override
  public String getReason() {
    return "repository is a mirror";
  }

  @Override
  public boolean isReadOnly(String repositoryId) {
    return false;
  }

  @Override
  public boolean isReadOnly(String permission, String repositoryId) {
    return FORBIDDEN_PERMISSIONS.contains(permission)
      && configurationStore.hasConfiguration(repositoryId);
  }
}
