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

import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

/**
 * CoreMidiDestination - implementation for Apple CoreMIDI
 *
 */

public class CoreMidiDestination implements MidiDevice {

  private final CoreMidiDeviceInfo info;

  private boolean isOpen;
  private long startTime;  // The system time in microseconds when the port was opened

  /**
   * Default constructor. 
   * 
   * @param info	a CoreMidiDeviceInfo object providing details of the MIDI interface
   * 
   */

  CoreMidiDestination(final CoreMidiDeviceInfo info) {

    this.info = info;

    this.isOpen = false;

  }

  /** 
   * Gets the MIDI Info object
   * 
   * @return the MIDI Info object, which provides details about the interface
   * 
   */

  @Override
  public Info getDeviceInfo() {

    return info;

  }

  /**
   * Opens the Core MIDI Device
   * 
   * @throws MidiUnavailableException
   * 
   */

  @Override
  public void open() throws MidiUnavailableException {

    isOpen = true;
  
    // Get the system time in microseconds
    startTime = this.getMicroSecondTime();

  }

  /**
   * Closes the Core MIDI Device
   * 
   */

  @Override
  public void close() {

    // Reset the context data
    isOpen = false;

  }

  /**
   * Checks to see if the MIDI Device is open
   * 
   * @see javax.sound.midi.MidiDevice#isOpen()
   * 
   * @return true if the device is open, otherwise false;
   *  
   */

  @Override
  public boolean isOpen() {

    return isOpen;

  }

  /**
   * Obtains the time in microseconds that has elapsed since this MIDI Device was opened.
   *
   * @return the time in microseconds that has elapsed since this MIDI Device was opened.
   * 
   * @see javax.sound.midi.MidiDevice#getMicrosecondPosition()
   * 
   */

  @Override
  public long getMicrosecondPosition() {

    // Return the elapsed time in Microseconds
    return this.getMicroSecondTime() - startTime;

  }

  /**
   * Gets the maximum number of receivers that can be attached to this device.
   * 
   * @see javax.sound.midi.MidiDevice#getMaxReceivers()
   * 
   * @return the maximum number of receivers that can be attached to this device. -1 is returned to indicate that the number is unlimited
   */

  @Override
  public int getMaxReceivers() {

    // A CoreMidiDestination can support any number of receivers
    return -1;

  }

  /**
   * Gets the maximum number of transmitters that can be attached to this device.
   * 
   * @see javax.sound.midi.MidiDevice#getMaxTransmitters()
   * 
   * @return the maximum number of transmitters that can be attached to this device. -1 is returned to indicate that the number is unlimited
   * 
   */

  @Override
  public int getMaxTransmitters() {

    // A CoreMIDI Destination has no transmitters
    return 0;

  }

  /**
   * Creates and returns a MIDI Receiver for use with this Device
   * 
   * @see javax.sound.midi.MidiDevice#getReceiver()
   * 
   * @return the created receiver
   * 
   */

  @Override
  public Receiver getReceiver() throws MidiUnavailableException {

    return (Receiver) new CoreMidiReceiver(this);

  }

  /**  
   * Gets a list of receivers connected to the device
   * 
   * @see javax.sound.midi.MidiDevice#getReceivers()
   * 
   * @return NULL - we do not maintain a list of receivers 
   * 
   */

  @Override
  public List<Receiver> getReceivers() {

    // We do not maintain a list of receivers, as they tend to be transitory context
    return null;
  }

  /**
   * Gets a transmitter for this device (which is also added to the internal list
   * 
   * @see javax.sound.midi.MidiDevice#getTransmitter()
   * 
   * @return  a transmitter for this device
   * 
   */

  @Override
  public Transmitter getTransmitter() throws MidiUnavailableException {

    throw new MidiUnavailableException("CoreMidiDestination has no sources (Transmitters)");

  }

  /**
   * Gets the list of transmitters registered with this MIDI device
   * 
   * @see javax.sound.midi.MidiDevice#getTransmitters()
   * 
   * @return 	The list of transmitters registered with this MIDI device
   * 
   */

  @Override
  public List<Transmitter> getTransmitters() {

    // A CoreMIDI Destination has no transmitters
    return null;

  }

  //////////////////////////////
  ///// JNI Interfaces
  //////////////////////////////

  /**
   * Static method for loading the native library 
   * 
   */

  static {

    System.loadLibrary("CoreMIDI4J");

  }

  /**
   * Obtains the current system time in microseconds.
   *
   * @return The current system time in microseconds.
   * 
   */

  private native long getMicroSecondTime();

}
