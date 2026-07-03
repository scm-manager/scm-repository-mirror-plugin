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
import { Link, Repository } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import { useUnmirrorRepository } from "../useMirrorRepository";
import { useHistory } from "react-router-dom";
import { Button, ErrorNotification, Level, Modal, DangerZone } from "@scm-manager/ui-components";
import { SyncButton } from "./SyncButton";
import { useConfigLink } from "@scm-manager/ui-api";
import { LocalMirrorFilterConfigurationDto } from "../types";

const UnmirrorRepo: FC<{ repository: Repository; link: string }> = ({ link, repository }) => {
  const [t] = useTranslation("plugins");
  const [openModal, setOpenModal] = useState(false);
  const { isLoading, error, unmirror } = useUnmirrorRepository(repository, {
    onSuccess: () => history.push(`/repo/${repository.namespace}/${repository.name}`),
  });
  const history = useHistory();

  return (
    <>
      <ErrorNotification error={error} />
      {openModal ? (
        <Modal
          active={openModal}
          body={
            <>
              <p>{t("scm-repository-mirror-plugin.form.unmirror.description")}</p>
            </>
          }
          title={t("scm-repository-mirror-plugin.form.unmirror.subtitle")}
          closeFunction={() => setOpenModal(false)}
          headColor="danger"
          headTextColor="white"
          footer={
            <Button
              icon="ban"
              color="danger"
              action={() => unmirror(link)}
              loading={isLoading}
              label={t("scm-repository-mirror-plugin.form.unmirror.button")}
            />
          }
        />
      ) : null}
      <Level
        left={
          <p>
            <strong>{t("scm-repository-mirror-plugin.form.unmirror.subtitle")}</strong>
            <br />
            {t("scm-repository-mirror-plugin.form.unmirror.shortDescription")}
          </p>
        }
        right={
          <Button
            color="danger"
            icon="ban"
            label={t("scm-repository-mirror-plugin.form.unmirror.button")}
            action={() => setOpenModal(true)}
            loading={isLoading}
            type="button"
          />
        }
      />
    </>
  );
};

const LfsSyncDangerZone: FC<{ syncLink: string }> = ({ syncLink }) => {
  const [t] = useTranslation("plugins");
  return (
    <DangerZone className="px-4 py-5" borderColor={"warning"}>
      <Level
        left={
          <p>
            <strong>{t("scm-repository-mirror-plugin.form.manualSyncWithLfs.subtitle")}</strong>
            <br />
            {t("scm-repository-mirror-plugin.form.manualSyncWithLfs.shortDescription")}
          </p>
        }
        right={<SyncButton link={syncLink} reloadLfs />}
      />
    </DangerZone>
  );
};

export const MirrorDangerZone: FC<{ repository: Repository; unmirrorLink: string; syncLink: string }> = ({
  repository,
  unmirrorLink,
  syncLink,
}) => {
  return (
    <>
      <hr />
      {syncLink ? <LfsSyncDangerZone syncLink={syncLink} /> : null}
      <br />
      <DangerZone className="px-4 py-5">
        <UnmirrorRepo repository={repository} link={unmirrorLink} />
      </DangerZone>
    </>
  );
};
