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
        .filter(type => "mirror" in type._links)
        .map(type => {
          return {
            label: type.displayName,
            value: type.name
          };
        });
    }
    return [];
  };

  return (
    <Select
      label={t("repository.type")}
      onChange={repositoryTypeName =>
        onChange(repositoryTypes._embedded.repositoryTypes.find(type => type.name === repositoryTypeName))
      }
      value={value}
      options={createSelectOptions(repositoryTypes)}
      helpText={t("help.typeHelpText")}
      disabled={disabled}
    />
  );
};

export default MirrorRepositoryTypeSelect;
