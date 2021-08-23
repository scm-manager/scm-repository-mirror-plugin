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

import com.cloudogu.scm.mirror.LogEntry;
import com.cloudogu.scm.mirror.LogStore;
import com.cloudogu.scm.mirror.MirrorAccessConfiguration;
import com.cloudogu.scm.mirror.MirrorConfiguration;
import com.cloudogu.scm.mirror.MirrorConfigurationStore;
import com.cloudogu.scm.mirror.MirrorProxyConfiguration;
import com.cloudogu.scm.mirror.MirrorService;
import com.cloudogu.scm.mirror.MirrorStatus;
import com.cloudogu.scm.mirror.MirrorStatus.Result;
import com.fasterxml.jackson.databind.JsonNode;
import de.otto.edison.hal.Links;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.RepositoryLinkProvider;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.web.JsonMockHttpRequest;
import sonia.scm.web.JsonMockHttpResponse;
import sonia.scm.web.RestDispatcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.google.inject.util.Providers.of;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.web.MockScmPathInfoStore.forUri;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
class MirrorRootResourceTest {

  private final RestDispatcher dispatcher = new RestDispatcher();

  @Mock
  private RepositoryLinkProvider repositoryLinkProvider;
  @Mock
  private MirrorService mirrorService;
  @Mock
  private RepositoryManager repositoryManager;
  @Mock
  private MirrorConfigurationStore configurationStore;
  @Mock
  private LogStore logStore;

  private final GlobalMirrorConfigurationToGlobalConfigurationDtoMapper toDtoMapper = Mappers.getMapper(GlobalMirrorConfigurationToGlobalConfigurationDtoMapper.class);
  private final MirrorAccessConfigurationToConfigurationDtoMapper configurationToConfigurationDtoMapper = Mappers.getMapper(MirrorAccessConfigurationToConfigurationDtoMapper.class);
  private final MirrorFilterConfigurationToDtoMapper toFiltersDtoMapper = Mappers.getMapper(MirrorFilterConfigurationToDtoMapper.class);

  @BeforeEach
  void initResource() {
    MirrorResource mirrorResource = new MirrorResource(configurationStore, mirrorService, repositoryManager, configurationToConfigurationDtoMapper, logStore, toFiltersDtoMapper);
    configurationToConfigurationDtoMapper.setScmPathInfoStore(forUri("/"));
    toDtoMapper.setScmPathInfoStore(forUri("/"));
    toFiltersDtoMapper.setScmPathInfoStore(forUri("/"));
    dispatcher.addSingletonResource(new MirrorRootResource(of(mirrorResource), repositoryLinkProvider, mirrorService, configurationStore, toDtoMapper));
  }

  @Nested
  class ForNewMirror {
    @BeforeEach
    void initMocks() {
      when(mirrorService.createMirror(any(), any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(1));
      when(repositoryLinkProvider.get(any())).thenAnswer(invocationOnMock -> {
        NamespaceAndName namespaceAndName = invocationOnMock.getArgument(0, NamespaceAndName.class);
        return format("/repository/%s/%s", namespaceAndName.getNamespace(), namespaceAndName.getName());
      });
    }

    @Test
    void shouldExecuteBasicRequest() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest.post("/v2/mirror/repositories")
        .json("{'namespace':'hitchhiker', 'name':'HeartOfGold', 'type':'git', 'url':'http://hog/git', 'synchronizationPeriod':42, 'gpgVerificationType':'NONE','proxyConfiguration':{}}");
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(201);
      assertThat(response.getOutputHeaders())
        .containsEntry("Location", singletonList(URI.create("/repository/hitchhiker/HeartOfGold")));
      verify(mirrorService).createMirror(
        argThat(mirrorRequest -> {
          assertThat(mirrorRequest.getUrl()).isEqualTo("http://hog/git");
          assertThat(mirrorRequest.getSynchronizationPeriod()).isEqualTo(42);
          return true;
        }),
        argThat(repository -> {
          assertThat(repository.getName()).isEqualTo("HeartOfGold");
          assertThat(repository.getNamespace()).isEqualTo("hitchhiker");
          return true;
        })
      );
    }

