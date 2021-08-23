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

import com.cloudogu.scm.mirror.MirrorProxyConfiguration;
import com.google.common.base.Strings;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sonia.scm.repository.Repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@Mapper
public abstract class MirrorProxyConfigurationMapper {

  @Mapping(ignore = true, target = "attributes")
  abstract MirrorProxyConfigurationDto map(MirrorProxyConfiguration configuration, @Context Repository repository);
  abstract MirrorProxyConfiguration map(MirrorProxyConfigurationDto configurationDto);

  @Mapping(target = "excludes")
  String map(Collection<String> value) {
    return value == null || value.isEmpty() ? null : String.join(",", value);
  }

  @Mapping(target = "excludes")
  Collection<String> map(String value) {
    return Strings.isNullOrEmpty(value) ? Collections.emptyList() : Arrays.asList(value.split(","));
  }

}
