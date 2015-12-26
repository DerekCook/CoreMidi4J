/**
 * Title:        CoreMIDI4J - CoreMidiClient
 * Description:  Implementation of the native functions for the CoreMidiClient class
 * Copyright:    Copyright (c) 2015
 * Company:      x.factory Librarians
 * @author       Derek Cook, James Elliott
 *
 * This is part of the native side of my Core MIDI Service Provider Interface for Java on OS X, inplemented as an XCODE C++ DYLIB project
 *
 */

#include "CoreMidiClient.h"


//////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Global variable data
//////////////////////////////////////////////////////////////////////////////////////////////////////////////

// This is retained so we can free the allocated memory when the client is disposed of
static MIDI_CALLBACK_PARAMETERS *g_callbackParameters;

/////////////////////////////////////////////////////////
// Native functions for CoreMidiClient
/////////////////////////////////////////////////////////


/*
 * The Java callback that is called when a MIDINotification message is received
 *
 * @param message             The MIDINotification Message received
 * @param callbackParameters  The refCon passed to MIDIClientCreate
 *
 */

void javaNotifyCallback(const MIDINotification *message, MIDI_CALLBACK_PARAMETERS *callbackParameters) {
	
	JNIEnv *env;
	
	// Get a JNIEnv reference from the cached JVM
	int getEnvStat = callbackParameters->jvm->GetEnv((void **) &env, 0);
	
	// If the ENV is not attached to the current thread then attach it
	if (getEnvStat == JNI_EDETACHED) {
		
		if ( callbackParameters->jvm->AttachCurrentThread((void **) &env, NULL) != 0) {
			
			std::cout << "** javaNotifyCallback: Failed to attach" << std::endl;
			
		}
		
	} else if (getEnvStat == JNI_OK) {
		
		// Do Nothing
		
	} else if (getEnvStat == JNI_EVERSION) {
		
		std::cout << "** javaNotifyCallback: GetEnv: version not supported" << std::endl;
		
	}
	
	// TODO - should this code be in JNI_OK?
	
	// Call the Java callback to pass the MIDI data to Java
	env->CallVoidMethod(callbackParameters->object, callbackParameters->methodID);
	
	// Check for and describe any exceptions
	if ( env->ExceptionCheck() ) {
		
		env->ExceptionDescribe();
		
	}
	
	// And finally detach the thread
	callbackParameters->jvm->DetachCurrentThread();
	
}

/*
 * The native callback that is called when a MIDINotification message is received
 *
 * @param message					A pointer to the MIDINotification message.
 * @param notifyRefCon		The refCon pointer provided to MIDIClientCreate
 *
 */

void notifyCallback(const MIDINotification *message, void *notifyRefCon) {
  
  // IDs (enum MIDINotificationMessageID)
  //      1 kMIDIMsgSetupChanged
  //      2 kMIDIMsgObjectAdded
  //      3 kMIDIMsgObjectRemoved
  //      4 kMIDIMsgPropertyChanged
  //
  //  In experimenting with changes, you get four messages, in the sequence 4, 3, 3, 1 when removing an interface and 4, 2, 2, 1 when adding an interface
  //
  //  I think we only need to react to one message, the final one, not all four, assuming that we will use this as a trigger to rescan the interfaces.
    
  // Uncomment the following if you wish to see the messages within the native client
  //printf("MIDI Notify Message %d.\n",message->messageID);
	
	if ( message->messageID == 1 ) {

		// Notify Java
		javaNotifyCallback(message, (MIDI_CALLBACK_PARAMETERS *) notifyRefCon);
		
	}
	
}

/*
 * Creates a MIDI client reference
 *
 * Class:     com_xfactoryLibrarians_CoreMidiClient
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

JNIEXPORT jint JNICALL Java_com_xfactoryLibrarians_CoreMidiClient_createClient(JNIEnv *env, jobject obj, jstring clientName) {
    
  __block MIDIClientRef client;
  __block OSStatus status;
  __block CFStringRef cfClientName = CFStringCreateWithCString(NULL,env->GetStringUTFChars(clientName,0),kCFStringEncodingMacRoman);
	
	// Allocate memory for the callback parameters
	g_callbackParameters = (MIDI_CALLBACK_PARAMETERS *) malloc(sizeof(MIDI_CALLBACK_PARAMETERS));
	
	// Throw exception if memory allocation failed
	if ( g_callbackParameters == NULL ) {
		
		ThrowException(env,CFSTR("MIDIClientCreate"),-1);
		
	}
	
	// Cache the information needed for the callback
	g_callbackParameters->object = env->NewGlobalRef(obj);
	g_callbackParameters->methodID =  env->GetMethodID(env->GetObjectClass(obj), "notifyCallback", "()V");
	jint result = env->GetJavaVM(&g_callbackParameters->jvm);
	
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
 * Class:     com_xfactoryLibrarians__CoreMidiClient
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

JNIEXPORT void JNICALL Java_com_xfactoryLibrarians_CoreMidiClient_disposeClient(JNIEnv *env, jobject obj, jint clientReference) {
    
  OSStatus status;
    
  // Release the client reference
  status = MIDIClientDispose(clientReference);
	
	if ( g_callbackParameters != NULL ) {
		
		free(g_callbackParameters);
		
	}
    
  // Thow an exception if the status is non-zero
  if ( status != 0) {
        
    ThrowException(env,CFSTR("MIDIClientDispose"),status);
        
  }
    
}

