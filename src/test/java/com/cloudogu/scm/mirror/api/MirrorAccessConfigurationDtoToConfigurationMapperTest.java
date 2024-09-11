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

import com.cloudogu.scm.mirror.MirrorAccessConfiguration;
import com.cloudogu.scm.mirror.MirrorProxyConfiguration;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MirrorAccessConfigurationDtoToConfigurationMapperTest {

  private final MirrorAccessConfigurationDtoToConfigurationMapper mapper = Mappers.getMapper(MirrorAccessConfigurationDtoToConfigurationMapper.class);

  @Test
  void shouldMapToDao() {
    MirrorProxyConfigurationDto proxyConfiguration = new MirrorProxyConfigurationDto();
    proxyConfiguration.setHost("foo.bar");
    proxyConfiguration.setPort(1337);
    proxyConfiguration.setUsername("trillian");
    proxyConfiguration.setPassword("secret123");

    final MirrorAccessConfigurationDto input = new MirrorAccessConfigurationDto();
    input.setUrl("https://foo.bar");
    input.setManagingUsers(ImmutableList.of("freddy", "bernard", "harold"));
    input.setSynchronizationPeriod(42);
    input.setUsernamePasswordCredential(new UsernamePasswordCredentialDto("trillian", "secretpassword"));
    input.setCertificateCredential(new CertificateCredentialDto("aGVsbG8=", "evenmoresecretpassword"));
    input.setProxyConfiguration(proxyConfiguration);

    final MirrorAccessConfiguration output = mapper.map(input);

    assertThat(output.getUrl()).isEqualTo("https://foo.bar");
    assertThat(output.getManagingUsers()).contains("freddy", "bernard", "harold");
    assertThat(output.getSynchronizationPeriod()).isEqualTo(42);
    assertThat(output.getUsernamePasswordCredential()).isNotNull();
    assertThat(output.getUsernamePasswordCredential().getPassword()).isEqualTo("secretpassword");
    assertThat(output.getUsernamePasswordCredential().getUsername()).isEqualTo("trillian");
    assertThat(output.getCertificateCredential()).isNotNull();
    assertThat(output.getCertificateCredential().getPassword()).isEqualTo("evenmoresecretpassword");
    assertThat(output.getCertificateCredential().getCertificate()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    assertThat(output.getProxyConfiguration()).isNotNull();
    final MirrorProxyConfiguration outputProxyConfiguration = output.getProxyConfiguration();
    assertThat(outputProxyConfiguration.getHost()).isEqualTo("foo.bar");
    assertThat(outputProxyConfiguration.getPort()).isEqualTo(1337);
    assertThat(outputProxyConfiguration.getUsername()).isEqualTo("trillian");
    assertThat(outputProxyConfiguration.getPassword()).isEqualTo("secret123");
    assertThat(outputProxyConfiguration.getExcludes()).isNotNull();
  }

}
