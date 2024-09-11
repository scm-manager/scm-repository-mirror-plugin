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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.notifications.Type;
import sonia.scm.xml.XmlInstantAdapter;

import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "mirror-status")
public class MirrorStatus {

  private static final Logger LOG = LoggerFactory.getLogger(MirrorStatus.class);

  private Result result;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant started;
  @Nullable
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant ended;

  public MirrorStatus(Result result) {
    this.result = result;
  }

  static MirrorStatus initialStatus() {
    MirrorStatus status = new MirrorStatus(Result.NOT_YET_RUN);
    status.started = Instant.now();
    return status;
  }

  public static MirrorStatus create(Result type, Instant startTime) {
    MirrorStatus status = new MirrorStatus(type);
    return withBaseValues(status, startTime);
  }

  private static MirrorStatus withBaseValues(MirrorStatus status, Instant startTime) {
    status.started = startTime;
    status.ended = Instant.now();
    return status;
  }

  public enum Result {
    SUCCESS("mirrorSuccess", Type.SUCCESS, "com/cloudogu/mail/emailnotification/mirror_success.mustache", "mirrorSuccess"),
    FAILED_UPDATES("mirrorUpdatesRejected", Type.WARNING, "com/cloudogu/mail/emailnotification/mirror_rejected_updates.mustache", "mirrorRejectedUpdates"),
    FAILED("mirrorFailed", Type.ERROR, "com/cloudogu/mail/emailnotification/mirror_failed.mustache", "mirrorFailed"),
    NOT_YET_RUN("notYetRun", Type.INFO, null, null);

    private final String notificationKey;
    private final Type notificationType;
    private final String mailTemplatePath;
    private final String mailSubjectKey;

    Result(String notificationKey, Type notificationType, String mailTemplatePath, String mailSubjectKey) {
      this.notificationKey = notificationKey;
      this.notificationType = notificationType;
      this.mailTemplatePath = mailTemplatePath;
      this.mailSubjectKey = mailSubjectKey;
    }

    public String getNotificationKey() {
      return notificationKey;
    }

    public Type getNotificationType() {
      return notificationType;
    }

    public String getMailTemplatePath() {
      return mailTemplatePath;
    }

    public String getMailSubjectKey() {
      return mailSubjectKey;
    }
  }
}
