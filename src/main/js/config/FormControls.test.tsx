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

// @vitest-environment jsdom

import React, { act, useEffect } from "react";
import { createRoot } from "react-dom/client";
import { DefaultValues, useForm } from "react-hook-form";
import { beforeAll, describe, expect, it, vi } from "vitest";
import { coalesceFormValue, CredentialsControl, FileInputControl } from "./FormControls";
import { MirrorAccessConfigurationForm } from "../types";

let renderToStaticMarkup: (element: React.ReactNode) => string;

beforeAll(async () => {
  ({ renderToStaticMarkup } = await vi.importActual("react-dom/server"));
});

vi.mock("react-i18next", () => ({ useTranslation: () => [(key: string) => key] }));
vi.mock("@scm-manager/ui-api", () => ({}));
vi.mock("@scm-manager/ui-core/src/base/helpers/devbuild", () => ({
  createAttributesForTesting: (testId?: string) => ({ "data-testid": testId }),
}));
vi.mock("@scm-manager/ui-core", async () => ({
  FileInputField: (await import("@scm-manager/ui-core/src/base/forms/input/FileInputField")).default,
  RadioGroupField: ({
    label,
    options,
    helpText,
    name,
    value,
    onValueChange,
  }: {
    label: string;
    options: { value: string; label: string }[];
    helpText?: string;
    name: string;
    value?: string;
    onValueChange: (value: string) => void;
  }) => (
    <fieldset>
      <legend>{label}</legend>
      {helpText ? <span>{helpText}</span> : null}
      {options.map((option) => (
        <label key={option.value}>
          <input
            type="radio"
            name={name}
            value={option.value}
            checked={value === option.value}
            onChange={() => onValueChange(option.value)}
          />
          {option.label}
        </label>
      ))}
    </fieldset>
  ),
  InputField: ({
    label,
    testId,
    type,
    helpText,
  }: {
    label: string;
    testId?: string;
    type?: string;
    helpText?: string;
  }) => (
    <div>
      <input aria-label={label} data-testid={testId} type={type} />
      {helpText ? <span>{helpText}</span> : null}
    </div>
  ),
}));
vi.mock("@scm-manager/ui-components/src/Help", () => ({ default: () => null }));
vi.mock("@scm-manager/ui-components", () => ({
  MemberNameTagGroup: () => null,
  createAttributesForTesting: () => ({}),
}));

it("should store the selected certificate as base64", async () => {
  const onValuesChange = vi.fn();
  const CertificateForm = () => {
    const { control, subscribe } = useForm<MirrorAccessConfigurationForm>();
    useEffect(
      () => subscribe({ formState: { values: true }, callback: ({ values }) => onValuesChange(values) }),
      [subscribe],
    );
    return <FileInputControl control={control} isReadonly={false} />;
  };

  const container = document.createElement("div");
  const root = createRoot(container);
  vi.stubGlobal("IS_REACT_ACT_ENVIRONMENT", true);
  try {
    await act(() => root.render(<CertificateForm />));
    const input = container.querySelector<HTMLInputElement>('input[type="file"]')!;
    const file = new File(["certificate"], "mirror.p12");
    Object.defineProperty(input, "files", { value: [file] });

    await act(async () => {
      input.dispatchEvent(new Event("change", { bubbles: true }));
      await vi.waitFor(() =>
        expect(onValuesChange).toHaveBeenLastCalledWith({
          certificateCredential: { certificate: "Y2VydGlmaWNhdGU=" },
        }),
      );
    });
    expect(container.textContent).toContain(file.name);
  } finally {
    await act(() => root.unmount());
    vi.unstubAllGlobals();
  }
});

const CredentialsForm = ({ defaultValues }: { defaultValues: DefaultValues<MirrorAccessConfigurationForm> }) => {
  const { control } = useForm<MirrorAccessConfigurationForm>({ defaultValues });
  return <CredentialsControl control={control} isReadonly={false} />;
};

