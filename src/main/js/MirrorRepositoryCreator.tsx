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
          repositoryType={repositoryType?.name}
          disabled={loading}
          NameForm={NameForm}
          InformationForm={InformationForm}
        />
      ) : null}
    </>
  );
};

export default MirrorRepositoryCreator;
