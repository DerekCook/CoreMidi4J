/**
 * Title:        CoreMIDI4J
 * Description:  Core MIDI Device Provider for Java on OS X
 * Copyright:    Copyright (c) 2015-2016
 * Company:      x.factory Librarians
 *
 * @author Derek Cook, James Elliott
 * 
 * CoreMIDI4J is an open source Service Provider Interface for supporting external MIDI devices on MAC OS X
 * 
 * CREDITS - This library uses principles established by OSXMIDI4J, but converted so it operates at the JNI level with no additional libraries required
 * 
 */

package uk.co.xfactorylibrarians.coremidi4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

/**
 * CoreMidiDevice - a MidiDevice implementation for Apple CoreMIDI
 * 
 */

public class CoreMidiSource implements MidiDevice {

  private final CoreMidiDeviceInfo info;
  private boolean isOpen;
  private CoreMidiInputPort input = null;
  private final List<Transmitter> transmitters;

  private int currentMessage = 0;  								// Will contain the status byte (> 127) while gathering a multi-byte message.
  private boolean currentDataIsSingleByte;  			// Is true if currentMessage only needs one byte of data.
  private byte firstDataByte;  										// Will hold the first data byte received when gathering two-byte messages.
  private boolean wasFirstByteReceived = false;  	// Gets set to true when we read first byte of two-byte message.
  private Vector<byte[]> sysexMessageData;  			// Accumulates runs of SYSEX data values until we see the end of message.
  private int sysexMessageLength = 0;  						// Tracks the total SYSEX data length accumulated.
  private long startTime;                         // The system time in microseconds when the port was opened

  /**
   * Default constructor.
   *
   * @param info a CoreMidiDeviceInfo object providing details of the MIDI interface
   * 
   * @throws CoreMidiException
   * 
   */

  CoreMidiSource(CoreMidiDeviceInfo info) throws CoreMidiException {

    this.info = info;
    this.isOpen = false;
    transmitters = new ArrayList<Transmitter>();

  }

  /**
   * Gets the MIDI Info object
   *
   * @return the MIDI Info object, which provides details about the interface
   * 
   */

  @Override
  public Info getDeviceInfo() {

    return info;

  }

  /**
   * Opens the Core MIDI Device
   *
   * @throws MidiUnavailableException
   * 
   */

  @Override
  public void open() throws MidiUnavailableException {

    try {

      // Create the input port if not already created
      if (this.input == null) {

        this.input = CoreMidiDeviceProvider.getMIDIClient().inputPortCreate("Core Midi Provider Input");

      }

      // And connect to it
      this.input.connectSource(this);
      isOpen = true;
      
      // Get the system time in microseconds
      startTime = this.getMicroSecondTime();

    } catch (CoreMidiException e) {

      e.printStackTrace();
      throw new MidiUnavailableException(e.getMessage());

    }

  }

  /**
   * Closes the Core MIDI Device
   * 
   */

  @Override
  public void close() {

    try {

      // If the port is created then disconnect from it
      if (this.input != null) {

        this.input.disconnectSource(this);

      }

      // Clear the transmitter list
      synchronized (transmitters) {

        transmitters.clear();

      }

    } catch (CoreMidiException e) {

      e.printStackTrace();

    } finally {

      // Reset the context data
      isOpen = false;

    }

  }

  /**
   * Checks to see if the MIDI Device is open
   *
   * @return true if the device is open, otherwise false;
   * 
   * @see javax.sound.midi.MidiDevice#isOpen()
   * 
   */

  @Override
  public boolean isOpen() {

    return isOpen;

  }

  /**
   * Obtains the time in microseconds that has elapsed since this MIDI Device was opened.
   *
   * @return the time in microseconds that has elapsed since this MIDI Device was opened.
   * 
   * @see javax.sound.midi.MidiDevice#getMicrosecondPosition()
   * 
   */

  @Override
  public long getMicrosecondPosition() {

    // Return the elapsed time in Microseconds
    return this.getMicroSecondTime() - startTime;

  }

