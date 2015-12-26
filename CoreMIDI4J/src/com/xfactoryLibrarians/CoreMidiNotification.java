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
 * CoreMidiNotification is a listener interface which is used to register for change notifications when CoreMIDI4J detects changes in the MIDI environment
 *
 */

public interface CoreMidiNotification {
	
	/**
	 * Called when a notification occurs
	 * 
	 * @throws CoreMidiException
	 * 
	 */
	
	public void midiSystemUpdated() throws CoreMidiException;

}
