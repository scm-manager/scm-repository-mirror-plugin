package com.cloudogu.scm.mirror.api;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GlobalMirrorConfigurationDto extends HalRepresentation {

  @NotNull
  private boolean httpsOnly;

  @NotNull
  private List<String> branchesAndTagsPatterns;

  GlobalMirrorConfigurationDto(Links links) {
    super(links);
  }

}
