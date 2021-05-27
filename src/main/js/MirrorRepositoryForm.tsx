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

import { Checkbox, InputField, Level, SubmitButton } from "@scm-manager/ui-components";
import React, { FC, useEffect, useState } from "react";
import { MirrorConfigurationDto, MirrorRequestDto } from "./types";
import { useController, useForm } from "react-hook-form";
import { RepositoryCreation } from "@scm-manager/ui-types";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import { extensionPoints } from "@scm-manager/ui-extensions";
import {
  coalesceFormValue,
  ControlProps,
  FileInputControl,
  SynchronizationPeriodControl,
  UrlControl
} from "./config/FormControls";

const Column = styled.div`
  padding: 0 0.75rem;
  margin-bottom: 0.5rem;
`;

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

const CredentialsFormControl: FC<Omit<ControlProps, "isReadonly">> = ({ control }) => {
  const [t] = useTranslation("plugins");
  const [showBaseAuthCredentials, setShowBaseAuthCredentials] = useState(false);
  const [showKeyAuthCredentials, setShowKeyAuthCredentials] = useState(false);
  const { field: usernameField } = useController({ control, name: "usernamePasswordCredential.username" });
  const { field: passwordField } = useController({ control, name: "usernamePasswordCredential.password" });
  const { field: certificatePasswordField } = useController({ control, name: "certificateCredential.password" });

  return (
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
              {...usernameField}
            />
          </Column>
          <Column className="column is-half">
            <InputField
              label={t("scm-repository-mirror-plugin.form.password.label")}
              type="password"
              helpText={t("scm-repository-mirror-plugin.form.password.helpText")}
              {...passwordField}
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
            <FileInputControl control={control} isReadonly={false} />
          </Column>
          <Column className="column is-half">
            <InputField
              label={t("scm-repository-mirror-plugin.form.certificate.password.label")}
              type="password"
              helpText={t("scm-repository-mirror-plugin.form.certificate.password.helpText")}
              {...certificatePasswordField}
            />
          </Column>
        </>
      ) : null}
    </>
  );
};

type Props = {
  onSubmit: (output: MirrorRequestDto) => void;
  disabled?: boolean;
  InformationForm: extensionPoints.RepositoryCreatorComponentProps["informationForm"];
  NameForm: extensionPoints.RepositoryCreatorComponentProps["nameForm"];
  repositoryType: string;
};

const MirrorRepositoryForm: FC<Props> = ({ repositoryType, onSubmit, disabled, NameForm, InformationForm }) => {
  const [t] = useTranslation("plugins");
  const { handleSubmit, formState, watch, control } = useForm<MirrorConfigurationDto>({
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
  const url = watch("url", "");

  useEffect(() => {
    if (url && !repository.name) {
      // If the repository name is not fill we set a name suggestion
      const match = url.match(/([^\/]+?)(?:.git)?$/);
      if (match && match[1]) {
        setRepository({ ...repository, name: match[1] });
      }
    }
  }, [url]);

  const isValid = () => Object.values(valid).every(v => v) && formState.isValid;
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
        <CredentialsFormControl control={control} />
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
