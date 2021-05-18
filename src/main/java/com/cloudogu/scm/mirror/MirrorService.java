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
import sonia.scm.repository.api.MirrorCommandBuilder;

import javax.inject.Inject;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

public class MirrorService {

  private final RepositoryManager manager;
  private final MirrorConfigurationStore configurationStore;
  private final MirrorWorker mirrorWorker;
  private final MirrorStatusStore statusStore;

  @Inject
  MirrorService(RepositoryManager manager,
                MirrorConfigurationStore configurationStore,
                MirrorWorker mirrorWorker,
                MirrorStatusStore statusStore) {
    this.manager = manager;
    this.mirrorWorker = mirrorWorker;
    this.configurationStore = configurationStore;
    this.statusStore = statusStore;
  }

  public Repository createMirror(MirrorConfiguration configuration, Repository repository) {
    RepositoryPermissions.create().check();
    checkMirrorSupport(repository);

    RepositoryPermission ownerPermission = new RepositoryPermission(
      SecurityUtils.getSubject().getPrincipal().toString(),
      "OWNER",
      false);
    repository.setPermissions(singletonList(ownerPermission));

    return manager.create(
      repository,
      createMirrorCallback(configuration)
    );
  }

  public void updateMirror(Repository repository) {
    MirrorPermissions.checkMirrorPermission(repository);
    MirrorConfiguration configuration = configurationStore.getConfiguration(repository);
    mirrorWorker.withMirrorCommandDo(repository, configuration, MirrorCommandBuilder::update);
  }

  private Consumer<Repository> createMirrorCallback(MirrorConfiguration configuration) {
    return repository -> {
      configurationStore.setConfiguration(repository, configuration);
      statusStore.setStatus(repository, MirrorStatus.initialStatus());
      mirrorWorker.withMirrorCommandDo(repository, configuration, MirrorCommandBuilder::initialCall);
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
