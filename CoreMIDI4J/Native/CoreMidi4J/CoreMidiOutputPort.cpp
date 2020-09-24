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

#include "CoreMidiOutputPort.h"

/////////////////////////////////////////////////////////
// Native functions for CoreMidiOutputPort
/////////////////////////////////////////////////////////

/*
 * Creates a MIDI Output port
 *
 * Class:     com_coremidi4j_CoreMidiOutputPort
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

JNIEXPORT jint JNICALL Java_uk_co_xfactorylibrarians_coremidi4j_CoreMidiOutputPort_createOutputPort(JNIEnv *env, jobject obj, jint clientReference, jstring portName) {

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
 * Class:     com_coremidi4j_CoreMidiOutputPort
 * Method:    sendMidiMessage
 * Signature: (IILjavax/sound/midi/MidiMessage;)V
 *
 * @param env                   The JNI environment
 * @param obj                   The reference to the java object instance that called this native method
 * @param outputPortReference   The reference of the output port to use
 * @param endPointReference     The reference of the destination end point to send the message to
 * @param timestamp             The time in microseconds at which the message should take effect, with 0 meaning now
 * @param midiMessage           The message to send
 *
 * @throws                      CoreMidiException if the OSStatus code from MIDISend is non zero
 *
 */

JNIEXPORT void JNICALL Java_uk_co_xfactorylibrarians_coremidi4j_CoreMidiOutputPort_sendMidiMessage(JNIEnv *env, jobject obj, jint outputPortReference, jint endPointReference, jobject midiMessage, jlong timestamp) {

  static mach_timebase_info_data_t sTimebaseInfo;  // Will hold conversion factor for timestamps

  // If this is the first time we've run, get the timebase.
  // We can use denom == 0 to indicate that sTimebaseInfo is
  // uninitialised because it makes no sense to have a zero
  // denominator in a fraction.
  if ( sTimebaseInfo.denom == 0 ) {

    (void) mach_timebase_info(&sTimebaseInfo);

  }

  OSStatus status = 0;

  int messageLength;
  int bufferLength;
  signed char *messageData;
  jobject mvdata;

  // Find the class definitions that we need
  jclass mmClass = env->FindClass("javax/sound/midi/MidiMessage");

  // Get the message length
  messageLength = env->GetIntField(midiMessage, env->GetFieldID(mmClass,"length","I"));

  // Calculate the length of the buffer, allow some extra space for CoreMIDI data that may be added to the packet list.
  bufferLength = 1000 + messageLength;

  // Get the message data
  mvdata = env->GetObjectField(midiMessage, env->GetFieldID(mmClass,"data","[B"));
  jbyteArray *array = reinterpret_cast<jbyteArray*>(&mvdata);
  messageData = env->GetByteArrayElements(*array, NULL);

  // Allocate the buffer
  char *buffer = (char *) malloc(bufferLength);

  // Convert timestamp from microseconds to Mach Absolute Time Units unless it is zero meaning "now"
  uint64_t coreTimestamp = (timestamp == 0) ? 0 : ((timestamp * sTimebaseInfo.denom) / sTimebaseInfo.numer) * 1000;

  // Check for success
  if ( buffer != NULL ) {

    MIDIPacketList *packets = (MIDIPacketList *)buffer;

    MIDIPacket *packet = MIDIPacketListInit(packets);
    if (messageData[0] == -9) {

      // Java represents continuations of incomplete SysEx messages as having a status code of 0xf7, which we need to strip off.
      MIDIPacketListAdd(packets, bufferLength, packet, coreTimestamp, messageLength - 1, (Byte *) messageData + 1);

    } else {

      MIDIPacketListAdd(packets, bufferLength, packet, coreTimestamp, messageLength, (Byte *) messageData);  // A normal message.

    }

    status = MIDISend(outputPortReference, endPointReference, packets);

    free(buffer);

  } else {

    ThrowException(env,CFSTR("MIDISend - Memory Allocation Fail"),-1);

  }

  // And release the array
  env->ReleaseByteArrayElements(*array, messageData, 0);

  // Thow an exception if the status is non-zero
  if ( status != 0) {

    ThrowException(env,CFSTR("MIDISend"),status);

  }

}
