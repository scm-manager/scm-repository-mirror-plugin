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
