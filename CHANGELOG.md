# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 2.4.2 - 2024-03-14
### Fixed
- Different names for "Mirror" in german and english

## 2.4.1 - 2024-02-15
### Fixed
- Application of the "Do not load LFS files" option in the mirror settings

## 2.4.0 - 2024-02-02
### Added
- Filter option to ignore LFS files on mirroring

## 2.3.1 - 2023-10-11
### Fixed
- Secrets appearing in the audit log, are now masked

## 2.3.0 - 2022-11-04
### Added
- Shorter refresh periods ([#28](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/28))

## 2.2.2 - 2022-01-07
### Fixed
- High contrast mode findings ([#24](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/24))

## 2.2.1 - 2022-01-03
### Fixed
- Fix form validation to prevent rerender loop

## 2.2.0 - 2021-10-07
### Added
- Local proxy configuration ([#19](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/19))
- Unmirror repository function ([#21](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/21))

### Fixed
- Visual artifacts on repository mirror tags for firefox users ([#20](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/20))

## 2.1.1 - 2021-07-08
### Fixed
- Read and list repositories without "OWNER" permission ([#17](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/17))

## 2.1.0 - 2021-07-06
### Added
- Option to disable automatic synchronization ([#14](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/14))

### Fixed
- Mirror with certificate credential configurable without setting certificate again ([#15](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/15))

## 2.0.1 - 2021-06-18
### Fixed
- Runtime dependency to mail plugin

## 2.0.0 - 2021-06-16
### Added
- Implement global configuration ([#11](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/11))

### Fixed
- Send mails on each status change ([#12](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/12))
- Add topic for personal mail settings ([#12](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/12))

## 1.0.1 - 2021-06-08
### Fixed
- Failures due to permission checks in other plugins ([#10](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/10))

## 1.0.0 - 2021-06-04
### Added
- Creation of mirrors ([#9](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/9))
- Basic authentication ([#9](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/9))
- Pkcs12 certificate authentication ([#9](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/9))
- Filtering by name ([#9](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/9))
- Filtering by signatures ([#9](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/9))
- Filtering by fast forward ([#9](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/9))
- Emergency contacts ([#9](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/9))
- Synchronization logs ([#9](https://github.com/scm-manager/scm-repository-mirror-plugin/pull/9))

