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
import com.cloudogu.scm.mirror.MirrorGpgVerificationType;
import com.cloudogu.scm.mirror.MirrorProxyConfiguration;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class MirrorCreationDtoMapperTest {

  private final MirrorCreationDtoMapper mapper = Mappers.getMapper(MirrorCreationDtoMapper.class);

  @Test
  void shouldMapToDto() {
    MirrorProxyConfigurationDto mirrorProxyConfigurationDto = new MirrorProxyConfigurationDto();
    mirrorProxyConfigurationDto.setHost("foo.bar");
    mirrorProxyConfigurationDto.setPort(1337);
    mirrorProxyConfigurationDto.setUsername("admin");
    mirrorProxyConfigurationDto.setPassword("secret123");

    MirrorCreationDto input = new MirrorCreationDto();
    input.setUrl("https://foo.bar");
    input.setManagingUsers(ImmutableList.of("freddy", "bernard", "harold"));
    input.setSynchronizationPeriod(42);
    input.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
    input.setAllowedGpgKeys(ImmutableList.of(new RawGpgKeyDto("foo", "bar")));
    input.setFastForwardOnly(true);
    input.setIgnoreLfs(true);
    input.setBranchesAndTagsPatterns("foo,bar");
    input.setContact("bruno");
    input.setProxyConfiguration(mirrorProxyConfigurationDto);

    final MirrorConfiguration output = mapper.mapToConfiguration(input);

    assertThat(output.getGpgVerificationType()).isEqualTo(MirrorGpgVerificationType.KEY_LIST);
    assertThat(output.getAllowedGpgKeys().get(0).getDisplayName()).isEqualTo("foo");
    assertThat(output.getAllowedGpgKeys().get(0).getRaw()).isEqualTo("bar");
    assertThat(output.isFastForwardOnly()).isTrue();
    assertThat(output.isIgnoreLfs()).isTrue();
    assertThat(output.getBranchesAndTagsPatterns()).contains("foo", "bar");
    assertThat(output.getUrl()).isEqualTo("https://foo.bar");
    assertThat(output.getProxyConfiguration()).isNotNull();
    final MirrorProxyConfiguration proxyConfiguration = output.getProxyConfiguration();
    assertThat(proxyConfiguration.getHost()).isEqualTo("foo.bar");
    assertThat(proxyConfiguration.getPort()).isEqualTo(1337);
    assertThat(proxyConfiguration.getUsername()).isEqualTo("admin");
    assertThat(proxyConfiguration.getPassword()).isEqualTo("secret123");
    assertThat(proxyConfiguration.getExcludes()).isNotNull();
  }
}
