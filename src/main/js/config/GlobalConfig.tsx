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

import React, { FC, useEffect } from "react";
import { Checkbox, ConfigurationForm, InputField } from "@scm-manager/ui-components";
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
  const { initialConfiguration, update, isReadonly, ...formProps } = useConfigLink<GlobalConfigurationDto>(link);
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
      isReadonly={isReadonly}
      onSubmit={handleSubmit(update)}
      {...formProps}
    >
      <Checkbox
        label={t("scm-repository-mirror-plugin.form.httpsOnly.label")}
        helpText={t("scm-repository-mirror-plugin.form.httpsOnly.helpText")}
        disabled={isReadonly}
        {...register("httpsOnly")}
      />
      <InputField
        label={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.label")}
        helpText={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.helpText")}
        disabled={isReadonly}
        {...register("branchesAndTagsPatterns")}
      />
      <GpgVerificationControl control={control} isReadonly={isReadonly} />
    </ConfigurationForm>
  );
};

export default GlobalConfig;