//
//  CoreMidi4JUtilities.h
//  CoreMidi4J
//
//  Created by Derek on 18/12/2015.
//  Copyright (c) 2015 Derek. All rights reserved.
//

#ifndef CoreMidi4J_CoreMidi4JUtilities_h
#define CoreMidi4J_CoreMidi4JUtilities_h

#include "CoreMidi4J.h"
#include <CoreMIDI/CoreMIDI.h>

/*
 * Helper function for throwing exceptions to Java
 *
 * @param env           The JNI environment
 * @param function      The name of the function that caused the error
 * @param status        The status code  which provides details about the error
 *
 * TODO - At some point it might be useful to decode the error here. Could also store more information in the exception?
 *
 */

void ThrowException(JNIEnv *env, CFStringRef function, OSStatus status);

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Type Definitions
//////////////////////////////////////////////////////////////////////////////////////////////////////////////

typedef struct midiCallBackParameters {
    
    JavaVM   *jvm;       // The JVM Reference
    jobject   object;    // The Java object that will be called
    jmethodID methodID;  // The Java messageCallback method that will be called
    
} MIDI_CALLBACK_PARAMETERS;

#endif
