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

import org.apache.shiro.authz.UnauthorizedException;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.InMemoryConfigurationStoreFactory;
import sonia.scm.web.security.AdministrationContext;
import sonia.scm.web.security.PrivilegedAction;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ShiroExtension.class)
@ExtendWith(MockitoExtension.class)
@SubjectAware(
  value = "trillian"
)
class MirrorConfigurationStoreTest {

  public static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock
  private MirrorScheduler scheduler;

  @Mock
  private RepositoryManager repositoryManager;
  @Mock
  private AdministrationContext administrationContext;

  private final InMemoryConfigurationStoreFactory storeFactory = new InMemoryConfigurationStoreFactory();
  private MirrorConfigurationStore store;

  @BeforeEach
  void createService() {
    store = new MirrorConfigurationStore(storeFactory, scheduler, repositoryManager, administrationContext);
  }

  @BeforeAll
  static void setRepositoryId() {
    REPOSITORY.setId("42");
  }

  @Test
  void shouldFailToGetConfigurationWithoutPermission() {
    assertThrows(UnauthorizedException.class,
      () -> store.getConfiguration(REPOSITORY));
  }

  @Test
  void shouldFailToSetConfigurationWithoutPermission() {
    MirrorAccessConfiguration configuration = new MirrorAccessConfigurationImpl();
    assertThrows(UnauthorizedException.class,
      () -> store.setAccessConfiguration(REPOSITORY, configuration));
  }

  @Nested
  @SubjectAware(
    value = "trillian",
    permissions = "repository:read,mirror:42"
  )
  class WithPermission {

    @Test
    void shouldReturnEmptyOptionalForUnknownRepository() {
      Optional<MirrorConfiguration> configuration = store.getConfiguration(REPOSITORY);

      assertThat(configuration).isEmpty();
    }

    @Test
    void shouldGetConfiguration() {
      MirrorConfiguration existingConfiguration = new MirrorConfiguration();
      mockExistingConfiguration(existingConfiguration);

      Optional<MirrorConfiguration> configuration = store.getConfiguration(REPOSITORY);

      assertThat(configuration).get().isSameAs(existingConfiguration);
    }

