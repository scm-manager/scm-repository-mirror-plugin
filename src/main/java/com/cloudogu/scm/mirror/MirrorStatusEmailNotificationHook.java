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

import com.github.legman.Subscribe;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.mail.api.MailTemplateType;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.Repository;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Maps.asMap;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static java.util.ResourceBundle.getBundle;

@Extension
@EagerSingleton
@Requires("scm-mail-plugin")
public class MirrorStatusEmailNotificationHook {

  private static final Logger LOG = LoggerFactory.getLogger(MirrorStatusEmailNotificationHook.class);

  private static final String SCM_REPOSITORY_URL_PATTERN = "{0}/repo/{1}/{2}/mirror-logs/";

  private static final String SUBJECT_PATTERN = "{0}/{1} {2}";

  private static final Map<Locale, ResourceBundle> SUBJECT_BUNDLES =
    asMap(of(ENGLISH, GERMAN), locale -> getBundle("com.cloudogu.mail.emailnotification.Subjects", locale));

  private final MailService mailService;
  private final Provider<MirrorConfigurationStore> configStoreProvider;
  private final ScmConfiguration scmConfiguration;

  @Inject
  public MirrorStatusEmailNotificationHook(MailService mailService, Provider<MirrorConfigurationStore> mirrorConfigurationStore, ScmConfiguration scmConfiguration) {
    this.mailService = mailService;
    this.configStoreProvider = mirrorConfigurationStore;
    this.scmConfiguration = scmConfiguration;
  }

  @Subscribe
  public void handleEvent(MirrorStatusChangedEvent event) {
    if (event.getNewResult() != event.getPreviousResult()) {
      LOG.trace("Sending mails for repository {} because state changed from {} to {}", event.getRepository(), event.getPreviousResult(), event.getNewResult());
      Optional<MirrorConfiguration> mirrorConfiguration = configStoreProvider.get().getConfiguration(event.getRepository());
      if (mirrorConfiguration.isPresent() && shouldSendEmail(mirrorConfiguration.get())) {
        for (String user : mirrorConfiguration.get().getManagingUsers()) {
          sendMail(event, user);
        }
      }
    }
  }

  private void sendMail(MirrorStatusChangedEvent event, String user) {
    try {
      LOG.trace("Sending mail to user {} for changed mirror status of repository {}", user, event.getRepository());
      mailService
        .emailTemplateBuilder()
        .onTopic(MirrorMailTopics.TOPIC_MIRROR_STATUS_CHANGED)
        .toUser(user)
        .withSubject(getMailSubject(event, ENGLISH))
        .withSubject(GERMAN, getMailSubject(event, GERMAN))
        .withTemplate(event.getNewResult().getMailTemplatePath(), MailTemplateType.MARKDOWN_HTML)
        .andModel(getTemplateModel(event))
        .send();
    } catch (MailSendBatchException e) {
      LOG.warn("Could not send email for changed mirror synchronization to user {} for repository {}", user, event.getRepository(), e);
    }
  }

  private boolean shouldSendEmail(MirrorConfiguration mirrorConfiguration) {
    return mirrorConfiguration.getManagingUsers() != null;
  }

  private String getMailSubject(MirrorStatusChangedEvent event, Locale locale) {
    Repository repository = event.getRepository();
    String displayEventName = SUBJECT_BUNDLES.get(locale).getString(event.getNewResult().getMailSubjectKey());
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
