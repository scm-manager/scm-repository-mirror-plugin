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

import { Control, useController } from "react-hook-form";
import { MirrorConfigurationDto } from "../types";
import React, { ChangeEvent, FC, useState } from "react";
import { useTranslation } from "react-i18next";
import { SelectValue } from "@scm-manager/ui-types";
import {
  AutocompleteAddEntryToTableField,
  Checkbox,
  FileInput,
  InputField,
  MemberNameTagGroup,
  Select,
  SelectItem
} from "@scm-manager/ui-components";
import { useUserSuggestions } from "@scm-manager/ui-api";
import readBinaryFileAsBase64String from "../readBinaryFileAsBase64String";
import styled from "styled-components";

const Column = styled.div`
  padding: 0 0.75rem;
  margin-bottom: 0.5rem;
`;

export type ControlProps = { control: Control<MirrorConfigurationDto>; isReadonly: boolean };

export const ManagingUsersControl: FC<ControlProps> = ({ control, isReadonly }) => {
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
    <Column className="column is-full">
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
    </Column>
  );
};

export const FileInputControl: FC<ControlProps> = ({ control, isReadonly }) => {
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

export const CredentialsControl: FC<ControlProps> = ({ control, isReadonly }) => {
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
              disabled={isReadonly}
              {...usernameField}
            />
          </Column>
          <Column className="column is-half">
            <InputField
              label={t("scm-repository-mirror-plugin.form.password.label")}
              type="password"
              helpText={t("scm-repository-mirror-plugin.form.password.helpText")}
              disabled={isReadonly}
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
            <FileInputControl control={control} isReadonly={isReadonly} />
          </Column>
          <Column className="column is-half">
            <InputField
              label={t("scm-repository-mirror-plugin.form.certificate.password.label")}
              type="password"
              helpText={t("scm-repository-mirror-plugin.form.certificate.password.helpText")}
              disabled={isReadonly}
              {...certificatePasswordField}
            />
          </Column>
        </>
      ) : null}
    </>
  );
};

export const createPeriodOptions: (t: (key: string) => string) => SelectItem[] = t => [
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

export const SynchronizationPeriodControl: FC<ControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field } = useController({ control, name: "synchronizationPeriod", defaultValue: 60 });

  return (
    <Column className="column is-full">
      <Select
        label={t("scm-repository-mirror-plugin.form.period.label")}
        helpText={t("scm-repository-mirror-plugin.form.period.helpText")}
        options={createPeriodOptions(t)}
        disabled={isReadonly}
        {...field}
      />
    </Column>
  );
};

export const UrlControl: FC<ControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field, fieldState } = useController({
    control,
    name: "url",
    rules: {
      required: {
        value: true,
        message: t("scm-repository-mirror-plugin.form.url.errors.required")
      },
      pattern: {
        value: /^[A-Za-z0-9]+:\/\/[^\s$.?#].[^\s]*$/,
        message: t("scm-repository-mirror-plugin.form.url.errors.invalid")
      }
    }
  });

  return (
    <Column className="column is-full">
      <InputField
        label={t("scm-repository-mirror-plugin.form.url.label")}
        helpText={t("scm-repository-mirror-plugin.form.url.helpText")}
        errorMessage={fieldState.error?.message}
        validationError={!!fieldState.error}
        disabled={isReadonly}
        {...field}
      />
    </Column>
  );
};

export const coalesceFormValue = <T extends MirrorConfigurationDto>(value: T): T => {
  const output: MirrorConfigurationDto = { ...value };

  if (!output.usernamePasswordCredential?.username) {
    delete output.usernamePasswordCredential;
  }
  if (!output.certificateCredential?.certificate) {
    delete output.certificateCredential;
  }

  return output as T;
};
