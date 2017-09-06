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

import java.util.*;

import javax.sound.midi.*;
import javax.sound.midi.spi.MidiDeviceProvider;

/**
 * The OS X CoreMIDI Device Provider
 *
 */

public class CoreMidiDeviceProvider extends MidiDeviceProvider implements CoreMidiNotification {

  private static final int DEVICE_MAP_SIZE = 20;

  private static final class MidiProperties {

    private CoreMidiClient client;
    private CoreMidiOutputPort output;
    private final Map<Integer, MidiDevice> deviceMap = new LinkedHashMap<>(DEVICE_MAP_SIZE);

  }

  private static final MidiProperties midiProperties = new MidiProperties();

  /**
   * Initialises the system
   * 
   * @throws CoreMidiException if there is a problem communicating with CoreMIDI
   * 
   */

  private synchronized void initialise() throws CoreMidiException {

    if ( midiProperties.client == null ) {

      midiProperties.client = new CoreMidiClient("Core MIDI Provider");
      midiProperties.output = midiProperties.client.outputPortCreate("Core Midi Provider Output");
      buildDeviceMap();

    }

  }

  /**
   * Class constructor
   * 
   * @throws CoreMidiException if there is a problem initializing the provider
   * 
   */

  public CoreMidiDeviceProvider() throws CoreMidiException {

    // If the dynamic library failed to load, leave ourselves in an uninitialised state, so we simply always return
    // an empty device map.
    if (isLibraryLoaded()) {

      // If the client has not been initialised then we need to set up the static fields in the class
      if ( midiProperties.client == null ) {

        initialise();

      }

      midiProperties.client.addNotificationListener(this);

    }

  }

  /**
   * Builds the device map
   * 
   * @throws CoreMidiException if there is a problem communicating with CoreMIDI
   * 
   */

  private void buildDeviceMap() throws CoreMidiException {

    Set<Integer> devicesSeen = new HashSet<>();

    // Iterate through the sources
    for (int i = 0; i < getNumberOfSources(); i++) {

      // Get the end point reference and its unique ID
      final int endPointReference = getSource(i);
      final int uniqueID = getUniqueID(endPointReference);

      // Keep track of the IDs of all the devices we see
      devicesSeen.add(uniqueID);

      // If the unique ID of the end point is not in the map then create a CoreMidiSource object and add it to the map.
      if ( !midiProperties.deviceMap.containsKey(uniqueID) ) {

        midiProperties.deviceMap.put(uniqueID, new CoreMidiSource(getMidiDeviceInfo(endPointReference)));

      } else {  // We already know about the device, but may need to update its information (e.g. user renamed it).

        CoreMidiSource existingDevice = (CoreMidiSource) midiProperties.deviceMap.get(uniqueID);
        existingDevice.updateDeviceInfo(getMidiDeviceInfo(endPointReference));

      }

    }

    // Iterate through the destinations
    for (int i = 0; i < getNumberOfDestinations(); i++) {

      // Get the end point reference and its unique ID
      final int endPointReference = getDestination(i);
      final int uniqueID = getUniqueID(endPointReference);

      // Keep track of the IDs of all the devices we see
      devicesSeen.add(uniqueID);

      // If the unique ID of the end point is not in the map then create a CoreMidiDestination object and add it to the map.
      if ( !midiProperties.deviceMap.containsKey(uniqueID) ) {

        midiProperties.deviceMap.put(uniqueID, new CoreMidiDestination(getMidiDeviceInfo(endPointReference)));

      } else {  // We already know about the device, but may need to update its information (e.g. user renamed it).

        CoreMidiDestination existingDevice = (CoreMidiDestination) midiProperties.deviceMap.get(uniqueID);
        existingDevice.updateDeviceInfo(getMidiDeviceInfo(endPointReference));

      }

    }

    // Finally, remove any devices from the map which were no longer available according to CoreMIDI, and close them
    // appropriately as needed.
    Set<Integer> devicesInMap = new HashSet<>(midiProperties.deviceMap.keySet());

    for (Integer uniqueID : devicesInMap) {

      if ( !devicesSeen.contains(uniqueID) ) {

        MidiDevice vanishedDevice = midiProperties.deviceMap.remove(uniqueID);

        try {

          if (vanishedDevice instanceof CoreMidiSource) {

            // Must handle specially to avoid trying to interact with defunct CoreMIDI device
            ((CoreMidiSource) vanishedDevice).deviceDisappeared();

          } else {

            vanishedDevice.close();  // CoreMidiDestination close is safe to call even after the device is gone

          }

        } catch (Exception e) {

          System.err.println("Problem trying to clean up vanished MIDI device " + vanishedDevice + ": " + e);
          e.printStackTrace();
          
        }

      }

    }

  }

  /**
   * Gets the CoreMidiClient object 
   * 
   * @return	The CoreMidiClient object 
   * 
   * @throws 	CoreMidiException if there is a problem communicating with CoreMIDI
   * 
   */

