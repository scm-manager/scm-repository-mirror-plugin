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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.web.security.AdministrationContext;

import javax.inject.Inject;
import javax.inject.Provider;

class TaskDecoratorFactory {

  private static final Logger LOG = LoggerFactory.getLogger(TaskDecoratorFactory.class);

  private final AdministrationContext administrationContext;
  private final Provider<PrivilegedMirrorRunner> privilegedMirrorRunner;

  @Inject
  public TaskDecoratorFactory(AdministrationContext administrationContext, Provider<PrivilegedMirrorRunner> privilegedMirrorRunner) {
    this.administrationContext = administrationContext;
    this.privilegedMirrorRunner = privilegedMirrorRunner;
  }

  Runnable decorate(Runnable runnable) {
    return withTryCatch(
      asAdmin(
        withMirrorPrivileges(runnable)));
  }

  private Runnable withTryCatch(Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (Exception e) {
        LOG.error("got exception running scheduled mirror call", e);
      }
    };
  }

  private Runnable asAdmin(Runnable runnable) {
    return () -> administrationContext.runAsAdmin(runnable::run);
  }

  private Runnable withMirrorPrivileges(Runnable runnable) {
    return () -> privilegedMirrorRunner.get().exceptedFromReadOnly(runnable);
  }
}
