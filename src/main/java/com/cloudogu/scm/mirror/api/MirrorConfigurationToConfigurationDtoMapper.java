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

import com.cloudogu.scm.mirror.MirrorConfiguration;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.NamespaceAndName;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Base64;

@Mapper
abstract class MirrorConfigurationToConfigurationDtoMapper {

  @Inject
  Provider<ScmPathInfoStore> scmPathInfoStore;

  abstract MirrorConfigurationDto map(MirrorConfiguration configuration, @Context NamespaceAndName namespaceAndName);

  abstract MirrorConfigurationDto.UsernamePasswordCredentialDto map(MirrorConfiguration.UsernamePasswordCredential credential);
  abstract MirrorConfigurationDto.CertificateCredentialDto map(MirrorConfiguration.CertificateCredential credential);

  @ObjectFactory
  MirrorConfigurationDto createConfigurationDto(@Context NamespaceAndName namespaceAndName) {
    String configurationUrl = new LinkBuilder(scmPathInfoStore.get().get(), MirrorRootResource.class, MirrorResource.class)
      .method("repository").parameters(namespaceAndName.getNamespace(), namespaceAndName.getName())
      .method("getMirrorConfiguration").parameters()
      .href();
    return new MirrorConfigurationDto(Links.linkingTo().self(configurationUrl).build());
  }

  String mapBase64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }
}
