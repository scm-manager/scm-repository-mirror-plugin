package com.cloudogu.scm.mirror;

import com.google.inject.util.Providers;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.Repository;

import javax.inject.Provider;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({ShiroExtension.class, MockitoExtension.class})
@SubjectAware(
  value = "trillian"
)class RepositoryLinkEnricherTest {

  @Mock
  private HalAppender appender;

  private Provider<ScmPathInfoStore> scmPathInfoStoreProvider;
  private RepositoryLinkEnricher enricher;

  @BeforeEach
  void setUp() {
    ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    scmPathInfoStore.set(() -> URI.create("https://scm-manager.org/scm/api/"));
    scmPathInfoStoreProvider = Providers.of(scmPathInfoStore);
  }

  @Test
  @SubjectAware(
    permissions = "repository:mirror:42"
  )
  void shouldEnrich() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider);
    Repository repo = new Repository("42", "git", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender).appendLink("unmirror", "https://scm-manager.org/scm/api/v2/mirror/repositories/space/name/unmirror");
  }

  @Test
  void shouldNotEnrichBecauseOfMissingPermission() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender, never()).appendLink(any(),any());
  }

}
