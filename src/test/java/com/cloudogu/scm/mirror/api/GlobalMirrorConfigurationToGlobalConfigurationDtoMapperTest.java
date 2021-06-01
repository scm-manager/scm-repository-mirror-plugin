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
import com.cloudogu.scm.mirror.MirrorGpgVerificationType;
import com.cloudogu.scm.mirror.RawGpgKey;
import com.google.common.collect.ImmutableList;
import com.google.inject.util.Providers;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import sonia.scm.api.v2.resources.ScmPathInfoStore;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SubjectAware(value = "trillian")
@ExtendWith(ShiroExtension.class)
class GlobalMirrorConfigurationToGlobalConfigurationDtoMapperTest {
  private final GlobalMirrorConfigurationToGlobalConfigurationDtoMapper mapper = Mappers.getMapper(GlobalMirrorConfigurationToGlobalConfigurationDtoMapper.class);

  @BeforeEach
  void setup() {
    final ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    scmPathInfoStore.set(() -> URI.create("/scm"));
    mapper.setScmPathInfoStore(Providers.of(scmPathInfoStore));
  }

  @SubjectAware(permissions = "configureMirror:read")
  @Test
  void shouldMapToDto() {
    GlobalMirrorConfiguration input = new GlobalMirrorConfiguration();
    input.setHttpsOnly(true);
    input.setBranchesAndTagsPatterns(ImmutableList.of("default", "feature/*"));
    input.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
    input.setAllowedGpgKeys(ImmutableList.of(new RawGpgKey("foo", "bar")));

    final GlobalMirrorConfigurationDto output = mapper.map(input);

    assertThat(output.isHttpsOnly()).isTrue();
    assertThat(output.getBranchesAndTagsPatterns()).contains("default", "feature/*");
    assertThat(output.getGpgVerificationType()).isEqualTo(MirrorGpgVerificationType.KEY_LIST);
    assertThat(output.getAllowedGpgKeys().get(0).getDisplayName()).isEqualTo("foo");
    assertThat(output.getAllowedGpgKeys().get(0).getRaw()).isEqualTo("bar");
  }

  @SubjectAware(permissions = "configureMirror:read,write")
  @Test
  void shouldAppendUpdateLink() {
    GlobalMirrorConfiguration input = new GlobalMirrorConfiguration();

    final GlobalMirrorConfigurationDto output = mapper.map(input);

    assertThat(output.getLinks().getLinkBy("update"))
      .hasValueSatisfying(link -> assertThat(link.getHref()).isEqualTo("/v2/mirror/configuration"));
  }

  @Test
  void shouldNotAppendUpdateLink() {
    GlobalMirrorConfiguration input = new GlobalMirrorConfiguration();

    final GlobalMirrorConfigurationDto output = mapper.map(input);

    assertThat(output.getLinks().getLinkBy("update")).isNotPresent();
  }
}
