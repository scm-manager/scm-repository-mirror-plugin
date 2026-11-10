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
import { DefaultValues, useForm } from "react-hook-form";
import { beforeAll, describe, expect, it, vi } from "vitest";
import Checkbox from "@scm-manager/ui-components/src/forms/Checkbox";
import { CredentialsControl } from "./FormControls";
import { MirrorAccessConfigurationForm } from "../types";

let renderToStaticMarkup: (element: React.ReactNode) => string;

beforeAll(async () => {
  ({ renderToStaticMarkup } = await vi.importActual("react-dom/server"));
});

vi.mock("react-i18next", () => ({ useTranslation: () => [(key: string) => key] }));
vi.mock("@scm-manager/ui-api", () => ({}));
vi.mock("@scm-manager/ui-core", () => ({}));
vi.mock("@scm-manager/ui-components/src/Help", () => ({ default: () => null }));
vi.mock("@scm-manager/ui-components", () => ({
  Checkbox: (props: React.ComponentProps<typeof Checkbox>) => <Checkbox {...props} />,
  InputField: () => null,
  FileInput: () => null,
  createAttributesForTesting: () => ({}),
}));

const CredentialsForm = ({ defaultValues }: { defaultValues: DefaultValues<MirrorAccessConfigurationForm> }) => {
  const { control } = useForm<MirrorAccessConfigurationForm>({ defaultValues });
  return <CredentialsControl control={control} isReadonly={false} />;
};

describe("CredentialsControl", () => {
  it.each(["usernamePasswordCredential", "certificateCredential"])(
    "should check the checkbox for configured %s",
    (credential) => {
      const markup = renderToStaticMarkup(<CredentialsForm defaultValues={{ [credential]: { enabled: true } }} />);

      expect(markup).toMatch(new RegExp(`name="${credential}\\.enabled"[^>]*checked=""`));
    },
  );

  it("should leave authentication unchecked without credentials", () => {
    const markup = renderToStaticMarkup(<CredentialsForm defaultValues={{}} />);

    expect(markup).not.toContain('checked=""');
  });
});
