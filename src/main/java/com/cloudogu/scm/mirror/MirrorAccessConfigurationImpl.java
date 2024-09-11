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

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MirrorAccessConfigurationImpl implements MirrorAccessConfiguration {

  private String url;
  private Integer synchronizationPeriod;
  private List<String> managingUsers;

  private MirrorAccessConfiguration.UsernamePasswordCredential usernamePasswordCredential;
  private MirrorAccessConfiguration.CertificateCredential certificateCredential;

  private MirrorProxyConfiguration proxyConfiguration = new MirrorProxyConfiguration();

  public List<String> getManagingUsers() {
    return managingUsers != null ? managingUsers : Collections.emptyList();
  }
}
