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

import com.cloudogu.scm.mirror.MirrorAccessConfiguration.CertificateCredential;
import com.cloudogu.scm.mirror.MirrorAccessConfiguration.UsernamePasswordCredential;
import com.cloudogu.scm.mirror.MirrorConfiguration;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sonia.scm.repository.Repository;

import java.util.Base64;
import java.util.List;

import static java.util.Collections.emptyList;

@Mapper(uses = {RawGpgKeyToKeyDtoMapper.class, MirrorProxyConfigurationMapper.class})
public abstract class MirrorCreationDtoMapper {

  @Mapping(target = "properties", ignore = true)
  @Mapping(target = "permissions", ignore = true)
  @Mapping(target = "lastModified", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "healthCheckFailures", ignore = true)
  @Mapping(target = "creationDate", ignore = true)
  @Mapping(target = "archived", ignore = true)
  abstract Repository mapToRepository(MirrorCreationDto mirrorCreationDto);

  @Mapping(target = "httpsOnly", ignore = true)
  abstract MirrorConfiguration mapToConfiguration(MirrorCreationDto mirrorCreationDto);

  @Mapping(target = "branchesAndTagsPatterns")
  List<String> map(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return emptyList();
    }
    return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(value);
  }

  abstract UsernamePasswordCredential map(UsernamePasswordCredentialDto credentialDto);

  abstract CertificateCredential map(CertificateCredentialDto credentialDto);

  @SuppressWarnings("java:S1168")
    // we want to signal that the certificate is not set and null signals this much better as an empty array
  byte[] mapBase64(String base64encoded) {
    if (base64encoded != null) {
      return Base64.getDecoder().decode(base64encoded.replace("\n", ""));
    }
    return null;
  }
}
