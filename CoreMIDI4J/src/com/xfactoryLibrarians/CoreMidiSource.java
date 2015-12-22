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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

/**
 * CoreMidiDevice - a MidiDevice implementation for Apple CoreMIDI
 *
 */

public class CoreMidiSource implements MidiDevice {
	
	private final CoreMidiDeviceInfo info;
	
	private boolean isOpen;
	
  private CoreMidiInputPort input = null;

  private final List<Transmitter> transmitters;
  
  private boolean inSysexMessage;
  private Vector<byte[]> messageData;
  private int sysexMessageLength = 0;

  /**
	 * Default constructor. 
	 * 
	 * @param info	a CoreMidiDeviceInfo object providing details of the MIDI interface
	 * 
   * @throws 				CoreMidiException 
	 * 
	 */
	
	CoreMidiSource(CoreMidiDeviceInfo info) throws CoreMidiException {
		
		this.info = info;
		
		this.isOpen = false;
		
    transmitters = new ArrayList<Transmitter>();
				
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
		
		try {
			
			if ( this.input == null ) {
				
				this.input = CoreMidiDeviceProvider.getMIDIClient().inputPortCreate("Core Midi Provider Input");
				
			}
			
			this.input.connectSource(this);
			isOpen = true;

		} catch (CoreMidiException e) {
			
			e.printStackTrace();
			throw new MidiUnavailableException(e.getMessage());
			
		}
		
	}

	/**
	 * Closes the Core MIDI Device
	 * 
	 */
	
