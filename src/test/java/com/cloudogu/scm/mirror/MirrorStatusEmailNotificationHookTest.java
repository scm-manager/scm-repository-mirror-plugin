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

import jakarta.inject.Provider;
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
