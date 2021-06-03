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

import { HalRepresentation, HalRepresentationWithEmbedded, Embedded, RepositoryCreation } from "@scm-manager/ui-types";

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
}

export type MirrorVerificationConfigurationDto = {
  branchesAndTagsPatterns: string;
  gpgVerificationType: MirrorGpgVerificationType;
  allowedGpgKeys?: PublicKey[];
  fastForwardOnly?: boolean;
};
export type MirrorConfigurationDto = HalRepresentation &
  MirrorVerificationConfigurationDto & {
    url: string;
    synchronizationPeriod: number;
    managingUsers: string[];
    usernamePasswordCredential?: UsernamePasswordCredentialDto;
    certificateCredential?: CertificateCredentialDto;
  };

export type MirrorConfigurationForm = MirrorConfigurationDto & {
  usernamePasswordCredential?: UsernamePasswordCredentialForm;
  certificateCredential?: CertificateCredentialForm;
}

export type MirrorRequestDto = MirrorConfigurationDto & RepositoryCreation;

export type GlobalConfigurationDto = MirrorVerificationConfigurationDto & {
  httpsOnly: boolean;
};

export const mirrorGpgVerificationTypes = ["NONE", "SIGNATURE", "SCM_USER_SIGNATURE", "KEY_LIST"] as const;
export type MirrorGpgVerificationType = typeof mirrorGpgVerificationTypes[number];

export type PublicKey = HalRepresentation & {
  displayName: string;
  raw: string;
};

export type MirrorStatusResult = "SUCCESS" | "FAILED" | "FAILED_UPDATES" | "NOT_YET_RUN";

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
