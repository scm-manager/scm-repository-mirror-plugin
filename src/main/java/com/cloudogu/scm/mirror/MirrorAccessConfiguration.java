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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sonia.scm.auditlog.AuditEntry;
import sonia.scm.xml.XmlCipherByteArrayAdapter;
import sonia.scm.xml.XmlCipherStringAdapter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;

public interface MirrorAccessConfiguration {
  String getUrl();

  Integer getSynchronizationPeriod();

  List<String> getManagingUsers();

  MirrorAccessConfiguration.UsernamePasswordCredential getUsernamePasswordCredential();

  MirrorAccessConfiguration.CertificateCredential getCertificateCredential();

  MirrorProxyConfiguration getProxyConfiguration();

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @XmlAccessorType(XmlAccessType.FIELD)
  class UsernamePasswordCredential implements sonia.scm.repository.api.UsernamePasswordCredential {
    private String username;
    @XmlJavaTypeAdapter(XmlCipherStringAdapter.class)
    private String password;

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
  @AllArgsConstructor
  @NoArgsConstructor
  @XmlAccessorType(XmlAccessType.FIELD)
  @AuditEntry(maskedFields = "certificate")
  class CertificateCredential {
    @XmlJavaTypeAdapter(XmlCipherByteArrayAdapter.class)
    private byte[] certificate;
    @XmlJavaTypeAdapter(XmlCipherStringAdapter.class)
    private String password;
  }
}
