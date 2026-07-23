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
import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import useMirrorProgress from "./useMirrorProgress";
import { MirrorStatus } from "./types";
import BannerNotification from "./BannerNotification";

type Props = {
  repository: Repository;
};

const MirrorFailedNotification: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const { data: progress } = useMirrorProgress(repository);

  const mirrorStatus = repository._embedded?.mirrorStatus as MirrorStatus | undefined;

  if (progress?.running || mirrorStatus?.result !== "FAILED") {
    return null;
  }

  const logsLink = `/repo/${repository.namespace}/${repository.name}/mirror-logs`;
  const hasLogs = !!repository._links["mirrorLogs"];

  return (
    <BannerNotification type="danger">
      <p className="is-size-5 has-text-weight-bold mb-2">{t("scm-repository-mirror-plugin.syncFailed.title")}</p>
      {hasLogs ? (
        <p>
          <Trans
            t={t}
            i18nKey="scm-repository-mirror-plugin.syncFailed.moreInformation"
            components={[<Link to={logsLink} />]}
          />
        </p>
      ) : null}
    </BannerNotification>
  );
};

export default MirrorFailedNotification;
