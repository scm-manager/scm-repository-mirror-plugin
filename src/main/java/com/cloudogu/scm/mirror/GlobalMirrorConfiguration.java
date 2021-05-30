package com.cloudogu.scm.mirror;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "global-mirror-configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class GlobalMirrorConfiguration {

  private boolean httpsOnly = false;
  private List<String> branchesAndTagsPatterns;

}