  static CoreMidiClient getMIDIClient() throws CoreMidiException {

    // If the client has not been initialised then we need to setup the static fields in the class
    if (midiProperties.client == null) {

      new CoreMidiDeviceProvider().initialise();

    }

    return midiProperties.client;

  }

  /**
   * Gets the output port
   * 
   * @return	the output port
   * 
   */

  static CoreMidiOutputPort getOutputPort() {

    // If the client has not been initialised then we need to setup the static fields in the class
    if (midiProperties.output == null) {

      try {

        new CoreMidiDeviceProvider().initialise();

      } catch (CoreMidiException e) {

        e.printStackTrace();

      }

    }

    return midiProperties.output;

  }



  /** 
   * Gets information on the installed Core MIDI Devices
   * 
   * @return an array of MidiDevice.Info objects
   * 
   */

  @Override
  public MidiDevice.Info[] getDeviceInfo() {

    // If there are no devices in the map, then return an empty array
    if (midiProperties.deviceMap == null) {

      return new MidiDevice.Info[0];

    }

    // Create the array and iterator
    final MidiDevice.Info[] info = new MidiDevice.Info[midiProperties.deviceMap.size()];
    final Iterator<MidiDevice> iterator = midiProperties.deviceMap.values().iterator();

    int counter = 0;

    // Iterate over the device map and populate the array
    while (iterator.hasNext()) {

      final MidiDevice device = iterator.next();

      info[counter] = device.getDeviceInfo();

      counter += 1;

    }

    return info;

  }

  /** 
   * Gets the MidiDevice specified by the supplied MidiDevice.Info structure
   * 
   * @param	info	The specifications of the device we wish to get
   * 
   * @return			The required MidiDevice
   * 
   * @throws			IllegalArgumentException if the device is not one that we provided
   * 
   * @see javax.sound.midi.spi.MidiDeviceProvider#getDevice(javax.sound.midi.MidiDevice.Info)
   * 
   */

  @Override
  public MidiDevice getDevice(MidiDevice.Info info) throws IllegalArgumentException {

    if ( !isDeviceSupported(info) ) {

      throw new IllegalArgumentException();

    }

    return midiProperties.deviceMap.get(((CoreMidiDeviceInfo) info).getEndPointUniqueID());

  }

  /**
   * Checks to see if the required device is supported by this MidiDeviceProvider
   * 
   * @param	info	The specifications of the device we wish to check
   * 
   * @return 			true if the device is supported, otherwise false
   * 
   * @see javax.sound.midi.spi.MidiDeviceProvider#isDeviceSupported(javax.sound.midi.MidiDevice.Info)
   * 
   */

  @Override
  public boolean isDeviceSupported(final MidiDevice.Info info) {

    boolean foundDevice = false;

    // The device map must be created and the info object must be a CoreMIDIDeviceInfo object 
    if ( ( midiProperties.deviceMap != null ) && ( info instanceof CoreMidiDeviceInfo ) ) {

      // Search for the device info UID within the device map
      if (midiProperties.deviceMap.containsKey(((CoreMidiDeviceInfo)info).getEndPointUniqueID())) {

        foundDevice = true;

      }

    }

    return foundDevice;

  }

  /**
   * Called when a change in the MIDI environment occurs
   *
   * @throws CoreMidiException if a problem occurs rebuilding the map of available MIDI devices
   *
   */

  public void midiSystemUpdated() throws CoreMidiException {

    // Update the device map
    buildDeviceMap();

  }

  /**
   * Adds a notification listener to the listener set maintained by this class. If it is a
   * {@link CoreMidiDeviceProvider}, only keep the most recent one, since Java will create many,
   * and we only want to update the device map once when the MIDI environment changes.
   * 
   * @param listener	The CoreMidiNotification listener to add
   * 
   * @throws 					CoreMidiException if it is not possible to provide change notifications
   * 
   */

  public static void addNotificationListener(CoreMidiNotification listener) throws CoreMidiException {

    // If the dynamic library failed to load, we cannot provide notifications
    if (!isLibraryLoaded()) {

      throw new CoreMidiException("libCoreMidi4J.dylib could not be loaded, CoreMIDI4J is not active.");

    }

    // If the client has not been initialised then we need to setup the static fields in the class
    if (midiProperties.client == null) {

      new CoreMidiDeviceProvider().initialise();

    }

    midiProperties.client.addNotificationListener(listener);

  }

  /**
   * Removes a notification listener from the listener list maintained by this class
   * 
   * @param listener	The CoreMidiNotification listener to remove
   * 
   * @throws 					CoreMidiException when we are unable to offer change notifications
   * 
   */

