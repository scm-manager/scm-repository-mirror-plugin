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
import com.cloudogu.scm.mirror.LogEntry;
import com.cloudogu.scm.mirror.LogStore;
import com.cloudogu.scm.mirror.MirrorAccessConfiguration;
import com.cloudogu.scm.mirror.MirrorConfiguration;
import com.cloudogu.scm.mirror.MirrorConfigurationStore;
import com.cloudogu.scm.mirror.MirrorService;
import com.cloudogu.scm.mirror.NotConfiguredForMirrorException;
import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.web.api.DtoValidator;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

import static org.mapstruct.factory.Mappers.getMapper;

public class MirrorResource {

  private final MirrorConfigurationStore configurationService;
  private final MirrorService mirrorService;
  private final RepositoryManager repositoryManager;
  private final LogStore logStore;

  private final MirrorAccessConfigurationDtoToConfigurationMapper fromAccessConfigurationDtoMapper = getMapper(MirrorAccessConfigurationDtoToConfigurationMapper.class);
  private final MirrorAccessConfigurationToConfigurationDtoMapper toAccessConfigurationDtoMapper;
  private final MirrorFilterConfigurationDtoToDaoMapper fromFiltersDtoMapper = getMapper(MirrorFilterConfigurationDtoToDaoMapper.class);
  private final MirrorFilterConfigurationToDtoMapper toFiltersDtoMapper;

  private final LogEntryMapper logEntryMapper = getMapper(LogEntryMapper.class);

  @Inject
  public MirrorResource(MirrorConfigurationStore configurationService,
                        MirrorService mirrorService,
                        RepositoryManager repositoryManager,
                        MirrorAccessConfigurationToConfigurationDtoMapper toAccessConfigurationDtoMapper,
                        LogStore logStore, MirrorFilterConfigurationToDtoMapper toFiltersDtoMapper) {
    this.configurationService = configurationService;
    this.mirrorService = mirrorService;
    this.repositoryManager = repositoryManager;
    this.logStore = logStore;
    this.toAccessConfigurationDtoMapper = toAccessConfigurationDtoMapper;
    this.toFiltersDtoMapper = toFiltersDtoMapper;
  }

  @GET
  @Path("/accessConfiguration")
  @Produces("application/json")
  public Response getAccessConfiguration(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    Repository repository = loadRepository(namespace, name);
    MirrorConfiguration configuration =
      configurationService.getConfiguration(repository)
      .orElseThrow(() -> new NotConfiguredForMirrorException(repository));
    return Response
      .status(200)
      .entity(toAccessConfigurationDtoMapper.map(configuration, repository))
      .build();
  }

  @PUT
  @Path("/accessConfiguration")
  @Consumes("application/json")
  public void setAccessConfiguration(@PathParam("namespace") String namespace, @PathParam("name") String name, @Valid MirrorAccessConfigurationDto configurationDto) {
    if (configurationDto.getProxyConfiguration().isOverwriteGlobalConfiguration()) {
      DtoValidator.validate(configurationDto.getProxyConfiguration());
    }
    MirrorAccessConfiguration configuration = fromAccessConfigurationDtoMapper.map(configurationDto);
    Repository repository = loadRepository(namespace, name);
    configurationService.setAccessConfiguration(repository, configuration);
  }

  @GET
  @Path("/filterConfiguration")
  @Produces("application/json")
  public Response getFilterConfiguration(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    Repository repository = loadRepository(namespace, name);
    MirrorConfiguration configuration =
      configurationService.getConfiguration(repository)
        .orElseThrow(() -> new NotConfiguredForMirrorException(repository));
    return Response
      .status(200)
      .entity(toFiltersDtoMapper.map(configuration, repository))
      .build();
  }

  @PUT
  @Path("/filterConfiguration")
  @Consumes("application/json")
  public void setFilterConfiguration(@PathParam("namespace") String namespace, @PathParam("name") String name, @Valid LocalMirrorFilterConfigurationDto filtersDto) {
    LocalFilterConfiguration configuration = fromFiltersDtoMapper.map(filtersDto);
    Repository repository = loadRepository(namespace, name);
    configurationService.setFilterConfiguration(repository, configuration);
  }

  @POST
  @Path("/sync")
  public void syncMirror(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    mirrorService.updateMirror(loadRepository(namespace, name));
  }

  @POST
  @Path("/unmirror")
  public void unmirror(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    mirrorService.unmirror(loadRepository(namespace, name));
  }

  @GET
  @Path("/logs")
  @Produces("application/json")
  public HalRepresentation getLogs(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name) {
    Repository repository = loadRepository(namespace, name);
    List<LogEntry> entries = logStore.get(repository);
    List<LogEntryDto> dtos = entries.stream().map(logEntryMapper::map).collect(Collectors.toList());

    Links links = Links.linkingTo().self(uriInfo.getAbsolutePath().toASCIIString()).build();
    Embedded embedded = Embedded.embedded("entries", dtos);

    return new HalRepresentation(links, embedded);
  }

  private Repository loadRepository(String namespace, String name) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    Repository repository = repositoryManager.get(namespaceAndName);
    if (repository == null) {
      throw new NotFoundException(Repository.class, namespaceAndName.toString());
    }
    return repository;
  }

}
