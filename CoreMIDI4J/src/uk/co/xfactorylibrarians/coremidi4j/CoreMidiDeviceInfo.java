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

import javax.sound.midi.MidiDevice;

/**
 * Provides information about a MIDI device which is implemented by CoreMidi4J.
 *
 */

public class CoreMidiDeviceInfo extends MidiDevice.Info {

  private final String deviceName;   // OS X Device Name
  private final int deviceReference; // OS X Device Reference
  private final int deviceUniqueID;  // OS X Device UID

  private final String entityName;   // OS X Entity Name
  private final int entityReference; // OS X Entity Reference
  private final int entityUniqueID;  // OS X Entity UID

  private final String endPointName;   // OS X Endpoint Name
  private final int endPointReference; // OS X Endpoint Reference
  private final int endPointUniqueID;  // OS X Endpoint UID

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
   * @param deviceName        The raw name of the device
   * @param deviceReference   The device reference
   * @param deviceUniqueID    The OS X unique identifier for the device
   * @param entityName        The raw name of the entity
   * @param entityReference   The entity reference
   * @param entityUniqueID    The OS X unique identifier for the entity
   * @param endPointName      The raw name of the end point
   * @param endPointReference The end point reference
   * @param endPointUniqueID  The OS X unique identifier for the end point
   *
   */

  public CoreMidiDeviceInfo(final String name,
                            final String vendor,
                            final String description,
                            final String version,
                            final String deviceName,
                            final int deviceReference,
                            final int deviceUniqueID,
                            final String entityName,
                            final int entityReference,
                            final int entityUniqueID,
                            final String endPointName,
                            final int endPointReference,
                            final int endPointUniqueID) {

    super("CoreMIDI4J - " + name, defaultForNull(vendor, "<Unknown vendor>"), defaultForNull(description, name), version);

    this.deviceName        = deviceName;
    this.deviceReference   = deviceReference;
    this.deviceUniqueID    = deviceUniqueID;
    this.entityName        = entityName;
    this.entityReference   = entityReference;
    this.entityUniqueID    = entityUniqueID;
    this.endPointName      = endPointName;
    this.endPointReference = endPointReference;
    this.endPointUniqueID  = endPointUniqueID;

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
   * Gets the endPoint name
   *
   * @return the endPoint name
   *
   */

  public String getEndPointName() {

    return endPointName;

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

  /**
   * Gets the OS X unique identifier for the end point
   *
   * @return  The OS X unique identifier for the end point
   *
   */

  public int getEndPointUniqueID() {

    return endPointUniqueID;

  }

  /**
   * Gets the entity name
   *
   * @return the entity name
   *
   */

  public String getEntityName() {

    return entityName;

  }

  /**
   * Gets the entityReference value
   *
   * @return the entityReference value
   *
   */

  public int getEntityReference() {

    return entityReference;

  }

  /**
   * Gets the OS X unique identifier for the entity
   *
   * @return  The OS X unique identifier for the entity
   *
   */

  public int getEntityUniqueID() {

    return entityUniqueID;

  }

  /**
   * Gets the device name
   *
   * @return the device name
   *
   */

  public String getDeviceName() {

    return deviceName;

  }

  /**
   * Gets the deviceReference value
   *
   * @return the deviceReference value
   *
   */

  public int getDeviceReference() {

    return deviceReference;

  }

  /**
   * Gets the OS X unique identifier for the device
   *
   * @return  The OS X unique identifier for the device
   *
   */

  public int getdeviceUniqueID() {

    return deviceUniqueID;

  }

}
