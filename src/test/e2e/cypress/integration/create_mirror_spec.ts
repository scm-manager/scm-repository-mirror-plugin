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