  /**
   * Gets the maximum number of receivers that can be attached to this device.
   *
   * @return the maximum number of receivers that can be attached to this device. This is always 0 as a CoreMidiSource has no receivers
   * 
   * @see javax.sound.midi.MidiDevice#getMaxReceivers()
   * 
   */

  @Override
  public int getMaxReceivers() {

    // A CoreMidiSource has no receivers
    return 0;

  }

  /**
   * Gets the maximum number of transmitters that can be attached to this device.
   *
   * @return the maximum number of transmitters that can be attached to this device. -1 is returned to indicate that the number is unlimited
   * 
   * @see javax.sound.midi.MidiDevice#getMaxTransmitters()
   * 
   */

  @Override
  public int getMaxTransmitters() {

    // Any number of transmitters is supported
    return -1;

  }

  /**
   * Creates and returns a MIDI Receiver for use with this Device
   *
   * @return the created receiver
   * 
   * @see javax.sound.midi.MidiDevice#getReceiver()
   * 
   */

  @Override
  public Receiver getReceiver() throws MidiUnavailableException {

    throw new MidiUnavailableException("CoreMidiSource has no receivers");

  }

  /**
   * Gets a list of receivers connected to the device
   *
   * @return NULL - we do not maintain a list of receivers
   * 
   * @see javax.sound.midi.MidiDevice#getReceivers()
   * 
   */

  @Override
  public List<Receiver> getReceivers() {

    // A CoreMidiSource has no receivers
    return null;

  }

  /**
   * Gets a transmitter for this device (which is also added to the internal list
   *
   * @return a transmitter for this device
   * 
   * @see javax.sound.midi.MidiDevice#getTransmitter()
   * 
   */

  @Override
  public Transmitter getTransmitter() throws MidiUnavailableException {

    // Create the transmitter
    Transmitter transmitter = new CoreMidiTransmitter(this);

    // Add it to the list
    synchronized (transmitters) {

      transmitters.add(transmitter);

    }

    // Finally return it
    return transmitter;

  }

  /**
   * Gets the list of transmitters registered with this MIDI device
   *
   * @return The list of transmitters registered with this MIDI device
   * 
   * @see javax.sound.midi.MidiDevice#getTransmitters()
   * 
   */

  @Override
  public List<Transmitter> getTransmitters() {

    // Create and return a list of transmitters
    synchronized (transmitters) {

      final List<Transmitter> list = new ArrayList<Transmitter>();

      list.addAll(transmitters);

      return list;

    }

  }

  /**
   * Checks whether a status byte represents a real-time message, which can occur even in the middle of another
   * multi-byte message.
   *
   * @param status the status byte which has just been received.
   *
   * @return true if the status byte represents a standalone real-time message which should be passed on without
   *         interrupting any other message being gathered.
   */
  private boolean isRealTimeMessage(byte status) {

    switch (status) {

      case (byte) ShortMessage.TIMING_CLOCK:
      case (byte) ShortMessage.START:
      case (byte) ShortMessage.CONTINUE:
      case (byte) ShortMessage.STOP:
      case (byte) ShortMessage.ACTIVE_SENSING:
      case (byte) ShortMessage.SYSTEM_RESET:
        return  true;

      default:
        return false;

    }

  }

  /**
   * Checks whether a status byte represents a running-status message, which means that multiple messages can be
   * sent without re-sending the status byte, for example to support multiple note-on messages in a row by simply
   * sending a stream of data byte pairs after the note-on status byte.
   *
   * @param status the status byte which is being processed.
   *
   * @return true if we should stay in this status after receiving our full complement of data bytes.
   */
  private boolean isRunningStatusMessage (int status) {

    switch(status & 0xF0) {

      case ShortMessage.NOTE_OFF:
      case ShortMessage.NOTE_ON: 
      case ShortMessage.POLY_PRESSURE:
      case ShortMessage.CONTROL_CHANGE:
      case ShortMessage.PROGRAM_CHANGE: 
      case ShortMessage.CHANNEL_PRESSURE:
      case ShortMessage.PITCH_BEND:
        return true;

      default:
        return false;

    }

  }

