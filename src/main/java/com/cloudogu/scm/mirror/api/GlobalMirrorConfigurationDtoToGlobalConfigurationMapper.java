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

import com.cloudogu.scm.mirror.GlobalMirrorConfiguration;
import com.google.common.base.Splitter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper( uses = RawGpgKeyDtoToKeyMapper.class)
public abstract class GlobalMirrorConfigurationDtoToGlobalConfigurationMapper {
  abstract GlobalMirrorConfiguration map(GlobalMirrorConfigurationDto configurationDto);

  @Mapping(target = "branchesAndTagsPatterns")
  List<String> map(String value) {
    return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(value);
  }
}
