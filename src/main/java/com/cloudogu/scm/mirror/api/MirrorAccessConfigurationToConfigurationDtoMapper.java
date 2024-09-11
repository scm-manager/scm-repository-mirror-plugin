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

import com.cloudogu.scm.mirror.MirrorAccessConfiguration.CertificateCredential;
import com.cloudogu.scm.mirror.MirrorAccessConfiguration.UsernamePasswordCredential;
import com.cloudogu.scm.mirror.MirrorConfiguration;
import com.cloudogu.scm.mirror.MirrorConfigurationStore;
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

import static de.otto.edison.hal.Link.link;

@Mapper(uses = {MirrorProxyConfigurationMapper.class})
public abstract class MirrorAccessConfigurationToConfigurationDtoMapper {

  @Inject
  Provider<ScmPathInfoStore> scmPathInfoStore;

  @Mapping(ignore = true, target = "attributes")
  abstract MirrorAccessConfigurationDto map(MirrorConfiguration configuration, @Context Repository repository);

  @Mapping(target = "password", constant = MirrorConfigurationStore.DUMMY_PASSWORD)
  abstract UsernamePasswordCredentialDto map(UsernamePasswordCredential credential);

  @Mapping(target = "certificate", expression = "java(null)")
  @Mapping(target = "password", constant = MirrorConfigurationStore.DUMMY_PASSWORD)
  abstract CertificateCredentialDto map(CertificateCredential credential);

  @ObjectFactory
  MirrorAccessConfigurationDto createConfigurationDto(@Context Repository repository) {
    return new MirrorAccessConfigurationDto(createLinks(repository));
  }

  private Links createLinks(Repository repository) {
    String configurationUrl = createUrl(repository, "getAccessConfiguration");
    String manualSyncUrl = createUrl(repository, "syncMirror");

    Links.Builder builder = Links.linkingTo().self(configurationUrl);
    if (MirrorPermissions.hasRepositoryMirrorPermission(repository)) {
      builder.single(link("update", configurationUrl));
      builder.single(link("syncMirror", manualSyncUrl));
    }
    return builder.build();
  }

  private String createUrl(Repository repository, String methodName) {
    return new LinkBuilder(scmPathInfoStore.get().get(), MirrorRootResource.class, MirrorResource.class)
      .method("repository").parameters(repository.getNamespace(), repository.getName())
      .method(methodName).parameters()
      .href();
  }

  @VisibleForTesting
  void setScmPathInfoStore(Provider<ScmPathInfoStore> scmPathInfoStore) {
    this.scmPathInfoStore = scmPathInfoStore;
  }
}
