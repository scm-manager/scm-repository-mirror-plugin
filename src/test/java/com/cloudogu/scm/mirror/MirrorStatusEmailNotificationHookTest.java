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

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailService;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import javax.inject.Provider;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MirrorStatusEmailNotificationHookTest {

  private static final Repository REPOSITORY = RepositoryTestData.create42Puzzle();

  @Mock
  private Provider<MirrorConfigurationStore> configStoreProvider;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MailService mailService;
  @Mock
  private ScmConfiguration scmConfiguration;

  @InjectMocks
  private MirrorStatusEmailNotificationHook hook;

  @BeforeEach
  void shouldInitHookConfig() {
    lenient().when(scmConfiguration.getBaseUrl()).thenReturn("http://hitchhiker.org/scm");
  }

  @Test
  void shouldSendMailForEveryManagingUser() {
    MirrorConfiguration mirrorConfig = new MirrorConfiguration();
    mirrorConfig.setManagingUsers(ImmutableList.of("trillian", "arthur", "zaphod"));
    mockConfigStore(mirrorConfig);

    hook.handleEvent(
      new MirrorStatusChangedEvent(
        REPOSITORY,
        createStatusResult(MirrorStatus.Result.SUCCESS),
        createStatusResult(MirrorStatus.Result.FAILED)
      )
    );

    verify(mailService, times(3)).emailTemplateBuilder();
  }

  @Test
  void shouldNotSendMailIfPreviousStatusWasAlsoFailed() {
    MirrorConfiguration mirrorConfig = new MirrorConfiguration();
    mirrorConfig.setManagingUsers(ImmutableList.of("trillian", "arthur", "zaphod"));

    hook.handleEvent(
      new MirrorStatusChangedEvent(
        REPOSITORY,
        createStatusResult(MirrorStatus.Result.FAILED),
        createStatusResult(MirrorStatus.Result.FAILED)
      )
    );

    verify(mailService, never()).emailTemplateBuilder();
  }

  private void mockConfigStore(MirrorConfiguration mirrorConfig) {
    MirrorConfigurationStore configurationStore = mock(MirrorConfigurationStore.class);
    when(configStoreProvider.get()).thenReturn(configurationStore);
    when(configurationStore.getConfiguration(REPOSITORY)).thenReturn(Optional.of(mirrorConfig));
  }

  private MirrorStatus.Result createStatusResult(MirrorStatus.Result result) {
    return MirrorStatus.create(result, Instant.now()).getResult();
  }
}
