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

import com.cloudogu.scm.mirror.MirrorGpgVerificationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MirrorCreationDto {

  private String namespace;
  @NotBlank
  private String name;
  @NotBlank
  private String type;
  @Email
  private String contact;
  private String description;

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

  private boolean overwriteGlobalConfiguration;
  private String branchesAndTagsPatterns;
  private MirrorGpgVerificationType gpgVerificationType;
  private List<RawGpgKeyDto> allowedGpgKeys;
  private boolean fastForwardOnly;
  private boolean ignoreLfs;
  private boolean allBranchesProtected = true;
}
