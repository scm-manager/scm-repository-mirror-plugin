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

import { Control, useController, useForm, useWatch } from "react-hook-form";
import {
  MirrorAccessConfigurationForm,
  MirrorFilterConfigurationDto,
  mirrorGpgVerificationTypes,
  PublicKey
} from "../types";
import React, { ChangeEvent, FC, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { SelectValue } from "@scm-manager/ui-types";
import {
  AutocompleteAddEntryToTableField,
  Button,
  Checkbox,
  FileInput,
  Icon,
  InputField,
  Level,
  MemberNameTagGroup,
  Select,
  SelectItem,
  Textarea
} from "@scm-manager/ui-components";
import { useUserSuggestions } from "@scm-manager/ui-api";
import readBinaryFileAsBase64String from "../readBinaryFileAsBase64String";
import styled from "styled-components";

const Column = styled.div`
  padding: 0 0.75rem;
  margin-bottom: 0.5rem;
`;

export type MirrorAccessConfigControlProps = { control: Control<MirrorAccessConfigurationForm>; isReadonly: boolean };
export type MirrorFilterConfigControlProps = {
  control: Control<MirrorFilterConfigurationDto>;
  isReadonly?: boolean;
};

export const ManagingUsersControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
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

export const FileInputControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
  const { field } = useController({ control, name: "certificateCredential.certificate" });
  const [t] = useTranslation("plugins");

  return (
    <FileInput
      label={t("scm-repository-mirror-plugin.form.certificate.label")}
      helpText={t("scm-repository-mirror-plugin.form.certificate.helpText")}
      disabled={isReadonly}
      testId="certificate-input"
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

export const CredentialsControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field: enabledField } = useController({ control, name: "usernamePasswordCredential.enabled" });
  const { field: usernameField } = useController({ control, name: "usernamePasswordCredential.username" });
  const { field: passwordField } = useController({ control, name: "usernamePasswordCredential.password" });

  const { field: certificateEnabledField } = useController({ control, name: "certificateCredential.enabled" });
  const { field: certificatePasswordField } = useController({ control, name: "certificateCredential.password" });

  return (
    <>
      <Column className="column is-full">
        <Checkbox
          label={t("scm-repository-mirror-plugin.form.withBaseAuth.label")}
          testId="base-auth-checkbox"
          {...enabledField}
        />
      </Column>
      {enabledField.value ? (
        <>
          <Column className="column is-half">
            <InputField
              label={t("scm-repository-mirror-plugin.form.username.label")}
              helpText={t("scm-repository-mirror-plugin.form.username.helpText")}
              disabled={isReadonly}
              testId="username-input"
              {...usernameField}
            />
          </Column>
          <Column className="column is-half">
            <InputField
              label={t("scm-repository-mirror-plugin.form.password.label")}
              type="password"
              helpText={t("scm-repository-mirror-plugin.form.password.helpText")}
              disabled={isReadonly}
              testId="username-password-input"
              {...passwordField}
            />
          </Column>
        </>
      ) : null}
      <Column className="column is-full">
        <Checkbox label={t("scm-repository-mirror-plugin.form.withKeyAuth.label")} {...certificateEnabledField} />
      </Column>
      {certificateEnabledField.value ? (
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
              testId="certificate-password-input"
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
    label: t("scm-repository-mirror-plugin.form.period.options.oneMinute"),
    value: "1"
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.twoMinutes"),
    value: "2"
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.threeMinutes"),
    value: "3"
  },
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
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.disabled"),
    value: "0"
  }
];

export const SynchronizationPeriodControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field } = useController({ control, name: "synchronizationPeriod", defaultValue: "60" });

  return (
    <Column className="column is-half">
      <Select
        label={t("scm-repository-mirror-plugin.form.period.label")}
        helpText={t("scm-repository-mirror-plugin.form.period.helpText")}
        options={createPeriodOptions(t)}
        disabled={isReadonly}
        testId="synchronization-period-input"
        {...field}
        value={field.value || "0"}
      />
    </Column>
  );
};

