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

import com.cloudogu.scm.mirror.MirrorConfiguration;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Base64;
import java.util.List;

import static java.util.Collections.emptyList;

@Mapper( uses = RawGpgKeyDtoToKeyMapper.class)
abstract class MirrorConfigurationDtoToConfigurationMapper {

  abstract MirrorConfiguration map(MirrorConfigurationDto configurationDto);

  abstract MirrorConfiguration.UsernamePasswordCredential map(MirrorConfigurationDto.UsernamePasswordCredentialDto credentialDto);
  abstract MirrorConfiguration.CertificateCredential map(MirrorConfigurationDto.CertificateCredentialDto credentialDto);

  @Mapping(target = "branchesAndTagsPatterns")
  List<String> map(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return emptyList();
    }
    return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(value);
  }

  @SuppressWarnings("java:S1168") // we want to signal that the certificate is not set and null signals this much better as an empty array
  byte[] mapBase64(String base64encoded) {
    if (base64encoded != null) {
      return Base64.getDecoder().decode(base64encoded.replace("\n", ""));
    }
    return null;
  }
}
