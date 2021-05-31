import { FC, useEffect } from "react";
import { Checkbox, ConfigurationForm, InputField, Select } from "@scm-manager/ui-components";
import { GlobalConfigurationDto } from "../types";
import { useForm } from "react-hook-form";
import { useConfigLink } from "@scm-manager/ui-api";
import React from "react";
import { GpgVerificationControl } from "./FormControls";

type Props = {
  link: string;
};

const GlobalConfig: FC<Props> = ({ link }) => {
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
      <Checkbox disabled={isReadonly} {...register("httpsOnly")} />
      <InputField disabled={isReadonly} {...register("branchesAndTagsPatterns")} />
      <GpgVerificationControl control={control} isReadonly={isReadonly} />
    </ConfigurationForm>
  );
};

export default GlobalConfig;
