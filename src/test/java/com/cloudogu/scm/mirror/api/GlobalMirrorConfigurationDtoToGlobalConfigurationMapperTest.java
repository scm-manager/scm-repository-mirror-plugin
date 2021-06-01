package com.cloudogu.scm.mirror.api;

import com.cloudogu.scm.mirror.GlobalMirrorConfiguration;
import com.cloudogu.scm.mirror.MirrorGpgVerificationType;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalMirrorConfigurationDtoToGlobalConfigurationMapperTest {

  private final GlobalMirrorConfigurationDtoToGlobalConfigurationMapper mapper = Mappers.getMapper(GlobalMirrorConfigurationDtoToGlobalConfigurationMapper.class);

  @Test
  void shouldMapToDao() {
    GlobalMirrorConfigurationDto input = new GlobalMirrorConfigurationDto();
    input.setHttpsOnly(true);
    input.setBranchesAndTagsPatterns(ImmutableList.of("default", "feature/*"));
    input.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
    input.setAllowedGpgKeys(ImmutableList.of(new RawGpgKeyDto("foo", "bar")));

    final GlobalMirrorConfiguration output = mapper.map(input);

    assertThat(output.isHttpsOnly()).isTrue();
    assertThat(output.getBranchesAndTagsPatterns()).contains("default", "feature/*");
    assertThat(output.getGpgVerificationType()).isEqualTo(MirrorGpgVerificationType.KEY_LIST);
    assertThat(output.getAllowedGpgKeys().get(0).getDisplayName()).isEqualTo("foo");
    assertThat(output.getAllowedGpgKeys().get(0).getRaw()).isEqualTo("bar");
  }

}
