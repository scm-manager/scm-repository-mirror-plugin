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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.web.security.AdministrationContext;
import sonia.scm.web.security.PrivilegedAction;

import static com.google.inject.util.Providers.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class TaskDecoratorFactoryTest {

  @Mock
  private AdministrationContext administrationContext;
  @Mock
  private PrivilegedMirrorRunner privilegedMirrorRunner;

  private TaskDecoratorFactory factory;

  private boolean runCalled = false;

  @BeforeEach
  void createFactory() {
    factory = new TaskDecoratorFactory(administrationContext, of(privilegedMirrorRunner));
  }

  @BeforeEach
  void mockMocks() {
    doAnswer(invocationOnMock -> {
      invocationOnMock.getArgument(0, PrivilegedAction.class).run();
      return null;
    }).when(administrationContext).runAsAdmin(any(PrivilegedAction.class));
    doAnswer(invocationOnMock -> {
      invocationOnMock.getArgument(0, Runnable.class).run();
      return null;
    }).when(privilegedMirrorRunner).exceptedFromReadOnly(any());
  }

  @Test
  void shouldRunRunnable() {
    factory.decorate(() -> runCalled = true).run();

    assertThat(runCalled).isTrue();
  }

  @Test
  void shouldFetchException() {
    factory.decorate(() -> {
      throw new RuntimeException("gotcha");
    }).run();

    // we're fine unless we do not get an exception
  }
}
