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

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;

@Mapper
abstract class MirrorRequestDtoToRequestMapper {

  abstract MirrorRequest map(MirrorConfigurationDto requestDto);

  abstract MirrorRequest.UsernamePasswordCredential map(MirrorConfigurationDto.UsernamePasswordCredentialDto credentialDto);
  abstract MirrorRequest.CertificateCredential map(MirrorConfigurationDto.CertificateCredentialDto credentialDto);

  byte[] mapBase64(String base64encoded) {
    return Base64.getDecoder().decode(base64encoded.replace("\n", ""));
  }

  @AfterMapping
  void mapCredentials(@MappingTarget MirrorRequest request, MirrorConfigurationDto requestDto) {
    Collection<MirrorRequest.Credential> credentials = new ArrayList<>();
    addIfNotNull(credentials, map(requestDto.getUsernamePasswordCredential()));
    addIfNotNull(credentials, map(requestDto.getCertificationCredential()));
    request.setCredentials(credentials);
  }

  void addIfNotNull(Collection<MirrorRequest.Credential> credentials, MirrorRequest.Credential credential) {
    if (credential != null) {
      credentials.add(credential);
    }
  }
}
