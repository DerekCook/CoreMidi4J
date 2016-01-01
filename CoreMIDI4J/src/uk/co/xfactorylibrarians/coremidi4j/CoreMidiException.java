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

/**
 * CoreMidiException - thrown by the JNI Native Code if errors are detected during processing.
 *
 */

public class CoreMidiException extends Exception {

	/**
	 * Default constructor
	 * 
	 * @param message	The error message to include in the exception
	 * 
	 */
	
	public CoreMidiException(String message) {

		super(message);
		
	}

}
