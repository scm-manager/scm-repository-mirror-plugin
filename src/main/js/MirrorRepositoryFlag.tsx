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
import { RepositoryFlag } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { MirrorStatus } from "./types";

type Props = {
  repository: Repository;
};

const MirrorRepositoryFlag: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();
  const mirrorStatus = repository._embedded?.mirrorStatus as MirrorStatus;

  let onClick = undefined;
  if (repository._links["mirrorLogs"]) {
    onClick = () => history.push(`/repo/${repository.namespace}/${repository.name}/mirror-logs`);
  }

  switch (mirrorStatus?.result) {
    case "SUCCESS":
      return (
        <RepositoryFlag onClick={onClick} variant="success" title={t("scm-repository-mirror-plugin.flag.success")}>
          {t("scm-repository-mirror-plugin.flag.label")}
        </RepositoryFlag>
      );
    case "FAILED_UPDATES":
      return (
        <RepositoryFlag variant="warning" title={t("scm-repository-mirror-plugin.flag.failedUpdates")}>
          {t("scm-repository-mirror-plugin.flag.label")}
        </RepositoryFlag>
      );
    case "FAILED":
      return (
        <RepositoryFlag onClick={onClick} variant="danger" title={t("scm-repository-mirror-plugin.flag.failed")}>
          {t("scm-repository-mirror-plugin.flag.label")}
        </RepositoryFlag>
      );
    case "NOT_YET_RUN":
      return (
        <RepositoryFlag onClick={onClick} title={t("scm-repository-mirror-plugin.flag.notYetRun")}>
          {t("scm-repository-mirror-plugin.flag.label")}
        </RepositoryFlag>
      );
    case "DISABLED":
      return (
        <RepositoryFlag onClick={onClick} title={t("scm-repository-mirror-plugin.flag.disabled")}>
          {t("scm-repository-mirror-plugin.flag.label")}
        </RepositoryFlag>
      );
    default:
      return null;
  }
};

export default MirrorRepositoryFlag;
