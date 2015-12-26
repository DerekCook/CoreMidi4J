/**
 * Title:        CoreMIDI4J
 * Description:  Core MIDI Device Provider for Java on OS X
 * Copyright:    Copyright (c) 2015
 * Company:      x.factory Librarians
 *
 * @author Derek Cook, James Elliott
 * 
 * CREDITS - This library uses principles established by OSXMIDI4J, but converted so it operates at the JNI level with no additional libraries required
 */

package com.xfactoryLibrarians;

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

	private boolean inSysexMessage = false;
	private Vector<byte[]> messageData;
	private int sysexMessageLength = 0;

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

			if (this.input == null) {

				this.input = CoreMidiDeviceProvider.getMIDIClient().inputPortCreate("Core Midi Provider Input");

			}

			this.input.connectSource(this);
			isOpen = true;

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

			if (this.input != null) {

				this.input.disconnectSource(this);

			}

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
	 * Obtains the current time-stamp of the device, in microseconds.
	 * This interface does not support time-stamps, so it should always return -1.
	 *
	 * @return Always -1 as this device does not support timestamps.
	 * 
	 * @see javax.sound.midi.MidiDevice#getMicrosecondPosition()
	 * 
	 */

	@Override
	public long getMicrosecondPosition() {

		// Not supported
		return -1;

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

		Transmitter t = new CoreMidiTransmitter(this);

		synchronized (transmitters) {

			transmitters.add(t);

		}

		return t;

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

		synchronized (transmitters) {

			final List<Transmitter> list = new ArrayList<Transmitter>();

			list.addAll(transmitters);

			return list;

		}

	}

	/**
	 * The message callback for receiving midi data from the JNI code
	 *
	 * @param packetlength The length of the packet of messages
	 * @param data         The data array that holds the messages
	 * 
	 * @throws InvalidMidiDataException
	 * 
	 */

	public void messageCallback(int packetlength, byte data[]) throws InvalidMidiDataException {

		MidiMessage message;
		int offset = 0;

		while (offset < packetlength) {

			if (inSysexMessage) {

				offset += processSysexData(packetlength, data, offset);

			} else if (data[offset] < (byte) 0xf0) {

				// Uncomment the following to show the received message whilst debugging
				//System.out.println("Message " + this.getHexString(data));

				switch (data[offset] & (byte) 0xf0) {

					case (byte) ShortMessage.NOTE_ON:
					case (byte) ShortMessage.NOTE_OFF:
					case (byte) ShortMessage.POLY_PRESSURE:
					case (byte) ShortMessage.CONTROL_CHANGE:
					case (byte) ShortMessage.PITCH_BEND:
						message = new ShortMessage(data[offset] & 0xff, data[offset + 1], data[offset + 2]);
						transmitMessage(message);
						offset += 3;
						break;

					case (byte) ShortMessage.PROGRAM_CHANGE:
					case (byte) ShortMessage.CHANNEL_PRESSURE:
						message = new ShortMessage(data[offset], data[offset + 1], 0);
						transmitMessage(message);
						offset += 2;
						break;

					default:
						throw new InvalidMidiDataException("Invalid Status Byte " + data[0]);

				}

			} else {

				switch (data[offset] & 0xff) {

					case ShortMessage.MIDI_TIME_CODE:
					case ShortMessage.SONG_POSITION_POINTER:
					case ShortMessage.SONG_SELECT:
						message = new ShortMessage(data[offset], data[offset + 1], data[offset + 2]);
						transmitMessage(message);
						offset += 3;
						break;

					case ShortMessage.TUNE_REQUEST:
					case ShortMessage.TIMING_CLOCK:
					case ShortMessage.START:
					case ShortMessage.CONTINUE:
					case ShortMessage.STOP:
					case ShortMessage.ACTIVE_SENSING:
					case ShortMessage.SYSTEM_RESET:
						message = new ShortMessage(data[offset] & 0xff);
						transmitMessage(message);
						offset += 1;
						break;

					case SysexMessage.SYSTEM_EXCLUSIVE:
						sysexMessageLength = 0;  // We are starting a SysEx message, may span multiple packets
						messageData = new Vector<byte[]>();
						offset += processSysexData(packetlength, data, offset);
						break;

					default:
						throw new InvalidMidiDataException("Invalid Status Byte ");

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
		for (int i = 0; i < messageData.size(); i += 1) {

			// Get the partial message
			byte sourceData[] = messageData.get(i);

			// Copy the partial message into the array
			System.arraycopy(sourceData, 0, data, index, sourceData.length);

			// Point the index to where the next partial message needs to be copied.
			index += sourceData.length;

		}

		// We are done with the message fragments, so allow them to be garbage collected
		messageData = null;

		// Create and return the new SYSYEX Message
		return new SysexMessage(data, sysexMessageLength);

	}

	/**
	 * Called when a SYSEX message is being received, either because an F0 byte has been seen at the start of a
	 * message, which starts the process of gathering a SYSEX potentially across multiple packets, or because a
	 * new packet has been received while we are still in the process of gathering bytes of a SYSEX which was
	 * started in a previous packet. The partial data is added to the messageData Vector. If we see an F7 byte,
	 * we know the SYSEX is finished, and so we can assemble and transmit it from any fragments which have been
	 * gathered.
	 *
	 * @param packetLength the length of the data packet
	 * @param sourceData   the source data received from Core MIDI
	 * @param startOffset  the position within the packet where the current message began
	 *
	 * @return the number of bytes consumed from the packet by the SYSEX message.
	 * 
	 * @throws InvalidMidiDataException 
	 * 
	 */
	
	private int processSysexData(int packetLength, byte sourceData[], int startOffset) throws InvalidMidiDataException {

		// Look for the end of the SYSEX or packet
		int messageLength = 0;
		boolean foundEnd = false;
		
		while (startOffset + messageLength < packetLength) {
			
			byte latest = sourceData[startOffset + messageLength++];
			
			if (latest == (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE) {
				
				foundEnd = true;
				break;  // We found the end of the message
				
			}
			
		}

		// Create an array to hold this part of the message (note the source array will be released by the native function)

		byte data[] = new byte[messageLength];

		//Copy the data to the array

		try {

			System.arraycopy(sourceData, startOffset, data, 0, messageLength);

		} catch (ArrayIndexOutOfBoundsException e) {

			e.printStackTrace();

			throw e;

		}

		// Add the message to the vector
		messageData.add(data);

		// Update the length of the SYSEX message
		sysexMessageLength += messageLength;

		// If we found the end, send it now
		if (foundEnd) {
			
			transmitMessage(constructSysexMessage());
			
		}

		inSysexMessage = !foundEnd;
		return messageLength;
		
	}


	/**
	 * Sends a MIDI message to all of the registered transmitters
	 *
	 * @param message the message to send
	 * 
	 */

	private void transmitMessage(final MidiMessage message) {

		// Uncomment the following to filter realtime messages during debugging
		//  	if ( ( message.getStatus() == ShortMessage.ACTIVE_SENSING ) || ( message.getStatus() == ShortMessage.TIMING_CLOCK ) ) {
		//  		
		//  		return;
		//  		
		//  	}

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

						receiver.send(message, -1);

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

}
