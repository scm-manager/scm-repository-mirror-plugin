import { RepositoryCreation } from "@scm-manager/ui-types";

export type UsernamePasswordCredentialDto = {
  username: string;
  password: string;
};

export type CertificationCredentialDto = {
  certificate: string;
  password: string;
};

export type MirrorConfigurationDto = {
  url: string;
  usernamePasswordCredential?: UsernamePasswordCredentialDto;
  certificationCredential?: CertificationCredentialDto;
};

export type MirrorRequestDto = MirrorConfigurationDto & RepositoryCreation;

export type RepositoryMirrorConfiguration = MirrorConfigurationDto;
