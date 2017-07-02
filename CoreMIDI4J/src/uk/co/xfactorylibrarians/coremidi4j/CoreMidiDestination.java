/*
 * Title:        CoreMIDI4J
 * Description:  Core MIDI Device Provider for Java on OS X
 * Copyright:    Copyright (c) 2015-2016
 * Company:      x.factory Librarians
 *
 * @author Derek Cook, James Elliott
 * 
 * CoreMIDI4J is an open source Service Provider Interface for supporting external MIDI devices on MAC OS X
 * 
 * CREDITS - This library uses principles established by OSXMIDI4J, but converted so it operates at the JNI level with no additional libraries required
 * 
 */

package uk.co.xfactorylibrarians.coremidi4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
  private final AtomicBoolean isOpen;  // Tracks whether we are conneted to CoreMIDI and can be used
  private final AtomicLong startTime;  // The system time in microseconds when the port was opened
  private final Set<CoreMidiReceiver> receivers;

  /**
   * Default constructor. 
   * 
   * @param info	a CoreMidiDeviceInfo object providing details of the MIDI interface
   * 
   */

  CoreMidiDestination(final CoreMidiDeviceInfo info) {

    this.info = info;
    isOpen = new AtomicBoolean(false);
    startTime = new AtomicLong(0);
    receivers = Collections.newSetFromMap(new ConcurrentHashMap<CoreMidiReceiver, Boolean>());

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
   * @throws MidiUnavailableException if the MIDI system cannot be accessed
   * 
   */

  @Override
  public void open() throws MidiUnavailableException {

    if (isOpen.compareAndSet(false, true)) {

      // Track the system time in microseconds
      startTime.set(getMicroSecondTime());

    }

  }

  /**
   * Closes the Core MIDI Device, which also closes all of its receivers
   * 
   */

  @Override
  public void close() {

    if (isOpen.compareAndSet(true, false)) {

      // Reset the context data
      startTime.set(0);

      // Close all our receivers, which will also clear the list.
      // We iterate on a copy of the receiver list to avoid issues with concurrent modification.
      for (Receiver receiver : getReceivers()) {

        receiver.close();

      }

    }

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

    return isOpen.get();

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
    return getMicroSecondTime() - startTime.get();

  }

  /**
   * Obtains the time in microseconds at which this MIDI Device was opened.
   *
   * @return the time in microseconds that was recorded when this device was opened.
   */

  public long getStartTime() {

    return startTime.get();

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

    // Create a new receiver
    CoreMidiReceiver receiver =  new CoreMidiReceiver(this);

    // Add it to the set of open receivers
    receivers.add(receiver);

    // Finally return it
    return receiver;

  }

  /**  
   * Gets a list of receivers connected to the device
   * 
   * @see javax.sound.midi.MidiDevice#getReceivers()
   * 
   * @return the list of receivers that have been created from this device that are still open
   * 
   */

  @Override
  public List<Receiver> getReceivers() {

    // Return an immutable copy of our current set of open receivers
    return Collections.unmodifiableList(new ArrayList<Receiver>(receivers));
    
  }

  /**
   * Reacts to the closing of a receiver by removing it from the set of active receivers
   *
   * @param receiver the transmitter which is reporting itself as having closed
   */

  void receiverClosed(CoreMidiReceiver receiver) {

    receivers.remove(receiver);

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
    return Collections.emptyList();

  }

  //////////////////////////////
  ///// JNI Interfaces
  //////////////////////////////

  /*
   * Static initializer for loading the native library
   *
   */

  static {

    try {

      Loader.load();

    } catch (Throwable t) {

      System.err.println("Unable to load native library, CoreMIDI4J will stay inactive: " + t);

    }

  }

  /**
   * Obtains the current system time in microseconds.
   *
   * @return The current system time in microseconds.
   * 
   */

  private native long getMicroSecondTime();

}
