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
import com.cloudogu.scm.mirror.MirrorStatus;
import com.cloudogu.scm.mirror.MirrorStatusStore;
import de.otto.edison.hal.HalRepresentation;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.web.MockScmPathInfoStore;

import javax.inject.Provider;

import static com.cloudogu.scm.mirror.MirrorStatus.Result.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware("trillian")
@ExtendWith({MockitoExtension.class, ShiroExtension.class})
class RepositoryEnricherTest {

  private static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  static {
    REPOSITORY.setId("42");
  }

  private final Provider<ScmPathInfoStore> scmPathInfoStore = MockScmPathInfoStore.forUri("/");

  @Mock
  private HalAppender appender;
  @Mock
  private MirrorConfigurationStore configurationService;
  @Mock
  private MirrorStatusStore statusStore;

  private RepositoryEnricher enricher;

  @BeforeEach
  void createEnricher() {
    enricher = new RepositoryEnricher(scmPathInfoStore, configurationService, statusStore);
  }

  @Test
  void shouldNotAppendLinkForRepositoryWithoutPermission() {
    HalEnricherContext context = HalEnricherContext.of(REPOSITORY);
    lenient().when(configurationService.hasConfiguration(REPOSITORY.getId())).thenReturn(true);
    when(statusStore.getStatus(REPOSITORY)).thenReturn(new MirrorStatus(SUCCESS));

    enricher.enrich(context, appender);

    verify(appender, never()).appendLink(any(), any());
  }

  @Test
  void shouldAppendStatusAsEmbeddedForMirrorRepository() {
    HalEnricherContext context = HalEnricherContext.of(REPOSITORY);
    lenient().when(configurationService.hasConfiguration(REPOSITORY.getId())).thenReturn(true);
    when(statusStore.getStatus(REPOSITORY)).thenReturn(new MirrorStatus(SUCCESS));

    enricher.enrich(context, appender);

    verify(appender).appendEmbedded(
      eq("mirrorStatus"),
      (HalRepresentation) argThat(status -> {
        assertThat(status).extracting("result").asInstanceOf(InstanceOfAssertFactories.type(MirrorStatus.Result.class)).isEqualTo(SUCCESS);
        return true;
      })
    );
  }

  @Test
  void shouldNotAppendStatusForNormalRepository() {
    HalEnricherContext context = HalEnricherContext.of(REPOSITORY);
    lenient().when(configurationService.hasConfiguration(REPOSITORY.getId())).thenReturn(false);

    enricher.enrich(context, appender);

    verify(appender, never()).appendEmbedded(any(), (HalRepresentation) any());
  }

  @Nested
  @SubjectAware(
    value = "trillian",
    permissions = "repository:mirror:42"
  )
  class WithPermission {

    @Test
    void shouldNotAppendLinkForRepositoryThatIsNoMirror() {
      HalEnricherContext context = HalEnricherContext.of(REPOSITORY);
      when(configurationService.hasConfiguration(REPOSITORY.getId())).thenReturn(false);

      enricher.enrich(context, appender);

      verify(appender, never()).appendLink(any(), any());
    }

    @Test
    void shouldAppendLinkForRepositoryThatIsAMirror() {
      HalEnricherContext context = HalEnricherContext.of(REPOSITORY);
      when(configurationService.hasConfiguration(REPOSITORY.getId())).thenReturn(true);
      when(statusStore.getStatus(REPOSITORY)).thenReturn(new MirrorStatus(SUCCESS));

      enricher.enrich(context, appender);

      verify(appender).appendLink("mirrorConfiguration", "/v2/mirror/repositories/hitchhiker/HeartOfGold/configuration");
    }

    @Test
    void shouldAppendLogLinks() {
      HalEnricherContext context = HalEnricherContext.of(REPOSITORY);
      when(configurationService.hasConfiguration(REPOSITORY.getId())).thenReturn(true);
      when(statusStore.getStatus(REPOSITORY)).thenReturn(new MirrorStatus(SUCCESS));

      enricher.enrich(context, appender);

      verify(appender).appendLink("mirrorLogs", "/v2/mirror/repositories/hitchhiker/HeartOfGold/logs");
    }
  }
}
