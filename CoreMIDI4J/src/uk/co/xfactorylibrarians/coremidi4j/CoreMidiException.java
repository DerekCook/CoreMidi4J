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

/**
 * Used to report errors and problems which occur both in the Java Native Interface bridge to Core MIDI,
 * and in the library itself.
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

  /**
   * Constructor with an underlying cause
   *
   * @param message the error message to include in the exception
   *
   * @param cause the underlying exception or other throwable being wrapped
   */
  public CoreMidiException(String message, Throwable cause) {

    super(message, cause);

  }

}
