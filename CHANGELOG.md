# Change Log

All notable changes to this project will be documented in this file.
This change log follows the conventions of
[keepachangelog.com](http://keepachangelog.com/).

## [Unreleased][unreleased]

### Fixed

- The method name `removeNotificationListener()` in
  `CoreMidiDeviceProvider` was previously
  `removedNotificationListener()`, which was inaccurate.

## [0.7] - 2016-04-20

### Added

- The native library is now bundled inside the CoreMidi4J jar and
  extracted automatically when appropriate, so CoreMidi4J can be
  embedded in other projects, avoiding the need for end users to
  install anything.
- An enhanced version of `getMidiDeviceInfo()` is available for use in
  place of the standard one found in `javax.sound.midi.MidiSystem`.
  This version filters out the broken MIDI device implementations
  offered by the standard Mac Java MIDI environment.

### Fixed

- MIDI devices with null descriptons or vendors coming from the native
  library are no longer considered erroneous, and instead are given
  default values equivalent to what the standard implementations have.

### Changed

- The installation instructions no longer recommend installing
  CoreMidi4J as a Java extension, and recommend that you remove any
  older version you have placed in the Extensions directory.

## [0.5] - 2016-03-19

### Fixed

- Under certain settings for system languages, MIDI devices were
  showing up with null values for their names, descriptions and
  vendors.

## [0.4] - 2016-01-18

### Fixed

- MIDI Timestamps are now properly translated between Java and
  CoreMidi. This may be the only MIDI SPI which achieves this.

## [0.3] - 2016-01-09

### Fixed

- Running status bytes are now handled properly, as is interleaving of
  all other MIDI commands with bytes of System Exclusive messages.
- Multiple messages sent in the same packet, as well as multi-packet
  messages from CoreMidi are properly processed.

### Changed

- Further progress towards supporting MIDI timestamps.


## [0.2] - 2016-01-01

### Fixed

- Handling of MIDI Timing Clock messages interleaved within the bytes
  of incoming System Exclusive messages.

### Added

- Notifications of changes to the MIDI environment can now be
  requested.

- First steps towards supporting MIDI timestamps.


## 0.1 - 2015-12-22

### Added

- Initial Public Release


[unreleased]: https://github.com/DerekCook/CoreMidi4J/compare/v0.7...HEAD
[0.7]: https://github.com/DerekCook/CoreMidi4J/compare/V0.5...v0.7
[0.5]: https://github.com/DerekCook/CoreMidi4J/compare/V0.4...V0.5
[0.4]: https://github.com/DerekCook/CoreMidi4J/compare/V0.3...V0.4
[0.3]: https://github.com/DerekCook/CoreMidi4J/compare/V0.2...V0.3
[0.2]: https://github.com/DerekCook/CoreMidi4J/compare/V0.1...V0.2
