package com.cloudogu.scm.mirror;

import sonia.scm.HandlerEventType;
import sonia.scm.event.Event;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryEvent;

@Event
public class RepositoryUnmirrorEvent extends RepositoryEvent {
  public RepositoryUnmirrorEvent(HandlerEventType eventType, Repository repository) {
    super(eventType, repository);
  }
}
