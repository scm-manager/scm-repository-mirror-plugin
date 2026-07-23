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

import React, { FC } from "react";
import { Repository } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import useMirrorProgress from "./useMirrorProgress";
import { MirrorStatus } from "./types";
import BannerNotification from "./BannerNotification";

type Props = {
  repository: Repository;
};

const clampWorked = (worked: number, totalWork: number) => {
  return Math.max(0, Math.min(worked, totalWork));
};

const STEP_BANDS: Record<string, [number, number]> = {
  remoteCountingobjects: [0, 3],
  remoteFindingsources: [3, 5],
  remoteGettingsizes: [5, 7],
  remoteCompressingobjects: [7, 10],
  Receivingobjects: [10, 75],
  Resolvingdeltas: [75, 95],
  LoadingLFSfiles: [95, 100],
  SynchronizingSVNrevisions: [0, 100],
};

const MirrorProgressNotification: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const { available, data: progress } = useMirrorProgress(repository);

  if (!available || !progress?.running) {
    return null;
  }

  const mirrorStatus = repository._embedded?.mirrorStatus as MirrorStatus | undefined;
  const isFirstSynchronization = mirrorStatus?.result === "NOT_YET_RUN";
  const synchronizationKey = isFirstSynchronization ? "firstSynchronization" : "followUpSynchronization";
  const typeKey = repository.type === "svn" ? "svn" : "git";
  const totalWork = Math.max(0, progress.totalWork);
  const worked = clampWorked(progress.worked, totalWork);

  const stepKey = progress.step ? progress.step.replace(/[^A-Za-z0-9]/g, "") : "";
  let stepTranslation = undefined;
  if (progress.step) {
    const stepTranslationKey = "scm-repository-mirror-plugin.progress.step." + stepKey;
    stepTranslation = t(stepTranslationKey);
    if (stepTranslation === stepTranslationKey) {
      stepTranslation = progress.step;
    }
  }

  const stepFraction = totalWork > 0 ? worked / totalWork : progress.stepFinished ? 1 : 0;
  const band = STEP_BANDS[stepKey];
  const percent = band
    ? Math.round(band[0] + stepFraction * (band[1] - band[0]))
    : progress.step
      ? Math.round(stepFraction * 100)
      : 0;

  return (
    <BannerNotification type="inherit">
      <p className="title is-5 mb-2">{t(`scm-repository-mirror-plugin.progress.${synchronizationKey}.header`)}</p>
      <p className="mb-5">
        {isFirstSynchronization
          ? t(`scm-repository-mirror-plugin.progress.firstSynchronization.description.${typeKey}`)
          : t("scm-repository-mirror-plugin.progress.followUpSynchronization.description")}
      </p>
      <div className="is-flex-tablet is-justify-content-space-between is-align-items-baseline mb-3">
        <strong className="is-block-mobile">
          {t("scm-repository-mirror-plugin.progress.progressLabel", { percent })}
        </strong>
        <span className="is-block-mobile">
          {stepTranslation || t("scm-repository-mirror-plugin.progress.starting")} …
        </span>
      </div>
      <progress className="progress is-primary is-loading has-background-white mb-0" value={percent} max={100}>
        {percent}%
      </progress>
    </BannerNotification>
  );
};

export default MirrorProgressNotification;
