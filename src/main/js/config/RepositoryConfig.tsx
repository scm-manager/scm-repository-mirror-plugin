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
import {
  AutocompleteAddEntryToTableField,
  ConfigurationForm,
  FileInput,
  InputField,
  MemberNameTagGroup,
  Select,
  SelectItem
} from "@scm-manager/ui-components";
import { Repository, SelectValue } from "@scm-manager/ui-types";
import { useConfigLink, useUserSuggestions } from "@scm-manager/ui-api";
import { MirrorConfigurationDto } from "../types";
import { useTranslation } from "react-i18next";
import { Control, useController, useForm, useWatch } from "react-hook-form";
import readBinaryFileAsBase64String from "../readBinaryFileAsBase64String";
import styled from "styled-components";

const Column = styled.div`
  padding: 0 0.75rem;
  margin-bottom: 0.5rem;
`;

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

const createPeriodOptions: (t: (key: string) => string) => SelectItem[] = t => [
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

type Props = {
  link: string;
};

type ControlProps = { control: Control<MirrorConfigurationDto>; isReadonly: boolean };

const ManagingUsersControl: FC<ControlProps> = ({ control, isReadonly }) => {
  const userSuggestions = useUserSuggestions();
  const { field } = useController({ control, name: "managingUsers" });
  const [t] = useTranslation("plugins");

  const addManagingUser = (selectValue: SelectValue) => {
    if (field.value?.includes(selectValue.value.id)) {
      return;
    }
    field.onChange([...(field.value || []), selectValue.value.id]);
  };

  return (
    <>
      <MemberNameTagGroup
        members={field.value || []}
        memberListChanged={field.onChange}
        label={t("scm-repository-mirror-plugin.form.managingUsers.label")}
        helpText={t("scm-repository-mirror-plugin.form.managingUsers.helpText")}
      />
      <AutocompleteAddEntryToTableField
        addEntry={addManagingUser}
        buttonLabel={t("scm-repository-mirror-plugin.form.managingUsers.addButton")}
        loadSuggestions={userSuggestions}
        placeholder={t("scm-repository-mirror-plugin.form.managingUsers.autocompletePlaceholder")}
        disabled={isReadonly}
      />
    </>
  );
};

const FileInputControl: FC<ControlProps> = ({ control, isReadonly }) => {
  const { field } = useController({ control, name: "certificateCredential.certificate" });
  const [t] = useTranslation("plugins");

  return (
    <FileInput
      label={t("scm-repository-mirror-plugin.form.certificate.label")}
      helpText={t("scm-repository-mirror-plugin.form.certificate.helpText")}
      disabled={isReadonly}
      {...field}
      onChange={(event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target?.files?.[0];
        if (file) {
          readBinaryFileAsBase64String(file).then(base64String => field.onChange(base64String));
        }
      }}
    />
  );
};

const RepositoryConfig: FC<Props> = ({ link }) => {
  const [t] = useTranslation("plugins");
  const { initialConfiguration, update, isReadonly, ...formProps } = useConfigLink<MirrorConfigurationDto>(link);
  const { register, formState, handleSubmit, control, reset } = useForm<MirrorConfigurationDto>({
    mode: "onChange"
  });

  useEffect(() => {
    if (initialConfiguration) {
      reset(initialConfiguration);
    }
  }, [initialConfiguration]);

  const credentialsForm = (
    <>
      <Column className="column is-half">
        <InputField
          label={t("scm-repository-mirror-plugin.form.username.label")}
          helpText={t("scm-repository-mirror-plugin.form.username.helpText")}
          disabled={isReadonly}
          {...register("usernamePasswordCredential.username")}
        />
      </Column>
      <Column className="column is-half">
        <InputField
          label={t("scm-repository-mirror-plugin.form.password.label")}
          type="password"
          helpText={t("scm-repository-mirror-plugin.form.password.helpText")}
          disabled={isReadonly}
          {...register("usernamePasswordCredential.password")}
        />
      </Column>
      <Column className="column is-half">
        <FileInputControl control={control} isReadonly={isReadonly} />
      </Column>
      <Column className="column is-half">
        <InputField
          label={t("scm-repository-mirror-plugin.form.certificate.password.label")}
          type="password"
          helpText={t("scm-repository-mirror-plugin.form.certificate.password.helpText")}
          disabled={isReadonly}
          {...register("certificateCredential.password")}
        />
      </Column>
    </>
  );

  return (
    <ConfigurationForm
      isValid={formState.isValid}
      isReadonly={isReadonly}
      onSubmit={handleSubmit(formValue => {
        const output: MirrorConfigurationDto = { ...formValue };

        if (!output.usernamePasswordCredential?.username) {
          delete output.usernamePasswordCredential;
        }
        if (!output.certificateCredential?.certificate) {
          delete output.certificateCredential;
        }

        update(output);
      })}
      {...formProps}
    >
      <Columns className="columns is-multiline">
        <Column className="column is-full">
          <InputField
            label={t("scm-repository-mirror-plugin.form.url.label")}
            helpText={t("scm-repository-mirror-plugin.form.url.helpText")}
            errorMessage={formState.errors.url?.message}
            validationError={!!formState.errors.url}
            disabled={isReadonly}
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
            options={createPeriodOptions(t)}
            disabled={isReadonly}
            {...register("synchronizationPeriod")}
          />
        </Column>
        <Column className="column is-full">
          <ManagingUsersControl control={control} isReadonly={isReadonly} />
        </Column>
      </Columns>
    </ConfigurationForm>
  );
};

export default RepositoryConfig;
