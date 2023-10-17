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

import { hri } from "human-readable-ids";

describe("Create Mirror", () => {
  let name, namespace, username, password;

  beforeEach(() => {
    // Create repo
    namespace = hri.random();
    name = hri.random();
    cy.restCreateRepo("git", namespace, name, true);

    // Create user and login
    username = hri.random();
    password = hri.random();
    cy.restCreateUser(username, password);
    cy.restLogin(username, password);
    cy.restSetUserPermissions(username, ["repository:create", "repository:read,pull:*"]);

    // Create mirror
    const baseUrl = Cypress.config().baseUrl;
    const repoUrl = `/repo/${namespace}/${name}`;
    cy.visit("/repos/create/mirror");
    cy.byTestId("url-input")
      .type(baseUrl + repoUrl)
      .blur();
    cy.contains("Namespace")
      .closest(".field")
      .find("input")
      .type("mirror");
    cy.get("ul[role=listbox]").find("li").click();
    cy.byTestId("base-auth-checkbox").click();
    cy.byTestId("username-input").type(username);
    cy.byTestId("username-password-input").type(password);
    cy.byTestId("submit-button").click();
  });

  it("should create a repository mirror as an authenticated and authorized user", () => {
    // Assert
    // -- The user is redirected to the repository's page
    cy.url().should("include", `mirror/${name}`);
    // -- There is a mirror badge
    cy.get("span.tag")
      .contains("mirror")
      .should("exist")
      .and("be.visible");
    // -- There is a success notification
    cy.get("#toastRoot")
      .should("exist")
      .and("contain", "mirroring succeeded", {});
  });
  it("should assure repository mirror is write protected", () => {
    // Act
    const mirrorBranchCreateUrl = `/repo/mirror/${name}/branches/create`;
    cy.visit(mirrorBranchCreateUrl);
    cy.get("input[name='name']").type("new_branch");
    cy.byTestId("submit-button").click();

    // Assert
    cy.get("div.notification")
      .should("exist")
      .and("contain", "Error:");
  });
  it("should delete repository mirror", () => {
    // Act
    const mirrorRestUrl = `api/v2/repositories/mirror/${name}`;
    cy.request({
      method: "DELETE",
      url: mirrorRestUrl,
      auth: {
        user: username,
        pass: password,
        sendImmediately: true
      }
    });
    // Assert
    cy.visit(`repo/mirror/${name}`);
    cy.get("div.notification")
      .should("exist")
      .and("contain", "Not found", {});
  });
});
