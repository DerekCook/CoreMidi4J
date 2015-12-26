/**
 * Title:        CoreMIDI4J - CoreMidiInputPort
 * Description:  Implementation of the native functions for the CoreMidiInputPort class
 * Copyright:    Copyright (c) 2015
 * Company:      x.factory Librarians
 * @author       Derek Cook
 *
 * This is part of the native side of my Core MIDI Service Provider Interface for Java on OS X, inplemented as an XCODE C++ DYLIB project
 *
 */

#include "CoreMidiInputPort.h"

/////////////////////////////////////////////////////////
// Native functions for CoreMidiInputPort
/////////////////////////////////////////////////////////

/*
 * The Java callback that is called when a MIDI message is received
 *
 * @param packet              The MIDI Message extracted from the list of messages passed to the native callback
 * @param readProcRefCon      The refCon passed to MIDIInputPortCreate
 * @param callbackParameters  The refCon passed to MIDIPortConnectSource
 *
 */

void javaMidiMessageCallback(const MIDIPacket *packet, void *readProcRefCon, MIDI_CALLBACK_PARAMETERS *callbackParameters) {
    
	JNIEnv *env;
    
	// Get a JNIEnv reference from the cached JVM
	int getEnvStat = callbackParameters->jvm->GetEnv((void **) &env, NULL);
    
	// If the ENV is not attached to the current thread then attach it
	if (getEnvStat == JNI_EDETACHED) {
        
		if ( callbackParameters->jvm->AttachCurrentThread((void **) &env, NULL) != 0) {
            
			std::cout << "Failed to attach" << std::endl;
            
		}
        
	} else if (getEnvStat == JNI_OK) {
        
		// Do Nothing
        
	} else if (getEnvStat == JNI_EVERSION) {
        
		std::cout << "GetEnv: version not supported" << std::endl;
        
	}
    
	// Create a java array from the MIDIPacket
	jbyteArray array = env->NewByteArray(packet->length);
	env->SetByteArrayRegion(array, 0, packet->length, (jbyte*) packet->data);
    
	// Call the Java callback to pass the MIDI data to Java
	env->CallVoidMethod(callbackParameters->object, callbackParameters->methodID,packet->length,array);
    
	// Release the array once we are finished with it
	env->ReleaseByteArrayElements(array, NULL, JNI_ABORT);
    
	// Check for and describe any exceptions
	if ( env->ExceptionCheck() ) {
        
		env->ExceptionDescribe();
        
	}
    
	// And finally detach the thread
	callbackParameters->jvm->DetachCurrentThread();
    
}

/*
 * The native callback that is called when a MIDI message is received
 *
 * @param packets         A list of MIDI packets that have been received.
 * @param readProcRefCon  The refCon passed to MIDIInputPortCreate
 * @param srcConnRefCon   The refCon passed to MIDIPortConnectSource
 *
 */

void MIDIInput (const MIDIPacketList *packets, void *readProcRefCon, void *srcConnRefCon) {
	
	MIDIPacket *packet = (MIDIPacket *) &packets->packet[0];
	
	for (int i = 0; i < packets->numPackets; ++i) {
		
		// Call the Java callback function
		javaMidiMessageCallback(packet,readProcRefCon, (MIDI_CALLBACK_PARAMETERS *) srcConnRefCon);
		
		packet = MIDIPacketNext(packet);
	
	}
	
}

/*
 * Creates the MIDI Input Port
 *
 * Class:     com_xfactoryLibrarians_CoreMidiInputPort
 * Method:    createInputPort
 * Signature: (ILjava/lang/String;)I
 *
 * @param env              The JNI environment
 * @param obj              The reference to the java object instance that called this native method
 * @param clientReference  The MIDI Client used to create the port
 * @param portName         The name of the input port
 *
 * @return                 A reference to the created input port
 *
 * @throws                 CoreMidiException if the input port cannot be created
 *
 */

JNIEXPORT jint JNICALL Java_com_xfactoryLibrarians_CoreMidiInputPort_createInputPort(JNIEnv *env, jobject obj, jint clientReference, jstring portName) {
    
	MIDIPortRef inputPort;
	OSStatus status;
    
	// Create a CFStringRef from the portName jstring
	const char *portNameString = env->GetStringUTFChars(portName,0);
	CFStringRef cfPortName = CFStringCreateWithCString(NULL,portNameString,kCFStringEncodingMacRoman);
    
	// Create the MIDI Input port
    
	status = MIDIInputPortCreate(clientReference, cfPortName, MIDIInput, NULL, &inputPort);
    
	// Relase the allocated string
	env->ReleaseStringUTFChars(portName, portNameString);
    
	// If the returned status is non zero then throw an exception
	if ( status != 0) {
        
		ThrowException(env,CFSTR("MIDIInputPortCreate"),status);
        
	}
    
	// Finally, return the reference
	return inputPort;
    
}

