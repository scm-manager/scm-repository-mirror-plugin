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
import com.cloudogu.scm.mirror.MirrorPermissions;
import com.google.common.annotations.VisibleForTesting;
import de.otto.edison.hal.Links;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.List;

import static de.otto.edison.hal.Link.link;

@Mapper( uses = RawGpgKeyToKeyDtoMapper.class)
public abstract class GlobalMirrorConfigurationToGlobalConfigurationDtoMapper {

  @Inject
  Provider<ScmPathInfoStore> scmPathInfoStore;

  @Mapping(ignore = true, target = "attributes")
  abstract GlobalMirrorConfigurationDto map(GlobalMirrorConfiguration configuration);

  @Mapping(target = "branchesAndTagsPatterns")
  String map(List<String> value) {
    return String.join(",", value);
  }

  @ObjectFactory
  GlobalMirrorConfigurationDto createGlobalConfigurationDto() {
    String configurationUrl = new LinkBuilder(scmPathInfoStore.get().get(), MirrorRootResource.class)
      .method("getGlobalMirrorConfiguration")
      .parameters()
      .href();
    return new GlobalMirrorConfigurationDto(createLinks(configurationUrl));
  }

  private Links createLinks(String configurationUrl) {
    Links.Builder builder = Links.linkingTo();
    if (MirrorPermissions.hasGlobalMirrorPermission()) {
      builder.self(configurationUrl);
      builder.single(link("update", configurationUrl));
    }
    return builder.build();
  }

  @VisibleForTesting
  void setScmPathInfoStore(Provider<ScmPathInfoStore> scmPathInfoStore) {
    this.scmPathInfoStore = scmPathInfoStore;
  }
}
