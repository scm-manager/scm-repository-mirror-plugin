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

import { Checkbox, InputField, Level, Notification, SubmitButton } from "@scm-manager/ui-components";
import React, { FC, useEffect, useState } from "react";
import { MirrorConfigurationDto, MirrorRequestDto } from "./types";
import { useForm } from "react-hook-form";
import { RepositoryCreation } from "@scm-manager/ui-types";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import { extensionPoints } from "@scm-manager/ui-extensions";
import {
  coalesceFormValue,
  CredentialsControl,
  GpgVerificationControl,
  SynchronizationPeriodControl,
  UrlControl
} from "./config/FormControls";

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

type Props = {
  onSubmit: (output: MirrorRequestDto) => void;
  disabled: boolean;
  InformationForm: extensionPoints.RepositoryCreatorComponentProps["informationForm"];
  NameForm: extensionPoints.RepositoryCreatorComponentProps["nameForm"];
  repositoryType: string;
};

const MirrorRepositoryForm: FC<Props> = ({ repositoryType, onSubmit, disabled, NameForm, InformationForm }) => {
  const [t] = useTranslation("plugins");
  const { handleSubmit, formState, getValues, control, register } = useForm<MirrorConfigurationDto>({
    mode: "onChange"
  });
  const [repository, setRepository] = useState<RepositoryCreation>({
    type: "",
    contact: "",
    description: "",
    name: "",
    namespace: "",
    contextEntries: {}
  });
  const [valid, setValid] = useState({ namespaceAndName: false, contact: true });
  const { isValid: isFormValid, touchedFields } = formState;

  useEffect(() => {
    const url = getValues("url")
    if (url && touchedFields.url && !repository.name) {
      // If the repository name is not fill we set a name suggestion
      const match = url.match(/([^\/]+?)(?:.git)?$/);
      if (match && match[1]) {
        setRepository({ ...repository, name: match[1] });
      }
    }
  }, [getValues, touchedFields.url]);

  const isValid = () => Object.values(valid).every(v => v) && isFormValid;
  const innerOnSubmit = (configFormValue: MirrorConfigurationDto) => {
    const request: MirrorRequestDto = {
      ...configFormValue,
      ...repository,
      type: repositoryType
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
        setValid={(namespaceAndName: boolean) => setValid({ ...valid, namespaceAndName })}
        disabled={disabled}
      />
      <InformationForm
        repository={repository}
        onChange={setRepository}
        disabled={disabled}
        setValid={(contact: boolean) => setValid({ ...valid, contact })}
      />
      <hr />
      <h4 className="subtitle is-4">{t("scm-repository-mirror-plugin.form.verificationFilters")}</h4>
      <Notification type={"inherit"}>{t("scm-repository-mirror-plugin.form.verificationFiltersHint")}</Notification>
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.fastForwardOnly.label")}
        helpText={t("scm-repository-mirror-plugin.form.fastForwardOnly.helpText")}
        disabled={disabled}
        {...register("fastForwardOnly")}
      />
      <InputField
        label={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.label")}
        helpText={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.helpText")}
        disabled={disabled}
        {...register("branchesAndTagsPatterns")}
      />
      <GpgVerificationControl control={control} isReadonly={disabled} />
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
