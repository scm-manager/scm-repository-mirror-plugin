import React, { FC } from "react";
import { Select } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { RepositoryType, RepositoryTypeCollection } from "@scm-manager/ui-types";

type Props = {
  value?: string;
  repositoryTypes: RepositoryTypeCollection;
  onChange: (repositoryType?: RepositoryType) => void;
  disabled?: boolean;
};

const MirrorRepositoryTypeSelect: FC<Props> = ({ value, disabled, repositoryTypes, onChange }) => {
  const [t] = useTranslation("repos");

  const createSelectOptions = (repositoryTypeCollection?: RepositoryTypeCollection) => {
    if (repositoryTypeCollection) {
      return repositoryTypeCollection._embedded.repositoryTypes
        .filter(it => "mirror" in it._links)
        .map(it => {
          return {
            label: it.displayName,
            value: it.name
          };
        });
    }
    return [];
  };

  return (
    <Select
      label={t("repository.type")}
      onChange={repositoryTypeName =>
        onChange(repositoryTypes._embedded.repositoryTypes.find(it => it.name === repositoryTypeName))
      }
      value={value}
      options={createSelectOptions(repositoryTypes)}
      helpText={t("help.typeHelpText")}
      disabled={disabled}
    />
  );
};

export default MirrorRepositoryTypeSelect;
