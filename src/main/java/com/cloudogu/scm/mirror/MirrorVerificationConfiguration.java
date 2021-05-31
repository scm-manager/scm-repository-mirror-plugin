package com.cloudogu.scm.mirror;

import java.util.List;

public class MirrorVerificationConfiguration {

  private List<String> branchesAndTagsPatterns;
  private MirrorGpgVerificationType gpgVerificationType;
  private List<RawGpgKey> allowedGpgKeys;

}
