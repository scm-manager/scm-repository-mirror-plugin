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

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.MirrorCommandBuilder;
import sonia.scm.repository.api.MirrorFilter;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.security.PublicKey;
import sonia.scm.security.PublicKeyParser;
import sonia.scm.web.proxy.ProxyConfiguration;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("UnstableApiUsage")
class MirrorCommandCallerTest {

  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;
  @Mock
  private RepositoryService repositoryService;
  @Mock(answer = Answers.RETURNS_SELF)
  private MirrorCommandBuilder mirrorCommandBuilder;
  @Mock
  private FilterBuilder filterBuilder;
  @Mock
  private PublicKeyParser publicKeyParser;

  @InjectMocks
  private MirrorCommandCaller caller;

  private final Repository repository = RepositoryTestData.createHeartOfGold();

  @BeforeEach
  void supportMirrorCommand() {
    lenient().when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
    lenient().when(repositoryService.getMirrorCommand()).thenReturn(mirrorCommandBuilder);
  }

  @Test
  void shouldSetFilterForConfiguration() {
    MirrorConfiguration configuration = createMirrorConfig();

    ConfigurableFilter expectedFilter = mock(ConfigurableFilter.class);
    when(filterBuilder.createFilter(configuration, emptyList())).thenReturn(expectedFilter);

    invokeCaller(configuration, null);

    verify(mirrorCommandBuilder).setFilter(argThat(filter -> {
      assertThat(filter).isSameAs(expectedFilter);
      return true;
    }));
  }

  @Test
  void shouldThrowErrorIfHttpsOnlyIsSetButUrlIsInsecure() {
    MirrorConfiguration configuration = createMirrorConfig();
    configuration.setHttpsOnly(true);
    configuration.setUrl("http://hog/");
    Assert.assertThrows(InsecureConnectionNotAllowedException.class, () -> invokeCaller(configuration, null));
  }

  @Test
  void shouldNotThrowErrorIfHttpsOnlyIsSetAndUrlIsSecure() {
    MirrorConfiguration configuration = createMirrorConfig();
    configuration.setHttpsOnly(true);
    invokeCaller(configuration, null);
    // No Assertions necessary, it just shouldnt throw an exception
  }

  @Test
  void shouldReturnResultFromCallback() {
    MirrorConfiguration configuration = createMirrorConfig();

    ConfigurableFilter expectedFilter = mock(ConfigurableFilter.class);
    when(filterBuilder.createFilter(configuration, emptyList())).thenReturn(expectedFilter);

    Object mockedResult = new Object();
    MirrorCommandCaller.CallResult<Object> actualResult = invokeCaller(configuration, mockedResult);

    assertThat(actualResult.getResultFromCallback()).isSameAs(mockedResult);
  }

  @Test
  void shouldSetKeyCredentialsInCommand() {
    byte[] key = {};
    MirrorConfiguration configuration = createMirrorConfig(new MirrorConfiguration.CertificateCredential(key, "hog"));

    invokeCaller(configuration, null);

    verify(mirrorCommandBuilder).setCredentials(argThat(
      credentials -> {
        assertThat(credentials).extracting("certificate").containsExactly(key);
        assertThat(credentials).extracting("password").containsExactly((Object) "hog".toCharArray());
        return true;
      }
    ));
  }

  @Test
  void shouldSetUsernamePasswordCredentialsInCommand() {
    MirrorConfiguration configuration = createMirrorConfig(new MirrorConfiguration.UsernamePasswordCredential("dent", "hog"));

    invokeCaller(configuration, null);

    verify(mirrorCommandBuilder).setCredentials(argThat(
      credentials -> {
        assertThat(credentials).extracting("username").containsExactly("dent");
        assertThat(credentials).extracting("password").containsExactly("hog");
        return true;
      }
    ));
  }

  @Test
  void shouldSetUrlInCommand() {
    MirrorConfiguration configuration = createMirrorConfig();

    invokeCaller(configuration, null);

    verify(mirrorCommandBuilder).setSourceUrl("https://hog/");
  }

  @Test
  void shouldSetProxyConfigurationInCommand() {
    final MirrorProxyConfiguration mirrorProxyConfiguration = new MirrorProxyConfiguration(true, "http://proxy.hog", 1337, emptyList(), "", "");
    MirrorConfiguration configuration = new MirrorConfiguration("https://hog/", 42, emptyList(), null, null, mirrorProxyConfiguration);

    invokeCaller(configuration, null);

    verify(mirrorCommandBuilder).setProxyConfiguration(mirrorProxyConfiguration);
  }

  @Test
  void shouldSetGpgKeysInCommand() {
    MirrorConfiguration configuration = createMirrorConfig();
    configuration.setGpgVerificationType(MirrorGpgVerificationType.KEY_LIST);
    configuration.setAllowedGpgKeys(singletonList(new RawGpgKey("trillian", "some raw gpg key")));
    PublicKey publicKey = mock(PublicKey.class);
    when(publicKeyParser.parse("some raw gpg key")).thenReturn(publicKey);

    invokeCaller(configuration, null);

    verify(mirrorCommandBuilder).setPublicKeys(argThat((Collection<PublicKey> keys) -> {
      assertThat(keys).contains(publicKey);
      return true;
    }));
  }

  private MirrorCommandCaller.CallResult<Object> invokeCaller(MirrorConfiguration configuration, Object mockedResult) {
    return caller.call(repository, configuration, mirrorCommandBuilder1 -> mockedResult);
  }

  private MirrorConfiguration createMirrorConfig() {
    return createMirrorConfig(null, null);
  }

  private MirrorConfiguration createMirrorConfig(MirrorConfiguration.UsernamePasswordCredential upc) {
    return createMirrorConfig(upc, null);
  }

  private MirrorConfiguration createMirrorConfig(MirrorConfiguration.CertificateCredential cc) {
    return createMirrorConfig(null, cc);
  }

  private MirrorConfiguration createMirrorConfig(MirrorConfiguration.UsernamePasswordCredential upc, MirrorConfiguration.CertificateCredential cc) {
    return new MirrorConfiguration("https://hog/", 42, emptyList(), upc, cc, null);
  }
}
