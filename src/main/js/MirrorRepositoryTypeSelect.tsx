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
