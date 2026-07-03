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

import React, { FC, useState } from "react";
import { useTranslation } from "react-i18next";
import { apiClient, Button, ErrorNotification } from "@scm-manager/ui-components";

export const SyncButton: FC<{ link: string; reloadLfs?: boolean }> = ({ link, reloadLfs }) => {
  const [t] = useTranslation("plugins");
  const [triggerError, setTriggerError] = useState<Error | undefined>();
  const [triggerLoading, setTriggerLoading] = useState<boolean>();

  const triggerMirroring = () => {
    setTriggerLoading(true);
    apiClient
      .post(reloadLfs ? link + "?reloadLfs=true" : link)
      .then(() => setTriggerLoading(false))
      .catch((error) => {
        setTriggerError(error);
        setTriggerLoading(false);
      });
  };

  return (
    <>
      <ErrorNotification error={triggerError} />
      <Button
        icon="sync-alt"
        action={triggerMirroring}
        label={t(
          reloadLfs
            ? "scm-repository-mirror-plugin.form.manualSyncWithLfs.button"
            : "scm-repository-mirror-plugin.form.manualSync",
        )}
        loading={triggerLoading}
        disabled={!link}
        type="button"
        color="warning"
      />
    </>
  );
};
