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

import com.cloudogu.scm.mirror.MirrorConfiguration;
import com.cloudogu.scm.mirror.MirrorGpgVerificationType;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MirrorConfigurationDtoToConfigurationMapperTest {

  private final MirrorConfigurationDtoToConfigurationMapper mapper = Mappers.getMapper(MirrorConfigurationDtoToConfigurationMapper.class);

  @Test
  void shouldMapToDao() {
    final MirrorConfigurationDto input = new MirrorConfigurationDto();
    input.setUrl("https://foo.bar");
    input.setManagingUsers(ImmutableList.of("freddy", "bernard", "harold"));
    input.setSynchronizationPeriod(42);
    input.setUsernamePasswordCredential(new MirrorConfigurationDto.UsernamePasswordCredentialDto("trillian", "secretpassword"));
    input.setCertificateCredential(new MirrorConfigurationDto.CertificateCredentialDto("aGVsbG8=", "evenmoresecretpassword"));
    input.setBranchesAndTagsPatterns("default, feature/*, ,,,");
    input.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
    input.setAllowedGpgKeys(ImmutableList.of(new RawGpgKeyDto("foo", "bar")));
    input.setFastForwardOnly(true);

    final MirrorConfiguration output = mapper.map(input);

    assertThat(output.getUrl()).isEqualTo("https://foo.bar");
    assertThat(output.getManagingUsers()).contains("freddy", "bernard", "harold");
    assertThat(output.getSynchronizationPeriod()).isEqualTo(42);
    assertThat(output.getUsernamePasswordCredential()).isNotNull();
    assertThat(output.getUsernamePasswordCredential().getPassword()).isEqualTo("secretpassword");
    assertThat(output.getUsernamePasswordCredential().getUsername()).isEqualTo("trillian");
    assertThat(output.getCertificateCredential()).isNotNull();
    assertThat(output.getCertificateCredential().getPassword()).isEqualTo("evenmoresecretpassword");
    assertThat(output.getCertificateCredential().getCertificate()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    assertThat(output.getBranchesAndTagsPatterns()).contains("default", "feature/*");
    assertThat(output.getGpgVerificationType()).isEqualTo(MirrorGpgVerificationType.KEY_LIST);
    assertThat(output.getAllowedGpgKeys().get(0).getDisplayName()).isEqualTo("foo");
    assertThat(output.getAllowedGpgKeys().get(0).getRaw()).isEqualTo("bar");
    assertThat(output.isFastForwardOnly()).isTrue();
  }

  @Test
  void shouldHandleEmptyPatternList() {
    final MirrorConfigurationDto input = new MirrorConfigurationDto();
    input.setUrl("https://foo.bar");
    input.setBranchesAndTagsPatterns(null);
    input.setGpgVerificationType(MirrorGpgVerificationType.NONE);

    final MirrorConfiguration output = mapper.map(input);

    assertThat(output.getBranchesAndTagsPatterns()).isEmpty();
  }
}
