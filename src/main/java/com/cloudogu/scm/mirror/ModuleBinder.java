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

import com.cloudogu.scm.mirror.api.GlobalMirrorConfigurationToGlobalConfigurationDtoMapper;
import com.cloudogu.scm.mirror.api.MirrorAccessConfigurationToConfigurationDtoMapper;
import com.cloudogu.scm.mirror.api.MirrorFilterConfigurationToDtoMapper;
import com.google.inject.AbstractModule;
import org.mapstruct.factory.Mappers;
import sonia.scm.plugin.Extension;

@Extension
public class ModuleBinder extends AbstractModule {
  @Override
  protected void configure() {
    bind(GlobalMirrorConfigurationToGlobalConfigurationDtoMapper.class)
      .to(Mappers.getMapperClass(GlobalMirrorConfigurationToGlobalConfigurationDtoMapper.class));
    bind(MirrorAccessConfigurationToConfigurationDtoMapper.class)
      .to(Mappers.getMapperClass(MirrorAccessConfigurationToConfigurationDtoMapper.class));
    bind(MirrorFilterConfigurationToDtoMapper.class)
      .to(Mappers.getMapperClass(MirrorFilterConfigurationToDtoMapper.class));
  }
}
