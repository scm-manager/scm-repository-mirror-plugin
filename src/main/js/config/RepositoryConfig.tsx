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
import {
  LocalMirrorFilterConfigurationDto,
  MirrorAccessConfigurationDto,
  MirrorAccessConfigurationForm
} from "../types";
import { useForm } from "react-hook-form";
import styled from "styled-components";
import {
  coalesceFormValue,
  CredentialsControl,
  GpgVerificationControl,
  ManagingUsersControl,
  ProxyHostControl,
  ProxyPasswordControl,
  ProxyPortControl,
  ProxyUsernameControl,
  SynchronizationPeriodControl,
  UrlControl
} from "./FormControls";
import { Link, Repository } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import { MirrorDangerZone } from "./MirrorDangerZone";

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

type Props = {
  repository: Repository;
  link: string;
};

export const SyncButton: FC<{ link: string }> = ({ link }) => {
  const [t] = useTranslation("plugins");
  const [triggerError, setTriggerError] = useState<Error | undefined>();
  const [triggerLoading, setTriggerLoading] = useState<boolean>();

  const triggerMirroring = () => {
    setTriggerLoading(true);
    apiClient
      .post(link)
      .then(() => setTriggerLoading(false))
      .catch(error => {
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
        label={t("scm-repository-mirror-plugin.form.manualSync")}
        loading={triggerLoading}
        disabled={!link}
        type="button"
        color="info"
      />
    </>
  );
};

const RepositoryMirrorAccessConfigForm: FC<Pick<Props, "link">> = ({ link }) => {
  const [t] = useTranslation("plugins");
  const { initialConfiguration, update, isReadOnly, ...formProps } = useConfigLink<MirrorAccessConfigurationDto>(link);
  const { formState, handleSubmit, control, reset, watch, register } = useForm<MirrorAccessConfigurationForm>({
    mode: "onChange"
  });
  const showProxyForm = watch("proxyConfiguration.overwriteGlobalConfiguration");

  useEffect(() => {
    if (initialConfiguration) {
      const form = { ...initialConfiguration } as MirrorAccessConfigurationForm;
      if (initialConfiguration.usernamePasswordCredential) {
        form.usernamePasswordCredential = {
          ...initialConfiguration.usernamePasswordCredential,
          enabled: true
        };
      }
      if (initialConfiguration.certificateCredential) {
        form.certificateCredential = {
          ...initialConfiguration.certificateCredential,
          enabled: true
        };
      }
      if (!initialConfiguration.synchronizationPeriod) {
        form.synchronizationPeriod = "0";
      }
      reset(form);
    }
  }, [initialConfiguration]);

  const onSubmit = handleSubmit(formValue =>
    // Because the url field is disabled (sets url to undefined) but the dto expects the url to be present in the request,
    // we have to manually set the url to the initial configuration
    update(coalesceFormValue({ ...formValue, url: initialConfiguration?.url || "" }))
  );

  return (
    <ConfigurationForm isValid={formState.isValid} isReadOnly={isReadOnly} onSubmit={onSubmit} {...formProps}>
      <SyncButton link={(initialConfiguration?._links.syncMirror as Link)?.href} />
      <hr />
      <Subtitle subtitle={t("scm-repository-mirror-plugin.form.subtitle")} />
      <Columns className="columns is-multiline">
        <UrlControl control={control} isReadonly={true} />
        <CredentialsControl control={control} isReadonly={isReadOnly} />
        <SynchronizationPeriodControl control={control} isReadonly={isReadOnly} />
        <ManagingUsersControl control={control} isReadonly={isReadOnly} />
      </Columns>
      <hr />
      <Subtitle subtitle={t("scm-repository-mirror-plugin.form.proxy.subtitle")} />
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.proxy.overwriteGlobalConfiguration.label")}
        helpText={t("scm-repository-mirror-plugin.form.proxy.overwriteGlobalConfiguration.helpText")}
        disabled={isReadOnly}
        {...register("proxyConfiguration.overwriteGlobalConfiguration")}
      />
      {showProxyForm ? (
        <Columns className="columns is-multiline">
          <ProxyHostControl control={control} isReadonly={isReadOnly} />
          <ProxyPortControl control={control} isReadonly={isReadOnly} />
          <ProxyUsernameControl control={control} isReadonly={isReadOnly} />
          <ProxyPasswordControl control={control} isReadonly={isReadOnly} />
        </Columns>
      ) : null}
    </ConfigurationForm>
  );
};

const RepositoryMirrorFilterConfigForm: FC<Pick<Props, "link">> = ({ link }) => {
  const [t] = useTranslation("plugins");
  const { initialConfiguration, update, isReadOnly, ...formProps } = useConfigLink<LocalMirrorFilterConfigurationDto>(
    link
  );
  const { formState, handleSubmit, control, reset, register, watch } = useForm<LocalMirrorFilterConfigurationDto>({
    mode: "onChange"
  });
  const showFilterForm = watch("overwriteGlobalConfiguration");

  useEffect(() => {
    if (initialConfiguration) {
      reset(initialConfiguration);
    }
  }, [initialConfiguration]);

  const onSubmit = handleSubmit(formValue =>
    // Because the url field is disabled (sets url to undefined) but the dto expects the url to be present in the request,
    // we have to manually set the url to the initial configuration
    update(
      !formValue.overwriteGlobalConfiguration
        ? ({ ...initialConfiguration, overwriteGlobalConfiguration: false, ignoreLfs: formValue.ignoreLfs } as LocalMirrorFilterConfigurationDto)
        : formValue
    )
  );

  return (
    <>
      <hr />
      <h2 className="subtitle">{t("scm-repository-mirror-plugin.form.lfs")}</h2>
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.ignoreLfs.label")}
        helpText={t("scm-repository-mirror-plugin.form.ignoreLfs.helpText")}
        disabled={isReadOnly}
        {...register("ignoreLfs",  { shouldUnregister: true })}
      />
      <hr />
      <h2 className="subtitle">{t("scm-repository-mirror-plugin.form.verificationFilters")}</h2>
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.overwriteGlobalConfiguration.label")}
        helpText={t("scm-repository-mirror-plugin.form.overwriteGlobalConfiguration.helpText")}
        disabled={isReadOnly}
        {...register("overwriteGlobalConfiguration")}
      />
      <ConfigurationForm isValid={formState.isValid} isReadOnly={isReadOnly} onSubmit={onSubmit} {...formProps}>
        {showFilterForm ? (
          <>
            <hr />
            <Checkbox
              label={t("scm-repository-mirror-plugin.form.fastForwardOnly.label")}
              helpText={t("scm-repository-mirror-plugin.form.fastForwardOnly.helpText")}
              disabled={isReadOnly}
              {...register("fastForwardOnly", { shouldUnregister: true })}
            />
            <InputField
              label={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.label")}
              helpText={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.helpText")}
              disabled={isReadOnly}
              {...register("branchesAndTagsPatterns", { shouldUnregister: true })}
            />
            <GpgVerificationControl control={control} isReadonly={isReadOnly} />
          </>
        ) : null}
      </ConfigurationForm>
    </>
  );
};

const RepositoryConfig: FC<Props> = ({ link, repository }) => {
  const filtersLink = repository._links["mirrorFilterConfiguration"];
  return (
    <>
      <RepositoryMirrorAccessConfigForm link={link} />
      {filtersLink ? <RepositoryMirrorFilterConfigForm link={(filtersLink as Link).href} /> : null}
      <MirrorDangerZone repository={repository} link={(repository._links["unmirror"] as Link).href} />
    </>
  );
};

export default RepositoryConfig;
