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

import com.cloudogu.scm.mirror.LocalFilterConfiguration;
import com.cloudogu.scm.mirror.MirrorFilterConfiguration;
import com.cloudogu.scm.mirror.MirrorGpgVerificationType;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class MirrorFilterConfigurationDtoToDaoMapperTest {

  private final MirrorFilterConfigurationDtoToDaoMapper mapper = Mappers.getMapper(MirrorFilterConfigurationDtoToDaoMapper.class);

  @Test
  void shouldMapToDao() {
    final LocalMirrorFilterConfigurationDto input = new LocalMirrorFilterConfigurationDto();
    input.setBranchesAndTagsPatterns("default, feature/*, ,,,");
    input.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
    input.setAllowedGpgKeys(ImmutableList.of(new RawGpgKeyDto("foo", "bar")));
    input.setFastForwardOnly(true);

    final LocalFilterConfiguration output = mapper.map(input);

    assertThat(output.getBranchesAndTagsPatterns()).contains("default", "feature/*");
    assertThat(output.getGpgVerificationType()).isEqualTo(MirrorGpgVerificationType.KEY_LIST);
    assertThat(output.getAllowedGpgKeys().get(0).getDisplayName()).isEqualTo("foo");
    assertThat(output.getAllowedGpgKeys().get(0).getRaw()).isEqualTo("bar");
    assertThat(output.isFastForwardOnly()).isTrue();
  }

  @Test
  void shouldHandleEmptyPatternList() {
    final LocalMirrorFilterConfigurationDto input = new LocalMirrorFilterConfigurationDto();
    input.setBranchesAndTagsPatterns(null);
    input.setGpgVerificationType(MirrorGpgVerificationType.NONE);

    final MirrorFilterConfiguration output = mapper.map(input);

    assertThat(output.getBranchesAndTagsPatterns()).isEmpty();
  }

}
