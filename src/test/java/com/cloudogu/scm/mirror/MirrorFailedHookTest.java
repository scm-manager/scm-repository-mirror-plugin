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
import sonia.scm.repository.api.MirrorCommandResult;

import javax.inject.Provider;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MirrorFailedHookTest {

  private static final Repository REPOSITORY = RepositoryTestData.create42Puzzle();

  @Mock
  private Provider<MirrorConfigurationStore> configStoreProvider;
  @Mock
  private MirrorStatusStore statusStore;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MailService mailService;
  @Mock
  private ScmConfiguration scmConfiguration;

  @InjectMocks
  private MirrorFailedHook hook;

  @BeforeEach
  void shouldInitHookConfig() {
    lenient().when(scmConfiguration.getBaseUrl()).thenReturn("http://hitchhiker.org/scm");
  }

  @Test
  void shouldOnlySendMailOnFailedMirror() {
    hook.handleEvent(new MirrorSyncEvent(REPOSITORY, new MirrorCommandResult(true, emptyList(), Duration.ZERO)));

    verify(configStoreProvider, never()).get();
  }

  @Test
  void shouldSendMailForEveryManagingUser() {
    MirrorConfiguration mirrorConfig = new MirrorConfiguration();
    mirrorConfig.setManagingUsers(ImmutableList.of("trillian", "arthur", "zaphod"));
    mockConfigStore(mirrorConfig);
    mockStatusStore(MirrorStatus.success(Instant.now()));

    hook.handleEvent(new MirrorSyncEvent(REPOSITORY, new MirrorCommandResult(false, emptyList(), Duration.ZERO)));

    verify(mailService, times(3)).emailTemplateBuilder();
  }

  @Test
  void shouldNotSendMailIfPreviousStatusWasAlsoFailed() {
    MirrorConfiguration mirrorConfig = new MirrorConfiguration();
    mirrorConfig.setManagingUsers(ImmutableList.of("trillian", "arthur", "zaphod"));
    mockConfigStore(mirrorConfig);
    mockStatusStore(MirrorStatus.failed(Instant.now()));

    hook.handleEvent(new MirrorSyncEvent(REPOSITORY, new MirrorCommandResult(false, emptyList(), Duration.ZERO)));

    verify(mailService, never()).emailTemplateBuilder();
  }

  private void mockStatusStore(MirrorStatus status) {
    when(statusStore.getStatus(REPOSITORY)).thenReturn(status);
  }

  private void mockConfigStore(MirrorConfiguration mirrorConfig) {
    MirrorConfigurationStore configurationStore = mock(MirrorConfigurationStore.class);
    when(configStoreProvider.get()).thenReturn(configurationStore);
    when(configurationStore.getConfiguration(REPOSITORY)).thenReturn(Optional.of(mirrorConfig));
  }
}
