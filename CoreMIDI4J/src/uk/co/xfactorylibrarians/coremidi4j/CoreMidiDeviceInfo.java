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

/**
 * CoreMidiDeviceInfo class
 *
 */

public class CoreMidiDeviceInfo extends MidiDevice.Info {

  private final int endPointReference; // OS X Endpoint
  private final int uid; 							 // OS X UID

  /**
   * Substitutes a default value if the value is null
   *
   * @param value     The value which may be null
   * @param fallback  The value to use if value is null
   * 
   * @return          The value string, or the fallback string if the value is null
   * 
   */

  private static String defaultForNull(final String value, final String fallback) {

    if (value == null) {

      return fallback;

    }

    return value;

  }

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

    super(name, defaultForNull(vendor, "Unknown vendor"), defaultForNull(description, name), Integer.toString(version));
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
