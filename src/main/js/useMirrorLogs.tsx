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
import { useQuery } from "react-query";
import { LogCollection } from "./types";

const createCacheKey = (repository: Repository) => {
  return ["repository", repository.namespace, repository.name, "mirror-logs"];
};

const fetchMirrorLogs = (link: string) => {
  return apiClient.get(link).then(res => res.json());
};

const useMirrorLogs = (repository: Repository) => {
  const link = (repository._links["mirrorLogs"] as Link)?.href;
  if (!link) {
    throw new Error("repository has not mirror logs");
  }
  const { error, isLoading, data } = useQuery<LogCollection, Error>(createCacheKey(repository), () =>
    fetchMirrorLogs(link)
  );

  return {
    error,
    isLoading,
    data
  };
};

export default useMirrorLogs;
