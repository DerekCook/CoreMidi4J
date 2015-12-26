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

import java.util.ArrayList;
import java.util.List;

/**
 * CoreMidiClient class
 *
 */

public class CoreMidiClient {

  private final int midiClientReference;
  
  private List<CoreMidiNotification> notificationListeners = new ArrayList<CoreMidiNotification>();

  /**
   * Constructor for class
   * 
   * @param name The name of the client		
   * 
   * @throws 			CoreMidiException
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
   * @return			A new CoreMidiInputPort
   * 
   * @throws 			CoreMidiException
   * 
   */
  
  public CoreMidiInputPort inputPortCreate(final String name) throws CoreMidiException {
  	
  	return new CoreMidiInputPort(midiClientReference,name);
  
  }

  /**
   * Creates a new CoreMidiOutputPort
   * 
   * @param name	The name of the port
   * 
   * @return			A new CoreMidiOutputPort
   * 
   * @throws 			CoreMidiException
   * 
   */
  
  public CoreMidiOutputPort outputPortCreate(final String name) throws CoreMidiException {
  	
  	return new CoreMidiOutputPort(midiClientReference,name);
  	
  }
  
  /**
   * The message callback for receiving notifications about changes in the MIDI environment from the JNI code
   * 
   * @throws CoreMidiException 
   * 
   */
    
  public void notifyCallback() throws CoreMidiException {
  	
  	synchronized(this) {
  		
    	// Iterate through the listeners (if any) and call them to advise that the environment has changed    	
  		for (CoreMidiNotification listener : notificationListeners) {

  			listener.midiSystemUpdated();

  		}
  		
  	}
  
  }
  
  /**
   * Adds a notification listener to the listener list maintained by this class
   * 
   * @param listener	The CoreMidiNotification listener to add
   * 
   */
  
  public void addNotificationListener(CoreMidiNotification listener) {
  	
  	// Need to ensure that any CoreMidiDeviceProvider are at the head of the notification list, so that the device map is updated before other listeners are called
  	if ( listener instanceof CoreMidiDeviceProvider ) {
  		
  		notificationListeners.add(0,listener);
  	
  	} else {
  		
  		notificationListeners.add(listener);
  	
  	}
  	
  }
  
  /**
   * Removes a notification listener from the listener list maintained by this class
   * 
   * @param listener	The CoreMidiNotification listener to remove
   * 
   */
  
  public void removeNotificationListener(CoreMidiNotification listener) {
  	
  	notificationListeners.remove(listener);
  	
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
   * Creates the MIDI Client
   * 
   * @param clientName 					The name of the client
   * 
   * @return										A reference to the MIDI client
   * 
   * @throws CoreMidiException	Thrown if the client cannot be created
   */
  
  protected native int createClient(String clientName) throws CoreMidiException;
  
	/**
	 * Disposes of a CoreMIDI Client
	 * 
	 * @param clientReference		The reference of the client to dispose of
	 * 
	 * @throws 									CoreMidiException 
	 * 
	 */
	
  protected native void disposeClient(int clientReference) throws CoreMidiException;
  	
}
