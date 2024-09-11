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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.web.security.AdministrationContext;

import jakarta.inject.Inject;

class TaskDecoratorFactory {

  private static final Logger LOG = LoggerFactory.getLogger(TaskDecoratorFactory.class);

  private final AdministrationContext administrationContext;

  @Inject
  public TaskDecoratorFactory(AdministrationContext administrationContext) {
    this.administrationContext = administrationContext;
  }

  Runnable decorate(Runnable runnable) {
    return withTryCatch(asAdmin(runnable));
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
}
