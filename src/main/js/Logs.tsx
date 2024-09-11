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

import React, { FC, useState } from "react";
import { Repository } from "@scm-manager/ui-types";
import { DateFromNow, Duration, ErrorNotification, Icon, Loading, RepositoryFlag, Subtitle } from "@scm-manager/ui-components";
import useMirrorLogs from "./useMirrorLogs";
import { LogEntry } from "./types";
import { Trans, useTranslation } from "react-i18next";
import styled from "styled-components";
import classNames from "classnames";

type Props = {
  repository: Repository;
};

const SuccessTag = () => {
  const [t] = useTranslation("plugins");
  return (
    <RepositoryFlag variant="success" title={t("scm-repository-mirror-plugin.logs.success.title")}>
      {t("scm-repository-mirror-plugin.logs.success.label")}
    </RepositoryFlag>
  );
};

const FailedTag = () => {
  const [t] = useTranslation("plugins");
  return (
    <RepositoryFlag variant="danger" title={t("scm-repository-mirror-plugin.logs.failed.title")}>
      {t("scm-repository-mirror-plugin.logs.failed.label")}
    </RepositoryFlag>
  );
};

const FailedUpdatesTag = () => {
  const [t] = useTranslation("plugins");
  return (
    <RepositoryFlag variant="warning" title={t("scm-repository-mirror-plugin.logs.failedUpdates.title")}>
      {t("scm-repository-mirror-plugin.logs.failedUpdates.label")}
    </RepositoryFlag>
  );
};

type ResultTagProps = {
  entry: LogEntry;
};

const ResultTag: FC<ResultTagProps> = ({ entry }) => {
  switch (entry.result) {
    case "SUCCESS":
      return <SuccessTag />;
    case "FAILED":
      return <FailedTag />;
    case "FAILED_UPDATES":
      return <FailedUpdatesTag />;
    default:
      throw new Error("unknown result type " + entry.result);
  }
};

type LogRowProps = {
  entry: LogEntry;
  initialOpenState: boolean;
};

type ColumnProps = {
  minWidth: number;
};

const TagColumn = styled.span<ColumnProps>`
  display: inline-block;
  min-width: ${props => props.minWidth}rem;
`;

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
  <ul className="m-3 p-3 has-background-secondary-less is-family-monospace is-size-7 has-text-secondary-most">
    {lines.map(line => (
      <li key={line}>{line}</li>
    ))}
  </ul>
);

const calcDuration = (entry: LogEntry) => {
  return new Date(entry.ended).getTime() - new Date(entry.started).getTime();
};

const LogRow: FC<LogRowProps> = ({ entry, initialOpenState }) => {
  const [open, setOpen] = useState(initialOpenState);
  const dateFromNow = <DateFromNow date={entry.ended} />;
  const duration = <Duration duration={calcDuration(entry)} />;
  return (
    <article>
      <div
        className={classNames(
          {
            "has-cursor-pointer": !!entry.log
          },
          "is-flex is-align-items-center"
        )}
        onClick={() => setOpen(!open)}
      >
        <Column minWidth={2}>
          <Icon name={open ? "angle-down" : "angle-right"} />
        </Column>
        <TagColumn minWidth={7}>
          <ResultTag entry={entry} />
        </TagColumn>
        <Column minWidth={15}>
          <Trans i18nKey="plugins:scm-repository-mirror-plugin.logs.finishedAt" components={[dateFromNow]} />
        </Column>
        <Column minWidth={15}>
          <Trans i18nKey="plugins:scm-repository-mirror-plugin.logs.duration" components={[duration]} />
        </Column>
      </div>
      {open && entry.log ? <LogLines lines={entry.log} /> : null}
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
