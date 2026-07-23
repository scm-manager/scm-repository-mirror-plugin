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

import { apiClient } from "@scm-manager/ui-components";
import { Link, Repository } from "@scm-manager/ui-types";
import { useQuery, useQueryClient } from "react-query";
import { useEffect, useRef } from "react";
import { MirrorProgress } from "./types";

const repositoryCacheKey = (repository: Repository) => {
  return ["repository", repository.namespace, repository.name];
};

const createCacheKey = (repository: Repository) => {
  return [...repositoryCacheKey(repository), "mirror-progress"];
};

const fetchMirrorProgress = (link: string) => {
  return apiClient.get(link).then((res) => res.json());
};

export const getProgressRefetchInterval = (progress?: MirrorProgress) => {
  return progress?.running ? 1000 : 5000;
};

export const getMirrorProgressLink = (repository: Repository) => {
  return (repository._links["mirrorProgress"] as Link)?.href;
};

const useMirrorProgress = (repository: Repository) => {
  const link = getMirrorProgressLink(repository);
  const available = !!link;
  const queryClient = useQueryClient();
  const { error, isLoading, data } = useQuery<MirrorProgress, Error>(
    createCacheKey(repository),
    () => fetchMirrorProgress(link || ""),
    {
      enabled: available,
      refetchInterval: getProgressRefetchInterval,
    },
  );

  const running = !!data?.running;
  const wasRunning = useRef(running);
  useEffect(() => {
    if (wasRunning.current && !running) {
      queryClient.invalidateQueries(repositoryCacheKey(repository));
    }
    wasRunning.current = running;
  }, [running, queryClient, repository.namespace, repository.name]);

  return {
    available,
    error,
    isLoading,
    data,
  };
};

export default useMirrorProgress;
