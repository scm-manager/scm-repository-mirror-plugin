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

import React, { act } from "react";
import { createRoot, Root } from "react-dom/client";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useController } from "react-hook-form";
import { MirrorAccessConfigurationDto } from "../types";
import RepositoryConfig from "./RepositoryConfig";

const state = vi.hoisted(() => ({
  configuration: {} as MirrorAccessConfigurationDto,
  isReadOnly: false,
  update: vi.fn(),
}));

vi.mock("@scm-manager/ui-api", () => ({
  useConfigLink: () => ({
    initialConfiguration: state.configuration,
    isReadOnly: state.isReadOnly,
    update: state.update,
  }),
}));
vi.mock("@scm-manager/ui-core", () => ({
  Subtitle: () => null,
  useRepositoryFromParams: () => ({
    _links: { mirrorAccessConfiguration: { href: "/configuration" }, unmirror: { href: "/unmirror" } },
  }),
}));
vi.mock("@scm-manager/ui-components", () => ({
  ConfigurationForm: ({ children, onSubmit }: React.PropsWithChildren<{ onSubmit: React.FormEventHandler }>) => (
    <form onSubmit={onSubmit}>
      {children}
      <button type="submit">Save</button>
    </form>
  ),
  Checkbox: () => null,
  InputField: () => null,
}));
vi.mock("react-i18next", () => ({ useTranslation: () => [(key: string) => key] }));
vi.mock("./SyncButton", () => ({ SyncButton: () => null }));
vi.mock("./MirrorDangerZone", () => ({ MirrorDangerZone: () => null }));
vi.mock("./FormControls", () => ({
  coalesceFormValue: (value: unknown) => value,
  UrlControl: ({ control, isReadonly }: Parameters<typeof import("./FormControls").UrlControl>[0]) => {
    const { field } = useController({ control, name: "url" });
    return (
      <input
        aria-label="URL"
        disabled={isReadonly}
        value={field.value || ""}
        onInput={(event) => field.onChange(event.currentTarget.value)}
      />
    );
  },
  CredentialsControl: () => null,
  SynchronizationPeriodControl: () => null,
  ManagingUsersControl: () => null,
  ProxyHostControl: () => null,
  ProxyPasswordControl: () => null,
  ProxyPortControl: () => null,
  ProxyUsernameControl: () => null,
  GpgVerificationControl: () => null,
}));

describe("mirror source URL configuration", () => {
  let container: HTMLDivElement;
  let root: Root;

  beforeEach(() => {
    state.configuration = {
      url: "https://old.example/repository",
      urlModifiable: true,
      managingUsers: [],
      _links: { update: { href: "/configuration" } },
      proxyConfiguration: { overwriteGlobalConfiguration: false, host: "", port: 0 },
    } as MirrorAccessConfigurationDto;
    vi.stubGlobal("IS_REACT_ACT_ENVIRONMENT", true);
    state.isReadOnly = false;
    state.update.mockClear();
    container = document.createElement("div");
    document.body.append(container);
    root = createRoot(container);
  });

  afterEach(async () => {
    await act(async () => root.unmount());
    container.remove();
    vi.unstubAllGlobals();
  });

  it.each([
    { capability: true, readOnly: false, disabled: false },
    { capability: false, readOnly: false, disabled: true },
    { capability: undefined, readOnly: false, disabled: true },
    { capability: true, readOnly: true, disabled: true },
  ])(
    "capability $capability and read-only permission $readOnly produce disabled=$disabled",
    async ({ capability, readOnly, disabled }) => {
      state.configuration = { ...state.configuration, urlModifiable: capability };
      state.isReadOnly = readOnly;
      await act(async () => root.render(<RepositoryConfig />));
      expect(container.querySelector("input")?.disabled).toBe(disabled);
    },
  );

  it("submits the edited source URL", async () => {
    await act(async () => root.render(<RepositoryConfig />));
    const input = container.querySelector("input")!;
    await act(async () => {
      input.value = "https://new.example/repository";
      input.dispatchEvent(new Event("input", { bubbles: true }));
    });
    await act(async () => {
      container.querySelector("form")!.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
    });
    expect(state.update).toHaveBeenCalledWith(expect.objectContaining({ url: "https://new.example/repository" }));
  });

  it("preserves the URL when saving a configuration with a fixed source", async () => {
    state.configuration = { ...state.configuration, urlModifiable: false };
    await act(async () => root.render(<RepositoryConfig />));
    await act(async () => {
      container.querySelector("form")!.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
    });
    expect(state.update).toHaveBeenCalledWith(expect.objectContaining({ url: "https://old.example/repository" }));
  });
});
