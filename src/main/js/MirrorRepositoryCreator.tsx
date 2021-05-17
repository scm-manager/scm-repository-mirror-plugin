import { extensionPoints } from "@scm-manager/ui-extensions";
import React, { useState } from "react";
import { RepositoryCreation, RepositoryType, RepositoryTypeCollection } from "@scm-manager/ui-types";
import { apiClient, ErrorNotification, Level, SubmitButton, Select, InputField } from "@scm-manager/ui-components";
import { useForm } from "react-hook-form";
import { MirrorConfigurationDto } from "./types";
import { useTranslation } from "react-i18next";
import styled from "styled-components";

const SelectWrapper = styled.div`
  flex: 1;
`;

const Column = styled.div`
  padding: 0 0.75rem;
`;

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

const MirrorRepositoryCreator: extensionPoints.RepositoryCreatorExtension["component"] = ({
  repositoryTypes,
  informationForm: InformationForm,
  nameForm: NameForm,
  index,
  namespaceStrategies
}) => {
  const [repositoryType, setRepositoryType] = useState<RepositoryType | undefined>();
  const { register, handleSubmit } = useForm<MirrorConfigurationDto>();
  const [repository, setRepository] = useState<RepositoryCreation>({
    type: "",
    contact: "",
    description: "",
    name: "",
    namespace: "",
    contextEntries: {}
  });
  const [valid, setValid] = useState({ namespaceAndName: false, contact: true, importUrl: false });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | undefined>();
  const [t] = useTranslation("plugins");

  const isValid = () => Object.values(valid).every(v => v);
  const onSubmit = () => {};

  const createSelectOptions = (repositoryTypes?: RepositoryTypeCollection) => {
    if (repositoryTypes) {
      return repositoryTypes._embedded.repositoryTypes
        .filter(repositoryType => true || "mirror" in repositoryType._links)
        .map(repositoryType => {
          return {
            label: repositoryType.displayName,
            value: repositoryType.name
          };
        });
    }
    return [];
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <ErrorNotification error={error} />

      <SelectWrapper>
        <Select
          label={t("repository.type")}
          onChange={type => setRepository({ ...repository, type })}
          value={repository ? repository.type : ""}
          options={createSelectOptions(repositoryTypes)}
          helpText={t("help.typeHelpText")}
          disabled={loading}
        />
      </SelectWrapper>
      {repository.type ? (
        <>
          <Columns className="columns is-multiline">
            <Column className="column is-full">
              <InputField
                label={t("import.importUrl")}
                helpText={t("help.importUrlHelpText")}
                errorMessage={t("validation.url-invalid")}
                disabled={loading}
                {...register("url")}
              />
            </Column>
            <Column className="column is-half">
              <InputField
                label={t("import.username")}
                helpText={t("help.usernameHelpText")}
                disabled={loading}
                {...register("usernamePasswordCredential.username")}
              />
            </Column>
            <Column className="column is-half">
              <InputField
                label={t("import.password")}
                type="password"
                helpText={t("help.passwordHelpText")}
                disabled={loading}
                {...register("usernamePasswordCredential.password")}
              />
            </Column>
          </Columns>
          <NameForm
            repository={repository}
            onChange={setRepository as React.Dispatch<React.SetStateAction<RepositoryCreation>>}
            setValid={(namespaceAndName: boolean) => setValid({ ...valid, namespaceAndName })}
            disabled={loading}
          />
          <InformationForm
            repository={repository}
            onChange={setRepository as React.Dispatch<React.SetStateAction<RepositoryCreation>>}
            disabled={loading}
            setValid={(contact: boolean) => setValid({ ...valid, contact })}
          />
          <Level
            right={<SubmitButton disabled={!isValid()} loading={loading} label={t("repositoryForm.submitImport")} />}
          />
        </>
      ) : null}
    </form>
  );
};

export default MirrorRepositoryCreator;
