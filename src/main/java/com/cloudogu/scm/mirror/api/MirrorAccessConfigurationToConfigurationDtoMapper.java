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

import javax.inject.Inject;
import javax.inject.Provider;

import static com.cloudogu.scm.mirror.MirrorConfiguration.*;
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