    @Test
    void shouldSetAccessConfigurationButNotFilters() {
      MirrorConfiguration existingMirrorConfiguration = new MirrorConfiguration();
      existingMirrorConfiguration.setOverwriteGlobalConfiguration(true);
      existingMirrorConfiguration.setBranchesAndTagsPatterns(singletonList("develop,feature/*"));
      existingMirrorConfiguration.setGpgVerificationType(MirrorGpgVerificationType.SCM_USER_SIGNATURE);
      existingMirrorConfiguration.setFastForwardOnly(true);
      existingMirrorConfiguration.setIgnoreLfs(true);

      MirrorProxyConfiguration newProxyConfiguration = new MirrorProxyConfiguration();
      newProxyConfiguration.setHost("foo.bar");
      newProxyConfiguration.setPort(1337);
      newProxyConfiguration.setUsername("trillian");
      newProxyConfiguration.setPassword("secret123");
      newProxyConfiguration.setOverwriteGlobalConfiguration(true);

      MirrorAccessConfigurationImpl newConfiguration = new MirrorAccessConfigurationImpl();
      newConfiguration.setManagingUsers(singletonList("trillian"));
      newConfiguration.setUrl("https://scm-manager.org");
      newConfiguration.setSynchronizationPeriod(10);
      newConfiguration.setCertificateCredential(new MirrorAccessConfiguration.CertificateCredential("hello".getBytes(StandardCharsets.UTF_8), "secret"));
      newConfiguration.setUsernamePasswordCredential(new MirrorAccessConfiguration.UsernamePasswordCredential("scm-manager", "scm-manager"));
      newConfiguration.setProxyConfiguration(newProxyConfiguration);

      storeFactory.get("mirror", REPOSITORY.getId()).set(existingMirrorConfiguration);
      store.setAccessConfiguration(REPOSITORY, newConfiguration);

      Object configurationFromStore = storeFactory.get("mirror", REPOSITORY.getId()).get();
      assertThat(configurationFromStore).isSameAs(existingMirrorConfiguration);
      verify(scheduler).schedule(eq(REPOSITORY), argThat(it -> {
        // Filter config should remain the same
        assertThat(it.getGpgVerificationType()).isEqualTo(MirrorGpgVerificationType.SCM_USER_SIGNATURE);
        assertThat(it.isFastForwardOnly()).isTrue();
        assertThat(it.isFastForwardOnly()).isTrue();
        assertThat(it.isOverwriteGlobalConfiguration()).isTrue();
        assertThat(it.getBranchesAndTagsPatterns()).containsExactly("develop,feature/*");

        // Access config should be overwritten
        assertThat(it.getManagingUsers()).containsExactly("trillian");
        assertThat(it.getUrl()).isEqualTo("https://scm-manager.org");
        assertThat(it.getSynchronizationPeriod()).isEqualTo(10);
        assertThat(it.getCertificateCredential()).matches(certificateCredential -> {
          assertThat(certificateCredential).isNotNull();
          assertThat(certificateCredential.getPassword()).isEqualTo("secret");
          assertThat(certificateCredential.getCertificate()).asString().isEqualTo("hello");
          return true;
        });
        assertThat(it.getUsernamePasswordCredential()).matches(usernamePasswordCredential -> {
          assertThat(usernamePasswordCredential).isNotNull();
          assertThat(usernamePasswordCredential.getPassword()).isEqualTo("scm-manager");
          assertThat(usernamePasswordCredential.getUsername()).isEqualTo("scm-manager");
          return true;
        });
        assertThat(it.getProxyConfiguration()).matches(proxyConfiguration -> {
          assertThat(proxyConfiguration.isOverwriteGlobalConfiguration()).isTrue();
          assertThat(proxyConfiguration.getHost()).isEqualTo("foo.bar");
          assertThat(proxyConfiguration.getPort()).isEqualTo(1337);
          assertThat(proxyConfiguration.getUsername()).isEqualTo("trillian");
          assertThat(proxyConfiguration.getPassword()).isEqualTo("secret123");
          assertThat(proxyConfiguration.getExcludes()).isNotNull();
          return true;
        });
        return true;
      }));
    }

    @Test
    void shouldResetProxyConfigurationOnDisable() {
      MirrorProxyConfiguration oldProxyConfiguration = new MirrorProxyConfiguration();
      oldProxyConfiguration.setHost("foo.bar");
      oldProxyConfiguration.setPort(1337);
      oldProxyConfiguration.setUsername("trillian");
      oldProxyConfiguration.setPassword("secret123");
      oldProxyConfiguration.setOverwriteGlobalConfiguration(true);

      MirrorConfiguration existingMirrorConfiguration = new MirrorConfiguration();
      existingMirrorConfiguration.setProxyConfiguration(oldProxyConfiguration);
      existingMirrorConfiguration.setSynchronizationPeriod(10);

      MirrorProxyConfiguration newProxyConfiguration = new MirrorProxyConfiguration();
      newProxyConfiguration.setHost("new.stuff");
      newProxyConfiguration.setPort(123);
      newProxyConfiguration.setUsername("troll");
      newProxyConfiguration.setPassword("ilikecookies");
      newProxyConfiguration.setOverwriteGlobalConfiguration(false);

      MirrorAccessConfigurationImpl newConfiguration = new MirrorAccessConfigurationImpl();
      newConfiguration.setProxyConfiguration(newProxyConfiguration);
      newConfiguration.setSynchronizationPeriod(10);

      storeFactory.get("mirror", REPOSITORY.getId()).set(existingMirrorConfiguration);
      store.setAccessConfiguration(REPOSITORY, newConfiguration);

      Object configurationFromStore = storeFactory.get("mirror", REPOSITORY.getId()).get();
      assertThat(configurationFromStore).isSameAs(existingMirrorConfiguration);
      verify(scheduler).schedule(eq(REPOSITORY), argThat(it -> {
        assertThat(it.getProxyConfiguration()).matches(proxyConfiguration -> {
          assertThat(proxyConfiguration.isOverwriteGlobalConfiguration()).isFalse();
          assertThat(proxyConfiguration.getHost()).isNull();
          assertThat(proxyConfiguration.getPort()).isZero();
          assertThat(proxyConfiguration.getUsername()).isNull();
          assertThat(proxyConfiguration.getPassword()).isNull();
          assertThat(proxyConfiguration.getExcludes()).isNotNull();
          return true;
        });
        return true;
      }));
    }

