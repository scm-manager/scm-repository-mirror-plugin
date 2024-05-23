/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
