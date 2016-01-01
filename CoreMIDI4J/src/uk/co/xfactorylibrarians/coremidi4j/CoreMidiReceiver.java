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
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;

/**
 * CoreMidiReceiver - used to receive data from the application and send it to the connected device.
 *
 */

public class CoreMidiReceiver implements MidiDeviceReceiver {
	
	private final CoreMidiDestination device;

	/**
	 * CoreMidiReceicer constructor
	 * 
	 * @param device	The MIDI device that contains the information required to send MIDI data via OSX core MIDI
	 */
	
	public CoreMidiReceiver(final CoreMidiDestination device) {

		this.device = device;
	
	}

	/** 
	 * Sends a MIDI message
	 * 
	 * @see javax.sound.midi.Receiver#send(javax.sound.midi.MidiMessage, long)
	 * 
	 */
	
	@Override
	public void send(MidiMessage message, long timeStamp) {
		
		try {
			
			CoreMidiDeviceProvider.getOutputPort().send(((CoreMidiDeviceInfo)device.getDeviceInfo()).getEndPointReference(), message);
			
		} catch (CoreMidiException e) {
			
			e.printStackTrace();

		}

	}
	
	/** 
	 * Closes the MIDI Receiver
	 * 
	 * @see javax.sound.midi.Receiver#close()
	 * 
	 */
	
	@Override
	public void close() {

		// Do nothing. No close action required, but this method must be implemented.

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
