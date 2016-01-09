/**
 * Title:        CoreMIDI4J
 * Description:  Core MIDI Device Provider for Java on OS X
 * Copyright:    Copyright (c) 2015-2016
 * Company:      x.factory Librarians
 *
 * @author Derek Cook
 * 
 * CoreMIDI4J is an open source Service Provider Interface for supporting external MIDI devices on MAC OS X
 * 
 * CREDITS - This library uses principles established by OSXMIDI4J, but converted so it operates at the JNI level with no additional libraries required
 * 
 */

package uk.co.xfactorylibrarians.coremidi4j;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceTransmitter;
import javax.sound.midi.Receiver;

/**
 * CoreMidiTransmitter - used to receive data from the connected device and send it to the application.
 *
 */

public class CoreMidiTransmitter implements MidiDeviceTransmitter {

  private final CoreMidiSource device;
  private Receiver receiver = null;

  /**
   * CoreMidiTransmitter constructor
   * 
   * @param device	The MIDI device that contains the information required to receive MIDI data via OSX core MIDI
   * 
   */

  public CoreMidiTransmitter(final CoreMidiSource device) {

    this.device = device;

  }

  /** 
   * Sets a receiver on this transmitter
   * 
   * @see javax.sound.midi.Transmitter#setReceiver(javax.sound.midi.Receiver)
   * 
   * @param receiver	The receiver to set
   * 
   */

  @Override
  public void setReceiver(Receiver receiver) {

    this.receiver = receiver;

  }

  /** 
   * Gets the receiver set on this transmitter
   * 
   * @see javax.sound.midi.Transmitter#setReceiver(javax.sound.midi.Receiver)
   * 
   * @return	The receiver set on this transmitter
   * 
   */

  @Override
  public Receiver getReceiver() {

    return receiver;

  }

  /**
   * Closes this transmitter
   * 
   * @see javax.sound.midi.Transmitter#close()
   * 
   */

  @Override
  public void close() {

    receiver = null;

  }

  /**
   * Gets the MIDI Device that this receiver is attached to
   * 
   * @return the MIDI Device that this receiver is attached to
   * 
   */

  @Override
  public MidiDevice getMidiDevice() {

    return device;

  }

}
