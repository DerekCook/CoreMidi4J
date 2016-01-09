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

#include "CoreMidiSource.h"

/*
 * Obtains the current system time in microseconds.
 *
 * Class:     uk_co_xfactorylibrarians_coremidi4j_CoreMidiSource
 * Method:    getMicroSecondTime
 * Signature: ()J
 *
 * @return    The current system time in microseconds.
 *
 */

JNIEXPORT jlong JNICALL Java_uk_co_xfactorylibrarians_coremidi4j_CoreMidiSource_getMicroSecondTime(JNIEnv *, jobject) {
    
  static mach_timebase_info_data_t sTimebaseInfo;  // Will hold conversion factor for timestamps
  
  // If this is the first time we've run, get the timebase.
  // We can use denom == 0 to indicate that sTimebaseInfo is
  // uninitialised because it makes no sense to have a zero
  // denominator in a fraction.
  if ( sTimebaseInfo.denom == 0 ) {
        
    (void) mach_timebase_info(&sTimebaseInfo);
        
  }
    
  uint64_t currentMachTime = mach_absolute_time();
    
  // Convert the timestamp from Mach Absolute Time Units to microseconds,
  // as expected by Java MIDI. The first step is based on Apple Tech Q&A 1398,
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
  uint64_t timestamp = (currentMachTime / 1000) * sTimebaseInfo.numer / sTimebaseInfo.denom;
    
  return timestamp;
    
}

