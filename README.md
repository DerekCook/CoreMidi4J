# CoreMidi4J
Core MIDI Service Provider Interface (SPI) for Java 1.7 and above on OS X.

Derek created CoreMidi4J as to our knowledge there is currently no Mac
Java MIDI implementation under active development that properly
supports sending System Exclusive messages, which still have not been
fixed in the Java core distribution.

In collaboration with James, we added support for hot-swapping MIDI
devices (the standard JAVA MIDI implementation will only recognize
devices which were already connected when Java started), and proper
support for inbound and outbound MIDI event timestamps, which can
extend over network MIDI sessions thanks to CoreMIDI&rsquo;s support
for them.

Hopefully one day third-party SPIs like
CoreMidi4J will not be required, but until then we are making this
available.

For years we both used MMJ, but that appears to longer be under
development and it does not work with later Java Runtimes. After
looking around for a replacement, we decided it was necessary to
create our own &ldquo;lightweight&rdquo; SPI, which Derek accomplished
in 2015, and that we would make it publicly available for others to
contribute to.

CoreMidi4J has been heavily used in some of our own projects for over
a year, and we have recently resolved the last known outstanding
issue, so we have labeled it version 1.0. Feedback on any new
problems or issues is always welcome.

## Installation

The recommended approach for use as a library is to
[embed CoreMidi4J](#embedding-coremidi4j) in your project and have its
native code loaded automatically when on the OS X platform, so that end
users do not need to worry about installing anything.

It is also still possible to
[download and install CoreMidi4J](#standalone-coremidi4j) separately,
to use it with applications that did not embed it (or if your
own project does not use the Maven dependency-management ecosystem).

[![License](https://img.shields.io/badge/License-Eclipse%20Public%20License%201.0-blue.svg)](#license)

## Using CoreMidi4J

Once installed, if all you want to do is use the enhanced MIDI devices
provided by CoreMidi4J, all you have to do is use the normal Java MIDI
API, but choose CoreMidi4J&rsquo;s device implementations instead of the
ones provided by the native MIDI SPI. You will be able to identify
them because their names will begin with `CoreMidi4J -`. These devices
will:

* properly support System Exclusive messages,
* provide, translate, and respect CoreMidi timestamps on MIDI events,
  and
* the list of devices available will correctly update even if you
  connect or detach devices after Java is already running.

### Checking CoreMidi4J's availability

If you would like to go further and filter out the non-working MIDI
devices that exist on the Mac, or take advantage of CoreMidi4J&rsquo;s
ability to notify your code when the MIDI environment changes, you
will need to access some of CoreMidi4J's classes directly. Unless you
are embedding CoreMidi4J in your application and certain that you are
running under Java 7 or later, you should use reflection to make sure
that CoreMidi4J is available before trying to do this, or your
application will fail to run in environments where CoreMidi4J's Java
classes have not been loaded.

> If you are [embedding CoreMidi4J](#embedding-coremidi4j), the only
> reason you would need to check if it is available is if you might be
> running in Java 6 or earlier, because CoreMidi4J requires Java 7. If
> your project already requires Java 7 or later, and you have embedded
> CoreMidi4J, it is safe to assume that it is present, and you can
> skip to checking if the native library is
> [active](#checking-if-coremidi4j-is-active), and
> [filtering out](#filtering-out-broken-midi-devices) broken MIDI
> device implementations.

Here is an example of how to test whether CoreMidi4J is available.
This class can safely be loaded on any system, and will check the
environment to see if it is safe to try and load the class in the
example that follows:

```java
public class Available {

    public static void main(String[] args) throws Exception {
        try {
            Class deviceProviderClass = Class.forName(
                "uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider");
            System.out.println("CoreMIDI4J Java classes are available.");
            System.out.println("Working MIDI Devices:");
            for (javax.sound.midi.MidiDevice.Info device : Example.getWorkingDeviceInfo()) {
                System.out.println("  " + device);
            }
            if (Example.isCoreMidiLoaded()) {
                System.out.println("CoreMIDI4J native library is running.");
                Example.watchForMidiChanges();
                System.out.println("Watching for MIDI environment changes for thirty seconds.");
                Thread.sleep(30000);
            } else {
                System.out.println("CoreMIDI4J native library is not available.");
            }
        } catch (Exception e) {
            System.out.println("CoreMIDI4J Java classes are not available.");
        }
    }
}
```

### Checking if CoreMidi4J is Active

This second class cannot be loaded on systems which lack the
CoreMidi4J classes, but shows an example of how to ask for a list of
only properly-working MIDI devices (filtering out the broken ones
provided by the standard Mac OS X MIDI implementation). It also shows
how to check whether the native library is available, and if it is, to
ask to be notified whenever there is a change in the MIDI environment
(in other words, a new device has become available, or an existing
device has been removed):

```java
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiNotification;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException;
import javax.sound.midi.MidiDevice;

public class Example {

    public static boolean isCoreMidiLoaded() throws CoreMidiException {
        return CoreMidiDeviceProvider.isLibraryLoaded();
    }

    public static void watchForMidiChanges() throws CoreMidiException {
        CoreMidiDeviceProvider.addNotificationListener(new CoreMidiNotification() {
                public void midiSystemUpdated() {
                    System.out.println("The MIDI environment has changed.");
                }
            });
    }

    public static MidiDevice.Info[] getWorkingDeviceInfo() {
        return CoreMidiDeviceProvider.getMidiDeviceInfo();
    }
}
```

### Filtering Out Broken MIDI Devices

If your application runs on Macs as well as other platforms, you can
ensure that your users only ever see MIDI devices whose
implementations work properly, by using the `getMidiDeviceInfo()`
method provided by
`uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider` instead
of the one in `javax.sound.midi.MidiSystem`. The CoreMidi4J version
works on any platform. If you call it on anything but a Mac, it simply
gives you the same result you would get from the standard method. On
the Mac, it filters out any devices which have broken SysEx
implementations, and returns the CoreMidi4J versions instead.

So to give your users the best experience possible, simply embed
CoreMidi4J, and use its implementation of `getMidiDeviceInfo()`
wherever you would otherwise have used the standard one, and your
users will always only see working MIDI devices.

Here is an example of what running the `Available` class (listed
above) on a Mac, with CoreMidi4J in the classpath, produces. Notice
that other than the sequencer and synthesizer, the only MIDI devices
returned are the inputs and outputs offered by CoreMidi4J:

```
java -cp coremidi4j-1.0.jar:. Available
CoreMIDI4J Java classes are available.
Working MIDI Devices:
  CoreMIDI4J - Bus 1
  CoreMIDI4J - Network
  CoreMIDI4J - Live Port
  CoreMIDI4J - User Port
  CoreMIDI4J - Traktor Virtual Output
  CoreMIDI4J - Bus 1
  CoreMIDI4J - Network
  CoreMIDI4J - Live Port
  CoreMIDI4J - User Port
  Gervill
  Real Time Sequencer
CoreMIDI4J native library is running.
Watching for MIDI environment changes for thirty seconds.
The MIDI environment has changed.
The MIDI environment has changed.
```

During the thirty seconds the code was running, a MIDI device was
plugged in and later unplugged, demonstrating the fact that CoreMidi4J
can adapt to changes in the MIDI environment, and notify the host
application about them.

### Embedding CoreMidi4J

If you want your project's users to be able to rely on a correct MIDI
implementation on Mac OS X without having to install anything, you can
embed CoreMidi4J and thereby make it automatically available. Releases
are available through
[Maven Central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22uk.co.xfactory-librarians%22%20AND%20a%3A%22coremidi4j%22).
[![Maven Central](https://img.shields.io/maven-central/v/uk.co.xfactory-librarians/coremidi4j.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22uk.co.xfactory-librarians%22%20AND%20a%3A%22coremidi4j%22)

> It is safe to embed CoreMidi4J in cross-platform Java projects; the
> native library will be loaded only when needed, on Mac OS X, and the
> Java library will remain inactive on other platforms: it will not
> attempt to provide any MIDI devices, and its implementation of
> `getMidiDeviceInfo()` will simply delegate to the standard one.

If you are building a project with code like the examples above, you
will need to configure CoreMidi4J as a dependency of your project.
This will also enable build tools like Maven and Leiningen to build a
consolidated Jar containing your own classes as well as those of
CoreMidi4J, and any other libraries you depend on.

Click on the Maven Central link above, then the version you want to
use, to see the configuration snippets you can use to add that version
of CoreMidi4J as a dependency of your project in Maven, Gradle,
Leiningen, or your build tool of choice.

### Standalone CoreMidi4J

If you want to use CoreMidi4J with another Java program that does not
embed it, or in a project of your own that does not use the Maven
dependency management approach, you can download the standalone jar
from the
[releases](https://github.com/DerekCook/CoreMidi4J/releases) page.
[![jar](https://img.shields.io/github/downloads/DerekCook/CoreMidi4J/total.svg)](https://github.com/DerekCook/CoreMidi4J/releases)

Then simply place the CoreMidi4J jar on the classpath when that
program runs, and CoreMidi4J's devices will be available to it.

### Building CoreMidi4J

In order to build CoreMidi4J from source, in addition to cloning this
repository, you will need to install Apple&rsquo;s
[Xcode](https://developer.apple.com/xcode/download/) and Apache
[Maven](https://maven.apache.org). (We recommend using
[Homebrew](http://brew.sh) to install Maven: once you have followed
Homebrew&rsquo;s own install instructions, simply run `brew install
maven` to install Maven.)

Once you have Xcode and Maven, to build CoreMidi4J `cd` into the
directory containing the Maven project specification `pom.xml` (you
will find it in the `CoreMidi4J` subdirectory of your clone of this
repository), and use normal Maven build commands. To build the
standalone jar, for example,

```sh
cd CoreMidi4J/CoreMidi4J
mvn package
```

That will compile the Java classes, generate the JNI header, compile
the native library, and build the standalone jar file which embeds
everything needed at runtime, using the standard Maven location and
naming convention of `target/coremidi4j-{version}.jar` (it also builds
the source and javadoc jars needed for deployment to Maven Central).

## Device Names

In release 1.1 we changed the way device names are reported to Java in
order to accommodate situations where people have several of the same
device attached to their system (see the [Issue 21
discussion](https://github.com/DerekCook/CoreMidi4J/issues/21) for
details.

Previously, we would simply return the CoreMIDI "Endpoint" name as the
device name. The problem with this is that the endpoint name for all
identical devices would be the same, and there is no way for the user
to edit them to distinguish between their devices.

Now, we instead return the CoreMIDI "Device" name associated with the
endpoint as the Java MIDI device name. This device name can be edited
by the user as described [below](#editing-device-names) to distinguish
between their devices of the same type. And for devices that have
multiple endpoints associated with them, for example a controller with
different kinds of ports, we combine both the editable Device name
followed by the non-editable Endpoint name.

To make this concrete, my Ableton Push 2 controller has two output
ports, `Live Port` and `User Port`. Under previous releases of
CoreMidi4J, these would show up in Java named simply `Live Port` and
`User Port`, and there was no way to change them. In release 1.0 and
later they show up as `Ableton Push 2 Live Port` and `Ableton Push 2
User Port` and the "Ableton Push 2" name can be changed to whatever I
want using Audio Midi Setup as described
[below](#editing-device-names).

> :wrench: This means that if you update your application which embeds
> CoreMidi4J to use a current release and you were previously using
> release 1.0 or earlier, you may need to warn your users that their
> device names have likely changed, so they need to check and update
> their saved configuration settings appropriately.

If you need even more details about the device, the
[`CoreMidiDeviceInfo` class](https://github.com/DerekCook/CoreMidi4J/blob/master/CoreMIDI4J/src/uk/co/xfactorylibrarians/coremidi4j/CoreMidiDeviceInfo.java)
returned by CoreMidi4J to describe its devices has additional
properties which provide access to CoreMIDI-specific device
attributes. When you know you are dealing with a
[`MidiDevice.Info`](https://docs.oracle.com/javase/8/docs/api/javax/sound/midi/MidiDevice.Info.html)
object returned by CoreMidi4J, you can cast it into a
`CoreMidiDeviceInfo` object and access this additional information.

### Editing Device Names

Users can change the device names associated with their MIDI devices
using Apple's **Audio Midi Setup** utility (found in the **Utilities**
subfolder within your main **Applications** folder, unless you have
moved it). Once the utility is launched, switch to the **MIDI Studio**
window (using the **Window** menu to open it if needed):

<image src="doc/assets/AudioMidiSetup.png" alt="Audio MIDI Setup" width="930">

To rename the very-generic "USB MIDI Device" shown at the bottom right
of my MIDI Studio window, I can either double-click on it, or click
once to select it and then click the Information button in the
toolbar. That opens a Properties window where the device name can be edited:

<image src="doc/assets/EditDeviceName.png" alt="Editing Device Name" width="536">

When editing a device name like this, as soon as you click the
**Apply** button in the Properties window, CoreMIDI4J will report a
MIDI environment change event, and will use the newly assigned device
name when reporting the connected MIDI devices.
