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
  MutualTlsAuthenticationMethod,
  UserAuthenticationMethod,
  mirrorGpgVerificationTypes,
  PublicKey,
} from "../types";
import React, { ChangeEvent, FC, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { AutocompleteObject, Option } from "@scm-manager/ui-types";
import { MemberNameTagGroup } from "@scm-manager/ui-components";
import { useUserSuggestions } from "@scm-manager/ui-api";
import readBinaryFileAsBase64String from "../readBinaryFileAsBase64String";
import styled from "styled-components";
import {
  Button,
  ComboboxField,
  InputField,
  Field,
  FieldMessage,
  FileInputField,
  Icon,
  IconButton,
  Label,
  Level,
  RadioGroupField,
  SelectField,
  Textarea,
} from "@scm-manager/ui-core";

type SelectItem = NonNullable<React.ComponentProps<typeof SelectField>["options"]>[number];

const Column = styled.div`
  padding: 0 0.75rem;
  margin-bottom: 0.5rem;
`;

// Temporary Figma border color preview for the credential input areas.
const CredentialInputPreviewColumn = styled(Column)`
  padding: 1.5rem;
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

  const addManagingUser = (userId: string) => {
    const trimmedId = userId.trim();
    if (trimmedId && !field.value?.includes(trimmedId)) {
      field.onChange([...(field.value || []), trimmedId]);
    }
  };

  const loadAutocompletion = (query: string) => {
    if (query.length < 2) return [];

    return userSuggestions(query).then((options: any) => {
      const apiResults = options.map((opt: any) => ({
        value: opt.value,
        label: opt.label,
      }));

      const exactMatch = apiResults.find((o: any) => o.value.id.toLowerCase() === query.toLowerCase());

      if (!exactMatch) {
        const customOption = {
          value: { id: query, displayName: query },
          label: query,
        };
        return [customOption, ...apiResults];
      }

      return apiResults;
    });
  };

  const onSelectFromAutocomplete = (option?: Option<AutocompleteObject>) => {
    if (option?.value?.id) {
      addManagingUser(option.value.id);
    }
  };

  return (
    <Column className="column is-full">
      <MemberNameTagGroup
        members={field.value || []}
        memberListChanged={field.onChange}
        label={t("scm-repository-mirror-plugin.form.managingUsers.label")}
        helpText={t("scm-repository-mirror-plugin.form.managingUsers.helpText")}
      />
      <ComboboxField
        label={t("scm-repository-mirror-plugin.form.managingUsers.autocompletePlaceholder")}
        disabled={isReadonly}
        placeholder={t("scm-repository-mirror-plugin.form.managingUsers.autocompletePlaceholder")}
        options={loadAutocompletion}
        onChange={onSelectFromAutocomplete}
        value={null}
      />
    </Column>
  );
};

export const FileInputControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
  const {
    field: { name, onBlur, onChange, ref },
  } = useController({ control, name: "certificateCredential.certificate" });
  const [t] = useTranslation("plugins");

  return (
    <FileInputField
      label={t("scm-repository-mirror-plugin.form.certificate.label")}
      descriptionText={t("scm-repository-mirror-plugin.form.certificate.helpText")}
      disabled={isReadonly}
      testId="certificate-input"
      name={name}
      onBlur={onBlur}
      ref={ref}
      onChange={(event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target?.files?.[0];
        if (file) {
          readBinaryFileAsBase64String(file).then(onChange);
        }
      }}
    />
  );
};

export const CredentialsControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field: userAuthenticationField } = useController({
    control,
    name: "userAuthentication",
    defaultValue: "NONE",
  });
  const { field: usernameField, fieldState: usernameFieldState } = useController({
    control,
    name: "usernamePasswordCredential.username",
    rules: {
      required:
        userAuthenticationField.value === "USERNAME_PASSWORD"
          ? t("scm-repository-mirror-plugin.form.username.errors.required")
          : false,
    },
  });
  const { field: passwordField, fieldState: passwordFieldState } = useController({
    control,
    name: "usernamePasswordCredential.password",
    rules: {
      required:
        userAuthenticationField.value !== "NONE"
          ? t(
              userAuthenticationField.value === "TOKEN"
                ? "scm-repository-mirror-plugin.form.token.errors.required"
                : "scm-repository-mirror-plugin.form.password.errors.required",
            )
          : false,
    },
  });

  const { field: mutualTlsAuthenticationField } = useController({
    control,
    name: "mutualTlsAuthentication",
    defaultValue: "NONE",
  });
  const { field: certificatePasswordField } = useController({ control, name: "certificateCredential.password" });

  const userAuthenticationOptions: { value: UserAuthenticationMethod; label: string }[] = [
    {
      value: "NONE",
      label: t("scm-repository-mirror-plugin.form.userAuthentication.options.none"),
    },
    {
      value: "USERNAME_PASSWORD",
      label: t("scm-repository-mirror-plugin.form.userAuthentication.options.usernamePassword"),
    },
    {
      value: "TOKEN",
      label: t("scm-repository-mirror-plugin.form.userAuthentication.options.token"),
    },
  ];
  const mutualTlsAuthenticationOptions: { value: MutualTlsAuthenticationMethod; label: string }[] = [
    {
      value: "NONE",
      label: t("scm-repository-mirror-plugin.form.mutualTlsAuthentication.options.none"),
    },
    {
      value: "CERTIFICATE",
      label: t("scm-repository-mirror-plugin.form.mutualTlsAuthentication.options.certificate"),
    },
  ];

  return (
    <>
      <Column className="column is-full">
        <h2 className="subtitle">{t("scm-repository-mirror-plugin.form.sourceRepositoryAuthentication")}</h2>
      </Column>
      <Column className="column is-full">
        <RadioGroupField
          label={t("scm-repository-mirror-plugin.form.userAuthentication.label")}
          options={userAuthenticationOptions}
          name={userAuthenticationField.name}
          value={userAuthenticationField.value}
          onValueChange={userAuthenticationField.onChange}
          onBlur={userAuthenticationField.onBlur}
          disabled={isReadonly}
          data-testid="user-authentication-radio-group"
        />
      </Column>
      {userAuthenticationField.value === "USERNAME_PASSWORD" ? (
        <CredentialInputPreviewColumn className="column is-full">
          <InputField
            label={t("scm-repository-mirror-plugin.form.username.label")}
            error={usernameFieldState.error?.message}
            disabled={isReadonly}
            required={true}
            testId="username-input"
            {...usernameField}
          />
          <InputField
            label={t("scm-repository-mirror-plugin.form.password.label")}
            type="password"
            error={passwordFieldState.error?.message}
            disabled={isReadonly}
            required={true}
            testId="username-password-input"
            {...passwordField}
          />
        </CredentialInputPreviewColumn>
      ) : userAuthenticationField.value === "TOKEN" ? (
        <CredentialInputPreviewColumn className="column is-full">
          <InputField
            label={t("scm-repository-mirror-plugin.form.token.label")}
            descriptionText={t("scm-repository-mirror-plugin.form.token.helpText")}
            type="password"
            error={passwordFieldState.error?.message}
            disabled={isReadonly}
            required={true}
            testId="token-input"
            {...passwordField}
          />
        </CredentialInputPreviewColumn>
      ) : null}
      <Column className="column is-full">
        <RadioGroupField
          label={t("scm-repository-mirror-plugin.form.mutualTlsAuthentication.label")}
          helpText={t("scm-repository-mirror-plugin.form.mutualTlsAuthentication.helpText")}
          options={mutualTlsAuthenticationOptions}
          name={mutualTlsAuthenticationField.name}
          value={mutualTlsAuthenticationField.value}
          onValueChange={mutualTlsAuthenticationField.onChange}
          onBlur={mutualTlsAuthenticationField.onBlur}
          disabled={isReadonly}
          data-testid="mutual-tls-authentication-radio-group"
        />
      </Column>
      {mutualTlsAuthenticationField.value === "CERTIFICATE" ? (
        <CredentialInputPreviewColumn className="column is-full">
          <FileInputControl control={control} isReadonly={isReadonly} />
          <InputField
            label={t("scm-repository-mirror-plugin.form.certificate.password.label")}
            type="password"
            descriptionText={t("scm-repository-mirror-plugin.form.certificate.password.helpText")}
            disabled={isReadonly}
            testId="certificate-password-input"
            {...certificatePasswordField}
          />
        </CredentialInputPreviewColumn>
      ) : null}
    </>
  );
};

export const createPeriodOptions: (t: (key: string) => string) => SelectItem[] = (t) => [
  {
    label: t("scm-repository-mirror-plugin.form.period.options.oneMinute"),
    value: "1",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.twoMinutes"),
    value: "2",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.threeMinutes"),
    value: "3",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.fiveMinutes"),
    value: "5",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.fifteenMinutes"),
    value: "15",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.thirtyMinutes"),
    value: "30",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.oneHour"),
    value: "60",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.twoHours"),
    value: "120",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.fourHours"),
    value: "240",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.twelveHours"),
    value: "720",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.oneDay"),
    value: "1440",
  },
  {
    label: t("scm-repository-mirror-plugin.form.period.options.disabled"),
    value: "0",
  },
];

export const SynchronizationPeriodControl: FC<MirrorAccessConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field } = useController({ control, name: "synchronizationPeriod", defaultValue: "60" });

  return (
    <Column className="column is-half">
      <SelectField
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

export const UrlControl: FC<{ isCreation: boolean } & MirrorAccessConfigControlProps> = ({
  control,
  isReadonly,
  isCreation,
}) => {
  const [t] = useTranslation("plugins");
  const { field, fieldState } = useController({
    control,
    name: "url",
    rules: {
      required: {
        value: true,
        message: t("scm-repository-mirror-plugin.form.url.errors.required"),
      },
      pattern: {
        value: /^[A-Za-z0-9]+:\/\/[^\s$.?#].[^\s]*$/,
        message: t("scm-repository-mirror-plugin.form.url.errors.invalid"),
      },
    },
  });

  return (
    <Column className="column is-full">
      <InputField
        label={t("scm-repository-mirror-plugin.form.url.label")}
        descriptionText={
          isReadonly || isCreation ? undefined : t("scm-repository-mirror-plugin.form.url.descriptionText")
        }
        error={fieldState.error?.message}
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
        message: t("scm-repository-mirror-plugin.form.proxy.host.errors.required"),
      },
      pattern: {
        value: /^[^\s$.?#].[^\s]*$/,
        message: t("scm-repository-mirror-plugin.form.proxy.host.errors.invalid"),
      },
    },
    shouldUnregister: true,
  });

  return (
    <div className="column is-full">
      <InputField
        label={t("scm-repository-mirror-plugin.form.proxy.host.label")}
        helpText={t("scm-repository-mirror-plugin.form.proxy.host.helpText")}
        error={fieldState.error?.message}
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
        message: t("scm-repository-mirror-plugin.form.proxy.port.errors.required"),
      },
      min: {
        value: 1,
        message: t("scm-repository-mirror-plugin.form.proxy.port.errors.range"),
      },
      max: {
        value: 65535,
        message: t("scm-repository-mirror-plugin.form.proxy.port.errors.range"),
      },
    },
    shouldUnregister: true,
  });

  return (
    <div className="column is-full">
      <InputField
        label={t("scm-repository-mirror-plugin.form.proxy.port.label")}
        helpText={t("scm-repository-mirror-plugin.form.proxy.port.helpText")}
        error={fieldState.error?.message}
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
    shouldUnregister: true,
  });

  return (
    <div className="column is-full">
      <InputField
        label={t("scm-repository-mirror-plugin.form.proxy.username.label")}
        helpText={t("scm-repository-mirror-plugin.form.proxy.username.helpText")}
        error={fieldState.error?.message}
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
    shouldUnregister: true,
  });

  return (
    <div className="column is-full">
      <InputField
        label={t("scm-repository-mirror-plugin.form.proxy.password.label")}
        helpText={t("scm-repository-mirror-plugin.form.proxy.password.helpText")}
        type="password"
        error={fieldState.error?.message}
        disabled={isReadonly}
        className="mb-0"
        {...field}
      />
    </div>
  );
};

export const coalesceFormValue = <T extends MirrorAccessConfigurationForm>(value: T): T => {
  const output: MirrorAccessConfigurationForm = { ...value };

  if (output.userAuthentication === "TOKEN") {
    if (output.usernamePasswordCredential?.password) {
      output.usernamePasswordCredential = {
        ...output.usernamePasswordCredential,
        username: "",
      };
    } else {
      delete output.usernamePasswordCredential;
    }
  } else if (output.userAuthentication === "USERNAME_PASSWORD") {
    if (!output.usernamePasswordCredential?.username) {
      delete output.usernamePasswordCredential;
    }
  } else {
    delete output.usernamePasswordCredential;
  }

  if (output.mutualTlsAuthentication !== "CERTIFICATE") {
    delete output.certificateCredential;
  }

  delete output.userAuthentication;
  delete output.mutualTlsAuthentication;

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
      validate: (v) => v?.length > 0,
    },
    shouldUnregister: true,
  });
  const { register, handleSubmit, reset, formState } = useForm<PublicKey>({
    mode: "onChange",
  });
  const gpgVerificationType = useWatch({ control, name: "gpgVerificationType" });
  const { isValid, errors } = formState;
  const { value } = field;

  useEffect(() => {
    field.onChange(field.value);
  }, [gpgVerificationType]);

  const deleteKey = (displayName: string) =>
    field.onChange(value?.filter((key) => key.displayName !== displayName) || []);
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
                <IconButton
                  title={t("scm-repository-mirror-plugin.form.keyList.delete")}
                  aria-label={t("scm-repository-mirror-plugin.form.keyList.delete")}
                  onClick={() => deleteKey(displayName)}
                >
                  <Icon>trash</Icon>
                </IconButton>
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
              error={
                errors.displayName
                  ? t("scm-repository-mirror-plugin.form.keyList.new.displayName.errors.required")
                  : undefined
              }
              {...register("displayName", {
                required: true,
              })}
            />
            <Field>
              <Label htmlFor="gpg-key-raw">{t("scm-repository-mirror-plugin.form.keyList.new.raw.label")}</Label>
              <Textarea id="gpg-key-raw" aria-describedby="gpg-key-raw-help" {...register("raw", { required: true })} />
              <p id="gpg-key-raw-help" className="help">
                {t("scm-repository-mirror-plugin.form.keyList.new.raw.helpText")}
              </p>
              {errors.raw ? (
                <FieldMessage variant="danger">
                  {t("scm-repository-mirror-plugin.form.keyList.new.raw.errors.required")}
                </FieldMessage>
              ) : null}
            </Field>
            <Level
              right={
                <Button onClick={handleSubmit(addNewKey)} disabled={!isValid}>
                  {t("scm-repository-mirror-plugin.form.keyList.new.submit")}
                </Button>
              }
            />
          </form>
        </>
      )}
    </>
  );
};

export const createGpgVerificationTypeOptions: (t: (key: string) => string) => SelectItem[] = (t) =>
  mirrorGpgVerificationTypes.map((mirrorGpgVerificationType) => ({
    label: t(
      `scm-repository-mirror-plugin.form.gpgVerificationType.options.${mirrorGpgVerificationType.toLowerCase()}`,
    ),
    value: mirrorGpgVerificationType,
  }));

export const GpgVerificationControl: FC<MirrorFilterConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field: verificationTypeField } = useController({
    control,
    name: "gpgVerificationType",
    defaultValue: "NONE",
    shouldUnregister: true,
  });
  const { value } = verificationTypeField;
  const showKeyList = value === "KEY_LIST";
  return (
    <>
      <SelectField
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
