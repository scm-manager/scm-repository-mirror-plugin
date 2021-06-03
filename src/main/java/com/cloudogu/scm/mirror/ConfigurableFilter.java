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

import sonia.scm.repository.Signature;
import sonia.scm.repository.SignatureStatus;
import sonia.scm.repository.api.MirrorFilter;
import sonia.scm.security.PublicKey;
import sonia.scm.util.GlobUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static com.cloudogu.scm.mirror.MirrorGpgVerificationType.NONE;

@SuppressWarnings("UnstableApiUsage")
class ConfigurableFilter implements MirrorFilter {

  public static final String MESSAGE_PATTERN_NOT_MATCHED = "skipped: does not match configured patterns";
  public static final String MESSAGE_NO_VALID_SIGNATURE = "skipped: no valid signature";
  public static final String MESSAGE_NO_FAST_FORWARD = "skipped: no fast forward";

  private final MirrorConfiguration configuration;
  private final Collection<String> keysIds;

  private boolean issuesFound = false;

  public ConfigurableFilter(MirrorConfiguration configuration, List<PublicKey> keys) {
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
          return Result.accept();
        }
        return checkForAcceptedSignature(branch.getChangeset().getSignatures());
      }

      @Override
      public Result acceptTag(TagUpdate tag) {
        if (!matchesPattern(tag.getTagName())) {
          return Result.reject(MESSAGE_PATTERN_NOT_MATCHED);
        }
        if (configuration.getGpgVerificationType() == NONE) {
          return Result.accept();
        }
        return checkForAcceptedSignature(tag.getTag().getSignatures());
      }

      private Result checkForAcceptedSignature(List<Signature> signatures) {
        boolean result = hasAcceptedSignature(signatures);
        issuesFound |= !result;
        return result? Result.accept(): Result.reject(MESSAGE_NO_VALID_SIGNATURE);
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
