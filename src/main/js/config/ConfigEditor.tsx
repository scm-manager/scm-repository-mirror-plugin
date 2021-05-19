import React, { FC, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { MirrorConfigurationDto } from "../types";
import { InputField, Textarea } from "@scm-manager/ui-components";
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

  useEffect(() => onConfigurationChange(formValue, formState.isValid), [formValue, formState.isValid]);

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
      {credentialsForm}
    </Columns>
  );
};

export default ConfigEditor;
