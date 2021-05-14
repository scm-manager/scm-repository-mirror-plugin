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

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.RepositoryLinkProvider;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.web.JsonMockHttpRequest;
import sonia.scm.web.RestDispatcher;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.inject.util.Providers.of;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MirrorRootResourceTest {

  private final RestDispatcher dispatcher = new RestDispatcher();

  @Mock
  private RepositoryLinkProvider repositoryLinkProvider;
  @Mock
  private MirrorService mirrorService;

  private final URI baseUri = URI.create("/");

  @BeforeEach
  void initResource() {
    dispatcher.addSingletonResource(new MirrorRootResource(of(new MirrorResource()), repositoryLinkProvider, mirrorService));
  }

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
      .json("{'namespace':'hitchhiker', 'name':'heart-of-gold', 'url':'http://hog/git'}");
    MockHttpResponse response = new MockHttpResponse();

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getOutputHeaders())
      .containsEntry("Location", singletonList(URI.create("/repository/hitchhiker/heart-of-gold")));
    verify(mirrorService).createMirror(
      argThat(mirrorRequest -> {
        assertThat(mirrorRequest.getUrl()).isEqualTo("http://hog/git");
        return true;
      }),
      argThat(repository -> {
        assertThat(repository.getName()).isEqualTo("heart-of-gold");
        assertThat(repository.getNamespace()).isEqualTo("hitchhiker");
        return true;
      })
    );
  }
}
