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
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.Initable;
import sonia.scm.SCMContextProvider;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.web.security.AdministrationContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

@Singleton
public class MirrorConfigurationStore implements Initable {

  public static final String DUMMY_PASSWORD = "_DUMMY_";
  private static final String STORE_NAME = "mirror";
  private static final Logger LOG = LoggerFactory.getLogger(MirrorConfigurationStore.class);

  private final ConfigurationStoreFactory storeFactory;
  private final ConfigurationStore<GlobalMirrorConfiguration> globalStore;
  private final MirrorScheduler scheduler;
  private final RepositoryManager repositoryManager;
  private final AdministrationContext administrationContext;

  @Inject
  MirrorConfigurationStore(ConfigurationStoreFactory storeFactory, MirrorScheduler scheduler, RepositoryManager repositoryManager, AdministrationContext administrationContext) {
    this.storeFactory = storeFactory;
    this.scheduler = scheduler;
    this.repositoryManager = repositoryManager;
    this.administrationContext = administrationContext;
    this.globalStore = storeFactory.withType(GlobalMirrorConfiguration.class).withName(STORE_NAME).build();
  }

  public Optional<MirrorConfiguration> getApplicableConfiguration(Repository repository) {
    final Optional<MirrorConfiguration> localConfiguration = getConfiguration(repository);
    return localConfiguration.map(it -> {
      final GlobalMirrorConfiguration globalConfiguration = getGlobalConfiguration();
      it.setHttpsOnly(globalConfiguration.isHttpsOnly());
      return applyFilterConfiguration(it, getApplicableFilterConfiguration(repository));
    });
  }

  @VisibleForTesting
  MirrorFilterConfiguration getApplicableFilterConfiguration(Repository repository) {
    final GlobalMirrorConfiguration globalConfiguration = getGlobalConfiguration();
    if (globalConfiguration.isDisableRepositoryFilterOverwrite()) {
      return globalConfiguration;
    }
    final Optional<MirrorConfiguration> localConfiguration = getConfiguration(repository);
    if (localConfiguration.isPresent() && localConfiguration.get().isOverwriteGlobalConfiguration()) {
      return localConfiguration.get();
    }
    return globalConfiguration;
  }

  public Optional<MirrorConfiguration> getConfiguration(Repository repository) {
    RepositoryPermissions.read(repository).check();
    return createConfigurationStore(repository).getOptional();
  }

  public void setFilterConfiguration(Repository repository, LocalFilterConfiguration filterConfiguration) {
    setConfiguration(repository, it -> applyLocalFilterConfiguration(it, filterConfiguration));
  }

  public void setAccessConfiguration(Repository repository, MirrorAccessConfiguration accessConfiguration) {
    setConfiguration(repository, it -> applyAccessConfiguration(repository, it, accessConfiguration));
  }

  void setConfiguration(Repository repository, MirrorConfiguration mirrorConfiguration) {
    setConfiguration(repository, it -> {
      applyAccessConfiguration(repository, it, mirrorConfiguration);
      applyLocalFilterConfiguration(it, mirrorConfiguration);
      return it;
    });
  }

  private void setConfiguration(Repository repository, UnaryOperator<MirrorConfiguration> applicator) {
    MirrorPermissions.checkRepositoryMirrorPermission(repository);
    LOG.debug("setting new configuration for repository {}", repository);
    ConfigurationStore<MirrorConfiguration> store = createConfigurationStore(repository);
    MirrorConfiguration newConfiguration = store.getOptional().map(applicator).orElseGet(() -> applicator.apply(new MirrorConfiguration()));
    store.set(newConfiguration);
    if (newConfiguration.getSynchronizationPeriod() == null) {
      scheduler.cancel(repository);
    } else {
      scheduler.schedule(repository, newConfiguration);
    }
  }

  public void deleteConfiguration(Repository repository) {
    MirrorPermissions.checkRepositoryMirrorPermission(repository);
    LOG.debug("unmirror repository {}", repository);
    ConfigurationStore<MirrorConfiguration> store = createConfigurationStore(repository);
    store.set(null);
    scheduler.cancel(repository);
  }

  private MirrorConfiguration applyLocalFilterConfiguration(MirrorConfiguration existingConfiguration, LocalFilterConfiguration newFilterConfiguration) {
    existingConfiguration.setOverwriteGlobalConfiguration(newFilterConfiguration.isOverwriteGlobalConfiguration());
    return applyFilterConfiguration(existingConfiguration, newFilterConfiguration);
  }

  private MirrorConfiguration applyFilterConfiguration(MirrorConfiguration existingConfiguration, MirrorFilterConfiguration newFilterConfiguration) {
    existingConfiguration.setFastForwardOnly(newFilterConfiguration.isFastForwardOnly());
    existingConfiguration.setGpgVerificationType(newFilterConfiguration.getGpgVerificationType());
    existingConfiguration.setAllowedGpgKeys(newFilterConfiguration.getAllowedGpgKeys());
    existingConfiguration.setBranchesAndTagsPatterns(newFilterConfiguration.getBranchesAndTagsPatterns());
    return existingConfiguration;
  }

