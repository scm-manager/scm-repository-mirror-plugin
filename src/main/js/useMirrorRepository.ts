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

import { useMutation, useQueryClient } from "react-query";
import { Repository } from "@scm-manager/ui-types";
import { MirrorRequestDto } from "./types";
import { apiClient } from "@scm-manager/ui-components";

type UseCreateRepositoryRequest = {
  link: string;
  payload: MirrorRequestDto;
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
    mirror: (link: string, payload: MirrorRequestDto) => {
      mutate({ link, payload });
    },
    isLoading,
    error,
    repository: data
  };
};
