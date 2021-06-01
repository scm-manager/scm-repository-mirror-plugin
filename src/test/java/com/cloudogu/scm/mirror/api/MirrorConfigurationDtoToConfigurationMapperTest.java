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
    input.setBranchesAndTagsPatterns(ImmutableList.of("default", "feature/*"));
    input.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
    input.setAllowedGpgKeys(ImmutableList.of(new RawGpgKeyDto("foo", "bar")));

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
  }

}