    @Test
    void shouldSetFilterConfiguration() {
      MirrorConfiguration existingMirrorConfiguration = new MirrorConfiguration();
      existingMirrorConfiguration.setUrl("https://scm-manager.org");
      existingMirrorConfiguration.setManagingUsers(singletonList("trillian"));
      existingMirrorConfiguration.setSynchronizationPeriod(100);
      existingMirrorConfiguration.setCertificateCredential(new MirrorAccessConfiguration.CertificateCredential());
      existingMirrorConfiguration.setUsernamePasswordCredential(new MirrorAccessConfiguration.UsernamePasswordCredential());

      LocalFilterConfigurationImpl newConfiguration = new LocalFilterConfigurationImpl();
      newConfiguration.setOverwriteGlobalConfiguration(true);
      newConfiguration.setFastForwardOnly(true);
      newConfiguration.setBranchesAndTagsPatterns(singletonList("develop,feature/*"));
      newConfiguration.setGpgVerificationType(MirrorGpgVerificationType.SCM_USER_SIGNATURE);

      newConfiguration.setIgnoreLfs(true);

      storeFactory.get("mirror", REPOSITORY.getId()).set(existingMirrorConfiguration);
      store.setFilterConfiguration(REPOSITORY, newConfiguration);

      Object configurationFromStore = storeFactory.get("mirror", REPOSITORY.getId()).get();
      assertThat(configurationFromStore).isSameAs(existingMirrorConfiguration);
      verify(scheduler).schedule(eq(REPOSITORY), argThat(it -> {
        // Access config should remain the same
        assertThat(it.getUrl()).isEqualTo("https://scm-manager.org");
        assertThat(it.getManagingUsers()).containsExactly("trillian");
        assertThat(it.getSynchronizationPeriod()).isEqualTo(100);
        assertThat(it.getUsernamePasswordCredential()).isNotNull();
        assertThat(it.getCertificateCredential()).isNotNull();

        assertThat(it.isIgnoreLfs()).isTrue();

        // Filters should be overwritten
        assertThat(it.isOverwriteGlobalConfiguration()).isTrue();
        assertThat(it.isFastForwardOnly()).isTrue();
        assertThat(it.getGpgVerificationType()).isEqualTo(MirrorGpgVerificationType.SCM_USER_SIGNATURE);
        assertThat(it.getBranchesAndTagsPatterns()).containsExactly("develop,feature/*");
        return true;
      }));
    }

    @Test
    void shouldHaveDisabledProxyConfigurationByDefault() {
      MirrorConfiguration existingConfiguration = new MirrorConfiguration();
      mockExistingConfiguration(existingConfiguration);

      Optional<MirrorConfiguration> configuration = store.getConfiguration(REPOSITORY);

      assertThat(configuration.map(MirrorConfiguration::getProxyConfiguration)).isPresent();
      assertThat(configuration.map(it -> it.getProxyConfiguration().isOverwriteGlobalConfiguration())).hasValue(false);
      assertThat(configuration.map(it -> it.getProxyConfiguration().getExcludes())).isPresent();

    }

