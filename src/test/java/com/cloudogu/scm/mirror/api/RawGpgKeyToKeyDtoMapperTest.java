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

import com.cloudogu.scm.mirror.RawGpgKey;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class RawGpgKeyToKeyDtoMapperTest {

  private RawGpgKeyToKeyDtoMapper mapper = Mappers.getMapper(RawGpgKeyToKeyDtoMapper.class);

  @Test
  void shouldMapRawGpgKeyToDto() {
    RawGpgKey input = new RawGpgKey("foo", "bar");
    final RawGpgKeyDto output = mapper.map(input);
    assertThat(output.getDisplayName()).isEqualTo("foo");
    assertThat(output.getRaw()).isEqualTo("bar");
  }

}
