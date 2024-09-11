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

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sonia.scm.net.ProxyConfiguration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class MirrorProxyConfiguration implements ProxyConfiguration {

  private boolean overwriteGlobalConfiguration;
  private String host;
  private int port;
  private String username;
  private String password;

  @Override
  public boolean isEnabled() {
    return overwriteGlobalConfiguration;
  }

  @Override
  public Collection<String> getExcludes() {
    return Collections.emptyList();
  }

  @Override
  public boolean isAuthenticationRequired() {
    return !Strings.isNullOrEmpty(getUsername()) && !Strings.isNullOrEmpty(getPassword());
  }
}