    @Test
    void shouldPullLocalProxyConfiguration() {
      final GlobalMirrorConfiguration globalMirrorConfiguration = new GlobalMirrorConfiguration();

      mockGlobalConfiguration(globalMirrorConfiguration);

      final MirrorProxyConfiguration proxyConfiguration = new MirrorProxyConfiguration(true, "https://proxy.scm-manager.org", 1337, "trillian", "secret123");

      final MirrorConfiguration mirrorConfiguration = new MirrorConfiguration();
      mirrorConfiguration.setProxyConfiguration(proxyConfiguration);
      mockExistingConfiguration(mirrorConfiguration);

      final Optional<MirrorConfiguration> applicableConfiguration = store.getApplicableConfiguration(REPOSITORY);

      assertThat(applicableConfiguration).hasValueSatisfying(it -> {
        assertThat(it.getProxyConfiguration()).isSameAs(proxyConfiguration);
      });
    }

    @Test
    void shouldNotTriggerForDisabledMirrorConfiguration() {
      MirrorConfiguration newMirrorConfiguration = new MirrorConfiguration();
      newMirrorConfiguration.setSynchronizationPeriod(null);

      store.setAccessConfiguration(REPOSITORY, newMirrorConfiguration);

      verify(scheduler, never()).schedule(any(), any());
      verify(scheduler).cancel(REPOSITORY);
    }

    @Test
    void shouldCreateNewEmptyCertificateCredentialIfExistingWasNull() {
      MirrorConfiguration existingConfiguration = new MirrorConfiguration();
      mockExistingConfiguration(existingConfiguration);

      MirrorAccessConfigurationImpl newConfiguration = new MirrorAccessConfigurationImpl();
      newConfiguration.setCertificateCredential(new MirrorAccessConfiguration.CertificateCredential(new byte[] {1, 2, 3}, "secret"));

      store.setAccessConfiguration(REPOSITORY, newConfiguration);

      assertThat(existingConfiguration.getCertificateCredential().getPassword()).isEqualTo("secret");
      assertThat(existingConfiguration.getCertificateCredential().getCertificate()).isEqualTo(new byte[] {1, 2, 3});
    }

    @Test
    void shouldNotCreateNewEmptyCertificateCredentialIfExistingWasNotNull() {
      MirrorConfiguration existingConfiguration = new MirrorConfiguration();
      MirrorAccessConfiguration.CertificateCredential certificateCredential = new MirrorAccessConfiguration.CertificateCredential(null, "secret");
      existingConfiguration.setCertificateCredential(certificateCredential);
      mockExistingConfiguration(existingConfiguration);

      MirrorAccessConfigurationImpl newConfiguration = new MirrorAccessConfigurationImpl();
      newConfiguration.setCertificateCredential(new MirrorAccessConfiguration.CertificateCredential(new byte[] {1, 2, 3}, "_DUMMY_"));

      store.setAccessConfiguration(REPOSITORY, newConfiguration);

      assertThat(existingConfiguration.getCertificateCredential().getPassword()).isEqualTo("secret");
      assertThat(existingConfiguration.getCertificateCredential().getCertificate()).isEqualTo(new byte[] {1, 2, 3});
    }

    @Test
    void shouldIgnoreDummyCredentialsAndEmptyCertificate() {
      MirrorConfiguration existingConfiguration = new MirrorConfiguration();
      existingConfiguration.setUsernamePasswordCredential(new MirrorAccessConfiguration.UsernamePasswordCredential("dent", "oldUsernamePassword"));
      existingConfiguration.setCertificateCredential(new MirrorAccessConfiguration.CertificateCredential(new byte[] {1, 2, 3}, "oldCertPassword"));
      mockExistingConfiguration(existingConfiguration);

      MirrorAccessConfigurationImpl newConfiguration = new MirrorAccessConfigurationImpl();
      newConfiguration.setUsernamePasswordCredential(new MirrorAccessConfiguration.UsernamePasswordCredential("dent", "_DUMMY_"));
      newConfiguration.setCertificateCredential(new MirrorAccessConfiguration.CertificateCredential(null, "_DUMMY_"));

      store.setAccessConfiguration(REPOSITORY, newConfiguration);

      assertThat(existingConfiguration.getUsernamePasswordCredential().getPassword()).isEqualTo("oldUsernamePassword");
      assertThat(existingConfiguration.getCertificateCredential().getPassword()).isEqualTo("oldCertPassword");
      assertThat(existingConfiguration.getCertificateCredential().getCertificate()).isEqualTo(new byte[] {1, 2, 3});
    }

