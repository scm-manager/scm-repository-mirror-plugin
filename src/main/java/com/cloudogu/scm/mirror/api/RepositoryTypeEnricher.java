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
