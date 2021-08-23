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

import com.cloudogu.scm.mirror.MirrorProxyConfiguration;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SubjectAware(value = "trillian")
@ExtendWith(ShiroExtension.class)
class MirrorProxyConfigurationMapperTest {

  private Repository repository;
  private final MirrorProxyConfigurationMapper mapper = Mappers.getMapper(MirrorProxyConfigurationMapper.class);

  @BeforeEach
  void setup() {
    final ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    scmPathInfoStore.set(() -> URI.create("/scm"));

    repository = RepositoryTestData.createHeartOfGold();
    repository.setId("42");
  }

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

    final MirrorProxyConfigurationDto output = mapper.map(input, repository);
    assertThat(output.getHost()).isEqualTo("foo.bar");
    assertThat(output.getPort()).isEqualTo(1337);
    assertThat(output.getUsername()).isEqualTo("trillian");
    assertThat(output.getPassword()).isEqualTo("secret123");
    assertThat(output.isOverwriteGlobalConfiguration()).isTrue();
  }

}