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

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.MeterRegistry;
import sonia.scm.metrics.Metrics;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Credential;
import sonia.scm.repository.api.MirrorCommandBuilder;
import sonia.scm.repository.api.Pkcs12ClientCertificateCredential;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MirrorWorker {

  private final RepositoryServiceFactory repositoryServiceFactory;
  private final ExecutorService executor;

  @Inject
  public MirrorWorker(RepositoryServiceFactory repositoryServiceFactory, MeterRegistry registry) {
    this(repositoryServiceFactory, registry, Executors.newFixedThreadPool(4));
  }

  @VisibleForTesting
  MirrorWorker(RepositoryServiceFactory repositoryServiceFactory, MeterRegistry registry, ExecutorService executor) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.executor = executor;
    Metrics.executor(registry, executor, "mirror", "fixed");
  }

  void withMirrorCommandDo(Repository repository, MirrorConfiguration configuration, Consumer<MirrorCommandBuilder> consumer) {
    // TODO Shiro context should either be set to admin or inherit the current subject
    executor.submit(
      () -> {
        try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
          MirrorCommandBuilder mirrorCommand = repositoryService.getMirrorCommand().setSourceUrl(configuration.getUrl());
          Collection<Credential> credentials = new ArrayList<>();
          if (configuration.getUsernamePasswordCredential() != null) {
            credentials.add(configuration.getUsernamePasswordCredential());
          }
          if (configuration.getCertificateCredential() != null) {
            credentials.add(new Pkcs12ClientCertificateCredential(configuration.getCertificateCredential().getCertificate(), configuration.getCertificateCredential().getPassword().toCharArray()));
          }
          mirrorCommand.setCredentials(credentials);
          consumer.accept(mirrorCommand);
        }
      }
    );
  }
}
