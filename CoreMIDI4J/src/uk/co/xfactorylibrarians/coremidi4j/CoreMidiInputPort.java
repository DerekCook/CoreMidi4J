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

import javax.sound.midi.MidiDevice.Info;

/**
 * Wraps a native macOS Core MIDI input port.
 *
 */

public class CoreMidiInputPort {

  private final boolean isVirtual;

  /** The OSX MIDI port reference */
  private int midiPortReference;

  /** For each connection to an OSX EndPoint, some data is allocated on the native side. This handle tracks the allocation so that it can be returned when this port is disconnected */
  private long memoryHandle;

  /** The client reference */
  private final int clientReference;

  /** The port name */
  private final String portName;

  /**
   * Constructor
   *
   * @param clientReference	The client reference
   * @param portName 				The name of the input port
   * @param isVirtual       If true, create a virtual endpoint instead of connecting a real device
   * @throws 								CoreMidiException if the input port cannot be created
   *
   */

  public CoreMidiInputPort(final int clientReference, String portName, boolean isVirtual) throws CoreMidiException {

    this.isVirtual = isVirtual;

    this.portName = portName;

    this.midiPortReference = isVirtual ? 0 : this.createInputPort(clientReference, portName);

    this.clientReference = clientReference;

  }

  /**
   * Connects a source to this input port
   *
   * @param sourceDevice		The source device that wishes to connect to the port
   *
   * @throws 								CoreMidiException if there is a problem establishing the connection
   *
   */

  public void connectSource(final CoreMidiSource sourceDevice) throws CoreMidiException {

    if (isVirtual) {

      midiPortReference = createVirtualPort(clientReference, portName, sourceDevice);

      // This is kind of ugly: read the device info from the CoreMidiDeviceProvider
      CoreMidiDeviceProvider provider = new CoreMidiDeviceProvider();

      provider.midiSystemUpdated();

      for (Info info : provider.getDeviceInfo()) {

        if (info instanceof CoreMidiDeviceInfo) {

          if (((CoreMidiDeviceInfo) info).getEndPointReference() == midiPortReference) {

            sourceDevice.updateDeviceInfo((CoreMidiDeviceInfo)info);

            break;

          }

        }

      }

    } else {

      memoryHandle = midiPortConnectSource(midiPortReference, sourceDevice);

    }

  }

  /**
   * Disconnects a source from input port
   *
   * @param sourceDevice	The source device that wishes to disconnect from the port
   *
   * @throws 							CoreMidiException if there is a problem removing the connection
   *
   */

  public void disconnectSource(final CoreMidiSource sourceDevice) throws CoreMidiException {

    if (isVirtual) {

      disposeVirtualPort(midiPortReference);

    } else {

      midiPortDisconnectSource(midiPortReference, memoryHandle, sourceDevice);

    }

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
   * Creates a CoreMIDI input port
   *
   * @param clientReference	The MIDI client reference
   * @param portName 				The name of the output port
   *
   * @return								A reference to the created input port
   *
   * @throws 								CoreMidiException if the port cannot be created
   *
   */

  private native int createInputPort(int clientReference, String portName) throws CoreMidiException;

  /**
   * Creates a CoreMIDI virtusal input port
   *
   * @param clientReference	The MIDI client reference
   * @param portName 				The name of the input port
   * @param sourceDevice						The virtual source device that receives the MIDI data
   *
   * @return								A reference to the created virtual input port
   *
   * @throws 								CoreMidiException if the port cannot be created
   *
   */

  private native int createVirtualPort(int clientReference, String portName, CoreMidiSource sourceDevice) throws CoreMidiException;


  /**
   * Disposes a virtual port
   *
   * @param inputPortReference			The reference to an virtual input port
   *
   * @throws 												CoreMidiException if there is a problem disconnecting the source
   *
   */
  private native void disposeVirtualPort(int inputPortReference) throws CoreMidiException;

  /**
   * Connects a source end point to a MIDI input
   *
   * @param inputPortReference			The reference to an input port
   * @param sourceDevice						The source device that wishes to connect to the port
   *
   * @return												A memory handle for the parameters the native side has associated with this call
   *
   * @throws 												CoreMidiException if there is a problem connecting the source
   *
   */

  private native long midiPortConnectSource(int inputPortReference, CoreMidiSource sourceDevice) throws CoreMidiException;

  /**
   * Disconnects a source end point to a MIDI input
   *
   * @param inputPortReference			The reference to an input port
   * @param memoryReference 				The memory handle that can now be released.
   * @param sourceDevice						The source device that wishes to disconnect from the port
   *
   * @throws 												CoreMidiException if there is a problem disconnecting the source
   *
   */

  private native void midiPortDisconnectSource(int inputPortReference, long memoryReference, CoreMidiSource sourceDevice) throws CoreMidiException;

}
