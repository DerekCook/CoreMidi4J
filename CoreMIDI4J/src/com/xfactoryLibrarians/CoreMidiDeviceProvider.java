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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.spi.MidiDeviceProvider;

/**
 * The OS X CoreMIDI Device Provider
 *
 */

public class CoreMidiDeviceProvider extends MidiDeviceProvider implements CoreMidiNotification {
	
  private static final int BUFFER_SIZE = 2048;

  private static final String DEVICE_NAME_PREFIX = "CoreMidi - ";

  private static final int DEVICE_MAP_SIZE = 20;

  private static final class MidiProperties {
      
  	private CoreMidiClient client;
  	private CoreMidiOutputPort output;
  	private Map<Integer, MidiDevice> deviceMap = new LinkedHashMap<Integer, MidiDevice>(DEVICE_MAP_SIZE);
  	
  }

  private static final MidiProperties midiProperties = new MidiProperties();
  
  /**
   * Initialises the system
   * 
   * @throws CoreMidiException
   * 
   */
  
  private static void initialise() throws CoreMidiException {
  	
  	midiProperties.client = new CoreMidiClient("Core MIDI Provider");
  	midiProperties.output = midiProperties.client.outputPortCreate("Core Midi Provider Output");
  	buildDeviceMap();

  }

  /**
   * Class constructor
   * 
   * @throws CoreMidiException
   * 
   */
  
  public CoreMidiDeviceProvider() throws CoreMidiException {
  	
  	if ( midiProperties.client == null ) {
  	
  		initialise();

  	}
  	
		midiProperties.client.addNotificationListener(this);
		
  }

  /**
   * Builds the device map
   * 
   * @throws CoreMidiException
   * 
   */
  
  private static void buildDeviceMap() throws CoreMidiException {

  	int count = getNumberOfSources();
  	
  	for (int i = 0; i < count; i++) {

  		final int endPointReference = getSource(i);

  		final int uniqueID = getUniqueID(endPointReference);
  		
  		if ( midiProperties.deviceMap.containsKey(uniqueID) == false ) {
  			
  			CoreMidiSource source = new CoreMidiSource(getMidiDeviceInfo(endPointReference));
  			
  			midiProperties.deviceMap.put(uniqueID,source);
  		
  		}

  	}
  	
  	count = getNumberOfDestinations();
  	
  	for (int i = 0; i < count; i++) {
  		
  		final int endPointReference = getDestination(i);
  		
  		final int uniqueID = getUniqueID(endPointReference);

  		if ( midiProperties.deviceMap.containsKey(uniqueID) == false ) {
  			
  			CoreMidiDestination destination = new CoreMidiDestination(getMidiDeviceInfo(endPointReference));

  			midiProperties.deviceMap.put(uniqueID, destination);
  		
  		}
  		
  	}
  	
  }

  /**
   * Gets the CoreMidiClient object 
   * 
   * @return	The CoreMidiClient object 
   * 
   * @throws 	CoreMidiException
   * 
   */
  
  static CoreMidiClient getMIDIClient() throws CoreMidiException {
  	
  	if (midiProperties.client == null) {
  		
  		initialise();
  		
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
  	
  	if (midiProperties.output == null) {
  		
  		try {
  			
				initialise();
				
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
	public Info[] getDeviceInfo() {
		
		
		if (midiProperties.deviceMap == null) {
			
			return new MidiDevice.Info[0];
			
		}
		
		final MidiDevice.Info[] info = new MidiDevice.Info[midiProperties.deviceMap.size()];
		
		final Iterator<MidiDevice> iterator = midiProperties.deviceMap.values().iterator();

		int counter = 0;
		
		while (iterator.hasNext()) {
		
			final MidiDevice device = iterator.next();
			
			info[counter++] = (CoreMidiDeviceInfo) device.getDeviceInfo();
			
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
	 * @throws			IllegalArgumentException
	 * 
	 * @see javax.sound.midi.spi.MidiDeviceProvider#getDevice(javax.sound.midi.MidiDevice.Info)
	 * 
	 */
	
	@Override
	public MidiDevice getDevice(Info info) throws IllegalArgumentException {

		if ( isDeviceSupported(info) == false ) {
			
			throw new IllegalArgumentException();
			
		}

		return (MidiDevice) midiProperties.deviceMap.get(((CoreMidiDeviceInfo) info).getUniqueID());

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

		if ( ( midiProperties.deviceMap != null ) && ( info instanceof CoreMidiDeviceInfo ) ) {

			if (midiProperties.deviceMap.containsKey(((CoreMidiDeviceInfo)info).getUniqueID())) {

				foundDevice = true;

			}

		}

		return foundDevice;
			
	}

	/**
	 * Called when a notification occurs
	 * 
	 * @throws CoreMidiException
	 * 
	 */
	
	public void midiSystemUpdated() throws CoreMidiException {
		
		// Clear the map and rebuild it
		midiProperties.deviceMap.clear();
		buildDeviceMap();
		
	}
	
  /**
   * Adds a notification listener to the listener list maintained by this class
   * 
   * @param listener	The CoreMidiNotification listener to add
   * 
   * @throws 					CoreMidiException 
   * 
   */
  
  public static void addNotificationListener(CoreMidiNotification listener) throws CoreMidiException {
  	
  	if (midiProperties.client == null) {
  		
  		initialise();
  		
  	}
  	
  	midiProperties.client.addNotificationListener(listener);
  	
  }
  
  /**
   * Removes a notification listener from the listener list maintained by this class
   * 
   * @param listener	The CoreMidiNotification listener to remove
   * 
   * @throws 					CoreMidiException 
   * 
   */
  
  public static void removedNotificationListener(CoreMidiNotification listener) throws CoreMidiException {
  	
  	if (midiProperties.client == null) {
  		
  		initialise();
  		
  	}
  	
  	midiProperties.client.removeNotificationListener(listener);
  	
  }
  
  
  //////////////////////////////
	///// JNI Interfaces
  //////////////////////////////
	
	/**
	 * Static method for loading the native library 
	 * 
	 */
	
  static {
  	
    System.loadLibrary("CoreMIDI4J");
      
  }
  
	/**
	 * Gets the number of sources supported by the system
	 * 
	 * @return	The number of sources supported by the system
	 * 
	 */
  
  public static native int getNumberOfSources();
  
	/**
	 * Gets the number of destinations supported by the system
	 * 
	 * @return	The number of destinations supported by the system
	 * 
	 */
  
  public static native int getNumberOfDestinations();
  
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
  
  public static native int getSource(int sourceIndex) throws CoreMidiException;
  
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
  
  public static native int getDestination(int destinationIndex) throws CoreMidiException;
  
  /**
   * Gets the unique ID for an object reference
   * 
   * @param reference 	the reference to the object to get the UID for 
   * 
   * @return						the UID of the referenced object
   * 
   * @throws CoreMidiException 
   * 
   */
  
  public static native int getUniqueID(int reference) throws CoreMidiException;
  

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
  
  public static native CoreMidiDeviceInfo getMidiDeviceInfo(int reference) throws CoreMidiException;

}