    @Test
    void shouldConfigureBasicAuth() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest.post("/v2/mirror/repositories")
        .json("{'namespace':'hitchhiker', 'name':'HeartOfGold', 'type':'git', 'url':'http://hog/git', 'synchronizationPeriod':42, 'usernamePasswordCredential':{'username':'trillian','password':'hog'}, 'gpgVerificationType':'NONE','proxyConfiguration':{}}");
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      verify(mirrorService).createMirror(
        argThat(mirrorRequest -> {
          assertThat(mirrorRequest.getUsernamePasswordCredential()).isNotNull();
          assertThat(mirrorRequest.getCertificateCredential()).isNull();
          assertThat(mirrorRequest.getUsernamePasswordCredential().getUsername()).isEqualTo("trillian");
          assertThat(mirrorRequest.getUsernamePasswordCredential().getPassword()).isEqualTo("hog");
          return true;
        }),
        any()
      );
    }

    @Test
    void shouldConfigureCertificateAuth() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest.post("/v2/mirror/repositories")
        .json("{'namespace':'hitchhiker', 'name':'HeartOfGold', 'type':'git', 'url':'http://hog/git', 'synchronizationPeriod':42, 'gpgVerificationType':'NONE', 'certificateCredential':{'certificate':'" + BASE64_ENCODED_CERTIFICATE + "','password':'hog'},'proxyConfiguration':{}}");
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      verify(mirrorService).createMirror(
        argThat(mirrorRequest -> {
          assertThat(mirrorRequest.getUsernamePasswordCredential()).isNull();
          assertThat(mirrorRequest.getCertificateCredential()).isNotNull();
          assertThat(mirrorRequest.getCertificateCredential().getCertificate()).contains(CERTIFICATE);
          assertThat(mirrorRequest.getCertificateCredential().getPassword()).isEqualTo("hog");
          return true;
        }),
        any()
      );
    }
  }

  @Test
  void shouldFailToGetAccessConfigurationForMissingRepository() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.get("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration");
    MockHttpResponse response = new MockHttpResponse();

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void shouldFailToGetFilterConfigurationForMissingRepository() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.get("/v2/mirror/repositories/hitchhiker/HeartOfGold/filterConfiguration");
    MockHttpResponse response = new MockHttpResponse();

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void shouldFailToSetAccessConfigurationForMissingRepository() throws URISyntaxException {
    JsonMockHttpRequest request = JsonMockHttpRequest
      .put("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration")
      .json("{'url':'http://hog/scm', 'synchronizationPeriod':42,'proxyConfiguration':{}}");
    MockHttpResponse response = new MockHttpResponse();

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void shouldFailToSetFilterConfigurationForMissingRepository() throws URISyntaxException {
    JsonMockHttpRequest request = JsonMockHttpRequest
      .put("/v2/mirror/repositories/hitchhiker/HeartOfGold/filterConfiguration")
      .json("{'gpgVerificationType':'NONE'}");
    MockHttpResponse response = new MockHttpResponse();

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void shouldValidateNewAccessConfiguration() throws URISyntaxException {
    JsonMockHttpRequest request = JsonMockHttpRequest
      .put("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration")
      .json("{}");
    MockHttpResponse response = new MockHttpResponse();

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(400);
    verify(configurationStore, never()).setAccessConfiguration(any(), any());
  }

  @Test
  void shouldRetrieveGlobalConfigurationFromService() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest
      .get("/v2/mirror/configuration");
    MockHttpResponse response = new MockHttpResponse();

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(204);
    verify(configurationStore).getGlobalConfiguration();
  }

  @Nested
  @ExtendWith(ShiroExtension.class)
  class ForSingleRepository {

    private final Repository repository = RepositoryTestData.createHeartOfGold();

    @BeforeEach
    void setUpManager() {
      doReturn(repository).when(repositoryManager).get(repository.getNamespaceAndName());
    }

    @Test
    void shouldNotValidateProxyConfigurationIfOverwriteIsFalse() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration")
        .json("{'url':'test.dummy','synchronizationPeriod':10,'proxyConfiguration':{'overwriteGlobalConfiguration':false}}");
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(204);
      verify(configurationStore)
        .setAccessConfiguration(
          eq(repository),
          argThat(configuration -> {
            assertThat(configuration.getProxyConfiguration()).isNotNull();
            assertThat(configuration.getProxyConfiguration().isOverwriteGlobalConfiguration()).isFalse();
            return true;
          }));
    }

    @Test
    void shouldValidateProxyConfiguration() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration")
        .json("{'url':'test.dummy','synchronizationPeriod':10,'proxyConfiguration':{'overwriteGlobalConfiguration':true,'url':'','port':-1}}");
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(400);
      verify(configurationStore, never()).setAccessConfiguration(any(), any());
    }

    @Test
    void shouldSetUrlAndPeriodInAccessConfiguration() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration")
        .json("{'url':'http://hog/scm', 'synchronizationPeriod':42,'proxyConfiguration':{}}");
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(204);
      verify(configurationStore)
        .setAccessConfiguration(
          eq(repository),
          argThat(configuration -> {
            assertThat(configuration.getUrl()).isEqualTo("http://hog/scm");
            assertThat(configuration.getSynchronizationPeriod()).isEqualTo(42);
            return true;
          }));
    }

    @Test
    void shouldSetUsernamePasswordCredentialInAccessConfiguration() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration")
        .json("{'url':'http://hog/scm', 'synchronizationPeriod':42, 'usernamePasswordCredential':{'username':'dent', 'password':'hg2g'},'proxyConfiguration':{}}");
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(204);
      verify(configurationStore)
        .setAccessConfiguration(
          any(),
          argThat(configuration -> {
            assertThat(configuration.getUsernamePasswordCredential().getUsername()).isEqualTo("dent");
            assertThat(configuration.getUsernamePasswordCredential().getPassword()).isEqualTo("hg2g");
            return true;
          }));
    }

    @Test
    void shouldExecuteUpdateRequest() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest.post("/v2/mirror/repositories/hitchhiker/HeartOfGold/sync");
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(204);
      verify(mirrorService).updateMirror(repository);
    }

    @Nested
    class WithExistingConfiguration {

      @BeforeEach
      void mockExistingConfiguration() {
        MirrorConfiguration existingConfiguration =
          new MirrorConfiguration(
            "http://hog/",
            42,
            emptyList(),
            new MirrorAccessConfiguration.UsernamePasswordCredential("dent", "hog"),
            new MirrorConfiguration.CertificateCredential(CERTIFICATE, "hg2g"),
            new MirrorProxyConfiguration(true, "foo.bar", 1337, Arrays.asList("foo", "bar"), "trillian", "secret123"));
        when(configurationStore.getConfiguration(repository))
          .thenReturn(Optional.of(existingConfiguration));
      }

      @Test
      void shouldGetAccessConfiguration() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration");
        JsonMockHttpResponse response = new JsonMockHttpResponse();

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        MirrorAccessConfigurationDto configurationDto = response.getContentAs(MirrorAccessConfigurationDto.class);
        assertThat(configurationDto.getUrl()).isEqualTo("http://hog/");
        assertThat(configurationDto.getUsernamePasswordCredential().getUsername()).isEqualTo("dent");
        assertThat(configurationDto.getUsernamePasswordCredential().getPassword()).isEqualTo("_DUMMY_");
        assertThat(configurationDto.getCertificateCredential().getCertificate()).isNull();
        assertThat(configurationDto.getCertificateCredential().getPassword()).isEqualTo("_DUMMY_");
        assertThat(configurationDto.getLinks().getLinkBy("self")).get().extracting("href")
          .isEqualTo("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration");
      }

      @Test
      @SubjectAware(
        value = "trillian",
        permissions = "repository:mirror:*"
      )
      void shouldCreateUpdateLinkWithPermission() throws URISyntaxException {
        MirrorConfiguration existingConfiguration =
          new MirrorConfiguration("http://hog/", 42, emptyList(), null, null, null);
        when(configurationStore.getConfiguration(repository))
          .thenReturn(Optional.of(existingConfiguration));
        MirrorAccessConfigurationDto mirrorConfigurationDto = new MirrorAccessConfigurationDto(new Links.Builder().build());
        mirrorConfigurationDto.setUrl("http://hog/");
        mirrorConfigurationDto.setSynchronizationPeriod(42);

        MockHttpRequest request = MockHttpRequest.get("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration");
        JsonMockHttpResponse response = new JsonMockHttpResponse();

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        MirrorAccessConfigurationDto configurationDto = response.getContentAs(MirrorAccessConfigurationDto.class);
        assertThat(configurationDto.getLinks().getLinkBy("update")).get().extracting("href")
          .isEqualTo("/v2/mirror/repositories/hitchhiker/HeartOfGold/accessConfiguration");
      }
    }
  }

  @Nested
  class LogTests {

    private final Repository repository = RepositoryTestData.createHeartOfGold();

    @BeforeEach
    void setUpManager() {
      doReturn(repository).when(repositoryManager).get(repository.getNamespaceAndName());
    }

    @Test
    void shouldReturnLogs() throws URISyntaxException, UnsupportedEncodingException {
      when(logStore.get(repository)).thenReturn(Collections.singletonList(log()));

      MockHttpRequest request = MockHttpRequest.get("/v2/mirror/repositories/hitchhiker/HeartOfGold/logs");
      JsonMockHttpResponse response = new JsonMockHttpResponse();

      dispatcher.invoke(request, response);

      System.out.println(response.getContentAsString());

      assertThat(response.getStatus()).isEqualTo(200);
      JsonNode json = response.getContentAsJson();

      JsonNode jsonEntry = json.get("_embedded").get("entries").get(0);
      assertThat(jsonEntry.get("result").asText()).isEqualTo("FAILED");
      assertThat(jsonEntry.get("started").asText()).isNotNull();
      assertThat(jsonEntry.get("ended").asText()).isNotNull();
      assertThat(jsonEntry.get("log").get(0).asText()).isEqualTo("not so awesome");
    }

    @Test
    void shouldReturnSelfLink() throws URISyntaxException {
      MockHttpRequest request = MockHttpRequest.get("/v2/mirror/repositories/hitchhiker/HeartOfGold/logs");
      JsonMockHttpResponse response = new JsonMockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(200);
      JsonNode json = response.getContentAsJson();
      assertThat(
        json.get("_links").get("self").get("href").asText()
      ).isEqualTo("/v2/mirror/repositories/hitchhiker/HeartOfGold/logs");
    }

    private LogEntry log() {
      Instant started = Instant.now().minusMillis(21L);
      return new LogEntry(MirrorStatus.create(Result.FAILED, started), "not so awesome");
    }

  }

  @BeforeAll
  @SuppressWarnings("UnstableApiUsage")
  static void readCertificate() throws IOException {
    CERTIFICATE = toByteArray(getResource("com/cloudogu/scm/mirror/client.pfx"));
  }

  private static byte[] CERTIFICATE;
  private static final String BASE64_ENCODED_CERTIFICATE = "MIIJ4QIBAzCCCacGCSqGSIb3DQEHAaCCCZgEggmUMIIJkDCCBEcGCSqGSIb3DQEHBqCCBDgwggQ0\\n" +
    "AgEAMIIELQYJKoZIhvcNAQcBMBwGCiqGSIb3DQEMAQYwDgQI9iXc2UvgHhwCAggAgIIEAKo7Vigr\\n" +
    "gQHP6JaQk8kcuBAYFxHv6rYUJol32pGWOakVS6q4Cimtxcnsfe6ev8pePgimm9hWlQM31ipKWf1Q\\n" +
    "8RaKszmos14UXq582b1YZdX7UbF8WmUMU3J5AHdfE/rxgqzbUfGqAJB7t77DUlFD7L5XYR9+beUC\\n" +
    "7/lKMpVl9iAhXUQiDTvF1sYkb7m6bNSEqCotzOhKX8/wQO+B8jDDHXgeNa8Viiq+NxQpuJhM/1tH\\n" +
    "1w1PphOTKttXVa7eymyzDZcgCyN4OBIvL8zKZrB4kIROSOe39ZmFV5v+mkUvlcpi4QULYOa6SQLX\\n" +
    "Ok/Elat2wfkhljwoxQ4SBKXyCAidJz2TkaZvdKYP9KlnEZSCxyzS2AGXj1yU8SG8aNzp9vJ+4x8r\\n" +
    "tTdih9yl1WKeJSaLfF1+QI8MduK/1bpi0U/8iM21EHaYKVGzDea5pgQd6tockVskXTkYxYYKAi+z\\n" +
    "NQwvf/ZzjbzBzIoGnV4qK1Af9K8HZi9qI4fxM39vgd79wUkGSuA54+QrbRXPVfvX2FtDP83OJ+Gr\\n" +
    "+0hpmrcr/mR7BRWFBJDFLBpQ/sTQZ1JzpXGbaOPNVtT9ynfRzVDSG0SnM9yRlfPAotyIIsaYDjAT\\n" +
    "qMjxQ3WnilHGNG5zN9afHOr/RGVRsR7txOXSgbJG98cMUwWbt/vhZouxKLQ1qEwz3krkqkeol3He\\n" +
    "NgZqhFW1qz60/2n3M0WzF+uBACHq/GhBfqjIGLHTStFbLVdR+NVW2LjJB048f5lWbP1bgQGapcO/\\n" +
    "7Krk1UUsXvnE8TjEJM/Lht4z6bHqKu6SCDdZapuMFLXRd13Tl4ZFDNbT6DGbVzTjrSdmEoC3bFZS\\n" +
    "hJVE3+BxrZzddKp6kkeVH75xwRGjecP7+JODGe2IaXTiiCn7cf6oT3Fg1rTg5i0+mVcCNS3XhOzD\\n" +
    "EaqR40YqpERrJ/Itu7Y1lUSEYLdjMPZVSgOlJJdyjHhu3r/0p5nk5PL/5Iry2OlHyn7I0x9OJsEn\\n" +
    "4aZnlgZMDbT2BFo97923MVktOZA3i6uh5yfo34xzQYOMbOtFVxe/4rjm3ZvafiNLMwo6bzbUQtWS\\n" +
    "jYXI9YltsO5c4dYlv451rkwUQEVc9z1q13asBONWlwkonAh6sqZlb9xPjWtFwtyjY7pTcU+/R3s/\\n" +
    "53AL4OipMvYzyND+luFyk8ukw+v6yOGuaFl6p3zlC2pUb0GVvzrtFNSE0YmDA/lvY1cYbyL6MfqO\\n" +
    "OzRjuB2u1HCamWiDYySpCZ2OJfO07N5mQZZXcslA7OoDiJcZKbZG0+WC7ocQPaeNDF48OMidYN+3\\n" +
    "8120x/zi+l2ddhERtvRF8TyWfj9SCjDYvU2di/ForfSWDVzCnwf0TB8QGr85uM10I74wggVBBgkq\\n" +
    "hkiG9w0BBwGgggUyBIIFLjCCBSowggUmBgsqhkiG9w0BDAoBAqCCBO4wggTqMBwGCiqGSIb3DQEM\\n" +
    "AQMwDgQIdyBkf7OSIxICAggABIIEyJdPPdDJWcBE+C5DWu8qm1fuq6hP7LoTytm2No6pAHj2HABU\\n" +
    "9Ql5kM1Q3zbLr18iK7780DHBfTOB8Bcc+P22xFdN84MTpCaehdiwHTpvThMAdGg89OU+SZVxUJaR\\n" +
    "InXU3d4Vzit0HyFSSKPOjx41UiRv3HyKLYi2zzaozJ+AchvIhOzg1vt8bjCFm/9s7f+Kj4/8yYzC\\n" +
    "+E5UbYgzyKvFeecUyHOvMq/58TI3gvlKhbD8yrrKVDeyJFQzHr3dfUohoF4jnjaK4RgKYRF/4ptI\\n" +
    "I+CejneLopBFUgN8fVr86QxVSzW7mKypnWfCO5Lxzbx7bL233ysRIscolU6+Pk9X2UvTGr+xIgMX\\n" +
    "L91QEQ3CaVb+dxHLTNNsR+rfJEEs/2F/bMNxtRs6ewowqZDPOuTZxsYiV9d8oyf6aSbLslwOYQrR\\n" +
    "f0C9Qg3U+7r3Hg3IzywisKSzORy/x484yB3uTmY041e8dVXJEqZLQSTDOPeRycJIYRdbuseT1bwZ\\n" +
    "xsitsjz9+ESIonzLj2T+h52hRKJ8xQOWkCpTuGyqVIoKA/sLIkkVqRcLfTXuBSiObvOXUv+WqEC1\\n" +
    "6R1MBTv+qJn60UFNH5H51E06p+hupzYkAmT/D73G63kVTFbcgQRc36RRc9Eng4qeignNSadpBmFz\\n" +
    "0lxVW1oGEYZ5KT6S88RS6X6iy6v7MSQxhYQqJCcNespRFwQjO9DkPLiuzSyczRWd6m4p3aezfZ2Q\\n" +
    "HuomKgS+njg3aXV5BUYnvDNT04xLvBn+u+oomIWNhNkDz9cRCjPRocrnuv7Df0sswvqYJAEpMzsj\\n" +
    "lZA5KLWMnoswCSU09jz2Came9EjVJXqP0EODc56obwQo/3h76KoizoL4HpGEsTqoToM0zFzb0Dva\\n" +
    "9w1ktUqiCxbbcdwfs2mvK9yDeJsHaHfvgUBObdOAYsPRl/d38fj17MqLwGmDgVjVaFm5JodutAci\\n" +
    "OctDMHbtWih5ORQMFjxFpz/Tg9HL8aapmLO03Azkut4Pg2w5yoXnXJwN+dBB23TkDg96p4o5odgk\\n" +
    "Fs0S4MRZLSMEHSfkhAuLHoGz7Ev1AxS8EfCW4LXjhlGNDX7K6EnQ6NhA/mGKT+hMWxccJ3ZAeDjm\\n" +
    "SoEEbLDxXjn1j7aL0OjYywArHUosIpadet1eCXl8fCVwSnl7zmPc3hHdGgMOfAHsZaAi08DNsBqJ\\n" +
    "nhZFwzlD+a4PAArKmgZI10Xxya2YqEWWRoS011+ram4PiAJhpQ64u/4czHBIpeHiuyrvvOEN/hGn\\n" +
    "ZLGR5fvf3mtwPCfl7R0pmnuf58djXtKK0NAFNJ0b0wUHFDYoRDRZZVe7hdii7wewCYjpMpLZGL1o\\n" +
    "nmpjPTCIz7EdSawNHiEc16DBcuYG9l6Z2YJFMELqbsvN03MZJztdYAnDFE+Ov/zoB0J5FrnMfNm+\\n" +
    "jQL8LCHAnZW4ZHHq7EQgnjzeCaP1YP6pcgGJ67j9tQMjeL35wCvdL2oBYeVFp757ofyq8/M0c8vg\\n" +
    "/VJ3uo66tWisv746mBgW0W3b30ad8mvPHVpBsGvcxtBU7j8d3WPxXLSmHWycBoImOCFZB99srUw3\\n" +
    "vlL8s+bqCc12mDJ6Gs4hHQU0GKPDLaSwIigghYYmHK8OGaJfv6NHgEE4GGL0nocYjjElMCMGCSqG\\n" +
    "SIb3DQEJFTEWBBR8h2YPZm9daVUTH8MKmuQqU4VDMzAxMCEwCQYFKw4DAhoFAAQUDae9qlD8UpOS\\n" +
    "4rMbz4JgOgYOwoEECI01fK5liCPiAgIIAA==";
}
