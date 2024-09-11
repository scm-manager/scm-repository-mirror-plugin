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

  @SubjectAware(permissions = "configuration:read,write:mirror")
  @Test
  void shouldMapToDto() {
    GlobalMirrorConfiguration input = new GlobalMirrorConfiguration();
    input.setHttpsOnly(true);
    input.setBranchesAndTagsPatterns(ImmutableList.of("default", "feature/*"));
    input.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
    input.setAllowedGpgKeys(ImmutableList.of(new RawGpgKey("foo", "bar")));
    input.setFastForwardOnly(true);

    final GlobalMirrorConfigurationDto output = mapper.map(input);

    assertThat(output.isHttpsOnly()).isTrue();
    assertThat(output.getBranchesAndTagsPatterns()).contains("default", "feature/*");
    assertThat(output.getGpgVerificationType()).isEqualTo(MirrorGpgVerificationType.KEY_LIST);
    assertThat(output.getAllowedGpgKeys().get(0).getDisplayName()).isEqualTo("foo");
    assertThat(output.getAllowedGpgKeys().get(0).getRaw()).isEqualTo("bar");
    assertThat(output.isFastForwardOnly()).isTrue();
  }

  @SubjectAware(permissions = "configuration:read,write:mirror")
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
