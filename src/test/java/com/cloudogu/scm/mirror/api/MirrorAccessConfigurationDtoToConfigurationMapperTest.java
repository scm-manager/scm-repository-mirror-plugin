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
