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

import javax.inject.Inject;
import javax.inject.Singleton;
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
