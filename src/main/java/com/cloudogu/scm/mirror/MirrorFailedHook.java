package com.cloudogu.scm.mirror;

import com.github.legman.Subscribe;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.Category;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.mail.api.MailTemplateType;
import sonia.scm.mail.api.Topic;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.MirrorCommandResult;

import javax.inject.Inject;
import javax.inject.Provider;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static java.util.ResourceBundle.getBundle;

@Extension
@EagerSingleton
@Requires("scm-mail-plugin")
public class MirrorFailedHook {

  private static final Logger LOG = LoggerFactory.getLogger(MirrorFailedHook.class);

  public static final String MIRROR_FAILED_EVENT_DISPLAY_NAME = "mirrorFailed";
  private static final Category CATEGORY = new Category("scm-manager-core");
  static final Topic TOPIC_MIRROR_FAILED = new Topic(CATEGORY, MIRROR_FAILED_EVENT_DISPLAY_NAME);
  protected static final String MIRROR_TEMPLATE_PATH = "com/cloudogu/mail/emailnotification/mirror_failed.mustache";
  private static final String SCM_REPOSITORY_URL_PATTERN = "{0}/repo/{1}/{2}/settings/general/";

  private static final String SUBJECT_PATTERN = "{0}/{1} {2}";

  private static final Map<Locale, ResourceBundle> SUBJECT_BUNDLES = Maps
    .asMap(new HashSet<>(Arrays.asList(ENGLISH, GERMAN)), locale -> getBundle("com.cloudogu.mail.emailnotification.Subjects", locale));


  private final MailService mailService;
  private final Provider<MirrorConfigurationStore> configStoreProvider;
  private final MirrorStatusStore statusStore;
  private final ScmConfiguration scmConfiguration;

  @Inject
  public MirrorFailedHook(MailService mailService, Provider<MirrorConfigurationStore> mirrorConfigurationStore, MirrorStatusStore statusStore, ScmConfiguration scmConfiguration) {
    this.mailService = mailService;
    this.configStoreProvider = mirrorConfigurationStore;
    this.statusStore = statusStore;
    this.scmConfiguration = scmConfiguration;
  }

  //We depend on stored data therefore this should happen synchronously to prevent race conditions
  @Subscribe(async = false)
  public void handleEvent(MirrorSyncEvent event) {
    if (event.getResult().getResult() != MirrorCommandResult.ResultType.OK) {
      Optional<MirrorConfiguration> mirrorConfiguration = configStoreProvider.get().getConfiguration(event.getRepository());
      if (mirrorConfiguration.isPresent() && shouldSendEmail(event, mirrorConfiguration.get())) {
        for (String user : mirrorConfiguration.get().getManagingUsers()) {
          try {
            mailService
              .emailTemplateBuilder()
              .onTopic(TOPIC_MIRROR_FAILED)
              .toUser(user)
              .withSubject(getMailSubject(event, ENGLISH))
              .withSubject(GERMAN, getMailSubject(event, GERMAN))
              .withTemplate(MIRROR_TEMPLATE_PATH, MailTemplateType.MARKDOWN_HTML)
              .andModel(getTemplateModel(event))
              .send();
          } catch (MailSendBatchException e) {
            LOG.warn("Could not send email for failing mirror attempt", e);
          }
        }
      }
    }
  }

  private boolean shouldSendEmail(MirrorSyncEvent event, MirrorConfiguration mirrorConfiguration) {
    return mirrorConfiguration.getManagingUsers() != null
      && statusStore.getStatus(event.getRepository()).getResult() != MirrorStatus.Result.FAILED;
  }

  private String getMailSubject(MirrorSyncEvent event, Locale locale) {
    Repository repository = event.getRepository();
    String displayEventName = SUBJECT_BUNDLES.get(locale).getString(MIRROR_FAILED_EVENT_DISPLAY_NAME);
    return MessageFormat.format(SUBJECT_PATTERN, repository.getNamespace(), repository.getName(), displayEventName);
  }

  private Map<String, Object> getTemplateModel(MirrorSyncEvent event) {
    Map<String, Object> result = Maps.newHashMap();
    result.put("namespace", event.getRepository().getNamespace());
    result.put("name", event.getRepository().getName());
    result.put("link", getRepositoryLink(event.getRepository()));
    return result;
  }

  private String getRepositoryLink(Repository repository) {
    String baseUrl = scmConfiguration.getBaseUrl();
    return MessageFormat.format(SCM_REPOSITORY_URL_PATTERN, baseUrl, repository.getNamespace(), repository.getName());
  }
}
