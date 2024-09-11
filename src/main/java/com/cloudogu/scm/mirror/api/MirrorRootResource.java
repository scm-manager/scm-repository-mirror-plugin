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
