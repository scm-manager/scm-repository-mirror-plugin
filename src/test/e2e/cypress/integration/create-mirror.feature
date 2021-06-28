#
# MIT License
#
# Copyright (c) 2020-present Cloudogu GmbH and Contributors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

Feature: Create Mirror

  Scenario: Authenticated and authorized user creates repository mirror
    Given User is authenticated
    And A git repository exists
    And User has permission to read repository and create repositories
    When User mirrors a repository
    Then The user is redirected to the repository's page
    And There is a mirror badge

  Scenario: Repository mirror is write protected
    Given User is authenticated
    And A git repository exists
    And User has permission to read repository and create repositories
    When User mirrors a repository
    And User creates a new branch in mirror
    Then There is an permission error message

  Scenario: Repository mirror is deletable
    Given User is authenticated
    And A git repository exists
    And User has permission to read repository and create repositories
    When User mirrors a repository
    And User deletes mirror
    Then Mirror does no longer exist

  Scenario: User gets notification after mirror synchronization
    Given User is authenticated
    And A git repository exists
    And User has permission to read repository and create repositories
    When User mirrors a repository
    Then User gets success notification
