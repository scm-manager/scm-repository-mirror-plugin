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
