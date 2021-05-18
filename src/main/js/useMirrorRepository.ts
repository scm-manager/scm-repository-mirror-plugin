import { useMutation, useQueryClient } from "react-query";
import { Repository } from "@scm-manager/ui-types";
import { MirrorRequestDto } from "./types";
import { apiClient } from "@scm-manager/ui-api";

type UseCreateRepositoryRequest = {
  link: string;
  payload: MirrorRequestDto;
};

const mirrorRepository = ({ link, payload }: UseCreateRepositoryRequest) => {
  return apiClient
    .post(link, payload, "application/vnd.scmm-repository+json;v=2")
    .then(response => {
      const location = response.headers.get("Location");
      if (!location) {
        throw new Error("Server does not return required Location header");
      }
      return apiClient.get(location);
    })
    .then(response => response.json());
};

export const useMirrorRepository = () => {
  const queryClient = useQueryClient();
  const { mutate, data, isLoading, error } = useMutation<Repository, Error, UseCreateRepositoryRequest>(
    mirrorRepository,
    {
      onSuccess: repository => {
        queryClient.setQueryData(["repository", repository.namespace, repository.name], repository);
        return queryClient.invalidateQueries(["repositories"]);
      }
    }
  );
  return {
    mirror: (link: string, payload: MirrorRequestDto) => {
      mutate({ link, payload });
    },
    isLoading,
    error,
    repository: data
  };
};
