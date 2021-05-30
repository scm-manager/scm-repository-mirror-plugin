package com.cloudogu.scm.mirror.api;

import com.cloudogu.scm.mirror.GlobalMirrorConfiguration;
import com.cloudogu.scm.mirror.MirrorPermissions;
import de.otto.edison.hal.Links;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;

import javax.inject.Inject;
import javax.inject.Provider;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class GlobalMirrorConfigurationToGlobalConfigurationDtoMapper {

  @Inject
  Provider<ScmPathInfoStore> scmPathInfoStore;

  abstract GlobalMirrorConfigurationDto map(GlobalMirrorConfiguration configuration);

  @ObjectFactory
  GlobalMirrorConfigurationDto createGlobalConfigurationDto() {
    String configurationUrl = new LinkBuilder(scmPathInfoStore.get().get(), MirrorRootResource.class)
      .method("getGlobalMirrorConfiguration").parameters()
      .href();
    return new GlobalMirrorConfigurationDto(createLinks(configurationUrl));
  }

  private Links createLinks(String configurationUrl) {
    Links.Builder builder = Links.linkingTo().self(configurationUrl);
    if (MirrorPermissions.hasGlobalMirrorPermission()) {
      builder.single(link("update", configurationUrl));
    }
    return builder.build();
  }
}
