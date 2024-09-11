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

import com.github.legman.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.shiro.SecurityUtils;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.ReadOnlyCheck;
import sonia.scm.repository.ReadOnlyException;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookFeature;
import sonia.scm.security.Authentications;
import sonia.scm.util.GlobUtil;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static sonia.scm.ContextEntry.ContextBuilder.entity;

@Extension
@Singleton
class MirrorReadOnlyCheck implements ReadOnlyCheck {

  private static final Collection<String> FORBIDDEN_PERMISSIONS = asList("push", "archive", "createPullRequest");

  private final MirrorConfigurationStore configurationStore;

  @Inject
  MirrorReadOnlyCheck(MirrorConfigurationStore configurationStore) {
    this.configurationStore = configurationStore;
  }

  @Override
  public String getReason() {
    return "repository is a mirror";
  }

  @Override
  public boolean isReadOnly(String repositoryId) {
    return false;
  }

  @Override
  public boolean isReadOnly(String permission, String repositoryId) {
    return FORBIDDEN_PERMISSIONS.contains(permission)
      && configurationStore.isReadOnly(repositoryId);
  }

  @Subscribe(async = false)
  public void checkForReadOnlyBranch(PreReceiveRepositoryHookEvent event) {
    if (Authentications.PRINCIPAL_SYSTEM.equals(SecurityUtils.getSubject().getPrincipal().toString())) {
      return;
    }
    Optional<MirrorConfiguration> configuration = configurationStore.getConfiguration(event.getRepository());
    if (configuration.isPresent() && event.getContext().isFeatureSupported(HookFeature.BRANCH_PROVIDER)) {
      List<String> branchesAndTagsPatterns = configuration.get().getBranchesAndTagsPatterns();
      if (branchesAndTagsPatterns.isEmpty() || isReadOnly(event, branchesAndTagsPatterns)) {
        throw new ReadOnlyException(entity(event.getRepository()).build(), getReason());
      }
    }
  }

  private boolean isReadOnly(PreReceiveRepositoryHookEvent event, List<String> branchesAndTagsPatterns) {
    HookBranchProvider branchProvider = event.getContext().getBranchProvider();
    return Stream.concat(
      branchProvider.getCreatedOrModified().stream(),
      branchProvider.getDeletedOrClosed().stream()
    ).anyMatch(branch -> branchesAndTagsPatterns.stream().anyMatch(globPattern -> GlobUtil.matches(globPattern, branch)));
  }
}
