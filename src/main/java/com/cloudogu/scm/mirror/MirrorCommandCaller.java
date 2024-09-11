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
