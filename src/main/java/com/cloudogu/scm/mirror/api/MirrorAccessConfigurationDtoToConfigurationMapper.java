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

import com.cloudogu.scm.mirror.MirrorAccessConfigurationImpl;
import com.cloudogu.scm.mirror.MirrorConfiguration;
import org.mapstruct.Mapper;

import java.util.Base64;

@Mapper(uses = {MirrorProxyConfigurationMapper.class})
abstract class MirrorAccessConfigurationDtoToConfigurationMapper {

  abstract MirrorAccessConfigurationImpl map(MirrorAccessConfigurationDto configurationDto);

  abstract MirrorConfiguration.UsernamePasswordCredential map(UsernamePasswordCredentialDto credentialDto);
  abstract MirrorConfiguration.CertificateCredential map(CertificateCredentialDto credentialDto);

  @SuppressWarnings("java:S1168") // we want to signal that the certificate is not set and null signals this much better as an empty array
  byte[] mapBase64(String base64encoded) {
    if (base64encoded != null) {
      return Base64.getDecoder().decode(base64encoded.replace("\n", ""));
    }
    return null;
  }
}
