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

class ConfigurableFilter implements MirrorFilter {

  private final MirrorConfiguration configuration;
  private final Collection<String> keysIds;

  public ConfigurableFilter(MirrorConfiguration configuration, List<PublicKey> keys) {
    this.configuration = configuration;
    this.keysIds = collectKeyIds(keys);
  }

  @Override
  public Filter getFilter(FilterContext context) {
    return new Filter() {
      @Override
      public boolean acceptBranch(BranchUpdate branch) {
        return matchesPattern(branch.getBranchName()) &&
          (configuration.getGpgVerificationType() == NONE || hasAcceptedSignature(branch.getChangeset().getSignatures()));
      }

      @Override
      public boolean acceptTag(TagUpdate tag) {
        return matchesPattern(tag.getTagName()) &&
          (configuration.getGpgVerificationType() == NONE || hasAcceptedSignature(tag.getTag().getSignatures()));
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
}
