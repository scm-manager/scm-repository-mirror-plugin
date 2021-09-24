package com.cloudogu.scm.mirror;

import com.cloudogu.scm.mirror.api.MirrorResource;
import com.cloudogu.scm.mirror.api.MirrorRootResource;
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
public class RepositoryLinkEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStoreProvider;

  @Inject
  public RepositoryLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStoreProvider) {
    this.scmPathInfoStoreProvider = scmPathInfoStoreProvider;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    Repository repository = context.oneRequireByType(Repository.class);
    if (MirrorPermissions.hasRepositoryMirrorPermission(repository)) {
      LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStoreProvider.get().get(), MirrorRootResource.class, MirrorResource.class);
      appender.appendLink("unmirror", linkBuilder.method("repository").parameters(repository.getNamespace(), repository.getName()).method("unmirror").parameters().href());
    }
  }
}
