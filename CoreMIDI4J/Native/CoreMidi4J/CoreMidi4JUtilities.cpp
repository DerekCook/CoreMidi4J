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

#include "CoreMidi4JUtilities.h"

/*
 * Helper function for throwing exceptions to Java
 *
 * @param env        The JNI environment
 * @param function   The name of the function that caused the error
 * @param status     The status code  which provides details about the error
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
    
	jclass Exception = env->FindClass("com/coremidi4j/CoreMidiException");
	env->ThrowNew(Exception,CFStringGetCStringPtr(CFStringCreateCopy(NULL, string), kCFStringEncodingMacRoman ));
    
}

/*
 * Helper function that prints out JNI Status Codes
 *
 * @param status	The status value to print
 *
 */

void printJniStatus(int status) {
	
	switch (status) {
			
		case JNI_OK:
			std::cout << "JNI_OK - success";
			break;
			
		case JNI_ERR:
			std::cout << "JNI_ERR - unknown error";
			break;
			
		case JNI_EDETACHED:
			std::cout << "JNI_EDETACHED - thread detached from the VM";
			break;
			
		case JNI_EVERSION:
			std::cout << "JNI_EVERSION - JNI version error";
			break;
			
		case JNI_ENOMEM:
			std::cout << "JNI_ENOMEM - not enough memory";
			break;
			
		case JNI_EEXIST:
			std::cout << "JNI_EEXIST - VM already created";
			break;
			
		case JNI_EINVAL:
			std::cout << "JNI_EINVAL - invalid arguments";
			break;
			
		default:
			std::cout << "!! Undefined Error Code";
			break;
			
	}
	
	std::cout << std::endl;
	
}

