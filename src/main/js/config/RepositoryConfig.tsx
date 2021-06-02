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
import React, { FC, useEffect, useState } from "react";
import {
  apiClient,
  Button,
  Checkbox,
  ConfigurationForm,
  ErrorNotification,
  InputField,
  Subtitle
} from "@scm-manager/ui-components";
import { useConfigLink } from "@scm-manager/ui-api";
import { MirrorConfigurationDto } from "../types";
import { useForm } from "react-hook-form";
import styled from "styled-components";
import {
  coalesceFormValue,
  CredentialsControl,
  GpgVerificationControl,
  ManagingUsersControl,
  SynchronizationPeriodControl,
  UrlControl
} from "./FormControls";
import { Link } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

type Props = {
  link: string;
};

export const TriggerButton: FC<{ link: string }> = ({ link }) => {
  const [t] = useTranslation("plugins");
  const [triggerError, setTriggerError] = useState<Error | undefined>();
  const [triggerLoading, setTriggerLoading] = useState<boolean>();

  const triggerMirroring = () => {
    setTriggerLoading(true);
    apiClient
      .post(link)
      .then(() => setTriggerLoading(false))
      .catch(setTriggerError);
  };

  return (
    <>
      <ErrorNotification error={triggerError} />
      <Button
        icon="sync-alt"
        action={triggerMirroring}
        label={t("scm-repository-mirror-plugin.form.manualSync")}
        loading={triggerLoading}
        disabled={!link}
        type="button"
        color="info"
      />
    </>
  );
};

const RepositoryConfig: FC<Props> = ({ link }) => {
  const [t] = useTranslation("plugins");
  const { initialConfiguration, update, isReadonly, ...formProps } = useConfigLink<MirrorConfigurationDto>(link);
  const { formState, handleSubmit, control, reset, register } = useForm<MirrorConfigurationDto>({
    mode: "onChange"
  });

  useEffect(() => {
    if (initialConfiguration) {
      reset(initialConfiguration);
    }
  }, [initialConfiguration]);

  const onSubmit = handleSubmit(formValue =>
    // Because the url field is disabled (sets url to undefined) but the dto expects the url to be present in the request,
    // we have to manually set the url to the initial configuration
    update(coalesceFormValue({ ...formValue, url: initialConfiguration.url }))
  );

  return (
    <ConfigurationForm isValid={formState.isValid} isReadonly={isReadonly} onSubmit={onSubmit} {...formProps}>
      <TriggerButton link={(initialConfiguration?._links.syncMirror as Link)?.href} />
      <hr />
      <Subtitle subtitle={t("scm-repository-mirror-plugin.form.subtitle")} />
      <Columns className="columns is-multiline">
        <UrlControl control={control} isReadonly={true} />
        <CredentialsControl control={control} isReadonly={isReadonly} />
        <SynchronizationPeriodControl control={control} isReadonly={isReadonly} />
        <ManagingUsersControl control={control} isReadonly={isReadonly} />
      </Columns>
      <hr />
      <h4 className="subtitle is-4">{t("scm-repository-mirror-plugin.form.verificationFilters")}</h4>
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.fastForwardOnly.label")}
        helpText={t("scm-repository-mirror-plugin.form.fastForwardOnly.helpText")}
        disabled={isReadonly}
        {...register("fastForwardOnly")}
      />
      <InputField
        label={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.label")}
        helpText={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.helpText")}
        disabled={isReadonly}
        {...register("branchesAndTagsPatterns")}
      />
      <GpgVerificationControl control={control} isReadonly={isReadonly} />
    </ConfigurationForm>
  );
};

export default RepositoryConfig;
