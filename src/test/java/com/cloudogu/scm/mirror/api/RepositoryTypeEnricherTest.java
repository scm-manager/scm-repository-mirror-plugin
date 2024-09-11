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

import jakarta.inject.Provider;

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
