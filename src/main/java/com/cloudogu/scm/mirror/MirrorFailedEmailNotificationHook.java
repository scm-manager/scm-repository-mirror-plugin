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
public class MirrorFailedEmailNotificationHook {

  private static final Logger LOG = LoggerFactory.getLogger(MirrorFailedEmailNotificationHook.class);

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
  private final ScmConfiguration scmConfiguration;

  @Inject
  public MirrorFailedEmailNotificationHook(MailService mailService, Provider<MirrorConfigurationStore> mirrorConfigurationStore, ScmConfiguration scmConfiguration) {
    this.mailService = mailService;
    this.configStoreProvider = mirrorConfigurationStore;
    this.scmConfiguration = scmConfiguration;
  }

  @Subscribe
  public void handleEvent(MirrorStatusChangedEvent event) {
    if (event.getNewResult() == MirrorStatus.Result.FAILED) {
      Optional<MirrorConfiguration> mirrorConfiguration = configStoreProvider.get().getConfiguration(event.getRepository());
      if (mirrorConfiguration.isPresent() && shouldSendEmail(event, mirrorConfiguration.get())) {
        for (String user : mirrorConfiguration.get().getManagingUsers()) {
          sendMail(event, user);
        }
      }
    }
  }

  private void sendMail(MirrorStatusChangedEvent event, String user) {
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

  private boolean shouldSendEmail(MirrorStatusChangedEvent event, MirrorConfiguration mirrorConfiguration) {
    return mirrorConfiguration.getManagingUsers() != null
      && event.getNewResult() != event.getPreviousResult();
  }

  private String getMailSubject(MirrorStatusChangedEvent event, Locale locale) {
    Repository repository = event.getRepository();
    String displayEventName = SUBJECT_BUNDLES.get(locale).getString(MIRROR_FAILED_EVENT_DISPLAY_NAME);
    return MessageFormat.format(SUBJECT_PATTERN, repository.getNamespace(), repository.getName(), displayEventName);
  }

  private Map<String, Object> getTemplateModel(MirrorStatusChangedEvent event) {
    Map<String, Object> result = Maps.newHashMap();
    Repository repository = event.getRepository();
    result.put("namespace", repository.getNamespace());
    result.put("name", repository.getName());
    result.put("link", getRepositoryLink(repository));
    return result;
  }

  private String getRepositoryLink(Repository repository) {
    String baseUrl = scmConfiguration.getBaseUrl();
    return MessageFormat.format(SCM_REPOSITORY_URL_PATTERN, baseUrl, repository.getNamespace(), repository.getName());
  }
}