/*
 * Connects a source endpoint to the specified input port. At this point we also cache the data that will be needed by the C++ to Java callback
 *
 * Class:     com_xfactoryLibrarians_CoreMidiInputPort
 * Method:    midiPortConnectSource
 * Signature: (ILcom/xfactoryLibrarians/CoreMidiSource;)V
 *
 * @param env                   The JNI environment
 * @param obj                   The reference to the java object instance that called this native method
 * @param inputPortReference    The reference of the input point that we wish to connect the end point to
 * @param sourceDevice          The reference of the source device
 *
 * @throws                      CoreMidiException if the output port cannot be created
 *
 */

JNIEXPORT jlong JNICALL Java_com_xfactoryLibrarians_CoreMidiInputPort_midiPortConnectSource(JNIEnv *env, jobject obj, jint inputPortReference, jobject sourceDevice) {
    
	OSStatus status;
    
	// Allocate memory for the callback parameters
	MIDI_CALLBACK_PARAMETERS *callbackParameters = (MIDI_CALLBACK_PARAMETERS *) malloc(sizeof(MIDI_CALLBACK_PARAMETERS));
    
	// Throw exception if memory allocation failed
	if ( callbackParameters == NULL ) {
        
		ThrowException(env,CFSTR("MIDIPortConnectSource"),-1);
        
	}
    
	// Cache the information needed for the callback
	callbackParameters->object = env->NewGlobalRef(sourceDevice);
	callbackParameters->methodID =  env->GetMethodID(env->GetObjectClass(sourceDevice), "messageCallback", "(I[B)V");
	jint result = env->GetJavaVM(&callbackParameters->jvm);
    
	//Ensure that the last call succeeded
	assert (result == JNI_OK);
    
	// Get the info object from the sourceDevice object
	jobject info = env->GetObjectField(sourceDevice, env->GetFieldID(env->GetObjectClass(sourceDevice), "info","Lcom/xfactoryLibrarians/CoreMidiDeviceInfo;"));
    
	// Get the endpoint reference from the info object
	int sourceEndPointReference = env->GetIntField(info, env->GetFieldID(env->GetObjectClass(info),"endPointReference","I"));
    
	// Connect the input port to the source endpoint.
	status = MIDIPortConnectSource(inputPortReference, sourceEndPointReference, callbackParameters);
    
	// If the returned status is non zero then throw an exception
	if ( status != 0) {
        
		ThrowException(env,CFSTR("MIDIPortConnectSource"),status);
        
	}
    
	// And return the pointer to Java so that it can cache it.....
	return (jlong) callbackParameters;
    
}

/*
 * Disconnects a source endpoint from the specified input port. At this point we will also null the cached data
 *
 * Class:     com_xfactoryLibrarians_CoreMidiInputPort
 * Method:    midiPortConnectSource
 * Signature: (ILcom/xfactoryLibrarians/CoreMidiSource;)V
 *
 * @param env                   The JNI environment
 * @param obj                   The reference to the java object instance that called this native method
 * @param inputPortReference    The reference of the input point that we wish to connect the end point to
 * @param memoryReference       The memory handle that can now be released.
 * @param sourceDevice          The reference of the source device
 *
 * @throws                      CoreMidiException if the output port cannot be created
 *
 */

JNIEXPORT void JNICALL Java_com_xfactoryLibrarians_CoreMidiInputPort_midiPortDisconnectSource(JNIEnv *env, jobject obj, jint inputPortReference, jlong memoryReference, jobject sourceDevice) {
    
	OSStatus status;
    
	/* get the info object from the sourceDevice object */
	jobject info = env->GetObjectField(sourceDevice, env->GetFieldID(env->GetObjectClass(sourceDevice), "info","Lcom/xfactoryLibrarians/CoreMidiDeviceInfo;"));
    
	// Get the endpoint reference from the info object
	int sourceEndPointReference = env->GetIntField(info, env->GetFieldID(env->GetObjectClass(info),"endPointReference","I"));
    
	status = MIDIPortDisconnectSource(inputPortReference, sourceEndPointReference);
    
	// Release the memory block that Java_com_xfactoryLibrarians_CoreMidiInputPort_midiPortConnectSource allocated
	free((void *) memoryReference);
    
	// If the returned status is non zero then throw an exception
	if ( status != 0) {
        
		ThrowException(env,CFSTR("MIDIPortDisconnectSource"),status);
        
	}
    
}