  private MirrorConfiguration applyAccessConfiguration(Repository repository, MirrorConfiguration existingConfiguration, MirrorAccessConfiguration newMirrorAccessConfiguration) {
    if (getGlobalConfiguration().isHttpsOnly() && !newMirrorAccessConfiguration.getUrl().startsWith("https")) {
      throw new InsecureConnectionNotAllowedException(repository);
    }
    updateUrl(existingConfiguration, newMirrorAccessConfiguration);
    updateUsernamePasswordCredentials(existingConfiguration, newMirrorAccessConfiguration);
    updateCertificateCredentials(existingConfiguration, newMirrorAccessConfiguration);
    existingConfiguration.setManagingUsers(newMirrorAccessConfiguration.getManagingUsers());
    existingConfiguration.setSynchronizationPeriod(newMirrorAccessConfiguration.getSynchronizationPeriod());
    if (newMirrorAccessConfiguration.getProxyConfiguration().isOverwriteGlobalConfiguration()) {
      existingConfiguration.setProxyConfiguration(newMirrorAccessConfiguration.getProxyConfiguration());
    } else {
      existingConfiguration.setProxyConfiguration(new MirrorProxyConfiguration());
    }
    return existingConfiguration;
  }

  private void updateUrl(MirrorConfiguration existingConfiguration, MirrorAccessConfiguration newMirrorAccessConfiguration) {
    if (Strings.isNullOrEmpty(existingConfiguration.getUrl())) {
      existingConfiguration.setUrl(newMirrorAccessConfiguration.getUrl());
    } else {
      doThrow()
        .violation("url must not be changed", "url")
        .when(!existingConfiguration.getUrl().equals(newMirrorAccessConfiguration.getUrl()));
    }
  }

  private void updateCertificateCredentials(MirrorConfiguration existingConfiguration, MirrorAccessConfiguration newMirrorAccessConfiguration) {
    if (newMirrorAccessConfiguration.getCertificateCredential() != null) {
      final boolean shouldUpdatePassword = !DUMMY_PASSWORD.equals(newMirrorAccessConfiguration.getCertificateCredential().getPassword());
      final boolean shouldUpdateCertificate = newMirrorAccessConfiguration.getCertificateCredential().getCertificate() != null;
      if (existingConfiguration.getCertificateCredential() == null && (shouldUpdatePassword || shouldUpdateCertificate)) {
        existingConfiguration.setCertificateCredential(new MirrorAccessConfiguration.CertificateCredential());
      }
      if (shouldUpdatePassword) {
        LOG.trace("updating password for certificate");
        existingConfiguration.getCertificateCredential().setPassword(newMirrorAccessConfiguration.getCertificateCredential().getPassword());
      }
      if (shouldUpdateCertificate) {
        LOG.trace("updating certificate");
        existingConfiguration.getCertificateCredential().setCertificate(newMirrorAccessConfiguration.getCertificateCredential().getCertificate());
      }
    } else {
      existingConfiguration.setCertificateCredential(null);
    }
  }

  private void updateUsernamePasswordCredentials(MirrorConfiguration existingConfiguration, MirrorAccessConfiguration newMirrorAccessConfiguration) {
    if (newMirrorAccessConfiguration.getUsernamePasswordCredential() != null) {
      final boolean shouldUpdatePassword = !DUMMY_PASSWORD.equals(newMirrorAccessConfiguration.getUsernamePasswordCredential().getPassword());
      final boolean usernameIsNullOrEmpty = Strings.isNullOrEmpty(newMirrorAccessConfiguration.getUsernamePasswordCredential().getUsername());
      if (usernameIsNullOrEmpty) {
        LOG.trace("empty username, deleting username-password-credentials");
        existingConfiguration.setUsernamePasswordCredential(null);
      } else {
        if (existingConfiguration.getUsernamePasswordCredential() == null) {
          existingConfiguration.setUsernamePasswordCredential(new MirrorAccessConfiguration.UsernamePasswordCredential());
        }
        LOG.trace("updating username");
        existingConfiguration.getUsernamePasswordCredential().setUsername(newMirrorAccessConfiguration.getUsernamePasswordCredential().getUsername());
        if (shouldUpdatePassword) {
          LOG.trace("updating password for username");
          existingConfiguration.getUsernamePasswordCredential().setPassword(newMirrorAccessConfiguration.getUsernamePasswordCredential().getPassword());
        }
      }
    } else {
      existingConfiguration.setUsernamePasswordCredential(null);
    }
  }

  public boolean hasConfiguration(String repositoryId) {
    return createConfigurationStore(repositoryId)
      .getOptional()
      .isPresent();
  }

  public GlobalMirrorConfiguration getGlobalConfiguration() {
    return globalStore.getOptional().orElse(new GlobalMirrorConfiguration());
  }

  public void setGlobalConfiguration(GlobalMirrorConfiguration globalConfig) {
    globalStore.set(globalConfig);
  }

  @Override
  public void init(SCMContextProvider context) {
    administrationContext.runAsAdmin(() -> repositoryManager.getAll().forEach(this::init));
  }

  private void init(Repository repository) {
    getConfiguration(repository)
      .filter(configuration -> configuration.getSynchronizationPeriod() != null)
      .ifPresent(configuration -> scheduler.scheduleNow(repository, configuration));
  }

  private ConfigurationStore<MirrorConfiguration> createConfigurationStore(Repository repository) {
    return createConfigurationStore(repository.getId());
  }

  private ConfigurationStore<MirrorConfiguration> createConfigurationStore(String repositoryId) {
    return storeFactory
      .withType(MirrorConfiguration.class)
      .withName(STORE_NAME)
      .forRepository(repositoryId).build();
  }
}
