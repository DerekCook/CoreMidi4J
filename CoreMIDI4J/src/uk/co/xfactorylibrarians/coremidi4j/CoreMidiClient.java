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
 * Provides the Java Native Interface bridge to CoreMIDI on macOS. This is an internal class which is managed by
 * the library itself.
 *
 */

public class CoreMidiClient {

  private final int midiClientReference;

  /**
   * Constructor for class
   *
   * @param name 	The name of the client
   *
   * @throws 			CoreMidiException if the client cannot be initialized
   *
   */

  public CoreMidiClient(String name) throws CoreMidiException {

    midiClientReference = this.createClient(name);

  }

  /**
   * Creates a new CoreMidiInputPort
   *
   * @param name	The name of the port
   *
   * @param createVirtual	If true, create a virtual input port
   *
   * @return			A new CoreMidiInputPort
   *
   * @throws 			CoreMidiException if the port cannot be created
   *
   */

  public CoreMidiInputPort inputPortCreate(final String name, final boolean createVirtual) throws CoreMidiException {

    return new CoreMidiInputPort(midiClientReference,name,createVirtual);

  }

  /**
   * Creates a new CoreMidiOutputPort
   *
   * @param name	The name of the port
   *
   * @return			A new CoreMidiOutputPort
   *
   * @throws 			CoreMidiException if the port cannot be created
   *
   */

  public CoreMidiOutputPort outputPortCreate(final String name) throws CoreMidiException {

    return new CoreMidiOutputPort(midiClientReference,name);

  }

  /**
   * The message callback for receiving notifications about changes in the MIDI environment from the JNI code
   *
   * @throws CoreMidiException if a problem occurs passing along the notification
   *
   */

  public void notifyCallback() throws CoreMidiException  {

    // Debug code - uncomment to see this function being called
    //System.out.println("** CoreMidiClient - MIDI Environment Changed");

    CoreMidiDeviceProvider.deliverCallbackToListeners();  // Try to deliver callback notifications to our listeners.

  }


  //////////////////////////////
  ///// JNI Interfaces
  //////////////////////////////

  /*
   * Static initializer for loading the native library
   *
   */

  static {

    try {

      Loader.load();

    } catch (Throwable t) {

      System.err.println("Unable to load native library, CoreMIDI4J will stay inactive: " + t);

    }

  }

  /**
   * Creates the MIDI Client
   *
   * @param clientName 					The name of the client
   *
   * @return										A reference to the MIDI client
   *
   * @throws CoreMidiException	if the client cannot be created
   *
   */

  private native int createClient(String clientName) throws CoreMidiException;

  /**
   * Disposes of a CoreMIDI Client
   *
   * @param clientReference		The reference of the client to dispose of
   *
   * @throws 									CoreMidiException if there is a problem disposing of the client
   *
   */

  private native void disposeClient(int clientReference) throws CoreMidiException;

}
