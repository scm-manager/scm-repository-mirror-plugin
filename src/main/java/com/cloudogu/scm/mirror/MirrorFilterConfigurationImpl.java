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

package com.cloudogu.scm.mirror;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import java.util.Collections;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class MirrorFilterConfigurationImpl implements MirrorFilterConfiguration {

  private List<String> branchesAndTagsPatterns;
  private MirrorGpgVerificationType gpgVerificationType = MirrorGpgVerificationType.NONE;
  private List<RawGpgKey> allowedGpgKeys;
  private boolean fastForwardOnly = false;
  private boolean ignoreLfs = false;

  public List<String> getBranchesAndTagsPatterns() {
    return branchesAndTagsPatterns != null ? branchesAndTagsPatterns : Collections.emptyList();
  }

  public List<RawGpgKey> getAllowedGpgKeys() {
    return allowedGpgKeys != null ? allowedGpgKeys : Collections.emptyList();
  }
}
