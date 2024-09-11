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
import { SecondaryNavigationItem } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";

type Props = {
  url: string;
};

const LogNavLink: FC<Props> = ({ url }) => {
  const [t] = useTranslation("plugins");
  return (
    <SecondaryNavigationItem
      to={`${url}/mirror-logs`}
      icon="fas fa-copy"
      label={t("scm-repository-mirror-plugin.logs.navLink")}
      title={t("scm-repository-mirror-plugin.logs.navLink")}
    />
  );
};

export default LogNavLink;