  /**
   * Determine how many data bytes are expected for a given MIDI message other than a SYSEX message, which varies.
   *
   * @param status the status byte introducing the MIDI message.
   *
   * @return the number of data bytes which must be received for the message to be complete.
   *
   * @throws InvalidMidiDataException if the status byte is not valid.
   */
  private int expectedDataLength (byte status) throws InvalidMidiDataException {

    // system common and system real-time messages

    switch(status &0xFF) {

      case ShortMessage.TUNE_REQUEST:
      case ShortMessage.END_OF_EXCLUSIVE:

        // System real-time messages
      case ShortMessage.TIMING_CLOCK:  
      case 0xF9:  // Undefined
      case ShortMessage.START:  
      case ShortMessage.CONTINUE:  
      case ShortMessage.STOP:  
      case 0xFD:  // Undefined
      case ShortMessage.ACTIVE_SENSING:  
      case ShortMessage.SYSTEM_RESET:  
        return 0;

      case ShortMessage.MIDI_TIME_CODE:
      case ShortMessage.SONG_SELECT:  
        return 1;

      case ShortMessage.SONG_POSITION_POINTER:  
        return 2;

      default:  // Fall through to next switch

    }

    // channel voice and mode messages
    switch(status & 0xF0) {

      case ShortMessage.NOTE_OFF: 
      case ShortMessage.NOTE_ON:  
      case ShortMessage.POLY_PRESSURE:
      case ShortMessage.CONTROL_CHANGE:  
      case ShortMessage.PITCH_BEND: 
        return 2;

      case ShortMessage.PROGRAM_CHANGE:  
      case ShortMessage.CHANNEL_PRESSURE:  
        return 1;

      default:
        throw new InvalidMidiDataException("Invalid status byte: " + status);

    }

  }

  /**
   * The message callback for receiving midi data from the JNI code
   * 
   * @param coreTimestamp  The time in microseconds since boot at which the messages should take effect
   * @param packetlength   The length of the packet of messages
   * @param data           The data array that holds the messages
   * 
   * @throws InvalidMidiDataException
   * 
   */

  public void messageCallback(long coreTimestamp, int packetlength, byte data[]) throws InvalidMidiDataException {

    int offset = 0;

    // Convert from CoreMIDI-oriented boot-relative microseconds to Java-oriented port-relative microsecends,
    // and from unsigned CoreMIDI semantics of 0 meaning now to signed Java semantics of -1 meaning now.
    final long timestamp = (coreTimestamp == 0) ? -1 : coreTimestamp - startTime;

    // An OSX MIDI packet may contain multiple messages
    while (offset < packetlength) {

      if (data[offset] >= 0) {

        // This is a regular data byte. Process it appropriately for our current message type.
        if (currentMessage == 0) {

          throw new InvalidMidiDataException("Data received outside of a message.");

        } else if (currentMessage == SysexMessage.SYSTEM_EXCLUSIVE) {

          // We are in the middle of gathering system exclusive data; continue.
          offset += processSysexData(packetlength, data, offset, timestamp);

        } else if (currentDataIsSingleByte) {

          // We are processing a message which only needs one data byte, this completes it
          transmitMessage(new ShortMessage(currentMessage, data[offset++], 0), timestamp);
          if (!isRunningStatusMessage(currentMessage)) currentMessage = 0;

        } else {

          // We are processing a message which needs two data bytes
          if (wasFirstByteReceived) {

            // We have the second data byte, the message is now complete
            transmitMessage(new ShortMessage(currentMessage, firstDataByte, data[offset++]), timestamp);
            wasFirstByteReceived = false;
            if (!isRunningStatusMessage(currentMessage)) currentMessage = 0;

          } else {

            // We have just received the first data byte of a message which needs two.
            firstDataByte = data[offset++];
            wasFirstByteReceived = true;
          }
        }

      } else {

        // This is a status byte, handle appropriately
        if (isRealTimeMessage(data[offset])) {

          // Real-time messages can come anywhere, including in between data bytes of other messages.
          // Simply transmit it and move on.
          transmitMessage(new ShortMessage(data[offset++] & 0xff), timestamp);

        } else if (data[offset] == (byte) ShortMessage.END_OF_EXCLUSIVE) {

          // This is the marker for the end of a system exclusive message. If we were gathering one,
          // process (finish) it.
          if (currentMessage == SysexMessage.SYSTEM_EXCLUSIVE) {

            offset += processSysexData(packetlength, data, offset, timestamp);
          } else {

            throw new InvalidMidiDataException("Received End of Exclusive marker outside SYSEX message");

          }

        } else if (data[offset] == (byte) SysexMessage.SYSTEM_EXCLUSIVE) {

          // We are starting to gather a SYSEX message.
          currentMessage = SysexMessage.SYSTEM_EXCLUSIVE;
          sysexMessageLength = 0;
          sysexMessageData = new Vector<byte[]>();
          offset += processSysexData(packetlength, data, offset, timestamp);

        } else {

          // Some ordinary MIDI message.
          switch (expectedDataLength(data[offset])) {

            case 0:  // No data bytes, this is a standalone message, so we can send it right away.
              transmitMessage(new ShortMessage(data[offset++] & 0xff), timestamp);
              currentMessage = 0;  // If we were in a running status, it's over now
              break;

            case 1: // We are expecting data before we can send this message.
              currentMessage = data[offset++] & 0xff;
              currentDataIsSingleByte = true;
              break;

            case 2: // We are expecting two bytes of data before we can send this message.
              currentMessage = data[offset++] & 0xff;
              currentDataIsSingleByte = false;
              wasFirstByteReceived = false;
              break;

            default:
              throw new InvalidMidiDataException("Unexpected data length: " +
                  expectedDataLength(data[offset]));

          }

        }

      }

    }

  }