  public static void removeNotificationListener(CoreMidiNotification listener) throws CoreMidiException {

    // If the dynamic library failed to load, we cannot provide notifications
    if (!isLibraryLoaded()) {

      throw new CoreMidiException("libCoreMidi4J.dylib could not be loaded, CoreMIDI4J is not active.");

    }

    // If the client has not been initialised then we need to setup the static fields in the class
    if (midiProperties.client == null) {

      new CoreMidiDeviceProvider().initialise();

    }

    midiProperties.client.removeNotificationListener(listener);

  }

  /**
   * Check whether we have been able to load the native library.
   *
   * @return true if the library was loaded successfully, and we are operational, and false if the library was
   *         not available, so we are idle and not going to return any devices or post any notifications.
   *
   * @throws CoreMidiException if something unexpected happens trying to load the native library on a Mac OS X system.
   */
  
  public static boolean isLibraryLoaded() throws CoreMidiException {

    return Loader.isAvailable();

  }

  /**
   * Determine the version of the library which is being used.
   *
   * @return the implementation version of the library, as compiled into the JAR manifest.
   * @since 0.9
   */

  public static String getLibraryVersion() {

    return Package.getPackage("uk.co.xfactorylibrarians.coremidi4j").getImplementationVersion();

  }

  /**
   * Obtains an array of information objects representing the set of all working MIDI devices available on the system.
   * This is a replacement for javax.sound.midi.MidiSystem.getMidiDeviceInfo(), and only returns fully-functional
   * MIDI devices. If you call it on a non-Mac system, it simply delegates to the javax.sound.midi implementation.
   * On a Mac, it calls that function, but filters out the broken devices, returning only the replacement versions
   * that CoreMidi4J provides. So by using this method rather than the standard one, you can give your users a
   * menu of MIDI devices which are guaranteed to properly support MIDI System Exclusive messages.
   *
   * A returned information object can then be used to obtain the corresponding device object,
   * by invoking javax.sound.midi.MidiSystem.getMidiDevice().
   *
   * @return an array of MidiDevice.Info objects, one for each installed and fully-functional MIDI device.
   *         If no such devices are installed, an array of length 0 is returned.
   */

  public static MidiDevice.Info[] getMidiDeviceInfo() {

    MidiDevice.Info[] allInfo = MidiSystem.getMidiDeviceInfo();

    try {

      if (isLibraryLoaded()) {

        List<MidiDevice.Info> workingDevices = new ArrayList<>(allInfo.length);
        
        for (MidiDevice.Info candidate : allInfo) {

          try {

            MidiDevice device = MidiSystem.getMidiDevice(candidate);

            if ( (device instanceof Sequencer) || 
                 (device instanceof Synthesizer) ||
                 (device instanceof CoreMidiDestination) || 
                 (device instanceof CoreMidiSource) ) {

              workingDevices.add(candidate);  // A working device, include it

            }
          
          } catch (MidiUnavailableException e) {

            System.err.println("Problem obtaining MIDI device which supposedly exists:" + e.getMessage());

          }
       
        }

        return workingDevices.toArray(new MidiDevice.Info[workingDevices.size()]);

      }

    } catch (CoreMidiException e) {

      System.err.println("Problem trying to determine native library status:" + e.getMessage());

    }

    return allInfo;

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
   * Gets the number of sources supported by the system
   * 
   * @return	The number of sources supported by the system
   * 
   */

  private native int getNumberOfSources();

  /**
   * Gets the number of destinations supported by the system
   * 
   * @return	The number of destinations supported by the system
   * 
   */

  private native int getNumberOfDestinations();

  /**
   * Gets the specified MIDI Source EndPoint
   * 
   * @param sourceIndex	The index of the source to get
   * 
   * @return 						The specified MIDI Source EndPoint
   * 
   * @throws 						CoreMidiException if the source index is not valid 
   * 
   */

  private native int getSource(int sourceIndex) throws CoreMidiException;

  /**
   * Gets the specified MIDI Destination EndPoint
   * 
   * @param destinationIndex	The index of the destination to get
   * 
   * @return 									The specified MIDI Destination EndPoint
   * 
   * @throws 									CoreMidiException if the destination index is not valid 
   * 
   */

  private native int getDestination(int destinationIndex) throws CoreMidiException;

  /**
   * Gets the unique ID for an object reference
   * 
   * @param reference 	The reference to the object to get the UID for 
   * 
   * @return						The UID of the referenced object
   * 
   * @throws 						CoreMidiException if there is a problem communicating with CoreMIDI
   * 
   */

  private native int getUniqueID(int reference) throws CoreMidiException; 

  /**
   * Gets a MidiDevice.Info class for the specified reference
   * 
   * @param reference	The Core MIDI reference to create a MidiDevice.Info class for
   * 
   * @return					The created MidiDevice.Info class
   * 
   * @throws 					CoreMidiException if the reference is not valid
   * 
   */

  private native CoreMidiDeviceInfo getMidiDeviceInfo(int reference) throws CoreMidiException;

}
