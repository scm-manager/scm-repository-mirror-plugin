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

import lombok.Getter;
import sonia.scm.xml.XmlInstantAdapter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
public class LogEntry {

  private MirrorStatus.Result result;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant started;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant ended;
  private List<String> log;

  private LogEntry() {
  }

  public LogEntry(MirrorStatus status, String... log) {
    this(status, Arrays.asList(log));
  }

  public LogEntry(MirrorStatus status, List<String> log) {
    this.result = status.getResult();
    this.started = status.getStarted();
    this.ended = status.getEnded();
    this.log = log;
  }

}
