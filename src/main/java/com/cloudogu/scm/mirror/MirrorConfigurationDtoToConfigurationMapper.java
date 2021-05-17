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

import org.mapstruct.Mapper;

import java.util.Base64;

@Mapper
abstract class MirrorConfigurationDtoToConfigurationMapper {

  abstract MirrorConfiguration map(MirrorConfigurationDto configurationDto);

  abstract MirrorConfiguration.UsernamePasswordCredential map(MirrorConfigurationDto.UsernamePasswordCredentialDto credentialDto);
  abstract MirrorConfiguration.CertificateCredential map(MirrorConfigurationDto.CertificateCredentialDto credentialDto);

  byte[] mapBase64(String base64encoded) {
    return Base64.getDecoder().decode(base64encoded.replace("\n", ""));
  }
}
