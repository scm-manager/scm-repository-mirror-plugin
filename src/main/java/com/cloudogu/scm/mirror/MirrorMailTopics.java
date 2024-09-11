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

import sonia.scm.mail.api.Category;
import sonia.scm.mail.api.Topic;
import sonia.scm.mail.api.TopicProvider;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;

import java.util.Collection;
import java.util.Collections;

@Extension
@Requires("scm-mail-plugin")
public class MirrorMailTopics implements TopicProvider {

  static final Topic TOPIC_MIRROR_STATUS_CHANGED =
    new Topic(new Category("scm-repository-mirror-plugin"), "mirrorStatusChanged");

  @Override
  public Collection<Topic> topics() {
    return Collections.singletonList(
      TOPIC_MIRROR_STATUS_CHANGED
    );
  }
}
