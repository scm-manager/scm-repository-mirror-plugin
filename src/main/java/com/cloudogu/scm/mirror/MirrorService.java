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

import org.apache.shiro.SecurityUtils;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryHandler;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryPermission;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.RepositoryType;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

class MirrorService {

  private final RepositoryManager manager;
  private final RepositoryServiceFactory repositoryServiceFactory;
  private final MirrorConfigurationService configurationService;

  @Inject
  MirrorService(RepositoryManager manager, RepositoryServiceFactory repositoryServiceFactory, MirrorConfigurationService configurationService) {
    this.manager = manager;
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.configurationService = configurationService;
  }

  Repository createMirror(MirrorRequest request, Repository repository) {
    RepositoryPermissions.create().check();
    RepositoryType t = type(manager, repository.getType());
    checkSupport(t, Command.PULL);

    RepositoryPermission ownerPermission = new RepositoryPermission(
      SecurityUtils.getSubject().getPrincipal().toString(),
      "OWNER",
      false);
    repository.setPermissions(singletonList(ownerPermission));

    Repository createdRepository;
    createdRepository = manager.create(
      repository,
      createMirrorCallback(request)
    );

    return createdRepository;
  }

  void updateMirror(Repository repository) {

  }

  Consumer<Repository> createMirrorCallback(MirrorRequest request) {
    return repository -> {
      try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
        repositoryService.getMirrorCommand().setSourceUrl(request.getUrl());
        configurationService.setConfiguration(repository, MirrorConfiguration.from(request));
      }
    };
  }

  private void checkSupport(RepositoryType type, Command cmd) {
    Set<Command> cmds = ((RepositoryType) type).getSupportedCommands();
    doThrow()
      .violation("type does not support command", "repository.type")
      .when(!cmds.contains(cmd));
  }

  private RepositoryType type(RepositoryManager manager, String type) {
    RepositoryHandler handler = manager.getHandler(type);
    doThrow()
      .violation("unknown repository type", "repository.type")
      .when(handler == null);
    return handler.getType();
  }
}
