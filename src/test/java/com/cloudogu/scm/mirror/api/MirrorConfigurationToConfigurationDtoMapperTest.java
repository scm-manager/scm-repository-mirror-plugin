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
import com.cloudogu.scm.mirror.MirrorSourceUrlSupport;
import com.cloudogu.scm.mirror.MirrorGpgVerificationType;
import com.cloudogu.scm.mirror.RawGpgKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.util.Providers;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SubjectAware(value = "trillian")
@ExtendWith(ShiroExtension.class)
class MirrorConfigurationToConfigurationDtoMapperTest {
  private Repository repository;
  private final MirrorAccessConfigurationToConfigurationDtoMapper mapper = Mappers.getMapper(MirrorAccessConfigurationToConfigurationDtoMapper.class);

  @BeforeEach
  void setup() {
    final ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    scmPathInfoStore.set(() -> URI.create("/scm"));
    mapper.sourceUrlSupport = mock(MirrorSourceUrlSupport.class);
    mapper.setScmPathInfoStore(Providers.of(scmPathInfoStore));

    repository = RepositoryTestData.createHeartOfGold();
    repository.setId("42");
  }

  @SubjectAware(permissions = "repository:mirror:42")
  @Test
  void shouldMapToDto() {
    MirrorConfiguration input = new MirrorConfiguration();
    input.setUrl("https://foo.bar");
    input.setManagingUsers(ImmutableList.of("freddy", "bernard", "harold"));
    input.setSynchronizationPeriod(42);
    input.setUsernamePasswordCredential(new MirrorConfiguration.UsernamePasswordCredential("trillian", "secretpassword"));
    input.setCertificateCredential(new MirrorConfiguration.CertificateCredential("hello".getBytes(StandardCharsets.UTF_8), "evenmoresecretpassword"));
    input.setBranchesAndTagsPatterns(ImmutableList.of("default", "feature/*"));
    input.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
    input.setAllowedGpgKeys(ImmutableList.of(new RawGpgKey("foo", "bar")));
    input.setFastForwardOnly(true);

    final MirrorAccessConfigurationDto output = mapper.map(input, repository);

    assertThat(output.getUrl()).isEqualTo("https://foo.bar");
    assertThat(output.getManagingUsers()).contains("freddy", "bernard", "harold");
    assertThat(output.getSynchronizationPeriod()).isEqualTo(42);
    assertThat(output.getUsernamePasswordCredential()).isNotNull();
    assertThat(output.getUsernamePasswordCredential().getPassword()).isEqualTo("_DUMMY_");
    assertThat(output.getUsernamePasswordCredential().getUsername()).isEqualTo("trillian");
    assertThat(output.getCertificateCredential()).isNotNull();
    assertThat(output.getCertificateCredential().getPassword()).isEqualTo("_DUMMY_");
    assertThat(output.getCertificateCredential().getCertificate()).isNull();
  }

  @SubjectAware(permissions = "repository:mirror:42")
  @Test
  void shouldAppendUpdateLink() {
    MirrorConfiguration input = new MirrorConfiguration();
    final MirrorAccessConfigurationDto output = mapper.map(input, repository);
    assertThat(output.getLinks().getLinkBy("update"))
      .hasValueSatisfying(link -> assertThat(link.getHref()).isEqualTo("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration"));
  }

  @Test
  void shouldExposeCapabilityWithoutUpdatePermission() {
    when(mapper.sourceUrlSupport.isUrlModifiable(repository)).thenReturn(true);

    MirrorAccessConfigurationDto output = mapper.map(new MirrorConfiguration(), repository);

    assertThat(output.isUrlModifiable()).isTrue();
    assertThat(output.getLinks().getLinkBy("update")).isEmpty();
  }

  @Test
  void shouldReportUnsupportedSourceChange() {
    assertThat(mapper.map(new MirrorConfiguration(), repository).isUrlModifiable()).isFalse();
  }

  @Test
  void shouldIgnoreClientSuppliedCapability() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    MirrorAccessConfigurationDto input = objectMapper.readValue(
      "{\"url\":\"https://example.org/source\",\"urlModifiable\":true}", MirrorAccessConfigurationDto.class);

    assertThat(input.isUrlModifiable()).isFalse();
    input.setUrlModifiable(true);
    assertThat(objectMapper.readTree(objectMapper.writeValueAsString(input)).get("urlModifiable").asBoolean()).isTrue();
  }

  @Test
  void shouldNotAppendUpdateLink() {
    MirrorConfiguration input = new MirrorConfiguration();
    final MirrorAccessConfigurationDto output = mapper.map(input, repository);
    assertThat(output.getLinks().getLinkBy("update")).isNotPresent();
  }
}
