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
package com.cloudogu.scm.mirror.api;

import com.cloudogu.scm.mirror.MirrorGpgVerificationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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

}
