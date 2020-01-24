#!/bin/sh
# This script sets things up so the xcode build can find the JNI
# headers in the Java SDK.
rm -f Native/CoreMidi4J/java_home
ln -s `/usr/libexec/java_home` Native/CoreMidi4J/java_home
