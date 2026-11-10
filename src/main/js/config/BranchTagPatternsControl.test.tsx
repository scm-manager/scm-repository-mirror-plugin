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
import { useForm } from "react-hook-form";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import BranchTagPatternsControl from "./BranchTagPatternsControl";

vi.mock("@scm-manager/ui-core", async () => ({
  ChipInputField: (await import("@scm-manager/ui-core/src/base/forms/chip-input/ChipInputField")).default,
}));
vi.mock("@scm-manager/ui-core/src/base/helpers", async () => ({
  useAriaId: (id?: string) => id ?? React.useId(),
  createAttributesForTesting: (testId?: string) => ({ "data-testid": testId }),
}));
vi.mock("react-i18next", () => ({ useTranslation: () => [(key: string) => key] }));

type TestRoot = { render: (element: React.ReactNode) => void; unmount: () => void };

type Values = { branchesAndTagsPatterns: string };
const submit = vi.fn();
function Form({ disabled = false }: { disabled?: boolean }) {
  const { control, handleSubmit, reset, register, watch } = useForm<Values & { override: boolean }>({
    defaultValues: { branchesAndTagsPatterns: " main, , release/* ", override: true },
  });
  return (
    <form onSubmit={handleSubmit(submit)}>
      <input type="checkbox" {...register("override")} />
      {watch("override") ? (
        <BranchTagPatternsControl
          control={control}
          name="branchesAndTagsPatterns"
          disabled={disabled}
          shouldUnregister
        />
      ) : null}
      <button type="button" onClick={() => reset({ branchesAndTagsPatterns: "v*", override: true })}>
        Reset
      </button>
      <button type="submit">Save</button>
    </form>
  );
}

describe("BranchTagPatternsControl", () => {
  let container: HTMLDivElement;
  let root: TestRoot;
  beforeEach(async () => {
    const { createRoot } = await vi.importActual<{ createRoot: (container: HTMLElement) => TestRoot }>(
      "react-dom/client",
    );
    Object.assign(globalThis, { IS_REACT_ACT_ENVIRONMENT: true });
    submit.mockClear();
    container = document.createElement("div");
    document.body.appendChild(container);
    root = createRoot(container);
  });
  afterEach(async () => {
    await act(async () => root.unmount());
    container.remove();
  });
  const render = async (disabled = false) => act(async () => root.render(<Form disabled={disabled} />));
  const input = () => container.querySelector<HTMLInputElement>('input:not([type="checkbox"])')!;
  const chips = () => Array.from(container.querySelectorAll("li span")).map((node) => node.textContent);
  const save = async () =>
    act(async () =>
      container.querySelector("form")!.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true })),
    );
  const enter = async (value: string) => {
    await act(async () => {
      input().value = value;
      input().dispatchEvent(new KeyboardEvent("keydown", { key: "Enter", bubbles: true, cancelable: true }));
    });
  };

  it("loads patterns, trims additions, removes chips, and saves the comma-separated value", async () => {
    await render();
    expect(chips()).toEqual(["main", "release/*"]);
    await enter(" feature/* ");
    await enter("   ");
    await enter("main");
    expect(chips()).toEqual(["main", "release/*", "feature/*"]);
    expect(submit).not.toHaveBeenCalled();
    await act(async () => container.querySelector<HTMLButtonElement>("li button")!.click());
    await save();
    expect(submit.mock.calls[0][0].branchesAndTagsPatterns).toBe("release/*,feature/*");
  });

  it("omits unconfirmed text on save and updates chips on reset", async () => {
    await render();
    input().value = "pending";
    await save();
    expect(submit.mock.calls[0][0].branchesAndTagsPatterns).toBe(" main, , release/* ");
    await act(async () => container.querySelector<HTMLButtonElement>('button[type="button"]:not(li button)')!.click());
    expect(chips()).toEqual(["v*"]);
  });

  it("prevents editing when disabled", async () => {
    await render(true);
    expect(input().disabled).toBe(true);
    expect(container.querySelector("li button")).toBeNull();
    await enter("feature/*");
    expect(chips()).toEqual(["main", "release/*"]);
  });

  it("unregisters patterns when overrides are hidden", async () => {
    await render();
    await act(async () => container.querySelector<HTMLInputElement>('input[type="checkbox"]')!.click());
    await save();
    expect(submit.mock.calls[0][0].branchesAndTagsPatterns).toBeUndefined();
    await act(async () => container.querySelector<HTMLInputElement>('input[type="checkbox"]')!.click());
    expect(chips()).toEqual([]);
  });
});
