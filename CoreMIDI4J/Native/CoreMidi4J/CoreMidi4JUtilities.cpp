/**
 * Title:        CoreMIDI4J - CoreMidi4JUtilities
 * Description:  Utility functions used by other classes
 * Copyright:    Copyright (c) 2015
 * Company:      x.factory Librarians
 * @author       Derek Cook
 *
 * This is part of the native side of my Core MIDI Service Provider Interface for Java on OS X, inplemented as an XCODE C++ DYLIB project
 *
 */


#include "CoreMidi4JUtilities.h"

/*
 * Helper function for throwing exceptions to Java
 *
 * @param env        The JNI environment
 * @param function   The name of the function that caused the error
 * @param status     The status code  which provides details about the error
 *
 * TODO - At some point it might be useful to decode the error here. Could also store more information in the exception?
 *
 */

void ThrowException(JNIEnv *env, CFStringRef function, OSStatus status) {
    
	CFMutableStringRef string = CFStringCreateMutable(NULL, 0);
    
	// Create the exception string
	if (string) {
        
		CFStringAppend(string, CFSTR("Exception in CoreMIDI JNI Library by \""));
		CFStringAppend(string, function);
		CFStringAppend(string, CFSTR("\" - OS STatus Code: "));
		CFStringAppend(string, CFStringCreateWithFormat(NULL, NULL, CFSTR("%8.8x"), status));
        
	}
    
	jclass Exception = env->FindClass("com/xfactoryLibrarians/CoreMidiException");
	env->ThrowNew(Exception,CFStringGetCStringPtr(CFStringCreateCopy(NULL, string), kCFStringEncodingMacRoman ));
    
}

