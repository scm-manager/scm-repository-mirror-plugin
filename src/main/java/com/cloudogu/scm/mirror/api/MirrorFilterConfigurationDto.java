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

import com.cloudogu.scm.mirror.MirrorGpgVerificationType;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("java:S2160") // equals not needed here
public class MirrorFilterConfigurationDto extends HalRepresentation {

  private String branchesAndTagsPatterns;

  private MirrorGpgVerificationType gpgVerificationType;
  private List<RawGpgKeyDto> allowedGpgKeys;
  private boolean fastForwardOnly;
  private boolean ignoreLfs;

  MirrorFilterConfigurationDto(Links links) {
    super(links);
  }

}
