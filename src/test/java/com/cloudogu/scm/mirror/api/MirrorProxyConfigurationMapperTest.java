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

import com.cloudogu.scm.mirror.MirrorProxyConfiguration;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class MirrorProxyConfigurationMapperTest {

  private final MirrorProxyConfigurationMapper mapper = Mappers.getMapper(MirrorProxyConfigurationMapper.class);

  @Test
  void shouldMapToDao() {
    MirrorProxyConfigurationDto input = new MirrorProxyConfigurationDto();
    input.setHost("foo.bar");
    input.setPort(1337);
    input.setUsername("trillian");
    input.setPassword("secret123");

    final MirrorProxyConfiguration output = mapper.map(input);
    assertThat(output.getHost()).isEqualTo("foo.bar");
    assertThat(output.getPort()).isEqualTo(1337);
    assertThat(output.getUsername()).isEqualTo("trillian");
    assertThat(output.getPassword()).isEqualTo("secret123");
  }

  @Test
  void shouldMapToDto() {
    MirrorProxyConfiguration input = new MirrorProxyConfiguration();
    input.setHost("foo.bar");
    input.setPort(1337);
    input.setUsername("trillian");
    input.setPassword("secret123");
    input.setOverwriteGlobalConfiguration(true);

    final MirrorProxyConfigurationDto output = mapper.map(input);
    assertThat(output.getHost()).isEqualTo("foo.bar");
    assertThat(output.getPort()).isEqualTo(1337);
    assertThat(output.getUsername()).isEqualTo("trillian");
    assertThat(output.getPassword()).isEqualTo("secret123");
    assertThat(output.isOverwriteGlobalConfiguration()).isTrue();
  }

}
