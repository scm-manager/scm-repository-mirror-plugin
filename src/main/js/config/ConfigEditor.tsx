import React, { FC, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { MirrorConfigurationDto } from "../types";
import { InputField, Select, SelectItem, Textarea } from "@scm-manager/ui-components";
import styled from "styled-components";
import { Repository } from "@scm-manager/ui-types";

const Column = styled.div`
  padding: 0 0.75rem;
  margin-bottom: 0.5rem;
`;

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

type Props = {
  onConfigurationChange: (config: MirrorConfigurationDto, valid: boolean) => void;
  initialConfiguration: MirrorConfigurationDto;
  readOnly: boolean;
  repository: Repository;
};

const ConfigEditor: FC<Props> = ({ initialConfiguration, onConfigurationChange, readOnly: disabled, repository }) => {
  const [t] = useTranslation("plugins");
  const { register, formState, watch } = useForm<MirrorConfigurationDto>({
    mode: "onChange",
    defaultValues: initialConfiguration
  });
  const formValue = watch();

  useEffect(() => {
    const output = { ...formValue };

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

  let credentialsForm = null;
  switch (repository.type) {
    case "git":
      credentialsForm = (
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
      );
      break;
    case "svn":
      credentialsForm = (
        <>
          <Column className="column is-full">
            <Textarea
              label={t("scm-repository-mirror-plugin.form.certificate.label")}
              helpText={t("scm-repository-mirror-plugin.form.certificate.helpText")}
              disabled={disabled}
              {...register("certificationCredential.certificate")}
            />
          </Column>
          <Column className="column is-full">
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
      break;
  }

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
      <Column className="column is-full">
        <Select
          label={t("scm-repository-mirror-plugin.form.period.label")}
          helpText={t("scm-repository-mirror-plugin.form.period.helpText")}
          options={periodOptions}
          disabled={disabled}
          {...register("synchronizationPeriod")}
        />
      </Column>
      {credentialsForm}
    </Columns>
  );
};

export default ConfigEditor;
