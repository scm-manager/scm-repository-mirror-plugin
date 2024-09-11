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

import { extensionPoints } from "@scm-manager/ui-extensions";
import React, { useEffect, useState } from "react";
import { Link, RepositoryType } from "@scm-manager/ui-types";
import { ErrorNotification } from "@scm-manager/ui-components";
import styled from "styled-components";
import { useHistory } from "react-router-dom";
import { useMirrorRepository } from "./useMirrorRepository";
import MirrorRepositoryForm from "./MirrorRepositoryForm";
import MirrorRepositoryTypeSelect from "./MirrorRepositoryTypeSelect";

const SelectWrapper = styled.div`
  flex: 1;
  margin-bottom: 1rem;
`;

const MirrorRepositoryCreator: extensionPoints.RepositoryCreatorExtension["component"] = ({
  repositoryTypes,
  informationForm: InformationForm,
  nameForm: NameForm
}) => {
  const { repository: createdRepository, mirror, error, isLoading: loading } = useMirrorRepository();
  const [repositoryType, setRepositoryType] = useState<RepositoryType | undefined>();
  const history = useHistory();
  const mirrorLink = (repositoryType?._links.mirror as Link)?.href;

  useEffect(() => {
    if (createdRepository) {
      history.push(`/repo/${createdRepository.namespace}/${createdRepository.name}/code/sources`);
    }
  }, [createdRepository, history]);

  const changeRepositoryType = (newRepositoryType?: RepositoryType) => {
    if (newRepositoryType) {
      setRepositoryType(newRepositoryType);
    }
  };

  return (
    <>
      <ErrorNotification error={error} />
      <SelectWrapper>
        <MirrorRepositoryTypeSelect
          repositoryTypes={repositoryTypes}
          value={repositoryType?.name}
          disabled={loading}
          onChange={changeRepositoryType}
        />
      </SelectWrapper>
      {repositoryType ? (
        <MirrorRepositoryForm
          onSubmit={requestPayload => mirror(mirrorLink, requestPayload)}
          repositoryType={repositoryType}
          disabled={loading}
          NameForm={NameForm}
          InformationForm={InformationForm}
        />
      ) : null}
    </>
  );
};

export default MirrorRepositoryCreator;
