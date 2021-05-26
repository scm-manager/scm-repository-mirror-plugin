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

import {
  InputField,
  Level,
  Select,
  SelectItem,
  SubmitButton,
  BlobFileInput,
  Checkbox
} from "@scm-manager/ui-components";
import React, { FC, useEffect, useState } from "react";
import { MirrorConfigurationDto, MirrorRequestDto } from "./types";
import { useForm } from "react-hook-form";
import { RepositoryCreation } from "@scm-manager/ui-types";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import { extensionPoints } from "@scm-manager/ui-extensions";
import readBinaryFileAsBase64String from "./readBinaryFileAsBase64String";

const Column = styled.div`
  padding: 0 0.75rem;
  margin-bottom: 0.5rem;
`;

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

type FormType = Omit<MirrorConfigurationDto, "managingUsers">;

type Props = {
  onSubmit: (output: MirrorRequestDto) => void;
  disabled?: boolean;
  InformationForm: extensionPoints.RepositoryCreatorComponentProps["informationForm"];
  NameForm: extensionPoints.RepositoryCreatorComponentProps["nameForm"];
  repositoryType: string;
};

const MirrorRepositoryForm: FC<Props> = ({ repositoryType, onSubmit, disabled, NameForm, InformationForm }) => {
  const [t] = useTranslation("plugins");
  const { register, handleSubmit, formState, watch, setValue } = useForm<FormType>({
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
  const watchUrl = watch("url", "");
  const [showBaseAuthCredentials, setShowBaseAuthCredentials] = useState(false);
  const [showKeyAuthCredentials, setShowKeyAuthCredentials] = useState(false);

  const periodOptions: SelectItem[] = [
    {
      label: t("scm-repository-mirror-plugin.form.period.options.fiveMinutes"),
      value: "5"
    },
    {
      label: t("scm-repository-mirror-plugin.form.period.options.fifteenMinutes"),
      value: "15"
    },
    {
      label: t("scm-repository-mirror-plugin.form.period.options.thirtyMinutes"),
      value: "30"
    },
    {
      label: t("scm-repository-mirror-plugin.form.period.options.oneHour"),
      value: "60"
    },
    {
      label: t("scm-repository-mirror-plugin.form.period.options.twoHours"),
      value: "120"
    },
    {
      label: t("scm-repository-mirror-plugin.form.period.options.fourHours"),
      value: "240"
    },
    {
      label: t("scm-repository-mirror-plugin.form.period.options.twelveHours"),
      value: "720"
    },
    {
      label: t("scm-repository-mirror-plugin.form.period.options.oneDay"),
      value: "1440"
    }
  ];

  useEffect(() => {
    if (!repository.name) {
      // If the repository name is not fill we set a name suggestion
      const match = watchUrl.match(/([^\/]+?)(?:.git)?$/);
      if (match && match[1]) {
        setRepository({ ...repository, name: match[1] });
      }
    }
  }, [watchUrl]);

  const isValid = () => Object.values(valid).every(v => v) && formState.isValid;
  const innerOnSubmit = (configFormValue: MirrorConfigurationDto) => {
    const request: MirrorRequestDto = {
      ...configFormValue,
      ...repository,
      type: repositoryType
    };
    if (!request.usernamePasswordCredential?.username) {
      delete request.usernamePasswordCredential;
    }
    if (!request.keyCredential?.key) {
      delete request.keyCredential;
    }
    onSubmit(request);
  };

  const credentialsForm = (
    <>
      <Column className="column is-full">
        <Checkbox
          label={t("scm-repository-mirror-plugin.form.withBaseAuth.label")}
          onChange={setShowBaseAuthCredentials}
          checked={showBaseAuthCredentials}
        />
      </Column>
      {showBaseAuthCredentials ? (
        <>
          <Column className="column is-half">
            <InputField
              label={t("scm-repository-mirror-plugin.form.username.label")}
              helpText={t("scm-repository-mirror-plugin.form.username.helpText")}
              disabled={disabled}
              {...register("usernamePasswordCredential.username")}
            />
          </Column>
          <Column className="column is-half">
            <InputField
              label={t("scm-repository-mirror-plugin.form.password.label")}
              type="password"
              helpText={t("scm-repository-mirror-plugin.form.password.helpText")}
              disabled={disabled}
              {...register("usernamePasswordCredential.password")}
            />
          </Column>
        </>
      ) : null}
      <Column className="column is-full">
        <Checkbox
          label={t("scm-repository-mirror-plugin.form.withKeyAuth.label")}
          onChange={setShowKeyAuthCredentials}
          checked={showKeyAuthCredentials}
        />
      </Column>
      {showKeyAuthCredentials ? (
        <>
          <Column className="column is-half">
            <BlobFileInput
              label={t("scm-repository-mirror-plugin.form.key.label")}
              helpText={t("scm-repository-mirror-plugin.form.key.helpText")}
              disabled={disabled}
              onChange={(files: FileList) =>
                readBinaryFileAsBase64String(files[0]).then(base64String =>
                  // @ts-ignore
                  setValue("keyCredential.key", base64String)
                )
              }
            />
          </Column>
          <Column className="column is-half">
            <InputField
              label={t("scm-repository-mirror-plugin.form.key.password.label")}
              type="password"
              helpText={t("scm-repository-mirror-plugin.form.key.password.helpText")}
              disabled={disabled}
              {...register("keyCredential.password")}
            />
          </Column>
        </>
      ) : null}
    </>
  );

  return (
    <form onSubmit={handleSubmit(innerOnSubmit)}>
      <Columns className="columns is-multiline">
        <Column className="column is-full">
          <InputField
            label={t("scm-repository-mirror-plugin.form.url.label")}
            helpText={t("scm-repository-mirror-plugin.form.url.helpText")}
            errorMessage={formState.errors.url?.message}
            validationError={!!formState.errors.url}
            disabled={disabled}
            {...register("url", {
              required: {
                value: true,
                message: t("scm-repository-mirror-plugin.form.url.errors.required")
              },
              pattern: {
                value: /^[A-Za-z0-9]+:\/\/[^\s$.?#].[^\s]*$/,
                message: t("scm-repository-mirror-plugin.form.url.errors.invalid")
              }
            })}
          />
        </Column>
        {credentialsForm}
        <Column className="column is-full">
          <Select
            defaultValue={"60"}
            label={t("scm-repository-mirror-plugin.form.period.label")}
            helpText={t("scm-repository-mirror-plugin.form.period.helpText")}
            options={periodOptions}
            disabled={disabled}
            {...register("synchronizationPeriod")}
          />
        </Column>
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
