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

import javax.inject.Inject;
import javax.inject.Provider;

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