    @Test
    void shouldDeleteUsernamePasswordCredentialsIfNewUsernameIsNullOrEmpty() {
      MirrorConfiguration existingConfiguration = new MirrorConfiguration();
      existingConfiguration.setUsernamePasswordCredential(new MirrorAccessConfiguration.UsernamePasswordCredential("dent", "oldUsernamePassword"));
      mockExistingConfiguration(existingConfiguration);

      MirrorAccessConfigurationImpl newConfiguration = new MirrorAccessConfigurationImpl();
      newConfiguration.setUsernamePasswordCredential(new MirrorAccessConfiguration.UsernamePasswordCredential("", "_DUMMY_"));

      store.setAccessConfiguration(REPOSITORY, newConfiguration);

      assertThat(existingConfiguration.getUsernamePasswordCredential()).isNull();
    }

    @Test
    void shouldNotChangeUrl() {
      MirrorConfiguration existingConfiguration = new MirrorConfiguration();
      existingConfiguration.setUrl("http://hog.net/");
      mockExistingConfiguration(existingConfiguration);

      MirrorAccessConfigurationImpl newConfiguration = new MirrorAccessConfigurationImpl();
      newConfiguration.setUrl("http://magrathea.com/");

      assertThrows(ScmConstraintViolationException.class, () -> store.setAccessConfiguration(REPOSITORY, newConfiguration));
    }

    @Test
    void shouldReturnEmptyIfLocalConfigurationIsNotSet() {
      final Optional<MirrorConfiguration> applicableConfiguration = store.getApplicableConfiguration(REPOSITORY);

      assertThat(applicableConfiguration).isNotPresent();
    }

    @Test
    void shouldReturnGlobalFiltersIfLocalAreNotPresent() {
      final GlobalMirrorConfiguration globalMirrorConfiguration = new GlobalMirrorConfiguration();
      globalMirrorConfiguration.setFastForwardOnly(true);
      mockGlobalConfiguration(globalMirrorConfiguration);

      final MirrorFilterConfiguration applicableConfiguration = store.getApplicableFilterConfiguration(REPOSITORY);

      assertThat(applicableConfiguration.isFastForwardOnly()).isTrue();
    }

    @Test
    void shouldApplyExclusivelyGlobalSettings() {
      final GlobalMirrorConfiguration globalMirrorConfiguration = new GlobalMirrorConfiguration();
      globalMirrorConfiguration.setHttpsOnly(true);

      mockGlobalConfiguration(globalMirrorConfiguration);

      final MirrorConfiguration mirrorConfiguration = new MirrorConfiguration();
      mockExistingConfiguration(mirrorConfiguration);

      final Optional<MirrorConfiguration> applicableConfiguration = store.getApplicableConfiguration(REPOSITORY);

      assertThat(applicableConfiguration).hasValueSatisfying(it -> {
        assertThat(it.isHttpsOnly()).isTrue();
      });
    }

    @Test
    void shouldNotOverrideLfsSettingWithGlobalSettings() {
      final GlobalMirrorConfiguration globalMirrorConfiguration = new GlobalMirrorConfiguration();
      globalMirrorConfiguration.setIgnoreLfs(false);

      mockGlobalConfiguration(globalMirrorConfiguration);

      final MirrorConfiguration mirrorConfiguration = new MirrorConfiguration();
      mirrorConfiguration.setIgnoreLfs(true);
      mockExistingConfiguration(mirrorConfiguration);

      final Optional<MirrorConfiguration> applicableConfiguration = store.getApplicableConfiguration(REPOSITORY);

      assertThat(applicableConfiguration).hasValueSatisfying(it -> {
        assertThat(it.isIgnoreLfs()).isTrue();
      });
    }

