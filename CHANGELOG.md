# Change Log

All notable changes to this project will be documented in this file.
This change log follows the conventions of
[keepachangelog.com](http://keepachangelog.com/).

## [Unreleased][unreleased]

### Fixed

- Calling `close()` more than once on a `CoreMidiSource` would lead to
  a [crash](https://github.com/DerekCook/CoreMidi4J/issues/19) as the
  same block of memory is attempted to be freed more than once.
- Implemented the rest of the MIDI SPI contract for tracking and
  closing transmitters and receivers.
- We now close any devices (and their transmitters or receivers) when
  the underlying CoreMIDI device disappears.


## [1.0] - 2017-05-14

### Fixed

- A long-standing
  [crash or hang](https://github.com/DerekCook/CoreMidi4J/issues/10)
  when trying to use CoreMidi4J from JavaFX was
  [fixed](https://github.com/DerekCook/CoreMidi4J/pull/18) by
  [@mpsalisbury](https://github.com/mpsalisbury).
- You no longer
 [need to call](https://github.com/DerekCook/CoreMidi4J/issues/9)
 `System.exit()` to end your program even when
  MIDI environment changes have occurred while it was running.
- A potentially uninitialized variable was fixed in the native code.

### Changed

- Dependencies and build tools were updated to their latest releases.

## [0.9] - 2016-08-23

### Added

- A new static method `getLibraryVersion()` in
  `CoreMidiDeviceProvider` so that client software can determine the
  version of CoreMIDI4J which is in use. (If this method is not found,
  then the version is unknown, but must be 0.8 or earlier.)

## [0.8] - 2016-07-03

### Fixed

- The method name `removeNotificationListener()` in
  `CoreMidiDeviceProvider` was previously
  `removedNotificationListener()`, which was inaccurate.
- A variety of gaps in the API documentation were addressed.

### Changed

- Dependencies and build tools were updated to their latest releases.

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


[unreleased]: https://github.com/DerekCook/CoreMidi4J/compare/V1.0...HEAD
[1.0]: https://github.com/DerekCook/CoreMidi4J/compare/V0.9...V1.0
[0.9]: https://github.com/DerekCook/CoreMidi4J/compare/V0.8...V0.9
[0.8]: https://github.com/DerekCook/CoreMidi4J/compare/v0.7...V0.8
[0.7]: https://github.com/DerekCook/CoreMidi4J/compare/V0.5...v0.7
[0.5]: https://github.com/DerekCook/CoreMidi4J/compare/V0.4...V0.5
[0.4]: https://github.com/DerekCook/CoreMidi4J/compare/V0.3...V0.4
[0.3]: https://github.com/DerekCook/CoreMidi4J/compare/V0.2...V0.3
[0.2]: https://github.com/DerekCook/CoreMidi4J/compare/V0.1...V0.2
