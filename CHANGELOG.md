<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Mismatched Lock Object Companion Changelog

## [Unreleased]

### Added

- A description page for the inspection in **Settings | Editor |
  Inspections**, which showed "Under construction".

### Changed

- The rating prompt's local counter keeps one-way fingerprints of findings
  instead of their file paths, and deletes the list that earlier versions
  kept.
- `PRIVACY.md` describes the values the plugin keeps in the IDE's local
  settings.

## [0.1.1]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace
  reviews page instead of the vendor's generic plugin list.

## [0.1.0]

### Added

- Real alias analysis proving non-identity between two `final`,
  `new`-initialized lock fields, flagging a field guarded by two
  provably different lock objects across different `synchronized`
  blocks in the same class -- broken mutual exclusion.

[Unreleased]: https://github.com/GapHunterLabs/mismatched-lock-object-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/mismatched-lock-object-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/mismatched-lock-object-companion/commits/0.1.0
