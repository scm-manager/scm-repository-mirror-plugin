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

import { Embedded, HalRepresentation, HalRepresentationWithEmbedded, RepositoryCreation } from "@scm-manager/ui-types";

export type UsernamePasswordCredentialDto = {
  username: string;
  password: string;
};

export type UsernamePasswordCredentialForm = UsernamePasswordCredentialDto & {
  enabled: boolean;
};

export type CertificateCredentialDto = {
  certificate: string;
  password: string;
};

export type CertificateCredentialForm = CertificateCredentialDto & {
  enabled: boolean;
};

export type MirrorFilterConfigurationDto = HalRepresentation & {
  branchesAndTagsPatterns: string;
  gpgVerificationType: MirrorGpgVerificationType;
  allowedGpgKeys?: PublicKey[];
  fastForwardOnly?: boolean;
  ignoreLfs?: boolean;
};

export type LocalMirrorFilterConfigurationDto = MirrorFilterConfigurationDto & {
  overwriteGlobalConfiguration?: boolean;
};

export type MirrorAccessConfigurationDto = HalRepresentation & {
  url: string;
  synchronizationPeriod?: string;
  managingUsers: string[];
  usernamePasswordCredential?: UsernamePasswordCredentialDto;
  certificateCredential?: CertificateCredentialDto;
  proxyConfiguration: MirrorProxyConfiguration;
};

export type MirrorAccessConfigurationForm = MirrorAccessConfigurationDto & {
  usernamePasswordCredential?: UsernamePasswordCredentialForm;
  certificateCredential?: CertificateCredentialForm;
};

export type MirrorCreationDto = MirrorAccessConfigurationDto & LocalMirrorFilterConfigurationDto & RepositoryCreation;
export type MirrorCreationForm = MirrorAccessConfigurationForm & LocalMirrorFilterConfigurationDto & RepositoryCreation;

export type GlobalConfigurationDto = MirrorFilterConfigurationDto & {
  httpsOnly: boolean;
  disableRepositoryFilterOverwrite: boolean;
};

export const mirrorGpgVerificationTypes = ["NONE", "SIGNATURE", "SCM_USER_SIGNATURE", "KEY_LIST"] as const;
export type MirrorGpgVerificationType = typeof mirrorGpgVerificationTypes[number];

export type PublicKey = HalRepresentation & {
  displayName: string;
  raw: string;
};

export type MirrorStatusResult = "SUCCESS" | "FAILED" | "FAILED_UPDATES" | "NOT_YET_RUN" | "DISABLED";

export type MirrorStatus = {
  result: MirrorStatusResult;
};

export type MirrorLogStatusResult = Omit<MirrorStatusResult, "NOT_YET_RUN">;

export type LogEntry = HalRepresentation & {
  result: MirrorLogStatusResult;
  started: string;
  ended: string;
  log?: string[] | null;
};

type EmbeddedLogEntries = Embedded & {
  entries: LogEntry[];
};

export type LogCollection = HalRepresentationWithEmbedded<EmbeddedLogEntries>;

export type MirrorProxyConfiguration = {
  host: string;
  port: number;
  username?: string;
  password?: string;
  overwriteGlobalConfiguration?: boolean;
};
