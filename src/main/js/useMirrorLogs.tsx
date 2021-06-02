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
