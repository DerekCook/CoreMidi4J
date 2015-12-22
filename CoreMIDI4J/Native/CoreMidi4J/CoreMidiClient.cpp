/**
 * Title:        CoreMIDI4J - CoreMidiClient
 * Description:  Implementation of the native functions for the CoreMidiClient class
 * Copyright:    Copyright (c) 2015
 * Company:      x.factory Librarians
 * @author       Derek Cook
 *
 * This is part of the native side of my Core MIDI Service Provider Interface for Java on OS X, inplemented as an XCODE C++ DYLIB project
 *
 */

#include "CoreMidiClient.h"

/////////////////////////////////////////////////////////
// Native functions for CoreMidiClient
/////////////////////////////////////////////////////////

/*
 * Creates a MIDI client reference
 *
 * Class:     com_xfactoryLibrarians_CoreMidiClient
 * Method:    createClient
 * Signature: ()I
 *
 * @param env                   The JNI environment
 * @param obj                   The reference to the java object instance that called this native method
 *
 * @return                      A reference to the created client
 *
 * @throws                      CoreMidiException if the client cannot be created
 *
 */

JNIEXPORT jint JNICALL Java_com_xfactoryLibrarians_CoreMidiClient_createClient(JNIEnv *env, jobject obj, jstring clientName) {
    
    MIDIClientRef client;
    OSStatus status;
    
    // Create a CFStringRef from the portName jstring
    const char *clientNameString = env->GetStringUTFChars(clientName,0);
    CFStringRef cfClientName = CFStringCreateWithCString(NULL,clientNameString,kCFStringEncodingMacRoman);
        
    // Create the MIDI client
    status = MIDIClientCreate(cfClientName, NULL, NULL, &client);
    
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
 * @param env                   The JNI environment
 * @param obj                   The reference to the java object instance that called this native method
 * @param clientReference       The client reference to release and dispose
 *
 * @throws                      CoreMidiException if the client cannot be disposed
 *
 */

JNIEXPORT void JNICALL Java_com_xfactoryLibrarians_CoreMidiClient_disposeClient(JNIEnv *env, jobject obj, jint clientReference) {
    
    OSStatus status;
    
    // Release the client reference
    status = MIDIClientDispose(clientReference);
    
    // Thow an exception if the status is non-zero
    if ( status != 0) {
        
        ThrowException(env,CFSTR("MIDIClientDispose"),status);
        
    }
    
}

