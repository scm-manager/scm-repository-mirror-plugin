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

import com.cloudogu.scm.mirror.GlobalMirrorConfiguration;
import com.cloudogu.scm.mirror.MirrorConfigurationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.RepositoryType;
import sonia.scm.repository.api.Command;
import sonia.scm.web.MockScmPathInfoStore;

import javax.inject.Provider;

import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.api.v2.resources.HalEnricherContext.of;

@ExtendWith(MockitoExtension.class)
class RepositoryTypeEnricherTest {

  @Mock
  private MirrorConfigurationStore configurationService;

  @Mock
  private HalAppender appender;

  private final Provider<ScmPathInfoStore> scmPathInfoStore = MockScmPathInfoStore.forUri("/");
  private RepositoryTypeEnricher enricher;

  @BeforeEach
  void setup() {
    enricher = new RepositoryTypeEnricher(scmPathInfoStore, configurationService);
  }

  @Test
  void shouldEnrichTypeWithMirrorSupport() {
    RepositoryType supportingType = new RepositoryType("git", "Git", singleton(Command.MIRROR));
    when(configurationService.getGlobalConfiguration()).thenReturn(new GlobalMirrorConfiguration());

    enricher.enrich(
      of(supportingType),
      appender
    );

    verify(appender).appendLink("mirror", "/v2/mirror/repositories");
  }

  @Test
  void shouldNotEnrichTypeWithoutMirrorSupport() {
    RepositoryType supportingType = new RepositoryType("dumb", "Dumb", singleton(Command.BLAME));

    enricher.enrich(
      of(supportingType),
      appender
    );

    verify(appender, never()).appendLink(any(), any());
  }

  @Test
  void shouldAddFilterConfigurationLinkToGit() {
    RepositoryType supportingType = new RepositoryType("git", "Git", singleton(Command.MIRROR));
    when(configurationService.getGlobalConfiguration()).thenReturn(new GlobalMirrorConfiguration());

    enricher.enrich(
      of(supportingType),
      appender
    );

    verify(appender).appendLink("mirrorFilterConfiguration", "/v2/mirror/repositories/%7Bnamespace%7D/%7Bname%7D/filterConfiguration");
  }

  @Test
  void shouldNotAddFilterConfigurationLinkForNotGit() {
    RepositoryType supportingType = new RepositoryType("svn", "SVN", singleton(Command.MIRROR));

    enricher.enrich(
      of(supportingType),
      appender
    );

    verify(appender, never()).appendLink(eq("mirrorFilterConfiguration"), anyString());
  }

  @Test
  void shouldNotAddFilterConfigurationLinkIfDisabledGlobally() {
    RepositoryType supportingType = new RepositoryType("git", "Git", singleton(Command.MIRROR));
    final GlobalMirrorConfiguration globalMirrorConfiguration = new GlobalMirrorConfiguration();
    globalMirrorConfiguration.setDisableRepositoryFilterOverwrite(true);
    when(configurationService.getGlobalConfiguration()).thenReturn(globalMirrorConfiguration);

    enricher.enrich(
      of(supportingType),
      appender
    );

    verify(appender, never()).appendLink(eq("mirrorFilterConfiguration"), anyString());
  }
}
