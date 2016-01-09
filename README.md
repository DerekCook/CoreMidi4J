# CoreMidi4J
Core MIDI Service Provider Interface (SPI) for Java 1.7 and above on OS X.

I have created CoreMIDI4J as to my knowledge there is currently no SPI under active development that overcomes the inherent Java MIDI SYSEX limitiations, which still have not been fixed in the Java Core software. Hopefully one day these SPIs will not be required, but until then one is needed.

For years I have used MMJ, but that appears to longer be under development and it does not work with later Java Runtimes. After looking around for a replacement, I decided it was necessary to create my own "lightweight" SPI, and that I would make it publicly available for others to contribute to.

The current release is a developer preview, and may or may not work on all systems with all interfaces. 
