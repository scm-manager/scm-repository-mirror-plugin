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

import { Checkbox, InputField, Level, SubmitButton, urls } from "@scm-manager/ui-components";
import React, { FC, useCallback, useEffect, useState } from "react";
import { MirrorCreationDto, MirrorCreationForm } from "./types";
import { useForm } from "react-hook-form";
import { RepositoryCreation, RepositoryType } from "@scm-manager/ui-types";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import { extensionPoints } from "@scm-manager/ui-extensions";
import {
  coalesceFormValue,
  CredentialsControl,
  GpgVerificationControl,
  ProxyHostControl,
  ProxyPasswordControl,
  ProxyPortControl,
  ProxyUsernameControl,
  SynchronizationPeriodControl,
  UrlControl
} from "./config/FormControls";
import { useLocation } from "react-router-dom";

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

type Props = {
  onSubmit: (output: MirrorCreationDto) => void;
  disabled: boolean;
  InformationForm: extensionPoints.RepositoryCreatorComponentProps["informationForm"];
  NameForm: extensionPoints.RepositoryCreatorComponentProps["nameForm"];
  repositoryType: RepositoryType;
};

const MirrorRepositoryForm: FC<Props> = ({ repositoryType, onSubmit, disabled, NameForm, InformationForm }) => {
  const [t] = useTranslation("plugins");
  const location = useLocation();
  const { handleSubmit, formState, getValues, control, register, watch } = useForm<MirrorCreationForm>({
    mode: "onChange"
  });
  const [repository, setRepository] = useState<RepositoryCreation>({
    type: "",
    contact: "",
    description: "",
    name: "",
    namespace: urls.getValueStringFromLocationByKey(location, "namespace") || "",
    contextEntries: {}
  });
  const [valid, setValid] = useState({ namespaceAndName: false, contact: true });
  const { isValid: isFormValid, touchedFields } = formState;
  const showFilterForm = watch("overwriteGlobalConfiguration");
  const showProxyForm = watch("proxyConfiguration.overwriteGlobalConfiguration");
  const allowLocalFilterConfiguration = !!repositoryType._links["mirrorFilterConfiguration"];
  const setContactValid = useCallback((contact: boolean) => setValid(currentValid => ({ ...currentValid, contact })), [
    setValid
  ]);
  const setNamespaceAndNameValid = useCallback(
    (namespaceAndName: boolean) => setValid(currentValid => ({ ...currentValid, namespaceAndName })),
    [setValid]
  );

  useEffect(() => {
    const url = getValues("url");
    if (url && touchedFields.url && !repository.name) {
      // If the repository name is not fill we set a name suggestion
      const match = url.match(/([^\/]+?)(?:.git)?$/);
      if (match && match[1]) {
        setRepository({ ...repository, name: match[1] });
      }
    }
  }, [getValues, touchedFields.url]);

  const isValid = () => Object.values(valid).every(v => v) && isFormValid;
  const innerOnSubmit = (configFormValue: MirrorCreationForm) => {
    const request: MirrorCreationForm = {
      ...configFormValue,
      ...repository,
      type: repositoryType?.name
    };
    onSubmit(coalesceFormValue(request));
  };

  return (
    <form onSubmit={handleSubmit(innerOnSubmit)}>
      <Columns className="columns is-multiline">
        <UrlControl control={control} isReadonly={false} />
        <CredentialsControl control={control} isReadonly={false} />
        <SynchronizationPeriodControl control={control} isReadonly={false} />
      </Columns>
      <hr />
      <NameForm
        repository={repository}
        onChange={setRepository}
        setValid={setNamespaceAndNameValid}
        disabled={disabled}
      />
      <InformationForm
        repository={repository}
        onChange={setRepository}
        disabled={disabled}
        setValid={setContactValid}
      />
      <h4 className="subtitle is-4">{t("scm-repository-mirror-plugin.form.lfs")}</h4>
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.ignoreLfs.label")}
        helpText={t("scm-repository-mirror-plugin.form.ignoreLfs.helpText")}
        disabled={disabled}
        {...register("ignoreLfs", { shouldUnregister: true })}
      />
      {allowLocalFilterConfiguration ? (
        <>
          <hr />
          <h4 className="subtitle is-4">{t("scm-repository-mirror-plugin.form.verificationFilters")}</h4>
          <Checkbox
            label={t("scm-repository-mirror-plugin.form.overwriteGlobalConfiguration.label")}
            helpText={t("scm-repository-mirror-plugin.form.overwriteGlobalConfiguration.helpText")}
            disabled={disabled}
            testId="overwrite-global-configuration-checkbox"
            {...register("overwriteGlobalConfiguration")}
          />
          {showFilterForm ? (
            <>
              <hr />
              <Checkbox
                label={t("scm-repository-mirror-plugin.form.fastForwardOnly.label")}
                helpText={t("scm-repository-mirror-plugin.form.fastForwardOnly.helpText")}
                disabled={disabled}
                {...register("fastForwardOnly", { shouldUnregister: true })}
              />
              <InputField
                label={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.label")}
                helpText={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.helpText")}
                disabled={disabled}
                {...register("branchesAndTagsPatterns", { shouldUnregister: true })}
              />
              <GpgVerificationControl control={control} isReadonly={disabled} />
            </>
          ) : null}
        </>
      ) : null}
      <hr />
      <h4 className="subtitle is-4">{t("scm-repository-mirror-plugin.form.proxy.subtitle")}</h4>
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.proxy.overwriteGlobalConfiguration.label")}
        helpText={t("scm-repository-mirror-plugin.form.proxy.overwriteGlobalConfiguration.helpText")}
        disabled={disabled}
        {...register("proxyConfiguration.overwriteGlobalConfiguration")}
      />
      {showProxyForm ? (
        <>
          <ProxyHostControl control={control} isReadonly={disabled} />
          <ProxyPortControl control={control} isReadonly={disabled} />
          <ProxyUsernameControl control={control} isReadonly={disabled} />
          <ProxyPasswordControl control={control} isReadonly={disabled} />
        </>
      ) : null}
      <hr />
      <Level
        right={
          <SubmitButton
            disabled={!isValid()}
            loading={disabled}
            label={t("scm-repository-mirror-plugin.repositoryForm.createButton")}
          />
        }
      />
    </form>
  );
};

export default MirrorRepositoryForm;
