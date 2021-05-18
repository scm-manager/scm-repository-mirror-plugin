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
