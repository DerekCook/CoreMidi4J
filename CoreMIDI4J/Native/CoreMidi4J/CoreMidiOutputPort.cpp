/**
 * Title:        CoreMIDI4J - CoreMidiOutputPort
 * Description:  Implementation of the native functions for the CoreMidiOutputPort class
 * Copyright:    Copyright (c) 2015
 * Company:      x.factory Librarians
 * @author       Derek Cook
 *
 * This is part of the native side of my Core MIDI Service Provider Interface for Java on OS X, inplemented as an XCODE C++ DYLIB project
 *
 */


#include "CoreMidiOutputPort.h"

/////////////////////////////////////////////////////////
// Native functions for CoreMidiOutputPort
/////////////////////////////////////////////////////////

/*
 * Creates a MIDI Output port
 *
 * Class:     com_xfactoryLibrarians_CoreMidiOutputPort
 * Method:    createOutputPort
 * Signature: (ILjava/lang/String;)I
 *
 * @param env               The JNI environment
 * @param obj               The reference to the java object instance that called this native method
 * @param clientReference   The MIDI Client used to create the port
 * @param portName          The name of the output port
 *
 * @return                  A reference to the created output port
 *
 * @throws                  CoreMidiException if the output port cannot be created
 *
 */

JNIEXPORT jint JNICALL Java_com_xfactoryLibrarians_CoreMidiOutputPort_createOutputPort(JNIEnv *env, jobject obj, jint clientReference, jstring portName) {
    
	MIDIPortRef outputPort;
	OSStatus status;
    
	// Create a CFStringRef from the portName jstring
	const char *portNameString = env->GetStringUTFChars(portName,0);
	CFStringRef cfPortName = CFStringCreateWithCString(NULL,portNameString,kCFStringEncodingMacRoman);
    
	// Create the MIDI Output port
	status = MIDIOutputPortCreate(clientReference, cfPortName, &outputPort);
    
	// Relase the allocated string
	env->ReleaseStringUTFChars(portName, portNameString);
    
	// If the returned status is non zero then throw an exception
	if ( status != 0) {
        
		ThrowException(env,CFSTR("MIDIOutputPortCreate"),status);
        
	}
    
	// Finally, return the reference
	return outputPort;
    
}


/*
 * Sends a MIDI message to the end point of the device
 *
 * Class:     com_xfactoryLibrarians_CoreMidiOutputPort
 * Method:    sendMidiMessage
 * Signature: (IILjavax/sound/midi/MidiMessage;)V
 *
 * @param env                   The JNI environment
 * @param obj                   The reference to the java object instance that called this native method
 * @param outputPortReference   The reference of the output port to use
 * @param endPointReference     The reference of the destination end point to send the message to
 * @param midiMessage           The message to send
 *
 * @throws                      CoreMidiException if the OSStatus code from MIDISend is non zero
 *
 */

JNIEXPORT void JNICALL Java_com_xfactoryLibrarians_CoreMidiOutputPort_sendMidiMessage(JNIEnv *env, jobject obj, jint outputPortReference, jint endPointReference, jobject midiMessage) {
	
	OSStatus status;
    
	int messageLength;
	signed char *messageData;
	jobject mvdata;
    
	// Find the class definitions that we need
	jclass mmClass = env->FindClass("javax/sound/midi/MidiMessage");
    
	// Get the message length
	messageLength = env->GetIntField(midiMessage, env->GetFieldID(mmClass,"length","I"));
    
	// Get the message data
	mvdata = env->GetObjectField(midiMessage, env->GetFieldID(mmClass,"data","[B"));
	jbyteArray *array = reinterpret_cast<jbyteArray*>(&mvdata);
	messageData = env->GetByteArrayElements(*array, NULL);
	
	// TODO - should we allocate this dynamically to ensure we have a large enough buffer for SYSEX messages?
	// TODO - need to understand better how this part of CoreMidi works?
	char buffer[2048];
	MIDIPacketList *packets = (MIDIPacketList *)buffer;
    
	MIDIPacket *packet = MIDIPacketListInit(packets);
	MIDIPacketListAdd(packets, 2048, packet, 0, messageLength, (Byte *) messageData);
    
	status = MIDISend(outputPortReference, endPointReference, packets);
    
	// And release the array
	env->ReleaseByteArrayElements(*array, messageData, 0);
    
	// Thow an exception if the status is non-zero
	if ( status != 0) {
        
		ThrowException(env,CFSTR("MIDISend"),status);
        
	}
    
}
