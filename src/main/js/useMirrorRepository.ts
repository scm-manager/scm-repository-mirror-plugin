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

import { useMutation, useQueryClient } from "react-query";
import { Repository } from "@scm-manager/ui-types";
import { MirrorCreationDto } from "./types";
import { apiClient } from "@scm-manager/ui-components";

type UseCreateRepositoryRequest = {
  link: string;
  payload: MirrorCreationDto;
};

const mirrorRepository = ({ link, payload }: UseCreateRepositoryRequest) => {
  return apiClient
    .post(link, payload, "application/json")
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
    mirror: (link: string, payload: MirrorCreationDto) => {
      mutate({ link, payload });
    },
    isLoading,
    error,
    repository: data
  };
};

export type UnmirrorOptions = {
  onSuccess: () => void;
};

export const useUnmirrorRepository = (repository: Repository, options?: UnmirrorOptions) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<unknown, Error, string>(
    link => {
      return apiClient.post(link);
    },
    {
      onSuccess: () => {
        if (options?.onSuccess) {
          options.onSuccess();
        }
        queryClient.invalidateQueries(["repository", repository.namespace, repository.name]);
        return queryClient.invalidateQueries(["repositories"]);
      }
    }
  );
  return {
    unmirror: (link: string) => mutate(link),
    isLoading,
    error
  };
};