export const UrlControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
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
        testId="url-input"
        required={true}
        aria-required={true}
        {...field}
      />
    </Column>
  );
};

export const ProxyHostControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field, fieldState } = useController({
    control,
    name: "proxyConfiguration.host",
    rules: {
      required: {
        value: true,
        message: t("scm-repository-mirror-plugin.form.proxy.host.errors.required")
      },
      pattern: {
        value: /^[^\s$.?#].[^\s]*$/,
        message: t("scm-repository-mirror-plugin.form.proxy.host.errors.invalid")
      }
    },
    shouldUnregister: true
  });

  return (
    <div className="column is-full">
      <InputField
        label={t("scm-repository-mirror-plugin.form.proxy.host.label")}
        helpText={t("scm-repository-mirror-plugin.form.proxy.host.helpText")}
        errorMessage={fieldState.error?.message}
        validationError={!!fieldState.error}
        disabled={isReadonly}
        className="mb-0"
        required={true}
        aria-required={true}
        {...field}
      />
    </div>
  );
};

export const ProxyPortControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field, fieldState } = useController({
    control,
    name: "proxyConfiguration.port",
    rules: {
      required: {
        value: true,
        message: t("scm-repository-mirror-plugin.form.proxy.port.errors.required")
      },
      min: {
        value: 1,
        message: t("scm-repository-mirror-plugin.form.proxy.port.errors.range")
      },
      max: {
        value: 65535,
        message: t("scm-repository-mirror-plugin.form.proxy.port.errors.range")
      }
    },
    shouldUnregister: true
  });

  return (
    <div className="column is-full">
      <InputField
        label={t("scm-repository-mirror-plugin.form.proxy.port.label")}
        helpText={t("scm-repository-mirror-plugin.form.proxy.port.helpText")}
        errorMessage={fieldState.error?.message}
        validationError={!!fieldState.error}
        type="number"
        disabled={isReadonly}
        className="mb-0"
        required={true}
        aria-required={true}
        {...field}
      />
    </div>
  );
};

export const ProxyUsernameControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field, fieldState } = useController({
    control,
    name: "proxyConfiguration.username",
    shouldUnregister: true
  });

  return (
    <div className="column is-full">
      <InputField
        label={t("scm-repository-mirror-plugin.form.proxy.username.label")}
        helpText={t("scm-repository-mirror-plugin.form.proxy.username.helpText")}
        errorMessage={fieldState.error?.message}
        validationError={!!fieldState.error}
        disabled={isReadonly}
        className="mb-0"
        {...field}
      />
    </div>
  );
};

export const ProxyPasswordControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field, fieldState } = useController({
    control,
    name: "proxyConfiguration.password",
    shouldUnregister: true
  });

  return (
    <div className="column is-full">
      <InputField
        label={t("scm-repository-mirror-plugin.form.proxy.password.label")}
        helpText={t("scm-repository-mirror-plugin.form.proxy.password.helpText")}
        type="password"
        errorMessage={fieldState.error?.message}
        validationError={!!fieldState.error}
        disabled={isReadonly}
        className="mb-0"
        {...field}
      />
    </div>
  );
};

export const coalesceFormValue = <T extends MirrorAccessConfigurationForm>(value: T): T => {
  const output: MirrorAccessConfigurationForm = { ...value };

  if (!output.usernamePasswordCredential?.enabled || !output.usernamePasswordCredential?.username) {
    delete output.usernamePasswordCredential;
  }
  if (!output.certificateCredential?.enabled) {
    delete output.certificateCredential;
  }
  if (output.synchronizationPeriod === "0") {
    delete output.synchronizationPeriod;
  }

  return output as T;
};

const VerticalAlignCell = styled.td`
  vertical-align: middle !important;
`;

