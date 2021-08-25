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
    mirrorProxyConfigurationDto.setExcludes("  the  , best, test  ");

    MirrorCreationDto input = new MirrorCreationDto();
    input.setUrl("https://foo.bar");
    input.setManagingUsers(ImmutableList.of("freddy", "bernard", "harold"));
    input.setSynchronizationPeriod(42);
    input.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
    input.setAllowedGpgKeys(ImmutableList.of(new RawGpgKeyDto("foo", "bar")));
    input.setFastForwardOnly(true);
    input.setBranchesAndTagsPatterns("foo,bar");
    input.setContact("bruno");
    input.setProxyConfiguration(mirrorProxyConfigurationDto);

    final MirrorConfiguration output = mapper.mapToConfiguration(input);

    assertThat(output.getGpgVerificationType()).isEqualTo(MirrorGpgVerificationType.KEY_LIST);
    assertThat(output.getAllowedGpgKeys().get(0).getDisplayName()).isEqualTo("foo");
    assertThat(output.getAllowedGpgKeys().get(0).getRaw()).isEqualTo("bar");
    assertThat(output.isFastForwardOnly()).isTrue();
    assertThat(output.getBranchesAndTagsPatterns()).contains("foo", "bar");
    assertThat(output.getUrl()).isEqualTo("https://foo.bar");
    assertThat(output.getProxyConfiguration()).isNotNull();
    final MirrorProxyConfiguration proxyConfiguration = output.getProxyConfiguration();
    assertThat(proxyConfiguration.getHost()).isEqualTo("foo.bar");
    assertThat(proxyConfiguration.getPort()).isEqualTo(1337);
    assertThat(proxyConfiguration.getUsername()).isEqualTo("admin");
    assertThat(proxyConfiguration.getPassword()).isEqualTo("secret123");
    assertThat(proxyConfiguration.getExcludes()).contains("the","best","test");
  }
}
