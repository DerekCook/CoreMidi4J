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

#include "CoreMidiInputPort.h"

/////////////////////////////////////////////////////////
// Callback functions for CoreMidiInputPort
/////////////////////////////////////////////////////////

/*
 * The native callback that is called when a MIDI message is received
 *
 * @param packets         A list of MIDI packets that have been received.
 * @param readProcRefCon  The refCon passed to MIDIInputPortCreate
 * @param srcConnRefCon   The refCon passed to MIDIPortConnectSource
 *
 */

void MIDIInput(const MIDIPacketList *packets, void *readProcRefCon, void *srcConnRefCon) {

  static mach_timebase_info_data_t sTimebaseInfo;  // Will hold conversion factor for timestamps
  JNIEnv *env;

  // If this is the first time we've run, get the timebase.
  // We can use denom == 0 to indicate that sTimebaseInfo is
  // uninitialised because it makes no sense to have a zero
  // denominator in a fraction.
  if ( sTimebaseInfo.denom == 0 ) {
    
    (void) mach_timebase_info(&sTimebaseInfo);
    
  }

  // Cast the supplied reference to the correct data type
  MIDI_CALLBACK_PARAMETERS *callbackParameters = (MIDI_CALLBACK_PARAMETERS *) srcConnRefCon;

  // Get a JNIEnv reference from the cached JVM
  int getEnvStat = callbackParameters->jvm->GetEnv((void **) &env, NULL);

  // If the ENV is not attached to the current thread then attach it
  if ( getEnvStat == JNI_EDETACHED ) {

    if ( callbackParameters->jvm->AttachCurrentThread((void **) &env, NULL) != 0) {

      std::cout << "Failed to attach" << std::endl;
      ThrowException(env,CFSTR("MIDIInput - Failed to attach"),-1);

    }

  } else if ( getEnvStat == JNI_EVERSION ) {

    std::cout << "GetEnv: version not supported" << std::endl;
    ThrowException(env,CFSTR("MIDIInput - GetEnv: version not supported"),-1);

  }

  // Loop over all the packets we have received, calling the Java callback for each one.
  MIDIPacket *packet = (MIDIPacket *) &packets->packet[0];

  for ( int i = 0; i < packets->numPackets; i += 1 ) {

    // Convert the CoreMIDI timestamp from Mach Absolute Time Units to microseconds,
    // as expected by Java MIDI, unless the value was zero, which means "now".
    // The conversion is based on Apple Tech Q&A 1398,
    // https://developer.apple.com/library/mac/qa/qa1398/_index.html
    //
    // Because we are converting to microseconds rather than nanoseconds, we can start
    // by dividing by 1000, which should eliminate the risk of overflow described in the
    // comment below (copied in from the Q&A), which evidently should not have been an issue
    // until 584.9 years after the most recent system boot anyway, according to
    // http://lists.apple.com/archives/darwin-kernel/2012/Sep/msg00008.html
    //
    // Do the maths. We hope that the multiplication doesn't
    // overflow; the price you pay for working in fixed point.
    uint64_t timestamp = (packet->timeStamp == 0) ? 0 : (packet->timeStamp / 1000) * sTimebaseInfo.numer / sTimebaseInfo.denom;

    // Create a java array from the MIDIPacket
    jbyteArray array = env->NewByteArray(packet->length);
    env->SetByteArrayRegion(array, 0, packet->length, (jbyte*) packet->data);

    // Call the Java callback to pass the MIDI data to Java
    env->CallVoidMethod(callbackParameters->object, callbackParameters->methodID, timestamp, packet->length, array);

    // Release the array once we are finished with it
    env->ReleaseByteArrayElements(array, NULL, JNI_ABORT);

    // Check for and describe any exceptions
    if ( env->ExceptionCheck() ) {

      env->ExceptionDescribe();

    }

    // Move on to the next packet
    packet = MIDIPacketNext(packet);

  }

  // And finally detach the thread
  callbackParameters->jvm->DetachCurrentThread();

}

/////////////////////////////////////////////////////////
// Native functions for CoreMidiInputPort
/////////////////////////////////////////////////////////

/*
 * Creates the MIDI Input Port
 *
 * Class:     com_coremidi4j_CoreMidiInputPort
 * Method:    createInputPort
 * Signature: (ILjava/lang/String;)I
 *
 * @param env                   The JNI environment
 * @param obj                   The reference to the java object instance that called this native method
 * @param clientReference       The MIDI Client used to create the port
 * @param portName              The name of the input port
 *
 * @return                      A reference to the created input port
 *
 * @throws                      CoreMidiException if the input port cannot be created
 *
 */

