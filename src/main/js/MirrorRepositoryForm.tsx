import { InputField, Level, SubmitButton, Textarea } from "@scm-manager/ui-components";
import React, { FC, useState } from "react";
import { MirrorConfigurationDto, MirrorRequestDto } from "./types";
import { useForm } from "react-hook-form";
import { RepositoryCreation } from "@scm-manager/ui-types";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import { extensionPoints } from "@scm-manager/ui-extensions";

const Column = styled.div`
  padding: 0 0.75rem;
`;

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

type Props = {
  onSubmit: (output: MirrorRequestDto) => void;
  disabled?: boolean;
  InformationForm: extensionPoints.RepositoryCreatorComponentProps["informationForm"];
  NameForm: extensionPoints.RepositoryCreatorComponentProps["nameForm"];
  repositoryType: string;
};

const MirrorRepositoryForm: FC<Props> = ({ repositoryType, onSubmit, disabled, NameForm, InformationForm }) => {
  const [t] = useTranslation("plugins");
  const { register, handleSubmit, formState } = useForm<MirrorConfigurationDto>();
  const [repository, setRepository] = useState<RepositoryCreation>({
    type: repositoryType,
    contact: "",
    description: "",
    name: "",
    namespace: "",
    contextEntries: {}
  });
  const [valid, setValid] = useState({ namespaceAndName: false, contact: true });

  const isValid = () => Object.values(valid).every(v => v) && formState.isValid;
  const innerOnSubmit = (configFormValue: MirrorConfigurationDto) => {
    const request: MirrorRequestDto = {
      ...configFormValue,
      ...repository
    };
    onSubmit(request);
  };

  let credentialsForm = null;
  switch (repository.type) {
    case "git":
      credentialsForm = (
        <>
          <Column className="column is-half">
            <InputField
              label={t("import.username")}
              helpText={t("help.usernameHelpText")}
              disabled={disabled}
              {...register("usernamePasswordCredential.username")}
            />
          </Column>
          <Column className="column is-half">
            <InputField
              label={t("import.password")}
              type="password"
              helpText={t("help.passwordHelpText")}
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
          <Column className="column is-half">
            <Textarea
              label={t("import.username")}
              helpText={t("help.usernameHelpText")}
              disabled={disabled}
              {...register("certificationCredential.certificate")}
            />
          </Column>
          <Column className="column is-half">
            <InputField
              label={t("import.password")}
              type="password"
              helpText={t("help.passwordHelpText")}
              disabled={disabled}
              {...register("certificationCredential.password")}
            />
          </Column>
        </>
      );
      break;
  }

  return (
    <form onSubmit={handleSubmit(innerOnSubmit)}>
      <Columns className="columns is-multiline">
        <Column className="column is-full">
          <InputField
            label={t("import.importUrl")}
            helpText={t("help.importUrlHelpText")}
            errorMessage={t("validation.url-invalid")}
            disabled={disabled}
            {...register("url")}
          />
        </Column>
        {credentialsForm}
      </Columns>
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
        right={<SubmitButton disabled={!isValid()} loading={disabled} label={t("repositoryForm.submitImport")} />}
      />
    </form>
  );
};

export default MirrorRepositoryForm;
