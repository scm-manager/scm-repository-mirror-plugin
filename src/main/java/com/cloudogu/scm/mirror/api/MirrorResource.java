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
import com.cloudogu.scm.mirror.MirrorConfigurationService;
import com.cloudogu.scm.mirror.MirrorService;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.NamespaceAndName;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static org.mapstruct.factory.Mappers.getMapper;

public class MirrorResource {

  private final MirrorConfigurationService configurationService;
  private final MirrorService mirrorService;
  private final MirrorConfigurationDtoToConfigurationMapper fromDtoMapper = getMapper(MirrorConfigurationDtoToConfigurationMapper.class);
  private final MirrorConfigurationToConfigurationDtoMapper toDtoMapper = getMapper(MirrorConfigurationToConfigurationDtoMapper.class);

  @Inject
  public MirrorResource(
    MirrorConfigurationService configurationService,
    MirrorService mirrorService,
    Provider<ScmPathInfoStore> scmPathInfoStore
  ) {
    this.configurationService = configurationService;
    this.mirrorService = mirrorService;
    toDtoMapper.scmPathInfoStore = scmPathInfoStore;
  }

  @GET
  @Path("/configuration")
  @Produces("application/json")
  public MirrorConfigurationDto getMirrorConfiguration(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    return toDtoMapper.map(configurationService.getConfiguration(namespace, name), new NamespaceAndName(namespace, name));
  }

  @POST
  @Path("/configuration")
  @Consumes("application/json")
  public void setMirrorConfiguration(@PathParam("namespace") String namespace, @PathParam("name") String name, @Valid MirrorConfigurationDto configurationDto) {
    MirrorConfiguration configuration = fromDtoMapper.map(configurationDto);
    configurationService.setConfiguration(namespace, name, configuration);
  }

  @POST
  @Path("/sync")
  public void syncMirror(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    mirrorService.updateMirror(namespace, name);
  }
}
