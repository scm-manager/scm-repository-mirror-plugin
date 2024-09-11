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
import com.cloudogu.scm.mirror.MirrorPermissions;
import com.google.common.annotations.VisibleForTesting;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.Repository;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.List;

import static de.otto.edison.hal.Link.link;

@Mapper(uses = RawGpgKeyToKeyDtoMapper.class)
public abstract class MirrorFilterConfigurationToDtoMapper {

  @Inject
  Provider<ScmPathInfoStore> scmPathInfoStore;

  @Mapping(target = "branchesAndTagsPatterns")
  String map(List<String> value) {
    return String.join(",", value);
  }

  @Mapping(ignore = true, target = "attributes")
  abstract LocalMirrorFilterConfigurationDto map(LocalFilterConfiguration configuration, @Context Repository repository);

  @ObjectFactory
  LocalMirrorFilterConfigurationDto createConfigurationDto(@Context Repository repository) {
    return new LocalMirrorFilterConfigurationDto(createLinks(repository));
  }

  private Links createLinks(Repository repository) {
    String configurationUrl = new LinkBuilder(scmPathInfoStore.get().get(), MirrorRootResource.class, MirrorResource.class)
      .method("repository").parameters(repository.getNamespace(), repository.getName())
      .method("getFilterConfiguration").parameters()
      .href();

    Links.Builder builder = Links.linkingTo().self(configurationUrl);
    if (MirrorPermissions.hasRepositoryMirrorPermission(repository)) {
      builder.single(link("update", configurationUrl));
    }
    return builder.build();
  }

  @VisibleForTesting
  void setScmPathInfoStore(Provider<ScmPathInfoStore> scmPathInfoStore) {
    this.scmPathInfoStore = scmPathInfoStore;
  }
}
