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

import javax.sound.midi.MidiDevice;

/**
 * CoreMidiDeviceInfo class
 *
 */

public class CoreMidiDeviceInfo extends MidiDevice.Info {
	
	private final int endPointReference; // OS X Endpoint
	private final int uid; 							 // OS X UID

	/**
	 * Constructs a CoreMidiDeviceInfo object from the parameters
	 * 
	 * @param name							The name of the device
	 * @param vendor						The manufacturer of the device
	 * @param description				A description of the device
	 * @param version						The version number of the device driver
	 * @param endPointReference The end point reference
	 * @param uid								The OS X unique identifier for the device 
	 * 
	 */
	
	public CoreMidiDeviceInfo(final String name, final String vendor, final String description, final int version, final int endPointReference, final int uid) {
		
		super(name, vendor, description, Integer.toString(version));
		this.endPointReference = endPointReference;
		this.uid = uid;
		
	}

	/**
	 * Gets the OS X unique identifier for the device
	 * 
	 * @return	The OS X unique identifier for the device
	 * 
	 */
	
	public int getUniqueID() {
		
		return uid;
		
	}
		
	/**
	 * Gets a string describing the device
	 * 
	 * @return A string describing the device
	 * 
	 */
	
	public String getInformationString() {
		
		return getVendor() + ": " + getName(); 
		
	}

	/**
	 * Gets the endPointReference value
	 *
	 * @return the endPointReference value
	 *
	 */
	
	public int getEndPointReference() {
	
		return endPointReference;
	}
		
}
