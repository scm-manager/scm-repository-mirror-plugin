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
import com.cloudogu.scm.mirror.MirrorConfiguration;
import com.cloudogu.scm.mirror.MirrorConfigurationStore;
import com.cloudogu.scm.mirror.MirrorService;
import com.google.common.base.Strings;
import org.mapstruct.factory.Mappers;
import sonia.scm.api.v2.resources.RepositoryLinkProvider;
import sonia.scm.repository.Repository;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.net.URI;

import static org.mapstruct.factory.Mappers.getMapper;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

@Path("v2/mirror/")
public class MirrorRootResource {

  private final Provider<MirrorResource> mirrorResource;
  private final RepositoryLinkProvider repositoryLinkProvider;
  private final MirrorService mirrorService;
  private final MirrorConfigurationStore configurationService;
  private final GlobalMirrorConfigurationDtoToGlobalConfigurationMapper fromDtoMapper = getMapper(GlobalMirrorConfigurationDtoToGlobalConfigurationMapper.class);
  private final GlobalMirrorConfigurationToGlobalConfigurationDtoMapper toDtoMapper;
  private final MirrorCreationDtoMapper mirrorCreationDtoMapper = Mappers.getMapper(MirrorCreationDtoMapper.class);

  @Inject
  public MirrorRootResource(Provider<MirrorResource> mirrorResource, RepositoryLinkProvider repositoryLinkProvider, MirrorService mirrorService, MirrorConfigurationStore configurationService, GlobalMirrorConfigurationToGlobalConfigurationDtoMapper toDtoMapper) {
    this.mirrorResource = mirrorResource;
    this.repositoryLinkProvider = repositoryLinkProvider;
    this.mirrorService = mirrorService;
    this.configurationService = configurationService;
    this.toDtoMapper = toDtoMapper;
  }

  @POST
  @Path("/repositories")
  @Consumes("application/json")
  public Response createMirrorRepository(@Valid MirrorCreationDto requestDto) {
    doThrow().violation("certificate must not be empty")
      .when(
        requestDto.getCertificateCredential() != null && Strings.isNullOrEmpty(requestDto.getCertificateCredential().getCertificate())
      );

    Repository repository = mirrorCreationDtoMapper.mapToRepository(requestDto);
    MirrorConfiguration configuration = mirrorCreationDtoMapper.mapToConfiguration(requestDto);
    Repository mirror = mirrorService.createMirror(configuration, repository);
    return Response.created(URI.create(repositoryLinkProvider.get(mirror.getNamespaceAndName()))).build();
  }

  @Path("/repositories/{namespace}/{name}")
  public MirrorResource repository(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    return mirrorResource.get();
  }

  @GET
  @Path("/configuration")
  @Produces("application/json")
  public GlobalMirrorConfigurationDto getGlobalMirrorConfiguration() {
    GlobalMirrorConfiguration configuration =
      configurationService.getGlobalConfiguration();
    return toDtoMapper.map(configuration);
  }

  @PUT
  @Path("/configuration")
  @Consumes("application/json")
  public void setGlobalMirrorConfiguration(@Valid GlobalMirrorConfigurationDto configurationDto) {
    GlobalMirrorConfiguration configuration = fromDtoMapper.map(configurationDto);
    configurationService.setGlobalConfiguration(configuration);
  }
}