describe("CredentialsControl", () => {
  it("should show the authentication heading and mutual TLS hint", () => {
    const markup = renderToStaticMarkup(<CredentialsForm defaultValues={{}} />);

    expect(markup).toContain("scm-repository-mirror-plugin.form.sourceRepositoryAuthentication");
    expect(markup).toContain("scm-repository-mirror-plugin.form.mutualTlsAuthentication.helpText");
  });

  it("should render the username and password fields below the selected authentication option", () => {
    const markup = renderToStaticMarkup(
      <CredentialsForm
        defaultValues={{
          userAuthentication: "USERNAME_PASSWORD",
          mutualTlsAuthentication: "NONE",
          usernamePasswordCredential: { username: "trillian", password: "secret" },
        }}
      />,
    );

    expect(markup).toContain('aria-label="scm-repository-mirror-plugin.form.username.label"');
    expect(markup).toContain('aria-label="scm-repository-mirror-plugin.form.password.label"');
    expect(markup).toContain('checked="" value="USERNAME_PASSWORD"');
  });

  it("should render only the token field for token authentication", () => {
    const markup = renderToStaticMarkup(
      <CredentialsForm
        defaultValues={{
          userAuthentication: "TOKEN",
          mutualTlsAuthentication: "NONE",
          usernamePasswordCredential: { username: "", password: "_DUMMY_" },
        }}
      />,
    );

    expect(markup).toContain('data-testid="token-input"');
    expect(markup).not.toContain('data-testid="username-input"');
    expect(markup).not.toContain('data-testid="username-password-input"');
    expect(markup).toContain('checked="" value="TOKEN"');
    expect(markup.indexOf('value="TOKEN"')).toBeLessThan(markup.indexOf('data-testid="token-input"'));
    expect(markup.indexOf('data-testid="token-input"')).toBeLessThan(
      markup.indexOf("scm-repository-mirror-plugin.form.mutualTlsAuthentication.label"),
    );
  });

  it("should render certificate fields when mutual TLS is selected", () => {
    const markup = renderToStaticMarkup(
      <CredentialsForm
        defaultValues={{
          userAuthentication: "NONE",
          mutualTlsAuthentication: "CERTIFICATE",
          certificateCredential: { certificate: "_DUMMY_", password: "_DUMMY_" },
        }}
      />,
    );

    expect(markup).toContain('checked="" value="CERTIFICATE"');
    expect(markup).toContain('type="file"');
    expect(markup).not.toContain('value="_DUMMY_"');
    expect(markup).toContain('data-testid="certificate-password-input"');
  });

  it("should select no authentication by default", () => {
    const markup = renderToStaticMarkup(<CredentialsForm defaultValues={{}} />);

    expect(markup).toContain('name="userAuthentication" checked="" value="NONE"');
    expect(markup).toContain('name="mutualTlsAuthentication" checked="" value="NONE"');
  });
});

describe("coalesceFormValue", () => {
  it("should submit token authentication with an empty username", () => {
    const result = coalesceFormValue({
      userAuthentication: "TOKEN",
      mutualTlsAuthentication: "CERTIFICATE",
      usernamePasswordCredential: { username: "ignored", password: "token" },
      certificateCredential: { certificate: "certificate", password: "password" },
    } as MirrorAccessConfigurationForm);

    expect(result.usernamePasswordCredential).toEqual({ username: "", password: "token" });
    expect(result.certificateCredential).toEqual({ certificate: "certificate", password: "password" });
    expect(result).not.toHaveProperty("userAuthentication");
    expect(result).not.toHaveProperty("mutualTlsAuthentication");
  });

  it("should omit credentials when no authentication is selected", () => {
    const result = coalesceFormValue({
      userAuthentication: "NONE",
      mutualTlsAuthentication: "NONE",
      usernamePasswordCredential: { username: "trillian", password: "secret" },
      certificateCredential: { certificate: "certificate", password: "password" },
    } as MirrorAccessConfigurationForm);

    expect(result.usernamePasswordCredential).toBeUndefined();
    expect(result.certificateCredential).toBeUndefined();
  });
});