    @Test
    void shouldKeepNonFilterValues() {
      final GlobalMirrorConfiguration globalMirrorConfiguration = new GlobalMirrorConfiguration();
      globalMirrorConfiguration.setFastForwardOnly(true);
      globalMirrorConfiguration.setBranchesAndTagsPatterns(singletonList("develop,feature/*"));

      mockGlobalConfiguration(globalMirrorConfiguration);

      final MirrorConfiguration mirrorConfiguration = new MirrorConfiguration();
      mirrorConfiguration.setUrl("test");
      mirrorConfiguration.setSynchronizationPeriod(100);
      mirrorConfiguration.setManagingUsers(singletonList("trillian"));
      mirrorConfiguration.setFastForwardOnly(false);
      mirrorConfiguration.setBranchesAndTagsPatterns(singletonList("develop"));
      mockExistingConfiguration(mirrorConfiguration);

      final Optional<MirrorConfiguration> applicableConfiguration = store.getApplicableConfiguration(REPOSITORY);

      assertThat(applicableConfiguration).hasValueSatisfying(it -> {
        assertThat(it.getUrl()).isEqualTo("test");
        assertThat(it.getSynchronizationPeriod()).isEqualTo(100);
        assertThat(it.getManagingUsers()).containsExactly("trillian");
        assertThat(it.isFastForwardOnly()).isTrue();
        assertThat(it.getBranchesAndTagsPatterns()).containsExactly("develop,feature/*");
      });
    }

    @Test
    void shouldUseGlobalFiltersIfLocalAreNotAllowed() {
      final GlobalMirrorConfiguration globalMirrorConfiguration = new GlobalMirrorConfiguration();
      globalMirrorConfiguration.setFastForwardOnly(true);
      globalMirrorConfiguration.setDisableRepositoryFilterOverwrite(true);
      mockGlobalConfiguration(globalMirrorConfiguration);

      final LocalFilterConfigurationImpl localFilterConfiguration = new LocalFilterConfigurationImpl();
      localFilterConfiguration.setFastForwardOnly(false);
      localFilterConfiguration.setOverwriteGlobalConfiguration(true);
      mockFilterConfiguration(localFilterConfiguration);

      final MirrorFilterConfiguration applicableConfiguration = store.getApplicableFilterConfiguration(REPOSITORY);

      assertThat(applicableConfiguration.isFastForwardOnly()).isTrue();
    }

    @Test
    void shouldUseGlobalFiltersIfLocalAreNotOverwritten() {
      final GlobalMirrorConfiguration globalMirrorConfiguration = new GlobalMirrorConfiguration();
      globalMirrorConfiguration.setFastForwardOnly(true);
      globalMirrorConfiguration.setDisableRepositoryFilterOverwrite(false);
      mockGlobalConfiguration(globalMirrorConfiguration);

      final LocalFilterConfigurationImpl localFilterConfiguration = new LocalFilterConfigurationImpl();
      localFilterConfiguration.setFastForwardOnly(false);
      localFilterConfiguration.setOverwriteGlobalConfiguration(false);
      mockFilterConfiguration(localFilterConfiguration);

      final MirrorFilterConfiguration applicableConfiguration = store.getApplicableFilterConfiguration(REPOSITORY);

      assertThat(applicableConfiguration.isFastForwardOnly()).isTrue();
    }

    @Test
    void shouldUseLocalFiltersIfOverwritten() {
      final GlobalMirrorConfiguration globalMirrorConfiguration = new GlobalMirrorConfiguration();
      globalMirrorConfiguration.setFastForwardOnly(false);
      globalMirrorConfiguration.setDisableRepositoryFilterOverwrite(false);
      mockGlobalConfiguration(globalMirrorConfiguration);

      final LocalFilterConfigurationImpl localFilterConfiguration = new LocalFilterConfigurationImpl();
      localFilterConfiguration.setFastForwardOnly(true);
      localFilterConfiguration.setOverwriteGlobalConfiguration(true);
      mockFilterConfiguration(localFilterConfiguration);

      final MirrorFilterConfiguration applicableConfiguration = store.getApplicableFilterConfiguration(REPOSITORY);

      assertThat(applicableConfiguration.isFastForwardOnly()).isTrue();
    }

