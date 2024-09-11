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

import { binder, extensionPoints } from "@scm-manager/ui-extensions";
import { ConfigurationBinder as configurationBinder } from "@scm-manager/ui-components";
import MirrorRepositoryCreator from "./MirrorRepositoryCreator";
import RepositoryConfig from "./config/RepositoryConfig";
import MirrorRepositoryFlag from "./MirrorRepositoryFlag";
import GlobalConfig from "./config/GlobalConfig";
import { Repository } from "@scm-manager/ui-types";
import LogRoute from "./LogRoute";
import LogNavLink from "./LogNavLink";

binder.bind<extensionPoints.RepositoryCreator>("repos.creator", {
  subtitle: "scm-repository-mirror-plugin.create.subtitle",
  path: "mirror",
  icon: "copy",
  label: "scm-repository-mirror-plugin.repositoryForm.createButton",
  component: MirrorRepositoryCreator
});

configurationBinder.bindRepositorySetting(
  "/mirror",
  "scm-repository-mirror-plugin.settings.navLink",
  "mirrorAccessConfiguration",
  RepositoryConfig
);

configurationBinder.bindGlobal(
  "/mirror",
  "scm-repository-mirror-plugin.settings.navLink",
  "mirrorConfiguration",
  GlobalConfig
);

binder.bind<extensionPoints.RepositoryFlags>("repository.flags", MirrorRepositoryFlag);

type PredicateProps = {
  repository: Repository;
};

const logPredicate = ({ repository }: PredicateProps) => {
  return !!repository._links["mirrorLogs"];
};

binder.bind("repository.route", LogRoute, logPredicate);
binder.bind("repository.navigation", LogNavLink, logPredicate);
