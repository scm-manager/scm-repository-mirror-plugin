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

package com.cloudogu.scm.mirror.api;

import com.cloudogu.scm.mirror.MirrorConfigurationStore;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.RepositoryType;
import sonia.scm.repository.api.Command;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

@Extension
@Enrich(RepositoryType.class)
public class RepositoryTypeEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStore;
  private final MirrorConfigurationStore configurationService;

  @Inject
  public RepositoryTypeEnricher(Provider<ScmPathInfoStore> scmPathInfoStore, MirrorConfigurationStore configurationService) {
    this.scmPathInfoStore = scmPathInfoStore;
    this.configurationService = configurationService;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    RepositoryType repositoryType = context.oneRequireByType(RepositoryType.class);
    if (repositoryType.getSupportedCommands().contains(Command.MIRROR)) {
      String createUrl = new LinkBuilder(scmPathInfoStore.get().get(), MirrorRootResource.class).method("createMirrorRepository").parameters().href();
      appender.appendLink("mirror", createUrl);
      if (repositoryType.getName().equals("git") && !configurationService.getGlobalConfiguration().isDisableRepositoryFilterOverwrite()) {
        String filterConfigurationLink = new LinkBuilder(scmPathInfoStore.get().get(), MirrorRootResource.class, MirrorResource.class)
          .method("repository").parameters("{namespace}", "{name}").method("getFilterConfiguration").parameters().href();
        appender.appendLink("mirrorFilterConfiguration", filterConfigurationLink);
      }
    }
  }
}
