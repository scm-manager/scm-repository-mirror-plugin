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

import React from "react";
import { ChipInputField } from "@scm-manager/ui-core";
import { FieldValues, useController, UseControllerProps } from "react-hook-form";
import { useTranslation } from "react-i18next";

type Props<T extends FieldValues> = Pick<UseControllerProps<T>, "control" | "name" | "shouldUnregister"> & {
  disabled?: boolean;
};

export default function BranchTagPatternsControl<T extends FieldValues>({ disabled, ...props }: Props<T>) {
  const { field, fieldState } = useController(props);
  const [t] = useTranslation("plugins");
  const value: string = typeof field.value === "string" ? field.value : "";
  const patterns = value
    .split(",")
    .map((pattern) => pattern.trim())
    .filter(Boolean);

  return (
    <ChipInputField<string>
      {...field}
      label={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.label")}
      helpText={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.helpText")}
      information={t("scm-repository-mirror-plugin.form.branchesAndTagsPatterns.information")}
      disabled={disabled}
      error={fieldState.error?.message}
      value={patterns.map((value) => ({ label: value, value }))}
      onChange={(options) =>
        field.onChange(
          options
            .map(({ value }) => value.trim())
            .filter(Boolean)
            .join(","),
        )
      }
    />
  );
}
