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
import React, { ChangeEvent, FC, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { MirrorConfigurationDto } from "../types";
import {
  AutocompleteAddEntryToTableField,
  FileInput,
  InputField,
  MemberNameTagGroup,
  Select,
  SelectItem
} from "@scm-manager/ui-components";
import styled from "styled-components";
import { Repository, SelectValue } from "@scm-manager/ui-types";
import { useUserSuggestions } from "@scm-manager/ui-api";
import readBinaryFileAsBase64String from "../readBinaryFileAsBase64String";

const Column = styled.div`
  padding: 0 0.75rem;
  margin-bottom: 0.5rem;
`;

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

type FormType = MirrorConfigurationDto;

type Props = {
  onConfigurationChange: (config: MirrorConfigurationDto, valid: boolean) => void;
  initialConfiguration: MirrorConfigurationDto;
  readOnly: boolean;
  repository: Repository;
};

const ConfigEditor: FC<Props> = ({ initialConfiguration, onConfigurationChange, readOnly: disabled, repository }) => {
  const userSuggestions = useUserSuggestions();
  const [t] = useTranslation("plugins");
  const { register, formState, getValues, setValue } = useForm<FormType>({
    mode: "onChange",
    defaultValues: initialConfiguration
  });
  const formValue = getValues();

  const addManagingUser = (selectValue: SelectValue) => {
    if (formValue.managingUsers?.includes(selectValue.value.id)) {
      return;
    }
    setValue("managingUsers", [...(formValue.managingUsers || []), selectValue.value.id]);
  };

  useEffect(() => {
    const output: MirrorConfigurationDto = { ...formValue };

    if (!output.usernamePasswordCredential?.username) {
      delete output.usernamePasswordCredential;
    }
    if (!output.certificationCredential?.certificate) {
      delete output.certificationCredential;
    }

    onConfigurationChange(output, formState.isValid);
  }, [formValue, formState.isValid]);

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

  const credentialsForm = (
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
      <Column className="column is-half">
        <FileInput
          label={t("scm-repository-mirror-plugin.form.certificate.label")}
          helpText={t("scm-repository-mirror-plugin.form.certificate.helpText")}
          disabled={disabled}
          onChange={(event: ChangeEvent<HTMLInputElement>) => {
            const file = event.target?.files?.[0];
            if (file) {
              readBinaryFileAsBase64String(file).then(base64String =>
                // @ts-ignore
                setValue("certificationCredential.certificate", base64String)
              );
            }
          }}
        />
      </Column>
      <Column className="column is-half">
        <InputField
          label={t("scm-repository-mirror-plugin.form.certificate.password.label")}
          type="password"
          helpText={t("scm-repository-mirror-plugin.form.certificate.password.helpText")}
          disabled={disabled}
          {...register("certificationCredential.password")}
        />
      </Column>
    </>
  );

  return (
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
          label={t("scm-repository-mirror-plugin.form.period.label")}
          helpText={t("scm-repository-mirror-plugin.form.period.helpText")}
          options={periodOptions}
          disabled={disabled}
          {...register("synchronizationPeriod")}
        />
      </Column>
      <Column className="column is-full">
        <MemberNameTagGroup
          members={formValue.managingUsers || []}
          memberListChanged={newManagingUsers => setValue("managingUsers", newManagingUsers)}
          label={t("scm-repository-mirror-plugin.form.managingUsers.label")}
          helpText={t("scm-repository-mirror-plugin.form.managingUsers.helpText")}
        />
        <AutocompleteAddEntryToTableField
          addEntry={addManagingUser}
          buttonLabel={t("scm-repository-mirror-plugin.form.managingUsers.addButton")}
          loadSuggestions={userSuggestions}
          placeholder={t("scm-repository-mirror-plugin.form.managingUsers.autocompletePlaceholder")}
        />
      </Column>
    </Columns>
  );
};

export default ConfigEditor;
