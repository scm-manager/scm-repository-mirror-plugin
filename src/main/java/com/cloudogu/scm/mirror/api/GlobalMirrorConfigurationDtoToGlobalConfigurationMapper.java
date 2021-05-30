package com.cloudogu.scm.mirror.api;

import com.cloudogu.scm.mirror.GlobalMirrorConfiguration;
import org.mapstruct.Mapper;

@Mapper
public abstract class GlobalMirrorConfigurationDtoToGlobalConfigurationMapper {
  abstract GlobalMirrorConfiguration map(GlobalMirrorConfigurationDto configurationDto);
}
