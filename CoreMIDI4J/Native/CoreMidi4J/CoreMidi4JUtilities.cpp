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

  jclass Exception = env->FindClass("uk/co/xfactorylibrarians/coremidi4j/CoreMidiException");
  char *message = SafeCFStringCopyToCString(string);
  env->ThrowNew(Exception, message);
  free(message);

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
char * SafeCFStringCopyToCString(CFStringRef aString) {
  
  if (aString == NULL) {
    
    return NULL;
    
  }

  CFIndex length = CFStringGetLength(aString);
  CFIndex maxSize = CFStringGetMaximumSizeForEncoding(length, CFStringGetSystemEncoding()) + 1;
  char *buffer = (char *)malloc(maxSize);
  
  if (CFStringGetCString(aString, buffer, maxSize, CFStringGetSystemEncoding())) {
 
    return buffer;
  
  }
  
  free(buffer); // We failed
  
  return NULL;
  
}

/*
 * Utilities for acquiring endpoint names (resolving connections if existing)
 */

static CFStringRef CreateEndpointName(MIDIEndpointRef endpoint, bool isExternal);

/*
 * Some endpoint names contain illegal characters in their CFStringRef form (e.g. '\0')
 * Convert to and from a C-string in order to clean these up.
 */
OSStatus MIDIObjectGetStringPropertyClean(MIDIObjectRef obj, CFStringRef propertyID, CFStringRef __nullable * __nonnull str) {
  
  CFStringRef raw = NULL;
  OSStatus status = MIDIObjectGetStringProperty(obj, propertyID, &raw);

  if (status == noErr && raw != NULL) {
    
    char *cString = SafeCFStringCopyToCString(raw);

    CFRelease(raw);

    if (cString != NULL) {

      *str = CFStringCreateWithCString(kCFAllocatorDefault, cString, kCFStringEncodingUTF8);
      free(cString);

      if (*str != NULL) {

        return noErr;

      }

    }

  }
  
  *str = NULL;
  return -1;
  
}

CFStringRef CreateConnectedEndpointName(MIDIEndpointRef endpoint) {
  
  CFMutableStringRef result = CFStringCreateMutable(NULL, 0);
  CFStringRef str;
  OSStatus err;
  
  // Does the endpoint have connections?
  CFDataRef connections = NULL;
  int nConnected = 0;
  bool anyStrings = false;
  err = MIDIObjectGetDataProperty(endpoint, kMIDIPropertyConnectionUniqueID, &connections);
  
  if (connections != NULL) {
    
    // It has connections, follow them
    // Concatenate the names of all connected devices
    nConnected = static_cast<int>(CFDataGetLength(connections) / sizeof(MIDIUniqueID));
    
    if (nConnected) {
      
      const SInt32 *pid = reinterpret_cast<const SInt32 *>(CFDataGetBytePtr(connections));
      
      for (int i = 0; i < nConnected; ++i, ++pid) {
        
        MIDIUniqueID id = EndianS32_BtoN(*pid);
        MIDIObjectRef connObject;
        MIDIObjectType connObjectType;
        err = MIDIObjectFindByUniqueID(id, &connObject, &connObjectType);
        
        if (err == noErr) {
          
          if (connObjectType == kMIDIObjectType_ExternalSource  ||
              connObjectType == kMIDIObjectType_ExternalDestination) {
            
            // Connected to an external device's endpoint (10.3 and later).
            str = CreateEndpointName(static_cast<MIDIEndpointRef>(connObject), true);
          
          } else {
            
            // Connected to an external device (10.2) (or something else, catch-all)
            str = NULL;
            MIDIObjectGetStringPropertyClean(connObject, kMIDIPropertyName, &str);
          
          }
          
          if (str != NULL) {
            
            if (anyStrings) {
              
              CFStringAppend(result, CFSTR(", "));
              
            } else {
              
              anyStrings = true;
            
            }
          
            CFStringAppend(result, str);
            CFRelease(str);
          
          }
        
        }
      
      }
    
    }
    
    CFRelease(connections);
    
  }
  
  if (anyStrings) {
  
    return result;
    
  } else {
    
    CFRelease(result);

  }
  
  // Here, either the endpoint had no connections, or we failed to obtain names for any of them.
  return CreateEndpointName(endpoint, false);

}

/*
 * Obtain the name of an endpoint without regard for whether it has connections.
 * The result should be released by the caller.
 */

static CFStringRef CreateEndpointName(MIDIEndpointRef endpoint, bool isExternal) {
  
  CFMutableStringRef result = CFStringCreateMutable(NULL, 0);
  CFStringRef str;
  
  // begin with the endpoint's name
  str = NULL;
  
  MIDIObjectGetStringPropertyClean(endpoint, kMIDIPropertyName, &str);
  
  if (str != NULL) {
    
    CFStringAppend(result, str);
    CFRelease(str);
    
  }
  
  MIDIEntityRef entity = NULL;
  MIDIEndpointGetEntity(endpoint, &entity);
  
  if (entity == 0) {
    
    return result; // probably virtual
    
  }
    
  if (CFStringGetLength(result) == 0) {
    
    // endpoint name has zero length -- try the entity
    str = NULL;
    
    MIDIObjectGetStringPropertyClean(entity, kMIDIPropertyName, &str);
    
    if (str != NULL) {
      
      CFStringAppend(result, str);
      CFRelease(str);
      
    }
    
  }
  
  // now consider the device's name
  MIDIDeviceRef device = NULL;
  MIDIEntityGetDevice(entity, &device);
  if (device == 0) return result;
  
  str = NULL;
  MIDIObjectGetStringPropertyClean(device, kMIDIPropertyName, &str);
  
  if (str != NULL) {
    
    // if an external device has only one entity, throw away
    // the endpoint name and just use the device name
    
    if (isExternal && MIDIDeviceGetNumberOfEntities(device) < 2) {
      
      CFRelease(result);
      return str;
      
    } else {
      
      // does the entity name already start with the device name?
      // (some drivers do this though they shouldn't)
      // if so, do not prepend
      if (CFStringCompareWithOptions(str /* device name */,
                                     result /* endpoint name */,
                                     CFRangeMake(0, CFStringGetLength(str)), 0) != kCFCompareEqualTo) {
        
        // prepend the device name to the entity name
        if (CFStringGetLength(result) > 0) {
          
          CFStringInsert(result, 0, CFSTR(" "));
          
        }
        
        CFStringInsert(result, 0, str);
        
      }
      
      CFRelease(str);
      
    }
    
  }
  
  return result;
  
}

