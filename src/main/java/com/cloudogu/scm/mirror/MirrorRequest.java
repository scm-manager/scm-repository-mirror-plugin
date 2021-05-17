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

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

import static java.util.Collections.emptyList;

@Getter
@Setter
public class MirrorRequest {

  private String url;

  private Collection<Credential> credentials = emptyList();

  interface Credential {
     sonia.scm.repository.api.Credential toCommandCredential();
  }

  @Getter
  @Setter
  static class UsernamePasswordCredential implements Credential, sonia.scm.repository.api.UsernamePasswordCredential {
    private String username;
    private String password;

    @Override
    public sonia.scm.repository.api.Credential toCommandCredential() {
      return this;
    }

    @Override
    public String username() {
      return username;
    }

    @Override
    public char[] password() {
      return password.toCharArray();
    }
  }

  @Getter
  @Setter
  static class CertificateCredential implements Credential {
    private byte[] certificate;
    private String password;

    @Override
    public sonia.scm.repository.api.Credential toCommandCredential() {
      return null;
    }
  }
}
