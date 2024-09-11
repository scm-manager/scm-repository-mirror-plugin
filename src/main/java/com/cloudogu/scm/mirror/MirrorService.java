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

package com.cloudogu.scm.mirror;

import com.google.common.collect.ImmutableList;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryHandler;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryPermission;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.RepositoryType;
import sonia.scm.repository.api.Command;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

@Singleton
public class MirrorService {

  private static final Logger LOG = LoggerFactory.getLogger(MirrorService.class);

  private final RepositoryManager manager;
  private final MirrorConfigurationStore configurationStore;
  private final MirrorWorker mirrorWorker;

  @Inject
  MirrorService(RepositoryManager manager,
                MirrorConfigurationStore configurationStore,
                MirrorWorker mirrorWorker) {
    this.manager = manager;
    this.mirrorWorker = mirrorWorker;
    this.configurationStore = configurationStore;
  }

  public Repository createMirror(MirrorConfiguration configuration, Repository repository) {
    RepositoryPermissions.create().check();
    checkMirrorSupport(repository);

    LOG.debug("creating new repository as mirror");
    String currentUser = SecurityUtils.getSubject().getPrincipal().toString();
    configuration.setManagingUsers(ImmutableList.of(currentUser));
    RepositoryPermission ownerPermission = new RepositoryPermission(
      currentUser,
      "OWNER",
      false);
    repository.setPermissions(singletonList(ownerPermission));

    return manager.create(
      repository,
      createMirrorCallback(configuration)
    );
  }

  public void updateMirror(Repository repository) {
    MirrorPermissions.checkRepositoryMirrorPermission(repository);
    MirrorConfiguration configuration = configurationStore.getApplicableConfiguration(repository)
      .orElseThrow(() -> new NotConfiguredForMirrorException(repository));
    mirrorWorker.startUpdate(repository, configuration);
  }

  public void unmirror(Repository repository) {
    MirrorPermissions.checkRepositoryMirrorPermission(repository);
    configurationStore.deleteConfiguration(repository);
  }

  private Consumer<Repository> createMirrorCallback(MirrorConfiguration configuration) {
    return repository -> {
      LOG.info("created new repository {} as mirror; initializing", repository);
      configurationStore.setConfiguration(repository, configuration);
      mirrorWorker.startInitialSync(repository, configurationStore.getApplicableConfiguration(repository).get());
    };
  }

  private void checkMirrorSupport(Repository repository) {
    RepositoryType type = type(repository.getType());
    Set<Command> supportedCommands = type.getSupportedCommands();
    doThrow()
      .violation("type does not support command", "repository.type")
      .when(!supportedCommands.contains(Command.MIRROR));
  }

  @SuppressWarnings("ConstantConditions") // We suppress null check for handler here, because it cannot be null after the 'doThrow'
  private RepositoryType type(String type) {
    RepositoryHandler handler = manager.getHandler(type);
    doThrow()
      .violation("unknown repository type", "repository.type")
      .when(handler == null);
    return handler.getType();
  }
}
