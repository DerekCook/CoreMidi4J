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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CoreMidiClient class
 *
 */

public class CoreMidiClient {

  private final int midiClientReference;

  private final Set<CoreMidiNotification> notificationListeners =
          Collections.newSetFromMap(new ConcurrentHashMap<CoreMidiNotification, Boolean>());

  /**
   * Keeps track of the latest {@link CoreMidiDeviceProvider} added to our listener list; this is the only one that
   * we want to call when the MIDI environment changes, since we only need to update the device map once, and Java
   * creates a vast number of instances of our device provider.
   */
  private CoreMidiNotification mostRecentDeviceProvider = null;

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
   * @return			A new CoreMidiInputPort
   * 
   * @throws 			CoreMidiException if the port cannot be created
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
   * @throws 			CoreMidiException if the port cannot be created
   * 
   */

  public CoreMidiOutputPort outputPortCreate(final String name) throws CoreMidiException {

    return new CoreMidiOutputPort(midiClientReference,name);

  }

  /**
   * Used to make sure we are only running one callback delivery loop at a time without having to serialize the process
   * in a way that will block the actual CoreMidi callback.
   */

  private final AtomicBoolean runningCallbacks = new AtomicBoolean(false);

  /**
   * Used to count the number of CoreMidi environment change callbacks we have received, so that if additional ones
   * come in while we are delivering callback messages to our listeners, we know to start another round at the end.
   */

  private final AtomicInteger callbackCount = new AtomicInteger( 0);

  /**
   * Check whether we are already in the process of delivering callbacks to our listeners; if not, start a background
   * thread to do so, and at the end of that process, see if additional callbacks were attempted while it was going on.
   */

  private void deliverCallbackToListeners() {

    final int initialCallbackCount = callbackCount.get();

    if (runningCallbacks.compareAndSet(false, true)) {

      new Thread(new Runnable() {

        @Override
        public void run() {

          try {

            int currentCallbackCount = initialCallbackCount;
            while ( currentCallbackCount > 0 ) {  // Loop until no new callbacks occur while delivering a set.

              // Iterate through the listeners (if any) and call them to advise that the environment has changed.
              final Set<CoreMidiNotification> listeners = Collections.unmodifiableSet(new HashSet<>(notificationListeners));

              // First notify the CoreMidiDeviceProvider object itself, so that the device map is
              // updated before any other listeners, from client code, are called.
              if (mostRecentDeviceProvider != null) {

                try {

                  mostRecentDeviceProvider.midiSystemUpdated();

                } catch (CoreMidiException e) {

                  throw new RuntimeException("Problem delivering MIDI environment change notification to CoreMidiDeviceProvider" , e);

                }

              }

              // Finally, notify any registered client code listeners, now that the device map is properly up to date.
              for ( CoreMidiNotification listener : listeners ) {

                try {

                  listener.midiSystemUpdated();

                } catch (CoreMidiException e) {

                  throw new RuntimeException("Problem delivering MIDI environment change notification" , e);

                }

              }

              synchronized (CoreMidiClient.this) {

                // We have handled however many callbacks occurred before this iteration started
                currentCallbackCount = callbackCount.addAndGet( -currentCallbackCount );

              }

            }

          } finally {

            runningCallbacks.set(false);   // We are terminating.

          }

        }

      }).start();

    }
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

    synchronized(this) {

      callbackCount.incrementAndGet();  // Record that a callback has come in.
      deliverCallbackToListeners();  // Try to deliver callback notifications to our listeners.

    }

  }

  /**
   * Adds a notification listener to the listener set maintained by this class
   * 
   * @param listener	The CoreMidiNotification listener to add
   * 
   */

  public void addNotificationListener(CoreMidiNotification listener) {

    if ( listener != null ) {

      // Our CoreMidiDeviceProvider is a special case, we only want to notify a single instance of that, even though
      // Java keeps creating new ones. So keep track of the most recent instance registered, do not add it to the list.
      if (listener instanceof CoreMidiDeviceProvider) {

        mostRecentDeviceProvider = listener;

      } else {

        notificationListeners.add(listener);

      }

    }

  }

  /**
   * Removes a notification listener from the listener set maintained by this class
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
