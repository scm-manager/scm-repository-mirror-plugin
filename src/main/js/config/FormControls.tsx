import { Control, useController, useForm } from "react-hook-form";
import { GlobalConfigurationDto, MirrorConfigurationDto, MirrorGpgVerificationType, PublicKey } from "../types";
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
  SelectItem,
  Icon,
  Textarea,
  SubmitButton,
  Level
} from "@scm-manager/ui-components";
import { useUserSuggestions } from "@scm-manager/ui-api";
import readBinaryFileAsBase64String from "../readBinaryFileAsBase64String";
import styled from "styled-components";

const Column = styled.div`
  padding: 0 0.75rem;
  margin-bottom: 0.5rem;
`;

export type MirrorConfigControlProps = { control: Control<MirrorConfigurationDto>; isReadonly: boolean };
export type GlobalMirrorConfigControlProps = { control: Control<GlobalConfigurationDto>; isReadonly: boolean };

export const ManagingUsersControl: FC<MirrorConfigControlProps> = ({ control, isReadonly }) => {
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

export const FileInputControl: FC<MirrorConfigControlProps> = ({ control, isReadonly }) => {
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

export const CredentialsControl: FC<MirrorConfigControlProps> = ({ control, isReadonly }) => {
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

export const SynchronizationPeriodControl: FC<MirrorConfigControlProps> = ({ control, isReadonly }) => {
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

export const UrlControl: FC<MirrorConfigControlProps> = ({ control, isReadonly }) => {
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

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

export const PublicKeysControl: FC<GlobalMirrorConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field } = useController({ control, name: "allowedGpgKeys", defaultValue: 0 });
  const { register, handleSubmit, reset } = useForm<PublicKey>();

  const deleteKey = (keyId: string) => field.onChange(field.value?.filter(key => key.id === keyId) || []);
  const addNewKey = (key: PublicKey) => {
    field.onChange([...(field.value || []), key]);
    reset();
  };

  return (
    <>
      <table className="card-table table is-hoverable is-fullwidth">
        <tr>
          <th>{t("scm-repository-mirror-plugin.form.keyList.displayName")}</th>
          <th className="is-hidden-mobile">{t("scm-repository-mirror-plugin.form.keyList.raw")}</th>
          <th />
        </tr>
        {field.value?.map(({ id, raw, displayName }, index) => (
          <tr key={index}>
            <td>{displayName}</td>
            <td>{raw}</td>
            <td>
              <span className="icon" onClick={() => deleteKey(id)}>
                <Icon name="trash" title={t("scm-repository-mirror-plugin.form.keyList.delete")} color="inherit" />
              </span>
            </td>
          </tr>
        ))}
      </table>
      {isReadonly ? null : (
        <form onSubmit={handleSubmit(addNewKey)}>
          <Columns>
            <Column>
              <InputField
                label={t("scm-repository-mirror-plugin.form.keyList.new.displayName.label")}
                helpText={t("scm-repository-mirror-plugin.form.keyList.new.displayName.helpText")}
                {...register("displayName")}
              />
              <Textarea
                label={t("scm-repository-mirror-plugin.form.keyList.new.raw.label")}
                helpText={t("scm-repository-mirror-plugin.form.keyList.new.raw.helpText")}
                {...register("raw")}
              />
            </Column>
          </Columns>
          <Level right={<SubmitButton label={t("scm-repository-mirror-plugin.form.keyList.new.submit")} />} />
        </form>
      )}
    </>
  );
};

export const createGpgVerificationTypeOptions: (t: (key: string) => string) => SelectItem[] = t => [
  {
    label: t("scm-repository-mirror-plugin.form.gpgVerificationType.options.none"),
    value: String(MirrorGpgVerificationType.NONE)
  },
  {
    label: t("scm-repository-mirror-plugin.form.gpgVerificationType.options.signature"),
    value: String(MirrorGpgVerificationType.SIGNATURE)
  },
  {
    label: t("scm-repository-mirror-plugin.form.gpgVerificationType.options.scmUserSignature"),
    value: String(MirrorGpgVerificationType.SCM_USER_SIGNATURE)
  },
  {
    label: t("scm-repository-mirror-plugin.form.gpgVerificationType.options.keyList"),
    value: String(MirrorGpgVerificationType.KEY_LIST)
  }
];

export const GpgVerificationControl: FC<GlobalMirrorConfigControlProps> = ({ control, isReadonly }) => {
  const [t] = useTranslation("plugins");
  const { field: verificationTypeField } = useController({ control, name: "gpgVerificationType", defaultValue: 0 });
  const showKeyList = verificationTypeField.value === MirrorGpgVerificationType.KEY_LIST;
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
