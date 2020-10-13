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
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.midi.*;
import javax.sound.midi.spi.MidiDeviceProvider;

/**
 * The OS X CoreMIDI Device Provider: this is the primary class with which Java itself and user
 * code will interact.
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

      addNotificationListener(this);

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
   * Will hold the thread that is watching for MIDI environment changes on non-macOS platforms if
   * the user has asked to be notified of them by adding a notification listener.
   */

  private static final AtomicReference<Thread> changeScanner = new AtomicReference<>(null);

  /**
   * Holds the interval, in milliseconds, at which we will scan for MIDI environment changes on
   * non-macOS platforms if the user has requested that we report them by adding a notification
   * listener.
   */

  private static final AtomicInteger scanInterval = new AtomicInteger(500);

  /**
   * Controls how often, in milliseconds, the MIDI environment should be examined for changes to report.
   * This will have no effect on macOS, because changes are delivered by CoreMIDI as soon as they occur.
   * It will also have no effect if there are currently no listeners registered via
   * {@link #addNotificationListener(CoreMidiNotification)}. The default interval is 500, or half a second.
   *
   * @param interval how often to check the MIDI environment for changes in the list of available devices, in ms
   * @throws IllegalArgumentException if {@code interval} is less than 10 or more than 60000 (one minute).
   */
  public static void setScanInterval(int interval) {

    if (interval < 10 || interval > 60000) {

      throw new IllegalArgumentException("interval must be between 10 and 60000");

    }

    scanInterval.set(interval);

  }

  /**
   * Check how often, in milliseconds, the MIDI environment should be examined for changes to report.
   * This value is meaningless on macOS, because changes are delivered by CoreMIDI as soon as they occur.
   * It is also irrelevant if there are currently no listeners registered via
   * {@link #addNotificationListener(CoreMidiNotification)}. The default interval is 500, or half a second.
   *
   * @return how often the MIDI environment will be checked for changes on non-macOS systems, in ms
   */

  public static int getScanInterval() {

    return scanInterval.get();

  }

  /**
   * Holds a snapshot of the current devices in the system, used when we are scanning for MIDI environment
   * changes on non-macOS systems. Each device is represented as a list of its device information strings,
   * for ease of comparison, since Sun didn't know how to write comparable data objects yet when the MIDI
   * classes were created.
   */

  private static final AtomicReference<Set<List<String>>> currentDevices = new AtomicReference<>(null);

  /**
   * Build a snapshot of the current MIDI devices in the system. Each device is represented as a list of its
   * device information strings, for ease of comparison, since Sun didn’t know how to write comparable data
   * objects yet when the MIDI classes were created.
   *
   * @return a summary of the current MIDI environment that can be deep-compared by calling {@link Set#equals(Object)}
   */

  private static Set<List<String>> snapshotCurrentEnvironment() {

    Set<List<String>> results = new HashSet<>();

    for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {

      List<String> summary = new LinkedList<>();
      summary.add(info.getName());
      summary.add(info.getDescription());
      summary.add(info.getVendor());
      summary.add(info.getVersion());
      results.add(Collections.unmodifiableList(summary));

    }

    return Collections.unmodifiableSet(results);
  }

  /**
   * This method is run on a daemon thread when we have been asked to report MIDI environment
   * changes and we are not on a macOS system, so CoreMIDI will not deliver them to us.
   */

  private static void watchForChanges() {

    // Keep running as long as we are needed. As soon as there are no listeners left, changeScanner will
    // be set to null. Even if a new listener is then added and a new thread is started up before we wake
    // from our sleep, the comparison below will fail, and we will exit.
    while (changeScanner.get() == Thread.currentThread()) {

      try {

        Thread.sleep(getScanInterval());
        Set<List<String>> newDevices = snapshotCurrentEnvironment();

        if (!newDevices.equals(currentDevices.get())) {  // There has been a change to the MIDI environment.

          currentDevices.set(newDevices);
          deliverCallbackToListeners();

        }
      } catch (Throwable t) {

        System.err.println("Problem while watching for MIDI environment changes: " + t);
        t.printStackTrace(System.err);

      }
    }
  }

  /**
   * Holds the registered listeners that should be notified when the MIDI environment changes.
   *
   */

  private static final Set<CoreMidiNotification> notificationListeners =
          Collections.newSetFromMap(new ConcurrentHashMap<CoreMidiNotification, Boolean>());

  /**
   * Keeps track of the latest {@link CoreMidiDeviceProvider} added to our listener list; this is the only one that
   * we want to call when the MIDI environment changes, since we only need to update the device map once, and Java
   * creates a vast number of instances of our device provider.
   */

  private static CoreMidiNotification mostRecentDeviceProvider = null;

  /**
   * <p>Adds a listener to be notified when the MIDI environment changes. If the current system
   * is not running macOS, then ensure that our watcher daemon thread is running in order to
   * be able to deliver these notifications, since we don't have CoreMIDI to initiate them.</p>
   *
   * <p>Instances of {@link CoreMidiDeviceProvider} register themselves when they are constructed,
   * so they can keep their lists of working CoreMIDI-backed devices up to date. We only keep the
   * most recent one, since the Java MIDI subsystem will create many, and we only want to update
   * the device map once when the MIDI environment changes. This self-registration will also not
   * cause the daemon thread to be started because such updates are only needed on macOS.</p>
   * 
   * @param listener The {@code CoreMidiNotification} listener to add
   *
   * @throws CoreMidiException if there is a problem loading the shared library
   */

  public static synchronized void addNotificationListener(CoreMidiNotification listener) throws CoreMidiException {

    if ( listener != null ) {

      // Our CoreMidiDeviceProvider is a special case, we only want to notify a single instance of that, even though
      // Java keeps creating new ones. So keep track of the most recent instance registered, do not add it to the list.
      if (listener instanceof CoreMidiDeviceProvider) {

        mostRecentDeviceProvider = listener;

      } else {

        notificationListeners.add(listener);

        // If the dynamic library is not loadable, set up our own daemon thread provide notifications.
        if (!isLibraryLoaded() && changeScanner.get() == null) {

          currentDevices.set(snapshotCurrentEnvironment());  // Establish a baseline for our first comparison.

          Thread scanner = new Thread(new Runnable() {
            @Override
            public void run() {
              watchForChanges();
            }
          }, "CoreMidi4J Environment Change Scanner");

          scanner.setDaemon(true);
          changeScanner.set(scanner);
          scanner.start();

        }

      }

    }

  }

  /**
   * Removes a listener that had been receiving notifications of MIDI environment changes. If there are
   * none remaining (other than {@link CoreMidiDeviceProvider} itself) after this operation, arranges
   * for the non-macOS daemon thread to stop running.
   * 
   * @param listener	The CoreMidiNotification listener to remove
   * 
   * @throws 					CoreMidiException when there is a problem interacting with the native library
   *
   */

  public static void removeNotificationListener(CoreMidiNotification listener) throws CoreMidiException {

    notificationListeners.remove(listener);

    if (notificationListeners.isEmpty()) {

      changeScanner.set(null);  // The thread will notice it is no longer desired and gracefully exit.

    }

  }

  /**
   * Used to count the number of CoreMidi environment change callbacks we have received, so that if additional ones
   * come in while we are delivering callback messages to our listeners, we know to start another round at the end.
   */

  static final AtomicInteger callbackCount = new AtomicInteger( 0);

  /**
   * Used to make sure we are only running one callback delivery loop at a time without having to serialize the process
   * in a way that will block the actual CoreMidi callback.
   */

  private static final AtomicBoolean runningCallbacks = new AtomicBoolean(false);

  /**
   * Check whether we are already in the process of delivering callbacks to our listeners; if not, start a background
   * thread to do so, and at the end of that process, see if additional callbacks were attempted while it was going on.
   */

  static synchronized void deliverCallbackToListeners() {

    final int initialCallbackCount = callbackCount.incrementAndGet();

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

                } catch (Throwable t) {

                  System.err.println("Problem delivering MIDI environment change notification to CoreMidiDeviceProvider: " + t);
                  t.printStackTrace(System.err);

                }

              }

              // Finally, notify any registered client code listeners, now that the device map is properly up to date.
              for ( CoreMidiNotification listener : listeners ) {

                try {

                  listener.midiSystemUpdated();

                } catch (Throwable t) {

                  System.err.println("Problem delivering MIDI environment change notification:" + t);
                  t.printStackTrace(System.err);

                }

              }

              synchronized (CoreMidiDeviceProvider.class) {

                // We have handled however many callbacks occurred before this iteration started
                currentCallbackCount = callbackCount.addAndGet( -currentCallbackCount );

                if ( currentCallbackCount < 1 ) {

                  runningCallbacks.set(false);  // We are terminating; if blocked trying to start another, allow it.

                }

              }

            }

          } finally {

            runningCallbacks.set(false);   // Record termination even if we exit due to an uncaught exception.

          }

        }

      }).start();

    }
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
