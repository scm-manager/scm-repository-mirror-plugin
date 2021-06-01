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

import React, { FC, useState } from "react";
import { Repository } from "@scm-manager/ui-types";
import {
  DateFromNow,
  Duration,
  ErrorNotification,
  Icon,
  Loading,
  RepositoryFlag,
  Subtitle
} from "@scm-manager/ui-components";
import useMirrorLogs from "./useMirrorLogs";
import { LogEntry } from "./types";
import { Trans, useTranslation } from "react-i18next";
import styled from "styled-components";

type Props = {
  repository: Repository;
};

const SuccessTag = () => {
  const [t] = useTranslation("plugins");
  return (
    <RepositoryFlag color={"success"} title={t("scm-repository-mirror-plugin.logs.success.title")}>
      {t("scm-repository-mirror-plugin.logs.success.label")}
    </RepositoryFlag>
  );
};

const FailedTag = () => {
  const [t] = useTranslation("plugins");
  return (
    <RepositoryFlag color={"danger"} title={t("scm-repository-mirror-plugin.logs.failed.title")}>
      {t("scm-repository-mirror-plugin.logs.failed.label")}
    </RepositoryFlag>
  );
};

type LogRowProps = {
  entry: LogEntry;
  initialOpenState: boolean;
};

type ColumnProps = {
  minWidth: number;
};

const Column = styled.span<ColumnProps>`
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: ${props => props.minWidth}rem;
`;

type LogLinesProps = {
  lines: string[];
};

const LogLines: FC<LogLinesProps> = ({ lines }) => (
  <ul className="m-3 p-3 has-background-white-ter">
    {lines.map(line => (
      <li key={line}>{line}</li>
    ))}
  </ul>
);

const LogRow: FC<LogRowProps> = ({ entry, initialOpenState }) => {
  const [open, setOpen] = useState(initialOpenState);
  const dateFromNow = <DateFromNow date={entry.finishedAt} />;
  const duration = <Duration duration={entry.duration} />;
  return (
    <article>
      <div className="has-cursor-pointer" onClick={() => setOpen(!open)}>
        <Column minWidth={2}>
          <Icon name={open ? "angle-down" : "angle-right"} />
        </Column>
        <Column minWidth={5}>{entry.success ? <SuccessTag /> : <FailedTag />}</Column>
        <Column minWidth={15}>
          <Trans i18nKey="plugins:scm-repository-mirror-plugin.logs.finishedAt" components={[dateFromNow]} />
        </Column>
        <Column minWidth={15}>
          <Trans i18nKey="plugins:scm-repository-mirror-plugin.logs.duration" components={[duration]} />
        </Column>
      </div>
      {open ? <LogLines lines={entry.log} /> : null}
    </article>
  );
};

type LogTableProps = {
  entries: LogEntry[];
};

const LogTable: FC<LogTableProps> = ({ entries }) => {
  return (
    <div>
      {entries.map((entry, i) => (
        <React.Fragment key={i}>
          {i > 0 ? <hr /> : null}
          <LogRow initialOpenState={i === 0} entry={entry} />
        </React.Fragment>
      ))}
    </div>
  );
};

const Logs: FC<Props> = ({ repository }) => {
  const { isLoading, error, data } = useMirrorLogs(repository);
  const [t] = useTranslation("plugins");
  return (
    <>
      <Subtitle>{t(t("scm-repository-mirror-plugin.logs.subtitle"))}</Subtitle>
      <ErrorNotification error={error} />
      {isLoading || !data ? <Loading /> : <LogTable entries={data._embedded.entries} />}
    </>
  );
};

export default Logs;
