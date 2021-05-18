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

package com.cloudogu.scm.mirror;

import org.mapstruct.factory.Mappers;
import sonia.scm.api.v2.resources.RepositoryLinkProvider;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("v2/mirror/")
public class MirrorRootResource {

  private final Provider<MirrorResource> mirrorResource;
  private final RepositoryLinkProvider repositoryLinkProvider;
  private final MirrorService mirrorService;

  // TODO check whether we should use the repository mapper from the core here
  private final MirrorRequestDtoToRepositoryMapper requestDtoToRepositoryMapper = Mappers.getMapper(MirrorRequestDtoToRepositoryMapper.class);
  private final MirrorConfigurationDtoToConfigurationMapper requestDtoToRequestMapper = Mappers.getMapper(MirrorConfigurationDtoToConfigurationMapper.class);

  @Inject
  public MirrorRootResource(Provider<MirrorResource> mirrorResource, RepositoryLinkProvider repositoryLinkProvider, MirrorService mirrorService) {
    this.mirrorResource = mirrorResource;
    this.repositoryLinkProvider = repositoryLinkProvider;
    this.mirrorService = mirrorService;
  }

  @POST
  @Path("/repositories")
  @Consumes("application/json")
  public Response createMirrorRepository(@Valid MirrorRequestDto requestDto) {
    Repository repository = requestDtoToRepositoryMapper.map(requestDto);
    MirrorConfiguration configuration = requestDtoToRequestMapper.map(requestDto);
    Repository mirror = mirrorService.createMirror(configuration, repository);
    return Response.created(URI.create(repositoryLinkProvider.get(mirror.getNamespaceAndName()))).build();
  }

  @Path("/repositories/{namespace}/{name}")
  public MirrorResource repository(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    return mirrorResource.get();
  }
}
