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
