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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
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
