package com.cloudogu.scm.mirror.api;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;

public class MirrorProxyConfigurationDto extends HalRepresentation {

  MirrorProxyConfigurationDto(Links links) {
    super(links);
  }
}
