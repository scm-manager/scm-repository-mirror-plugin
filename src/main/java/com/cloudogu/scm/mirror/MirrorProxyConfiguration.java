package com.cloudogu.scm.mirror;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import sonia.scm.net.ProxyConfiguration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.Collection;

@Getter
@Setter
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class MirrorProxyConfiguration implements ProxyConfiguration {

  private final boolean enabled;
  private final String host;
  private final int port;
  private final Collection<String> excludes;
  private final String username;
  private final String password;

  @Override
  public boolean isAuthenticationRequired() {
    return !Strings.isNullOrEmpty(getUsername()) && !Strings.isNullOrEmpty(getPassword());
  }
}
