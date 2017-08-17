/*
 * Title:        CoreMIDI4J
 * Description:  Core MIDI Device Provider for Java on OS X
 * Copyright:    Copyright (c) 2015-2016
 * Company:      x.factory Librarians
 *
 * @author Derek Cook
 * 
 * CoreMIDI4J is an open source Service Provider Interface for supporting external MIDI devices on MAC OS X
 * 
 * CREDITS - This library uses principles established by OSXMIDI4J, but converted so it operates at the JNI level with no additional libraries required
 * 
 */

package uk.co.xfactorylibrarians.coremidi4j;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CoreMidiReceiver - used to receive data from the application and send it to the connected device.
 *
 */

public class CoreMidiReceiver implements MidiDeviceReceiver {

  private final CoreMidiDestination device;
  private final AtomicBoolean closed = new AtomicBoolean(false);


  /**
   * CoreMidiReceicer constructor
   * 
   * @param device	The MIDI device that contains the information required to send MIDI data via OSX core MIDI
   */

  CoreMidiReceiver(final CoreMidiDestination device) {

    this.device = device;

  }

  /** 
   * Sends a MIDI message
   * 
   * @see javax.sound.midi.Receiver#send(javax.sound.midi.MidiMessage, long)
   * 
   */

  @Override
  public void send(MidiMessage message, long timeStamp) {

    if ( closed.get() == true ) {

      throw new IllegalStateException("Can't call send() with a closed receiver");

    }

    if ( device.isOpen() == false ) {

      throw new IllegalStateException("Can't call send with a receiver attached to a device that is not open: " + device);

    }

    try {

      // Convert from Java-oriented port-relative microsecends to CoreMIDI-oriented boot-relative microseconds,
      // and from signed Java semantics of -1 meaning now to unsigned CoreMIDI semantics of 0 meaning now.
      final long coreTimestamp = (timeStamp == -1) ? 0 : timeStamp + device.getStartTime();

      CoreMidiDeviceProvider.getOutputPort().send(((CoreMidiDeviceInfo)device.getDeviceInfo()).getEndPointReference(), message, coreTimestamp);

    } catch (CoreMidiException e) {

      e.printStackTrace();

    }

  }

  /** 
   * Closes the MIDI Receiver
   * 
   * @see javax.sound.midi.Receiver#close()
   * 
   */

  @Override
  public void close() {

    if ( closed.compareAndSet(false, true) == true ) {

      device.receiverClosed(this);

    }

  }

  /**
   * Gets the MIDI Device that this receiver is attached to
   * 
   * @return the MIDI Device that this receiver is attached to
   * 
   */

  @Override
  public MidiDevice getMidiDevice() {

    return device;

  }

}
