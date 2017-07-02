/*
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

import javax.sound.midi.MidiMessage;

/**
 * CoreMidiOutputPort
 *
 */

public class CoreMidiOutputPort {

  private final int midiPortReference;

  /**
   * Constructor
   * 
   * @param clientReference	The client reference
   * @param portName				The name of the port
   *  
   * @throws 								CoreMidiException if there is a problem creating the port
   * 
   */

  public CoreMidiOutputPort(final int clientReference, String portName) throws CoreMidiException {

    this.midiPortReference = this.createOutputPort(clientReference,portName);

  }

  /**
   * Sends a MIDI message on this output port to the specified destination end point
   * 
   * @param destinationEndPointReference	The destination end point to send the message to
   * @param message												The message to send
   * @param timestamp                     The time at which the message should take effect, in microseconds since the
   *                                      system booted, with 0 meaning "immediately".
   * 
   * @throws 															CoreMidiException if there is a problem sending the message
   */

  public void send(int destinationEndPointReference, MidiMessage message, long timestamp) throws CoreMidiException {

    sendMidiMessage(midiPortReference, destinationEndPointReference, message, timestamp);

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
   * Creates a CoreMIDI output port
   * 
   * @param clientReference	The MIDI client reference
   * @param portName 				The name of the output port
   * 
   * @return								A reference to the created output port
   * 
   * @throws 								CoreMidiException if the port cannot be created
   * 
   */

  private native int createOutputPort(int clientReference, String portName) throws CoreMidiException;

  /**
   * Transmits a MIDI message to the OSX CoreMidi device
   * 
   * @param midiPortReference              The output MIDI port reference
   * @param destinationEndPointReference   The device to send the message to
   * @param message                        The message to send
   * @param timestamp                      The time at which the message should take effect, in microseconds since the
   *                                       system booted, with zero meaning immediately.
   *
   * @throws 																CoreMidiException if there is a problem sending the message
   * 
   */

  private native void sendMidiMessage(int midiPortReference, int destinationEndPointReference, MidiMessage message, long timestamp) throws CoreMidiException;

}
