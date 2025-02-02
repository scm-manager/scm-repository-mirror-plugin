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

import com.cloudogu.scm.mirror.MirrorConfiguration;
import com.cloudogu.scm.mirror.MirrorConfigurationStore;
import com.cloudogu.scm.mirror.MirrorPermissions;
import com.cloudogu.scm.mirror.MirrorStatus;
import com.cloudogu.scm.mirror.MirrorStatusStore;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

@Extension
@Enrich(Repository.class)
public class RepositoryEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStore;
  private final MirrorConfigurationStore configurationService;
  private final MirrorStatusStore statusStore;

  @Inject
  public RepositoryEnricher(Provider<ScmPathInfoStore> scmPathInfoStore, MirrorConfigurationStore configurationService, MirrorStatusStore statusStore) {
    this.scmPathInfoStore = scmPathInfoStore;
    this.configurationService = configurationService;
    this.statusStore = statusStore;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    Repository repository = context.oneRequireByType(Repository.class);
    configurationService.getConfiguration(repository).ifPresent(
      configuration -> {
        appendStatusEmbedded(appender, repository, configuration);
        if (MirrorPermissions.hasRepositoryMirrorPermission(repository)) {
          LinkBuilder linkBuilder = linkBuilder(repository);
          appendConfigurationLinks(appender, linkBuilder);
          appendLogsLink(appender, linkBuilder);
        }
      }
    );
  }

  private void appendStatusEmbedded(HalAppender appender, Repository repository, MirrorConfiguration configuration) {
    if (configuration.getSynchronizationPeriod() == null) {
      appender.appendEmbedded("mirrorStatus", new MirrorStatusDto(MirrorStatusDto.Result.DISABLED));
    } else {
      MirrorStatus status = statusStore.getStatus(repository);
      appender.appendEmbedded("mirrorStatus", new MirrorStatusDto(MirrorStatusDto.Result.valueOf(status.getResult().name())));
    }
  }

  private void appendConfigurationLinks(HalAppender appender, LinkBuilder linkBuilder) {
    String accessConfigurationLink = linkBuilder.method("getAccessConfiguration").parameters().href();
    appender.appendLink("mirrorAccessConfiguration", accessConfigurationLink);
    if (!configurationService.getGlobalConfiguration().isDisableRepositoryFilterOverwrite()) {
      String filterConfigurationLink = linkBuilder.method("getFilterConfiguration").parameters().href();
      appender.appendLink("mirrorFilterConfiguration", filterConfigurationLink);
    }
    appender.appendLink("unmirror", linkBuilder.method("unmirror").parameters().href());
  }

  private void appendLogsLink(HalAppender appender, LinkBuilder linkBuilder) {
    String configurationUrl = linkBuilder.method("getLogs").parameters().href();
    appender.appendLink("mirrorLogs", configurationUrl);
  }

  private LinkBuilder linkBuilder(Repository repository) {
    return new LinkBuilder(scmPathInfoStore.get().get(), MirrorRootResource.class, MirrorResource.class)
      .method("repository").parameters(repository.getNamespace(), repository.getName());
  }
}
