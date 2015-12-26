/**
 * Title:        CoreMIDI4J - CoreMidiDeviceProvider
 * Description:  Implementation of the native functions for the CoreMidiDeviceProvider class
 * Copyright:    Copyright (c) 2015
 * Company:      x.factory Librarians
 * @author       Derek Cook
 *
 * This is part of the native side of my Core MIDI Service Provider Interface for Java on OS X, inplemented as an XCODE C++ DYLIB project
 *
 */

#include "CoreMidiDeviceProvider.h"

/////////////////////////////////////////////////////////
// Native functions for CoreMidiDeviceProvider
/////////////////////////////////////////////////////////

/*
 * Gets the number of MIDI sources provided by the Core MIDI system
 *
 * Class:     com_xfactoryLibrarians_CoreMidiDeviceProvider
 * Method:    getNumberOfSources
 * Signature: ()I
 *
 * @param env		The JNI environment
 * @param obj   The reference to the java object instance that called this native method
 *
 * @return      The number of MIDI sources provided by the Core MIDI system
 *
 */

JNIEXPORT jint JNICALL Java_com_xfactoryLibrarians_CoreMidiDeviceProvider_getNumberOfSources(JNIEnv *env, jobject obj) {
    
	return (jint) MIDIGetNumberOfSources();
    
}

/*
 * Gets the number of MIDI destinations provided by the Core MIDI system
 *
 * Class:     com_xfactoryLibrarians_CoreMidiDeviceProvider
 * Method:    getNumberOfDestinations
 * Signature: ()I
 *
 * @param env    The JNI environment
 * @param obj    The reference to the java object instance that called this native method
 *
 * @return       The number of MIDI destinations provided by the Core MIDI system
 *
 */

JNIEXPORT jint JNICALL Java_com_xfactoryLibrarians_CoreMidiDeviceProvider_getNumberOfDestinations(JNIEnv *env, jobject obj) {
    
	return (jint) MIDIGetNumberOfDestinations();
    
}

/*
 * Gets the specified Core MIDI Source
 *
 * Class:     com_xfactoryLibrarians_CoreMidiDeviceProvider
 * Method:    getSource
 * Signature: (I)I
 *
 * @param env            The JNI environment
 * @param obj            The reference to the java object instance that called this native method
 * @param sourceIndex    The index of the MIDI source to get
 *
 * @return               The specified Core MIDI Source
 *
 * @throws               CoreMidiEXception if the source index is not valid
 *
 */

JNIEXPORT jint JNICALL Java_com_xfactoryLibrarians_CoreMidiDeviceProvider_getSource(JNIEnv *env, jobject obj, jint sourceIndex) {
    
	if ( sourceIndex >= MIDIGetNumberOfSources() ) {
        
		ThrowException(env,CFSTR("MIDIGetSource"),sourceIndex);
        
	}
    
	return MIDIGetSource(sourceIndex);
    
}

/*
 * Gets the specified Core MIDI Destination
 *
 * Class:     com_xfactoryLibrarians_CoreMidiDeviceProvider
 * Method:    getDestination
 * Signature: (I)I
 *
 * @param env                   The JNI environment
 * @param obj                   The reference to the java object instance that called this native method
 * @param destinationIndex      The index of the MIDI destination to get
 *
 * @return                      The specified Core MIDI Destination
 *
 * @throws                      CoreMidiEXception if the destination index is not valid
 *
 */

JNIEXPORT jint JNICALL Java_com_xfactoryLibrarians_CoreMidiDeviceProvider_getDestination(JNIEnv *env, jobject obj, jint destinationIndex) {
    
	if ( destinationIndex >= MIDIGetNumberOfDestinations() ) {
        
		ThrowException(env,CFSTR("MIDIGetDestination"), destinationIndex);
        
	}
    
	return MIDIGetDestination(destinationIndex);
    
}

/*
 * Gets the unique ID (UID) of the specified end point
 *
 * Class:     com_xfactoryLibrarians_CoreMidiDeviceProvider
 * Method:    getUniqueID
 * Signature: (I)I
 *
 * @param env                   The JNI environment
 * @param obj                   The reference to the java object instance that called this native method
 * @param endPointReference     The end point reference
 *
 * @return                      The unique ID (UID) of the specified end point
 *
 */

JNIEXPORT jint JNICALL Java_com_xfactoryLibrarians_CoreMidiDeviceProvider_getUniqueID(JNIEnv *env, jobject obj, jint endPointReference) {
    
	SInt32 uid = 0;
    
	MIDIObjectGetIntegerProperty(endPointReference, kMIDIPropertyUniqueID, &uid);
    
	return uid;
    
}

/*
 * Creates and gets a MidiDevice.Info object for the specified end point reference
 *
 * Class:     com_xfactoryLibrarians_CoreMidiDeviceProvider
 * Method:    getMidiDeviceInfo
 * Signature: (I)Ljavax/sound/midi/MidiDevice/Info;
 *
 * @param env                   The JNI environment
 * @param obj                   The reference to the java object instance that called this native method
 * @param endPointReference     The end point reference
 *
 * @return                      A MidiDevice.Info object for the specified end point reference
 *
 */

JNIEXPORT jobject JNICALL Java_com_xfactoryLibrarians_CoreMidiDeviceProvider_getMidiDeviceInfo(JNIEnv *env, jobject obj, jint endPointReference) {
    
	// Find the Java CoreMIDIDeviceInfo class and methods we need
	jclass javaClass = env->FindClass("com/xfactoryLibrarians/CoreMidiDeviceInfo");
	jmethodID constructor = env->GetMethodID(javaClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;III)V");
    
	CFStringRef name;
	CFStringRef deviceName;
	CFStringRef	description;
	CFStringRef manufacturer;
	SInt32 version;
	SInt32 uid;
    
	// Get the device properties
	MIDIObjectGetStringProperty(endPointReference, kMIDIPropertyName, &name);
	MIDIObjectGetStringProperty(endPointReference, kMIDIPropertyName, &deviceName);
	MIDIObjectGetStringProperty(endPointReference, kMIDIPropertyModel, &description);
	MIDIObjectGetStringProperty(endPointReference, kMIDIPropertyManufacturer, &manufacturer);
	MIDIObjectGetIntegerProperty(endPointReference, kMIDIPropertyDriverVersion, &version);
	MIDIObjectGetIntegerProperty(endPointReference, kMIDIPropertyUniqueID, &uid);
    
	CFMutableStringRef buildName = CFStringCreateMutable(NULL, 0);
    
	// Add "Core MIDI " to our device name if we can
	if (buildName) {
        
		CFStringAppend(buildName, CFSTR("Core MIDI - "));
		CFStringAppend(buildName, name);
        
		// Overwrite
		deviceName = CFStringCreateCopy(NULL, buildName);
        
		CFRelease(buildName);
        
	}
    
	// Create the Java Object
	jobject info = env->NewObject(javaClass,
																constructor,
																env->NewStringUTF(CFStringGetCStringPtr(deviceName, kCFStringEncodingMacRoman)),
																env->NewStringUTF(CFStringGetCStringPtr(manufacturer, kCFStringEncodingMacRoman)),
																env->NewStringUTF(CFStringGetCStringPtr(description, kCFStringEncodingMacRoman)),
																version,
																endPointReference,
																uid);
    
	return info;
    
}