export const PublicKeysControl: FC<MirrorFilterConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field } = useController({
    control,
    name: "allowedGpgKeys",
    rules: {
      validate: v => v?.length > 0
    },
    shouldUnregister: true
  });
  const { register, handleSubmit, reset, formState } = useForm<PublicKey>({
    mode: "onChange"
  });
  const gpgVerificationType = useWatch({ control, name: "gpgVerificationType" });
  const { isValid, errors } = formState;
  const { value } = field;

  useEffect(() => {
    field.onChange(field.value);
  }, [gpgVerificationType]);

  const deleteKey = (displayName: string) =>
    field.onChange(value?.filter(key => key.displayName !== displayName) || []);
  const addNewKey = (key: PublicKey) => {
    field.onChange([...(value || []), key]);
    reset();
  };

  return (
    <>
      {!value || value.length === 0 ? (
        <div className="notification is-warning">
          {t("scm-repository-mirror-plugin.form.keyList.emptyNotification")}
        </div>
      ) : (
        <table className="card-table table is-hoverable is-fullwidth">
          <tr>
            <th>{t("scm-repository-mirror-plugin.form.keyList.displayName")}</th>
            <th />
          </tr>
          {value.map(({ raw, displayName }, index) => (
            <tr key={index}>
              <VerticalAlignCell>{displayName}</VerticalAlignCell>
              <td className="has-text-right">
                <button className="button" onClick={() => deleteKey(displayName)}>
                  <span className="icon">
                    <Icon name="trash" title={t("scm-repository-mirror-plugin.form.keyList.delete")} color="inherit" />
                  </span>
                </button>
              </td>
            </tr>
          ))}
        </table>
      )}
      {isReadonly ? null : (
        <>
          <h5 className="subtitle is-5">{t("scm-repository-mirror-plugin.form.keyList.new.title")}</h5>
          <form>
            <InputField
              className="mb-5"
              label={t("scm-repository-mirror-plugin.form.keyList.new.displayName.label")}
              helpText={t("scm-repository-mirror-plugin.form.keyList.new.displayName.helpText")}
              errorMessage={t("scm-repository-mirror-plugin.form.keyList.new.displayName.errors.required")}
              validationError={!!errors.displayName}
              {...register("displayName", {
                required: true
              })}
            />
            <Textarea
              label={t("scm-repository-mirror-plugin.form.keyList.new.raw.label")}
              helpText={t("scm-repository-mirror-plugin.form.keyList.new.raw.helpText")}
              errorMessage={t("scm-repository-mirror-plugin.form.keyList.new.raw.errors.required")}
              validationError={!!errors.raw}
              {...register("raw", {
                required: true
              })}
            />
            <Level
              right={
                <Button
                  action={handleSubmit(addNewKey)}
                  disabled={!isValid}
                  label={t("scm-repository-mirror-plugin.form.keyList.new.submit")}
                />
              }
            />
          </form>
        </>
      )}
    </>
  );
};

export const createGpgVerificationTypeOptions: (t: (key: string) => string) => SelectItem[] = t =>
  mirrorGpgVerificationTypes.map(mirrorGpgVerificationType => ({
    label: t(
      `scm-repository-mirror-plugin.form.gpgVerificationType.options.${mirrorGpgVerificationType.toLowerCase()}`
    ),
    value: mirrorGpgVerificationType
  }));

export const GpgVerificationControl: FC<MirrorFilterConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field: verificationTypeField } = useController({
    control,
    name: "gpgVerificationType",
    defaultValue: "NONE",
    shouldUnregister: true
  });
  const { value } = verificationTypeField;
  const showKeyList = value === "KEY_LIST";
  return (
    <>
      <Select
        label={t("scm-repository-mirror-plugin.form.gpgVerificationType.label")}
        helpText={t("scm-repository-mirror-plugin.form.gpgVerificationType.helpText")}
        disabled={isReadonly}
        options={createGpgVerificationTypeOptions(t)}
        {...verificationTypeField}
      />
      {showKeyList ? <PublicKeysControl control={control} isReadonly={isReadonly} /> : null}
    </>
  );
};
