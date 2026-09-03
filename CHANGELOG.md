<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Mismatched Lock Object Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Real alias analysis proving non-identity between two `final`,
  `new`-initialized lock fields, flagging a field guarded by two
  provably different lock objects across different `synchronized`
  blocks in the same class -- broken mutual exclusion.

[Unreleased]: https://github.com/GapHunterLabs/mismatched-lock-object-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/mismatched-lock-object-companion/commits/0.1.0
