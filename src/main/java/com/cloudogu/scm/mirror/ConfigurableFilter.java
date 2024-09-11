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

import sonia.scm.repository.Signature;
import sonia.scm.repository.SignatureStatus;
import sonia.scm.repository.api.MirrorFilter;
import sonia.scm.security.PublicKey;
import sonia.scm.util.GlobUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static com.cloudogu.scm.mirror.MirrorGpgVerificationType.NONE;
import static sonia.scm.repository.api.MirrorFilter.Result.accept;

@SuppressWarnings("UnstableApiUsage")
class ConfigurableFilter implements MirrorFilter {

  public static final String MESSAGE_PATTERN_NOT_MATCHED = "skipped: does not match configured patterns";
  public static final String MESSAGE_NO_VALID_SIGNATURE = "skipped: no valid signature";
  public static final String MESSAGE_NO_FAST_FORWARD = "skipped: no fast forward";

  private final MirrorConfiguration configuration;
  private final Collection<String> keysIds;

  private boolean issuesFound = false;

  ConfigurableFilter(MirrorConfiguration configuration, List<PublicKey> keys) {
    this.configuration = configuration;
    this.keysIds = collectKeyIds(keys);
  }

  @Override
  public Filter getFilter(FilterContext context) {
    return new Filter() {
      @Override
      public Result acceptBranch(BranchUpdate branch) {
        if (!matchesPattern(branch.getBranchName())) {
          return Result.reject(MESSAGE_PATTERN_NOT_MATCHED);
        }
        if (configuration.isFastForwardOnly() && branch.isForcedUpdate()) {
          issuesFound = true;
          return Result.reject(MESSAGE_NO_FAST_FORWARD);
        }
        if (configuration.getGpgVerificationType() == NONE) {
          return accept();
        }
        return branch.getChangeset().map(c -> checkForAcceptedSignature(c.getSignatures())).orElseGet(Result::accept);
      }

      @Override
      public Result acceptTag(TagUpdate tag) {
        if (!matchesPattern(tag.getTagName())) {
          return Result.reject(MESSAGE_PATTERN_NOT_MATCHED);
        }
        if (configuration.getGpgVerificationType() == NONE) {
          return accept();
        }
        return tag.getTag().map(t -> checkForAcceptedSignature(t.getSignatures())).orElseGet(Result::accept);
      }

      private Result checkForAcceptedSignature(List<Signature> signatures) {
        boolean result = hasAcceptedSignature(signatures);
        issuesFound |= !result;
        return result? accept(): Result.reject(MESSAGE_NO_VALID_SIGNATURE);
      }

      private boolean hasAcceptedSignature(List<Signature> signatures) {
        switch (configuration.getGpgVerificationType()) {
          case NONE:
            return true;
          case SIGNATURE:
            return signatures.stream().anyMatch(signature -> hasValidSignature(signature));
          case KEY_LIST:
            return signatures.stream().anyMatch(signature -> hasSignatureFromList(signature));
          case SCM_USER_SIGNATURE:
            return signatures.stream().anyMatch(signature -> hasSignatureFromUser(signature));
          default:
            throw new IllegalStateException("unexpected verification type: " + configuration.getGpgVerificationType());
        }
      }
    };
  }

  private boolean matchesPattern(String name) {
    return configuration.getBranchesAndTagsPatterns().isEmpty() ||
      configuration.getBranchesAndTagsPatterns().stream().anyMatch(globPattern -> GlobUtil.matches(globPattern, name));
  }

  private boolean hasSignatureFromUser(Signature signature) {
    return signature.getStatus() == SignatureStatus.VERIFIED;
  }

  private boolean hasSignatureFromList(Signature signature) {
    return signature.getStatus() == SignatureStatus.VERIFIED && keysIds.contains(signature.getKeyId());
  }

  private boolean hasValidSignature(Signature signature) {
    return signature.getStatus() == SignatureStatus.VERIFIED || signature.getStatus() == SignatureStatus.NOT_FOUND;
  }

  private Collection<String> collectKeyIds(List<PublicKey> keys) {
    Collection<String> allKeys = new HashSet<>();
    keys.forEach(key -> {
      allKeys.add(key.getId());
      allKeys.addAll(key.getSubkeys());
    });
    return allKeys;
  }

  public boolean hadIssues() {
    return issuesFound;
  }
}