  /**
   * Creates a SYSEX message from the received (potentially partial) messages. This function is called when F7 was
   * detected in the most recent message gathered, indicating the end of the SYSEX.
   *
   * @return The constructed SYSEX message
   * 
   * @throws InvalidMidiDataException
   * 
   */

  private SysexMessage constructSysexMessage() throws InvalidMidiDataException {

    // Create the array to hold the constructed message and reset the index (where the data will be copied)
    byte data[] = new byte[sysexMessageLength];
    int index = 0;

    // Iterate through the partial messages
    for (int i = 0; i < sysexMessageData.size(); i += 1) {

      // Get the partial message
      byte sourceData[] = sysexMessageData.get(i);

      // Copy the partial message into the array
      System.arraycopy(sourceData, 0, data, index, sourceData.length);

      // Point the index to where the next partial message needs to be copied.
      index += sourceData.length;

    }

    // We are done with the message fragments, so allow them to be garbage collected
    sysexMessageData = null;

    // Create and return the new SYSYEX Message
    return new SysexMessage(data, sysexMessageLength);

  }

  /**
   * Called when a SYSEX message is being received, either because an F0 byte has been seen at the start of a
   * message, which starts the process of gathering a SYSEX potentially across multiple packets, or because a
   * new packet has been received while we are still in the process of gathering bytes of a SYSEX which was
   * started in a previous message. The partial data is added to the sysexMessageData Vector. If we see another
   * status byte, except for one which represents a real-time message, we know the SYSEX is finished, and so we
   * can assemble and transmit it from any fragments which have been gathered.
   *
   * If we see a real-time message or the end of the packet, we return the data we have gathered,
   * and let the main loop decide what to do next.
   *
   * @param packetLength 	The length of the data packet
   * @param sourceData   	The source data received from Core MIDI
   * @param startOffset  	The position within the packet where the current message began
   * @param timestamp 		The message timestamp
   *
   * @return 							The number of bytes consumed from the packet by the SYSEX message.
   * 
   * @throws 							InvalidMidiDataException 
   * 
   */

