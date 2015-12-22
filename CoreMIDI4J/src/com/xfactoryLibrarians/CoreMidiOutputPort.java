/**
 * Title:        CoreMIDI4J
 * Description:  Core MIDI Device Provider for Java on OS X
 * Copyright:    Copyright (c) 2015
 * Company:      x.factory Librarians
 * @author       Derek Cook
 * 
 * CREDITS - This library uses principles established by OSXMIDI4J, but converted so it operates at the JNI level with no additional libraries required
 *
 */

package com.xfactoryLibrarians;

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
   * @throws 								CoreMidiException 
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
	 * 
	 * @throws 															CoreMidiException
	 */
	
	public void send(int destinationEndPointReference, MidiMessage message) throws CoreMidiException {
		
		sendMidiMessage(midiPortReference, destinationEndPointReference, message);
		
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
   * @param midiPortReference 							The output MIDI port reference
   * @param destinationEndPointReference		The device to send the message to
   * @param message													The message to send
   * @throws 																CoreMidiException 
   * 
   */
  
  private native void sendMidiMessage(int midiPortReference, int destinationEndPointReference, MidiMessage message) throws CoreMidiException;
  
}
