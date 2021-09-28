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

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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
