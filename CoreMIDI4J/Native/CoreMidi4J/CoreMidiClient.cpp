/**
 * Title:        CoreMIDI4J
 * Description:  Core MIDI Device Provider for Java on OS X
 * Copyright:    Copyright (c) 2015-2016
 * Company:      x.factory Librarians
 *
 * @author Derek Cook, James Elliott
 *
 * CoreMIDI4J is an open source Service Provider Interface for supporting external MIDI devices on MAC OS X
 *
 * This file is part of the XCODE project that provides the native implementation of CoreMIDI4J
 *
 * CREDITS - This library uses principles established by OSXMIDI4J, but converted so it operates at the JNI level with no additional libraries required
 *
 */

#include "CoreMidiClient.h"

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Global variable data
//////////////////////////////////////////////////////////////////////////////////////////////////////////////

// This is retained so we can free the allocated memory when the client is disposed of
MIDI_CALLBACK_PARAMETERS *g_callbackParameters;

/////////////////////////////////////////////////////////
// Callback function for CoreMidiClient
/////////////////////////////////////////////////////////

/*
 * The native callback that is called when a MIDINotification message is received
 *
 * @param message					A pointer to the MIDINotification message.
 * @param notifyRefCon		The refCon pointer provided to MIDIClientCreate
 *
 */

void notifyCallback(const MIDINotification *message, void *notifyRefCon) {

  // Message IDs (enum MIDINotificationMessageID)
  //      1 kMIDIMsgSetupChanged
  //      2 kMIDIMsgObjectAdded
  //      3 kMIDIMsgObjectRemoved
  //      4 kMIDIMsgPropertyChanged
  //
  //  In experimenting with notification of changes, you get four messages, in the sequence 4, 3, 3, 1 when removing an interface and 4, 2, 2, 1 when adding an interface
  //  I am guessing that the duplicate 3, 3 or 2, 2 is because the deves in question have a source and a destination.
  //
  //  I think we only need to react to one message, the final one, not all four, assuming that we will use this as a trigger to rescan the interfaces.

  // DEBUG CODE Uncomment the following if you wish to see the messages within the native client
  //std::cout << "MIDI Notify Message " << message->messageID << std::endl;

  if ( message->messageID == kMIDIMsgSetupChanged ) {

    JNIEnv *env;

    // Cast the supplied handle to the right data type
    MIDI_CALLBACK_PARAMETERS *callbackParameters = (MIDI_CALLBACK_PARAMETERS *) notifyRefCon;

    // Attach this thread to the JVM
    int attachResult = callbackParameters->jvm->AttachCurrentThread((void**) &env, NULL);

    // DEBUG Code, uncomment to view parameters
    //std::cout << "** javaNotifyCallback: AttachCurrentThread: ";
    //printJniStatus(attachResult);

    if ( attachResult == JNI_OK) {

      // Call the Java callback to pass the MIDI data to Java
      env->CallVoidMethod(callbackParameters->object, callbackParameters->methodID,127);

      // Check for and describe any exceptions
      if ( env->ExceptionCheck() ) {

        env->ExceptionDescribe();

      }

      // TODO Calling this function crashes the JVM!!!
      //			callbackParameters->jvm->DetachCurrentThread();

    } else {

      ThrowException(env,CFSTR("Notify Callback"),attachResult);

    }

  }

}

/////////////////////////////////////////////////////////
// Native functions for CoreMidiClient
/////////////////////////////////////////////////////////

/*
 * Creates a MIDI client reference
 *
 * Class:     com_coremidi4j_CoreMidiClient
 * Method:    createClient
 * Signature: ()I
 *
 * @param env   The JNI environment
 * @param obj   The reference to the java object instance that called this native method
 *
 * @return      A reference to the created client
 *
 * @throws      CoreMidiException if the client cannot be created
 *
 */

JNIEXPORT jint JNICALL Java_uk_co_xfactorylibrarians_coremidi4j_CoreMidiClient_createClient(JNIEnv *env, jobject obj, jstring clientName) {

  __block MIDIClientRef client;
  __block OSStatus status;
  __block CFStringRef cfClientName = CFStringCreateWithCString(NULL,env->GetStringUTFChars(clientName,0),kCFStringEncodingMacRoman);

  // Allocate memory for the callback parameters
  g_callbackParameters = (MIDI_CALLBACK_PARAMETERS *) malloc(sizeof(MIDI_CALLBACK_PARAMETERS));

  // Throw exception if memory allocation failed
  if ( g_callbackParameters == NULL ) {

    ThrowException(env,CFSTR("MIDIClientCreate"),-1);

  }

  // Cache the information needed for the callback, note we obtain a global reference to the CoreMidiClient object
  g_callbackParameters->object = env->NewGlobalRef(obj);
  g_callbackParameters->methodID =  env->GetMethodID(env->GetObjectClass(obj), "notifyCallback", "()V");
  jint result = env->GetJavaVM(&g_callbackParameters->jvm);

  // DEBUG Code, uncomment to view parameters
  //std::cout << "** createClient: object   : " << g_callbackParameters->object << std::endl;
  //std::cout << "** createClient: methodID : " << g_callbackParameters->methodID << std::endl;
  //std::cout << "** createClient: JVM      : " << g_callbackParameters->jvm << std::endl;

  //Ensure that the last call succeeded
  assert (result == JNI_OK);

  // Setup the client on the GCD Event Queue - this will allow it to receive notifications
  dispatch_sync(dispatch_get_main_queue(), ^{
    status = MIDIClientCreate(cfClientName, &notifyCallback, g_callbackParameters, &client);
  });

  // If status is non-zero then throw an exception
  if ( status != 0) {

    ThrowException(env,CFSTR("MIDIClientCreate"),status);

  }

  // Return the created client
  return client;

}

/*
 * Disposes of a MIDI client reference
 *
 * Class:     com_coremidi4j__CoreMidiClient
 * Method:    disposeClient
 * Signature: (I)V
 *
 * @param env               The JNI environment
 * @param obj               The reference to the java object instance that called this native method
 * @param clientReference   The client reference to release and dispose
 *
 * @throws                  CoreMidiException if the client cannot be disposed
 *
 */

JNIEXPORT void JNICALL Java_uk_co_xfactorylibrarians_coremidi4j_CoreMidiClient_disposeClient(JNIEnv *env, jobject obj, jint clientReference) {

  std::cout << "** disposeClient: ENV      : " << env << std::endl;

  OSStatus status;

  // Release the client reference
  status = MIDIClientDispose(clientReference);

  // Release the global reference to the Java object
  env->DeleteGlobalRef(g_callbackParameters->object);

  // Free up the allocated memory
  if ( g_callbackParameters != NULL ) {

    free(g_callbackParameters);

  }

  // Thow an exception if the status is non-zero
  if ( status != 0) {

    ThrowException(env,CFSTR("MIDIClientDispose"),status);

  }

}

