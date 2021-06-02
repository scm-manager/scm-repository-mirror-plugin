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

import org.junit.jupiter.api.Test;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.Signature;
import sonia.scm.repository.api.MirrorFilter;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static sonia.scm.repository.SignatureStatus.NOT_FOUND;

@SuppressWarnings("UnstableApiUsage")
class ConfigurableFilterTest {

  @Test
  void shouldAcceptAnySignatures() {
    MirrorConfiguration configuration = new MirrorConfiguration();
    configuration.setGpgVerificationType(MirrorGpgVerificationType.SIGNATURE);

    MirrorFilter.Filter filter = new ConfigurableFilter(configuration, emptyList()).getFilter(null);

    assertThat(filter.acceptBranch(branch("feature/hog", new Signature("dent", "gpg", NOT_FOUND, null, emptySet()))).isAccepted()).isTrue();
  }

  private MirrorFilter.BranchUpdate branch(String name, Signature signature) {
    return new MirrorFilter.BranchUpdate() {
      @Override
      public String getBranchName() {
        return name;
      }

      @Override
      public Changeset getChangeset() {
        Changeset changeset = new Changeset();
        changeset.setSignatures(singleton(signature));
        return changeset;
      }

      @Override
      public String getNewRevision() {
        return null;
      }

      @Override
      public Optional<String> getOldRevision() {
        return Optional.empty();
      }

      @Override
      public boolean isForcedUpdate() {
        return false;
      }
    };
  }
}