	@Override
	public void close() {

		try {
			
			if ( this.input != null ) {
				
				this.input.disconnectSource(this);
				
			}
			
			synchronized (transmitters) {
				
				transmitters.clear();
				
			}
			
		} catch (CoreMidiException e) {
			
			e.printStackTrace();
		
		} finally {
			
			// Reset the context data
			isOpen = false;

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

		return isOpen;
		
	}

	/**
	 * Obtains the current time-stamp of the device, in microseconds. 
	 * This interface does not support time-stamps, so it should always return -1.
	 * 
	 * @see javax.sound.midi.MidiDevice#getMicrosecondPosition()
	 * 
	 * @return Always -1 as this device does not support timestamps. 
	 * 
	 */
	
	@Override
	public long getMicrosecondPosition() {

		// Not supported
		return -1;
		
	}

	/**
	 * Gets the maximum number of receivers that can be attached to this device.
	 * 
	 * @see javax.sound.midi.MidiDevice#getMaxReceivers()
	 * 
	 * @return the maximum number of receivers that can be attached to this device. This is always 0 as a CoreMidiSource has no receivers
	 * 
	 */
	
	@Override
	public int getMaxReceivers() {
		
			// A CoreMidiSource has no receivers
			return 0;
			
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

		// Any number of transmitters is supported
		return -1;
		
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

		throw new MidiUnavailableException("CoreMidiSource has no receivers");
			
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
  	
  	// A CoreMidiSource has no receivers
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

		Transmitter t = new CoreMidiTransmitter(this);
  		
  	synchronized (transmitters) {
  		
  		transmitters.add(t);
  		
  	}
  	
  	return t;

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
  	
  	synchronized (transmitters) {

  		final List<Transmitter> list = new ArrayList<Transmitter>();

  		list.addAll(transmitters);

  		return list;

  	}

  }
	
  /**
   * The message callback for receiving midi data from the JNI code
   * 
   * @param messageLength	The length of the message
   * @param data					The data array that holds the message
   * 
   * @throws 							InvalidMidiDataException 
   * 
   */
    
  public void messageCallback(int messageLength,byte data[]) throws InvalidMidiDataException {
  	
  	MidiMessage message;
  	
  	if ( data[0] < (byte) 0xf0) {
  
  		// Uncomment the following to show the received message whilst debugging
  		//System.out.println("Message " + this.getHexString(data));
  		
    	switch (data[0] & (byte) 0xf0 ) {
    		
    		case (byte) ShortMessage.NOTE_ON:
    		case (byte) ShortMessage.NOTE_OFF:
    		case (byte) ShortMessage.POLY_PRESSURE:
    		case (byte) ShortMessage.CONTROL_CHANGE:
    		case (byte) ShortMessage.PITCH_BEND:
  				message = new ShortMessage(data[0] & 0xff, data[1], data[2]);
  			  transmitMessage(message);
  			  break;
  			  
    		case (byte) ShortMessage.PROGRAM_CHANGE:
    		case (byte) ShortMessage.CHANNEL_PRESSURE:
  				message = new ShortMessage(data[0], data[1], 0);
			 		transmitMessage(message);
			 		break;

    		default:
  				throw new InvalidMidiDataException("Invalid Status Byte " + data[0]);

    	}
    			
  	} else {
  		
    	switch ( data[0] & 0xff ) {
    		
    		case ShortMessage.MIDI_TIME_CODE:
    		case ShortMessage.SONG_POSITION_POINTER:
    		case ShortMessage.SONG_SELECT:
  				message = new ShortMessage(data[0], data[1], data[2]);
  			  transmitMessage(message);
  			  break;

     		case ShortMessage.TUNE_REQUEST:
    		case ShortMessage.TIMING_CLOCK:
    		case ShortMessage.START:
    		case ShortMessage.CONTINUE:
    		case ShortMessage.STOP:
    		case ShortMessage.ACTIVE_SENSING:
    		case ShortMessage.SYSTEM_RESET:
  				message = new ShortMessage( data[0] & 0xff );
  			  transmitMessage(message);
    			break;
    			
    		case SysexMessage.SYSTEM_EXCLUSIVE:
     			inSysexMessage = true;
    			sysexMessageLength = 0;
    			messageData = new Vector<byte[]>();
    			inSysexMessage = partialSysexData(messageLength,data);

    			if ( inSysexMessage == false) {

    				transmitMessage(constructSysxMessage());
    				
    			}
    			break;
    			
    		default:
    			if ( inSysexMessage == true ) {
    				
      			inSysexMessage = partialSysexData(messageLength, data);
      			
       			if ( inSysexMessage == false) {
      				
      				transmitMessage(constructSysxMessage());
      				
      			}

    			} else {
    				
    				throw new InvalidMidiDataException("Invalid Status Byte ");
 				
    			}
    			break;
    			
    	}
    	  	  	  	
  	}
  	
  }
  
  /**
   * Creates a SYSEX message from the received partial messages. This function is called when F7 is detected in the partial message
   * 
   * @return	The constructed SYSEX message	
   * 
   * @throws 	InvalidMidiDataException
   * 
   */
  
  private SysexMessage constructSysxMessage() throws InvalidMidiDataException {
  	
  	// Create the array to hold the constructed message and reset the index (where the data will be copied)
  	byte data[] = new byte[sysexMessageLength];
  	int index = 0;
  	
  	// Iterate through the partial messages
  	for (int i = 0; i < messageData.size(); i += 1 ) {
  		
  		// Get the partial message
  		byte sourceData[] = messageData.get(i); 
  		
  		// Copy the partial message into the array
    	System.arraycopy(sourceData, 0, data, index, sourceData.length);
    	
    	// Point the index to where the next partial message needs to be copied.
    	index += sourceData.length;
  		
  	}
  	
  	// Create and return the new SYSYEX Message
  	return new SysexMessage(data, sysexMessageLength);
  	
  }
  
  /**
   * Called when a SYSEX message is being received. The partial data is added to the Vector 
   * 
   * @param packetLength		the length of the data packet
   * @param sourceData			the source data received from Core MIDI
   * 
   * @return								true if no END_OF_EXCLUSIVE is received at the end of the data. If it is found then false is returned
   * 
   */
  private boolean partialSysexData(int packetLength, byte sourceData[]) {
  	
  	// Create an array (note the source array will be released by the native function
  	byte data[] = new byte[packetLength];
  	
  	//Copy the data to the array
  	
  	try { 
  	
  		System.arraycopy(sourceData, 0, data, 0, packetLength);
  		
  	} catch ( ArrayIndexOutOfBoundsException e ) {
  		
  		e.printStackTrace();
  		
  		throw e;
  		
  	}
  	
  	// Add the message to the vector
		messageData.add(data);
		
		// Update the length of the SYSEX message
		sysexMessageLength += packetLength;
		
		// Check and see if the end of the message has been received
		return ( sourceData[packetLength-1] != (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE);

  }
  
  
  /**
   * Sends a MIDI message to all of the registered transmitters
   * 
   * @param message		the message to send
   * 
   */
  
  private void transmitMessage(final MidiMessage message) {

// Uncomment the following to filter realtime messages during debugging
//  	if ( ( message.getStatus() == ShortMessage.ACTIVE_SENSING ) || ( message.getStatus() == ShortMessage.TIMING_CLOCK ) ) {
//  		
//  		return;
//  		
//  	}

  	synchronized (transmitters) {

			 // Get the iterators from the transmitters collection
  		final Iterator<Transmitter> iterator = transmitters.iterator();
  		
  		// Loop through the transmitters
  		while (iterator.hasNext()) {

  			// Get the next transmitter
  			final Transmitter transmitter = iterator.next();
  			
  			// If the transmitter is not null then get the receiver
  			if (transmitter != null) {

  				final Receiver receiver = transmitter.getReceiver();

  				// If the receiver is not null then get send the message
  				if (receiver != null) {

    	  		receiver.send(message, -1);

  				}

  			}

  		}

  	}
    
  }
  
	/**
	 * Formats the provided data into a HEX string, which is useful for debugging
	 * 
	 * @param aByte		The data to format
	 * 
	 * @return 				The formatted HEX string
	 *
	 */

	private String getHexString(byte[] aByte) {

		StringBuffer	sbuf = new StringBuffer(aByte.length * 3 + 2);

		for (int i = 0; i < aByte.length; i++) {

			sbuf.append(' ');
			byte	bhigh = (byte) ((aByte[i] &  0xf0) >> 4);
			sbuf.append((char) (bhigh > 9 ? bhigh + 'A' - 10: bhigh + '0'));
			byte	blow = (byte) (aByte[i] & 0x0f);
			sbuf.append((char) (blow > 9 ? blow + 'A' - 10: blow + '0'));

		}

		return new String(sbuf).trim();

	}

}
