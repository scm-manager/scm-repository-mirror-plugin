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

import com.cloudogu.scm.mirror.LocalFilterConfigurationImpl;
import com.cloudogu.scm.mirror.MirrorFilterConfigurationImpl;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

import static java.util.Collections.emptyList;

@Mapper( uses = RawGpgKeyDtoToKeyMapper.class)
public abstract class MirrorFilterConfigurationDtoToDaoMapper {

  abstract LocalFilterConfigurationImpl map(LocalMirrorFilterConfigurationDto configurationDto);

  @Mapping(target = "branchesAndTagsPatterns")
  List<String> map(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return emptyList();
    }
    return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(value);
  }

}
