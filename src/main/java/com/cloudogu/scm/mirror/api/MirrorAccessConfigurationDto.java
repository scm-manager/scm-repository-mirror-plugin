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

package com.cloudogu.scm.mirror.api;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("java:S2160") // Equals and Hashcode not needed for dto
public class MirrorAccessConfigurationDto extends HalRepresentation {

  @NotBlank
  private String url;
  @Min(1)
  private Integer synchronizationPeriod;
  private List<String> managingUsers;

  @Valid
  private UsernamePasswordCredentialDto usernamePasswordCredential;
  @Valid
  private CertificateCredentialDto certificateCredential;

  @NotNull
  private MirrorProxyConfigurationDto proxyConfiguration;

  MirrorAccessConfigurationDto(Links links) {
    super(links);
  }

}
