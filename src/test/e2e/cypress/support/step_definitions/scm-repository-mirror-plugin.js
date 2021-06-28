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

// This file is a template, please update the contents with step definitions for your plugin
// It is mandatory that you use this specific file, because otherwise the integration-test-runner
// might cause overlaps when copying all the different plugin tests together.
// If there are no steps specific to this plugin, you can leave this file blank.

Given("User has permission to read repository and create repositories", function() {
  cy.restSetUserPermissions(this.user.username, ["repository:create", "repository:read,pull:*"]);
});

When("User mirrors a repository", function() {
  const baseUrl = Cypress.config().baseUrl;
  const repoUrl = `/repo/${this.repository.namespace}/${this.repository.name}`;
  cy.visit("/repos/create/mirror");
  cy.byTestId("url-input")
    .type(baseUrl + repoUrl)
    .blur();
  cy.contains("Namespace")
    .closest("div.field")
    .find("input")
    .type("mirror")
    .blur();
  cy.byTestId("base-auth-checkbox").click();
  cy.byTestId("username-input").type(this.user.username);
  cy.byTestId("username-password-input").type(this.user.password);
  cy.byTestId("submit-button").click();
});

Then("The user is redirected to the repository's page", function() {
  cy.url({ timeout: 5000 }).should("include", `mirror/${this.repository.name}`);
});

Then("There is a mirror badge", function() {
  cy.get("span.tag")
    .contains("mirror")
    .should("exist")
    .and("be.visible");
});

When("Creates a new branch in mirror", function() {
  const mirrorBranchCreateUrl = `/repo/mirror/${this.repository.name}/branches/create`;
  cy.visit(mirrorBranchCreateUrl);
  cy.get("input[name='name']", { timeout: 500000 })
    .type("new_branch");
  cy.byTestId("submit-button")
    .click();
});

Then("There is an permission error message", function() {
  cy.get("div.notification")
    .should("exist")
    .and("contain", "Error:");
})