    @Test
    void shouldNotBeReadOnlyIfRepositoryIsNoMirror() {
      boolean readOnly = store.isReadOnly(REPOSITORY.getId());

      assertThat(readOnly).isFalse();
    }

    @Test
    void shouldBeReadOnlyByDefaultForMirro() {
      MirrorConfiguration existingMirrorConfiguration = new MirrorConfiguration();
      storeFactory.get("mirror", REPOSITORY.getId()).set(existingMirrorConfiguration);

      boolean readOnly = store.isReadOnly(REPOSITORY.getId());

      assertThat(readOnly).isTrue();
    }

    @Test
    void shouldNotBeReadOnlyForMirrorIfDeactivated() {
      MirrorConfiguration existingMirrorConfiguration = new MirrorConfiguration();
      existingMirrorConfiguration.setAllBranchesProtected(false);
      storeFactory.get("mirror", REPOSITORY.getId()).set(existingMirrorConfiguration);

      boolean readOnly = store.isReadOnly(REPOSITORY.getId());

      assertThat(readOnly).isFalse();
    }
  }

  @Nested
  @SubjectAware(
    value = "trillian",
    permissions = "*" // admin context
  )
  class AtStartup {

    @BeforeEach
    void mockPrivilegedAction() {
      doAnswer(
        invocation -> {
          invocation.getArgument(0, PrivilegedAction.class).run();
          return null;
        }
      ).when(administrationContext).runAsAdmin(any(PrivilegedAction.class));
    }

    @Test
    void shouldScheduleRepositoriesAtStartup() {
      Repository normalRepository = RepositoryTestData.createHeartOfGold();
      Repository mirrorRepository = RepositoryTestData.create42Puzzle();
      when(repositoryManager.getAll())
        .thenReturn(asList(normalRepository, mirrorRepository));
      MirrorConfiguration configuration = mock(MirrorConfiguration.class);
      when(configuration.getSynchronizationPeriod()).thenReturn(5);
      mockExistingConfiguration(configuration, mirrorRepository);

      store.init(null);

      verify(scheduler).scheduleNow(mirrorRepository, configuration);
      verify(scheduler, never()).scheduleNow(eq(normalRepository), any());
    }

    @Test
    void shouldNotScheduleDisabledRepositoriesAtStartup() {
      Repository mirrorRepository = RepositoryTestData.create42Puzzle();
      when(repositoryManager.getAll())
        .thenReturn(singletonList(mirrorRepository));
      MirrorConfiguration configuration = mock(MirrorConfiguration.class);
      when(configuration.getSynchronizationPeriod()).thenReturn(null);
      mockExistingConfiguration(configuration, mirrorRepository);

      store.init(null);

      verify(scheduler, never()).scheduleNow(any(), any());
    }
  }

  private void mockExistingConfiguration(MirrorConfiguration existingConfiguration) {
    mockExistingConfiguration(existingConfiguration, REPOSITORY);
  }

  @SuppressWarnings("unchecked")
  private void mockExistingConfiguration(MirrorConfiguration existingConfiguration, Repository repository) {
    storeFactory.get("mirror", repository.getId()).set(existingConfiguration);
  }

  private void mockGlobalConfiguration(GlobalMirrorConfiguration globalMirrorConfiguration) {
    store.setGlobalConfiguration(globalMirrorConfiguration);
  }

  private void mockFilterConfiguration(LocalFilterConfiguration localFilterConfiguration) {
    store.setFilterConfiguration(REPOSITORY, localFilterConfiguration);
  }
}
