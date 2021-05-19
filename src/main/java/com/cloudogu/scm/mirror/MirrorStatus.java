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

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "mirror-status")
public class MirrorStatus {

  private Result result;
  @XmlJavaTypeAdapter(InstantAdapter.class)
  private Instant started;
  @Nullable
  @XmlJavaTypeAdapter(InstantAdapter.class)
  private Instant ended;

  public MirrorStatus(Result result) {
    this.result = result;
  }

  static MirrorStatus initialStatus() {
    MirrorStatus status = new MirrorStatus(Result.NOT_YET_RUN);
    status.started = Instant.now();
    return status;
  }

  public static MirrorStatus success(Instant startTime) {
    MirrorStatus status = new MirrorStatus(Result.SUCCESS);
    status.started = startTime;
    status.ended = Instant.now();
    return status;
  }

  public static MirrorStatus failed(Instant startTime) {
    MirrorStatus status = new MirrorStatus(Result.FAILED);
    status.started = startTime;
    status.ended = Instant.now();
    return status;
  }

  public Optional<Instant> getEnded() {
    return ofNullable(ended);
  }

  public enum Result {
    SUCCESS,
    FAILED,
    NOT_YET_RUN
  }

  private static class InstantAdapter extends XmlAdapter<Long, Instant> {
    @Override
    public Instant unmarshal(Long v) throws Exception {
      return Instant.ofEpochMilli(v);
    }

    @Override
    public Long marshal(Instant v) throws Exception {
      return v.getEpochSecond();
    }
  }
}
