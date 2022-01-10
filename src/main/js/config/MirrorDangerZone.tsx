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
import React, { FC, useState } from "react";
import { Repository } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import { useUnmirrorRepository } from "../useMirrorRepository";
import { useHistory } from "react-router-dom";
import { Button, ErrorNotification, Level, Modal, DangerZone } from "@scm-manager/ui-components";

const UnmirrorRepo: FC<{ repository: Repository; link: string }> = ({ link, repository }) => {
  const [t] = useTranslation("plugins");
  const [openModal, setOpenModal] = useState(false);
  const { isLoading, error, unmirror } = useUnmirrorRepository(repository, {
    onSuccess: () => history.push(`/repo/${repository.namespace}/${repository.name}`)
  });
  const history = useHistory();
  const [initialFocusNode, setInitialFocusNode] = useState<HTMLButtonElement | null>(null);

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
          initialFocusNode={initialFocusNode}
          footer={
            <Button
              icon="ban"
              color="danger"
              action={() => unmirror(link)}
              loading={isLoading}
              label={t("scm-repository-mirror-plugin.form.unmirror.button")}
              ref={setInitialFocusNode}
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

export const MirrorDangerZone: FC<{ repository: Repository; link: string }> = ({ repository, link }) => {
  return (
    <>
      <hr />
      <DangerZone className="px-4 py-5">
        <UnmirrorRepo repository={repository} link={link} />
      </DangerZone>
    </>
  );
};
