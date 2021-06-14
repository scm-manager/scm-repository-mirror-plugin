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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.notifications.Type;
import sonia.scm.xml.XmlInstantAdapter;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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
