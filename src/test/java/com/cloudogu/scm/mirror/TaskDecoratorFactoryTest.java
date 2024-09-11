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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.web.security.AdministrationContext;
import sonia.scm.web.security.PrivilegedAction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class TaskDecoratorFactoryTest {

  @Mock
  private AdministrationContext administrationContext;

  private TaskDecoratorFactory factory;

  private boolean runCalled = false;

  @BeforeEach
  void createFactory() {
    factory = new TaskDecoratorFactory(administrationContext);
  }

  @BeforeEach
  void mockMocks() {
    doAnswer(invocationOnMock -> {
      invocationOnMock.getArgument(0, PrivilegedAction.class).run();
      return null;
    }).when(administrationContext).runAsAdmin(any(PrivilegedAction.class));
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
