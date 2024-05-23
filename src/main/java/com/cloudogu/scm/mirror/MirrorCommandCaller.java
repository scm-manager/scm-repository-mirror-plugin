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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Credential;
import sonia.scm.repository.api.MirrorCommandBuilder;
import sonia.scm.repository.api.Pkcs12ClientCertificateCredential;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.security.PublicKey;
import sonia.scm.security.PublicKeyParser;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@SuppressWarnings("UnstableApiUsage")
class MirrorCommandCaller {

  private static final Logger LOG = LoggerFactory.getLogger(MirrorCommandCaller.class);

  private final RepositoryServiceFactory repositoryServiceFactory;
  private final FilterBuilder filterBuilder;
  private final PublicKeyParser publicKeyParser;

  @Inject
  public MirrorCommandCaller(RepositoryServiceFactory repositoryServiceFactory,
                             FilterBuilder filterBuilder,
                             PublicKeyParser publicKeyParser) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.filterBuilder = filterBuilder;
    this.publicKeyParser = publicKeyParser;
  }

  <T> CallResult<T> call(Repository repository, MirrorConfiguration configuration, Function<MirrorCommandBuilder, T> callback) {
    ConfigurableFilter filter;
    T result;

    if (configuration.isHttpsOnly() && !configuration.getUrl().startsWith("https")) {
      throw new InsecureConnectionNotAllowedException(repository);
    }

    try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
      LOG.debug("using url {}", configuration.getUrl());

      List<PublicKey> keys = convertKeys(configuration);

      MirrorCommandBuilder mirrorCommand =
        repositoryService.getMirrorCommand()
          .setSourceUrl(configuration.getUrl())
          .setIgnoreLfs(configuration.isIgnoreLfs())
          .setProxyConfiguration(configuration.getProxyConfiguration());
      mirrorCommand.setPublicKeys(keys);
      filter = filterBuilder.createFilter(configuration, keys);
      mirrorCommand.setFilter(filter);
      setCredentials(configuration, mirrorCommand);
      result = callback.apply(mirrorCommand);
    }
    return new CallResult<>(result, filter);
  }

  private List<PublicKey> convertKeys(MirrorConfiguration configuration) {
    if (configuration.getGpgVerificationType() == MirrorGpgVerificationType.KEY_LIST) {
      return configuration.getAllowedGpgKeys()
        .stream()
        .map(key -> publicKeyParser.parse(key.getRaw()))
        .collect(Collectors.toList());
    } else {
      return emptyList();
    }
  }

  private void setCredentials(MirrorConfiguration configuration, MirrorCommandBuilder mirrorCommand) {
    Collection<Credential> credentials = new ArrayList<>();
    if (configuration.getUsernamePasswordCredential() != null) {
      LOG.debug("using username/password credential for sync");
      credentials.add(configuration.getUsernamePasswordCredential());
    }
    if (configuration.getCertificateCredential() != null) {
      LOG.debug("using certificate credential for sync");
      credentials.add(new Pkcs12ClientCertificateCredential(configuration.getCertificateCredential().getCertificate(), configuration.getCertificateCredential().getPassword().toCharArray()));
    }
    mirrorCommand.setCredentials(credentials);
  }

  static class CallResult<T> {
    private final T resultFromCallback;
    private final ConfigurableFilter appliedFilter;

    CallResult(T resultFromCallback, ConfigurableFilter appliedFilter) {
      this.resultFromCallback = resultFromCallback;
      this.appliedFilter = appliedFilter;
    }

    public T getResultFromCallback() {
      return resultFromCallback;
    }

    public ConfigurableFilter getAppliedFilter() {
      return appliedFilter;
    }
  }
}