JNIEXPORT jint JNICALL Java_uk_co_xfactorylibrarians_coremidi4j_CoreMidiInputPort_createInputPort(JNIEnv *env, jobject obj, jint clientReference, jstring portName) {

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
 * Class:     com_coremidi4j_CoreMidiInputPort
 * Method:    midiPortConnectSource
 * Signature: (ILuk/co/xfactorylibrarians/coremidi4j/CoreMidiSource;)V
 *
 * @param env                      The JNI environment
 * @param obj                      The reference to the java object instance that called this native method
 * @param inputPortReference       The reference of the input point that we wish to connect the end point to
 * @param sourceDevice             The reference of the source device
 *
 * @throws                         CoreMidiException if the output port cannot be created
 *
 */

JNIEXPORT jlong JNICALL Java_uk_co_xfactorylibrarians_coremidi4j_CoreMidiInputPort_midiPortConnectSource(JNIEnv *env, jobject obj, jint inputPortReference, jobject sourceDevice) {

  OSStatus status;

  // Allocate memory for the callback parameters
  MIDI_CALLBACK_PARAMETERS *callbackParameters = (MIDI_CALLBACK_PARAMETERS *) malloc(sizeof(MIDI_CALLBACK_PARAMETERS));

  // Throw exception if memory allocation failed
  if ( callbackParameters == NULL ) {

    ThrowException(env,CFSTR("MIDIPortConnectSource"),-1);

  }

  // Cache the information needed for the callback, noting that we obtain a l=global reference for the CoreMidiInputPortObject
  callbackParameters->object = env->NewGlobalRef(sourceDevice);
  callbackParameters->methodID =  env->GetMethodID(env->GetObjectClass(sourceDevice), "messageCallback", "(JI[B)V");
  jint result = env->GetJavaVM(&callbackParameters->jvm);

  //Ensure that the last call succeeded
  assert (result == JNI_OK);

  // Get the info object from the sourceDevice object
  jobject info = env->GetObjectField(sourceDevice, env->GetFieldID(env->GetObjectClass(sourceDevice), "info","Luk/co/xfactorylibrarians/coremidi4j/CoreMidiDeviceInfo;"));

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
 * Class:     com_coremidi4j_CoreMidiInputPort
 * Method:    midiPortConnectSource
 * Signature: (ILuk/co/xfactorylibrarians/coremidi4j/CoreMidiSource;)V
 *
 * @param env                      The JNI environment
 * @param obj                      The reference to the java object instance that called this native method
 * @param inputPortReference       The reference of the input point that we wish to connect the end point to
 * @param memoryReference          The memory handle that can now be released.
 * @param sourceDevice             The reference of the source device
 *
 * @throws                         CoreMidiException if the output port cannot be created
 *
 */

JNIEXPORT void JNICALL Java_uk_co_xfactorylibrarians_coremidi4j_CoreMidiInputPort_midiPortDisconnectSource(JNIEnv *env, jobject obj, jint inputPortReference, jlong memoryReference, jobject sourceDevice) {

  OSStatus status;

  // get the info object from the sourceDevice object
  jobject info = env->GetObjectField(sourceDevice, env->GetFieldID(env->GetObjectClass(sourceDevice), "info","Luk/co/xfactorylibrarians/coremidi4j/CoreMidiDeviceInfo;"));

  // Get the endpoint reference from the info object
  int sourceEndPointReference = env->GetIntField(info, env->GetFieldID(env->GetObjectClass(info),"endPointReference","I"));

  // Disconnect this input port from the end port
  status = MIDIPortDisconnectSource(inputPortReference, sourceEndPointReference);

  // Delete the global reference to the Java CoreMidiInputPort object
  env->DeleteGlobalRef(((MIDI_CALLBACK_PARAMETERS *) memoryReference)->object);

  //std::cout << "Trying to release " << memoryReference << std::endl;

  // Release the memory block that Java_uk_co_xfactorylibrarians_coremidi4j_CoreMidiInputPort_midiPortConnectSource allocated
  free((void *) memoryReference);

  // If the returned status is non zero then throw an exception
  if ( status != 0) {

    ThrowException(env,CFSTR("MIDIPortDisconnectSource"),status);

  }

}
