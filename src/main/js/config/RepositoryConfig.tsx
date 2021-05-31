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
import { ConfigurationForm } from "@scm-manager/ui-components";
import { useConfigLink } from "@scm-manager/ui-api";
import { MirrorConfigurationDto } from "../types";
import { useForm } from "react-hook-form";
import styled from "styled-components";
import {
  coalesceFormValue,
  CredentialsControl,
  ManagingUsersControl,
  SynchronizationPeriodControl,
  UrlControl
} from "./FormControls";

const Columns = styled.div`
  padding: 0.75rem 0 0;
`;

type Props = {
  link: string;
};

const RepositoryConfig: FC<Props> = ({ link }) => {
  const { initialConfiguration, update, isReadonly, ...formProps } = useConfigLink<MirrorConfigurationDto>(link);
  const { formState, handleSubmit, control, reset } = useForm<MirrorConfigurationDto>({
    mode: "onChange"
  });

  useEffect(() => {
    if (initialConfiguration) {
      reset(initialConfiguration);
    }
  }, [initialConfiguration]);

  const onSubmit = handleSubmit(formValue => update(coalesceFormValue(formValue)));

  return (
    <ConfigurationForm isValid={formState.isValid} isReadonly={isReadonly} onSubmit={onSubmit} {...formProps}>
      <Columns className="columns is-multiline">
        <UrlControl control={control} isReadonly={true} />
        <CredentialsControl control={control} isReadonly={isReadonly} />
        <SynchronizationPeriodControl control={control} isReadonly={isReadonly} />
        <ManagingUsersControl control={control} isReadonly={isReadonly} />
      </Columns>
    </ConfigurationForm>
  );
};

export default RepositoryConfig;
