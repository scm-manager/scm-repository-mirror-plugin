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

import React, { FC, useEffect } from "react";
import { Checkbox, ConfigurationForm, InputField, Notification, Title } from "@scm-manager/ui-components";
import { GlobalConfigurationDto } from "../types";
import { useForm } from "react-hook-form";
import { useConfigLink } from "@scm-manager/ui-api";
import { GpgVerificationControl } from "./FormControls";
import { useTranslation } from "react-i18next";

type Props = {
  link: string;
};

const GlobalConfig: FC<Props> = ({ link }) => {
  const [t] = useTranslation("plugins");
  const { initialConfiguration, update, isReadOnly, ...formProps } = useConfigLink<GlobalConfigurationDto>(link);
  const { formState, handleSubmit, register, reset, control } = useForm<GlobalConfigurationDto>({
    mode: "onChange"
  });

  useEffect(() => {
    if (initialConfiguration) {
      reset(initialConfiguration);
    }
  }, [initialConfiguration]);

  return (
    <ConfigurationForm
      isValid={formState.isValid}
      isReadOnly={isReadOnly}
      onSubmit={handleSubmit(update)}
      {...formProps}
    >
      <Title title={t("scm-repository-mirror-plugin.settings.title")} />
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.httpsOnly.label")}
        helpText={t("scm-repository-mirror-plugin.form.httpsOnly.helpText")}
        disabled={isReadOnly}
        {...register("httpsOnly")}
      />
      <hr />
      <h2 className="subtitle">{t("scm-repository-mirror-plugin.form.verificationFilters")}</h2>
      <Notification type="inherit">{t("scm-repository-mirror-plugin.form.verificationFiltersHint")}</Notification>
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.disableRepositoryFilterOverwrite.label")}
        helpText={t("scm-repository-mirror-plugin.form.disableRepositoryFilterOverwrite.helpText")}
        disabled={isReadOnly}
        {...register("disableRepositoryFilterOverwrite")}
      />
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.fastForwardOnly.label")}
        helpText={t("scm-repository-mirror-plugin.form.fastForwardOnly.helpText")}
        disabled={isReadOnly}
        {...register("fastForwardOnly", { shouldUnregister: true })}
      />
      <InputField
        label={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.label")}
        helpText={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.helpText")}
        disabled={isReadOnly}
        {...register("branchesAndTagsPatterns")}
      />
      <GpgVerificationControl control={control} isReadonly={isReadOnly} />
    </ConfigurationForm>
  );
};

export default GlobalConfig;