  private int processSysexData(int packetLength, byte sourceData[], int startOffset, long timestamp)
      throws InvalidMidiDataException {

    // Look for the end of the SYSEX or packet
    int messageLength = 0;
    boolean foundEnd = false;

    // Check to see if this packet contains the end of the message
    while ( ( startOffset + messageLength ) < packetLength) {

      byte latest = sourceData[startOffset + messageLength++];

      if (latest < 0 && messageLength + sysexMessageLength > 1) {

        // We have encountered another status byte (after the F0 which starts the message).
        // Is it the marker of the end of the SYSEX message?
        if (latest == (byte) ShortMessage.END_OF_EXCLUSIVE) {

          currentMessage = 0;  // Found end marker, ready to send this SYSEX and look for next message.
          foundEnd = true;

        } else if (isRealTimeMessage(latest)) {

          // Back up so the main loop can send this real-time message embedded in our data,
          // then call us again to keep gathering SYSEX data.
          --messageLength;

        } else {

          // Found the start of another message. Back up so it gets processed, and note that we have found
          // the end of our SYSEX data. If we decide we should not pass on incomplete SYSEX messages, the
          // code below the loop can use the fact that currentMessage is not 0 to do so.
          --messageLength;
          foundEnd = true;

        }

        break;  // One way or another, we are done gathering data bytes for now.

      }

    }

    // Create an array to hold this part of the message, if we received any actual data.
    // (Note the source array will be released by the native function.)
    if (messageLength > 0) {

      byte data[] = new byte[messageLength];

      //Copy the data to the array
      try {

        System.arraycopy(sourceData, startOffset, data, 0, messageLength);

      } catch (ArrayIndexOutOfBoundsException e) {

        e.printStackTrace();

        throw e;

      }

      // Add the message to the vector
      sysexMessageData.add(data);

    }

    // Update the length of the SYSEX message
    sysexMessageLength += messageLength;

    // If we found the end, send it now
    if (foundEnd) {

      // Again, here we could refrain from sending if currentMessage != 0, because that indicates we received
      // a partial SYSEX message, i.e. the next message started before we received the End of Exclusive marker.
      transmitMessage(constructSysexMessage(), timestamp);

    }

    return messageLength;

  }


  /**
   * Sends a MIDI message to all of the registered transmitters
   *
   * @param message 		the message to send
   * @param timestamp 	the time stamp
   * 
   */

  private void transmitMessage(final MidiMessage message, long timestamp) {

    // Uncomment the following to filter realtime messages during debugging
    //		if (isRealTimeMessage ((byte)message.getStatus())) {
    //
    //			return;
    //
    //		}

    synchronized (transmitters) {

      // Get the iterators from the transmitters collection
      final Iterator<Transmitter> iterator = transmitters.iterator();

      // Loop through the transmitters
      while (iterator.hasNext()) {

        // Get the next transmitter
        final Transmitter transmitter = iterator.next();

        // If the transmitter is not null then get the receiver
        if (transmitter != null) {

          final Receiver receiver = transmitter.getReceiver();

          // If the receiver is not null then get send the message
          if (receiver != null) {

            receiver.send(message, timestamp);

          }

        }

      }

    }

  }

  /**
   * Formats the provided data into a HEX string, which is useful for debugging
   *
   * @param aByte The data to format
   * 
   * @return The formatted HEX string
   * 
   */

  private String getHexString(byte[] aByte) {

    StringBuffer sbuf = new StringBuffer(aByte.length * 3 + 2);

    for (int i = 0; i < aByte.length; i++) {

      sbuf.append(' ');
      byte bhigh = (byte) ((aByte[i] & 0xf0) >> 4);
      sbuf.append((char) (bhigh > 9 ? bhigh + 'A' - 10 : bhigh + '0'));
      byte blow = (byte) (aByte[i] & 0x0f);
      sbuf.append((char) (blow > 9 ? blow + 'A' - 10 : blow + '0'));

    }

    return new String(sbuf).trim();

  }
  
  //////////////////////////////
  ///// JNI Interfaces
  //////////////////////////////

  /**
   * Static initializer for loading the native library
   *
   */

  static {

    try {

      Loader.load();

    } catch (Throwable t) {

      System.err.println("Unable to load native library, CoreMIDI4J will stay inactive: " + t);

    }

  }

  /**
   * Obtains the current system time in microseconds.
   *
   * @return The current system time in microseconds.
   * 
   */

  private native long getMicroSecondTime();

}
