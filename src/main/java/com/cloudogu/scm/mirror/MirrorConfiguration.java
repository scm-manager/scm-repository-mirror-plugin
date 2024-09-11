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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "mirror-configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class MirrorConfiguration implements MirrorAccessConfiguration, LocalFilterConfiguration {

  private String url;
  private Integer synchronizationPeriod;
  private List<String> managingUsers;
  private MirrorAccessConfiguration.UsernamePasswordCredential usernamePasswordCredential;
  private MirrorAccessConfiguration.CertificateCredential certificateCredential;
  private List<String> branchesAndTagsPatterns;
  private MirrorGpgVerificationType gpgVerificationType = MirrorGpgVerificationType.NONE;
  private List<RawGpgKey> allowedGpgKeys;
  private boolean fastForwardOnly = false;
  private boolean overwriteGlobalConfiguration = false;
  private boolean ignoreLfs = false;
  private boolean allBranchesProtected = true;
  private MirrorProxyConfiguration proxyConfiguration = new MirrorProxyConfiguration();

  @XmlTransient
  private boolean httpsOnly = false;

  public MirrorConfiguration(String url, int synchronizationPeriod, List<String> managingUsers, UsernamePasswordCredential usernamePasswordCredential, CertificateCredential certificateCredential, MirrorProxyConfiguration proxyConfiguration) {
    this.url = url;
    this.synchronizationPeriod = synchronizationPeriod;
    this.managingUsers = managingUsers;
    this.usernamePasswordCredential = usernamePasswordCredential;
    this.certificateCredential = certificateCredential;
    this.proxyConfiguration = proxyConfiguration;
  }

  public List<String> getManagingUsers() {
    return managingUsers != null ? managingUsers : Collections.emptyList();
  }

  public List<String> getBranchesAndTagsPatterns() {
    return branchesAndTagsPatterns != null ? branchesAndTagsPatterns : Collections.emptyList();
  }

  public List<RawGpgKey> getAllowedGpgKeys() {
    return allowedGpgKeys != null ? allowedGpgKeys : Collections.emptyList();
  }
}
