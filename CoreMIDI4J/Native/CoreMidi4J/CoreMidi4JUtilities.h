/**
 * Title:        CoreMIDI4J
 * Description:  Core MIDI Device Provider for Java on OS X
 * Copyright:    Copyright (c) 2015-2016
 * Company:      x.factory Librarians
 *
 * @author Derek Cook
 *
 * CoreMIDI4J is an open source Service Provider Interface for supporting external MIDI devices on MAC OS X
 *
 * This file is part of the XCODE project that provides the native implementation of CoreMIDI4J
 *
 * CREDITS - This library uses principles established by OSXMIDI4J, but converted so it operates at the JNI level with no additional libraries required
 *
 */

#ifndef CoreMidi4J_CoreMidi4JUtilities_h
#define CoreMidi4J_CoreMidi4JUtilities_h

#include <iostream>

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

/*
 * Helper function that prints out JNI Status Codes
 *
 * @param status	The status value to print
 *
 */

void printJniStatus(int status);

/*
 * Safely obtains a C string pointer from a CFStringRef. We must do it this way, because CFStringGetCStringPtr
 * is free to return NULL for a variety of reasons, including simply not having an efficient way to respond.
 * However, this means that it is the responsibility of the caller to free() the returned pointer when it is
 * no longer needed, unless we returned NULL.
 *
 * @param aString  The CFStringRef
 *
 * @return         A newly allocated C string holding the contents of aString, or NULL
 *
 */

char * SafeCFStringCopyToCString(CFStringRef aString);


//////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Type Definitions
//////////////////////////////////////////////////////////////////////////////////////////////////////////////

typedef struct midiCallBackParameters {

  JavaVM   *jvm;       // The JVM Reference
  jobject   object;    // The Java object that will be called
  jmethodID methodID;  // The Java messageCallback method that will be called

} MIDI_CALLBACK_PARAMETERS;

#endif
