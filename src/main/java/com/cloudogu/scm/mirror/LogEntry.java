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

import lombok.Getter;
import sonia.scm.repository.api.MirrorCommandResult;
import sonia.scm.xml.XmlInstantAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
public class LogEntry {

  private boolean success;
  @XmlJavaTypeAdapter(XmlDurationAdapter.class)
  private Duration duration;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant finishedAt;
  private List<String> log;

  private LogEntry() {
  }

  public LogEntry(MirrorCommandResult result) {
    this.duration = result.getDuration();
    this.success = result.isSuccess();
    this.finishedAt = Instant.now();
    this.log = result.getLog();
  }

  public static class XmlDurationAdapter extends XmlAdapter<Long, Duration> {

    @Override
    public Duration unmarshal(Long v) {
      return Duration.ofMillis(v);

    }

    @Override
    public Long marshal(Duration v) {
      return v.toMillis();
    }
  }

}
