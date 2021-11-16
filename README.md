# CoreMidi4J
Core MIDI Service Provider Interface (SPI) for Java 1.7 and above on
OS X, safe to load and interact with on any platform. (The SPI will
simply not try to provide devices on platforms where they are not
needed.)

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

Hopefully one day third-party SPIs like CoreMidi4J will not be
required, but until then we are making this available.

For years we both used MMJ, but that appears to longer be under
development and it does not work with later Java Runtimes. After
looking around for a replacement, we decided it was necessary to
create our own &ldquo;lightweight&rdquo; SPI, which Derek accomplished
in 2015, and that we would make it publicly available for others to
contribute to.

CoreMidi4J has been heavily used in some of our own projects for
several years, and after resolving the last known outstanding issue we
labeled it version 1.0. Over the next few years an occasional issue
was discovered and fixed, or a new feature was thought up and added,
leading to a new release.
[Feedback](https://github.com/DerekCook/CoreMidi4J/issues) on any new
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

> :musical_keyboard: **New to Jave MIDI?** CoreMidi4J is designed to
> be transparently compatible with the standard Java MIDI API, so we
> don&rsquo;t provide examples or explanations of how to use that.
> Oracle offers a
> [tutorial](https://docs.oracle.com/javase/tutorial/sound/overview-MIDI.html)
> and [API
> documentation](https://docs.oracle.com/javase/8/docs/api/index.html?javax/sound/midi/package-summary.html).

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

If you are using an application written by someone else, and are not
making any changes to it, then this is the best you can do; you will
need to remember to choose the right version of each MIDI device that
you want to use.

If you are writing your own program, or willing to change the source
code of the program you are using, you can make things even easier by
filtering out the broken MIDI devices, and only showing the ones that
work:

### Filtering Out Broken MIDI Devices

If your application runs on Macs as well as other platforms, you can
ensure that your users only ever see MIDI devices whose
implementations work properly, by using the
[`getMidiDeviceInfo()`](https://deepsymmetry.org/coremidi4j/apidocs/uk/co/xfactorylibrarians/coremidi4j/CoreMidiDeviceProvider.html#getMidiDeviceInfo())
method provided by
[`uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider`](https://deepsymmetry.org/coremidi4j/apidocs/uk/co/xfactorylibrarians/coremidi4j/CoreMidiDeviceProvider.html)
instead of the one in
[`javax.sound.midi.MidiSystem`](https://docs.oracle.com/javase/7/docs/api/javax/sound/midi/MidiSystem.html).
The CoreMidi4J version works on any platform. If you call it on
anything but a Mac, it simply gives you the same result you would get
from the standard method. On the Mac, it filters out any devices which
have broken SysEx implementations, and returns the CoreMidi4J versions
instead.

So to give your users the best experience possible, simply embed
CoreMidi4J, and use its implementation of `getMidiDeviceInfo()`
wherever you would otherwise have used the standard one, and your
users will always only see working MIDI devices.

### Watching for MIDI Device Changes

If you would like to be able to automatically update your user
interface when the user connects, disconnects, or powers on/off a MIDI
device, you can call
[`addNotificationListener(listener)`](https://deepsymmetry.org/coremidi4j/apidocs/uk/co/xfactorylibrarians/coremidi4j/CoreMidiDeviceProvider.html#addNotificationListener(uk.co.xfactorylibrarians.coremidi4j.CoreMidiNotification))
(also provided by the `CoreMidiDeviceProvider` class). This will call
register a listener method to be called whenever such changes to the
MIDI environment occur.

> NOTE: If you use `addNotificationListener` on a non-macOS system, it
> needs to periodically scan the MIDI environment on a background
> thread. We have learned that the version of `getMidiDeviceInfo` in
> `javax.sound.MidiSystem` is not thread-safe, and if more than one
> thread uses it simultaneously, it will return invalid devices which
> throw exceptions when they are used. Because of that, you should
> only use the version of `getMidiDeviceInfo` provided by
> `CoreMidiDeviceProvider`, which uses synchronization to avoid this
> problem. Even if you are not using `addNotificationListener`, our
> version of the method is safer if you are using multiple threads for
> your own purposes.

For more details, you can consult the CoreMidi4J [API
documentation](https://deepsymmetry.org/coremidi4j/apidocs/) or even
the source code, or simply keep reading.

Here is an example of what running the `Example` class (listed
below) on a Mac, with CoreMidi4J in the classpath, produces. Notice
that other than the sequencer and synthesizer, the only MIDI devices
returned are the inputs and outputs offered by CoreMidi4J:

```
java -cp coremidi4j-1.1.jar:. Example
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

### Sample Code

This class shows an example of how to ask CoreMidi4J for a list of
only properly-working MIDI devices (filtering out the broken ones
provided by the standard Mac OS X MIDI implementation). It also shows
how to check whether the native library is available (which will only
be true when you are running on a Mac), and how to ask to be notified
whenever there is a change in the MIDI environment (in other words, a
new device has become available, or an existing device has been
removed, which works on any platform starting with CoreMidi4J version
1.4):

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

    public static void main(String[] args) throws Exception {
        System.out.println("Working MIDI Devices:");
        for (javax.sound.midi.MidiDevice.Info device : CoreMidiDeviceProvider.getMidiDeviceInfo()) {
            System.out.println("  " + device);
        }

        if (Example.isCoreMidiLoaded()) {
            System.out.println("CoreMIDI4J native library is running.");
        } else {
            System.out.println("CoreMIDI4J native library is not available.");
        }

        watchForMidiChanges();
        System.out.println("Watching for MIDI environment changes for thirty seconds.");
        Thread.sleep(30000);
    }
}
```

### Embedding CoreMidi4J

If you want your project's users to be able to rely on a correct MIDI
implementation on Mac OS X without having to install anything, you can
embed CoreMidi4J and thereby make it automatically available. Releases
are available through
[Maven Central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22uk.co.xfactory-librarians%22%20AND%20a%3A%22coremidi4j%22).
[![Maven Central](https://img.shields.io/maven-central/v/uk.co.xfactory-librarians/coremidi4j)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22uk.co.xfactory-librarians%22%20AND%20a%3A%22coremidi4j%22)

> It is safe to embed CoreMidi4J in cross-platform Java projects; the
> native library will be loaded only when needed, on Mac OS X, and the
> Java library will remain inactive on other platforms: it will not
> attempt to provide any MIDI devices, and its implementation of
> `getMidiDeviceInfo()` will simply delegate to the standard one. This
> means that calling our version of `getMidiDeviceInfo()` will always
> give you the correct list of devices to use on any platform.
>
> Starting with CoreMidi4J version 1.4, you can even request to be
> notified of MIDI environment changes on any platform. If you are not
> on a Mac (where the underlying CoreMIDI library provides this
> service), CoreMidi4J will create a daemon thread which periodically
> scans the MIDI environment so that it can generate these
> notifications itself.

If you are building a project with code like the example above, you
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
program compiles and runs, and CoreMidi4J's devices will be available
to it.

### Building CoreMidi4J

In order to build CoreMidi4J from source, in addition to cloning this
repository, you will need to install Apple&rsquo;s
[Xcode](https://developer.apple.com/xcode/) and Apache
[Maven](https://maven.apache.org). (We recommend using
[Homebrew](http://brew.sh) to install Maven: once you have followed
Homebrew&rsquo;s own install instructions, simply run `brew install
maven` to install Maven.)

Of course you will also need a Java development environment. Even
though CoreMidi4J still can be used as far back as JDK 1.7, you need
at least JDK 1.8 to build it.

Once you have Xcode and Maven, to build CoreMidi4J `cd` into the
directory containing the Maven project specification `pom.xml` (you
will find it in the `CoreMidi4J` subdirectory of your clone of this
repository), and use normal Maven build commands. To build the
standalone jar, for example,

```sh
cd CoreMidi4J/CoreMidi4J
mvn package
```

That will compile the Java classes, generate the JNI headers, compile
the native library, and build the standalone jar file which embeds
everything needed at runtime, using the standard Maven location and
naming convention of `target/coremidi4j-{version}.jar` (it also builds
the source and javadoc jars needed for deployment to Maven Central).

## Device Names

In release 1.1 we changed the way that device names are reported to Java in
order to accommodate situations where people have several of the same
device attached to their system (see the [Issue 21
discussion](https://github.com/DerekCook/CoreMidi4J/issues/21) for
details).

Previously, we would simply return the CoreMIDI &ldquo;Endpoint&rdquo;
name as the device name. The problem with this is that the endpoint
name for all identical devices would be the same, and there is no way
for the user to edit these &ldquo;Endpoint&rdquo; names to distinguish between their devices.

Now, we instead return the CoreMIDI &ldquo;Device&rdquo; name
associated with the endpoint as the Java MIDI device name. This device
name can be edited by the user as described
[below](#editing-device-names) to distinguish between their devices of
the same type. And for devices that have multiple endpoints associated
with them, for example a controller with different kinds of ports, we
combine both the editable Device name followed by the non-editable
Endpoint name.

To provide an example: the Ableton Push 2 controller has two output
ports, `Live Port` and `User Port`. Under previous releases of
CoreMidi4J, these would show up in Java named simply `Live Port` and
`User Port`, and there was no way to change their names. In release 1.1 and
later they show up as `Ableton Push 2 Live Port` and `Ableton Push 2
User Port` and the &ldquo;Ableton Push 2&rdquo; name can be changed to
whatever you want using Audio Midi Setup as described
[below](#editing-device-names).

> :wrench: This means that if you update your application which embeds
> CoreMidi4J to use a current release and you were previously using
> release 1.0 or earlier, you may need to warn your users that their
> device names may have changed (if the Device and EndPoint names of
> any of their devices different, or for any device that has multiple
> Entities/Endpoints), so they need to check and update their saved
> configuration settings appropriately.

If you need even more details about the device, the
[`CoreMidiDeviceInfo` class](https://deepsymmetry.org/coremidi4j/apidocs/uk/co/xfactorylibrarians/coremidi4j/CoreMidiDeviceInfo.html)
returned by CoreMidi4J to describe its devices has additional
properties which provide access to CoreMIDI-specific device
attributes. When you know you are dealing with a
[`MidiDevice.Info`](https://docs.oracle.com/javase/8/docs/api/javax/sound/midi/MidiDevice.Info.html)
object returned by CoreMidi4J, you can cast it into a
`CoreMidiDeviceInfo` object and access this additional information.

### Editing Device Names

Users can change the device names associated with their MIDI devices
using Apple&rsquo;s **Audio Midi Setup** utility (found in the **Utilities**
subfolder within your main **Applications** folder, unless you have
moved it). Once the utility is launched, switch to the **MIDI Studio**
window (using the **Window** menu to open it if needed):

<image src="doc/assets/AudioMIDISetup.png" alt="Audio MIDI Setup" width="930">

To rename the very-generic &ldquo;USB MIDI Device&rdquo; shown at the
bottom right of this example MIDI Studio window, you could either
double-click on it, or click once to select it and then click the
Information button in the toolbar. That opens a Properties window
where the device name can be edited:

<image src="doc/assets/EditDeviceName.png" alt="Editing Device Name" width="520">

When editing a device name like this, as soon as you click the
**Apply** button in the Properties window, CoreMIDI4J will report a
MIDI environment change event, and will use the newly assigned device
name when reporting the connected MIDI devices.
