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
 * CoreMidiNotification is a listener interface which is used to register for change notifications when CoreMIDI4J detects changes in the MIDI environment
 *
 */

public interface CoreMidiNotification {

  /**
   * Called when a notification occurs
   * 
   * @throws CoreMidiException if there is a problem handling the notification
   * 
   */

  void midiSystemUpdated() throws CoreMidiException;

}
