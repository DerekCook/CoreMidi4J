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

The current release is a stable pre-release version, which has been
heavily used in some of our own projects, and is now considered stable
enough for users to experiment with. Feedback on any discovered
problems/issues is welcome.

As of release 0.7, it is possible to
[embed CoreMidi4J](#embedding-coremidi4j) in another project and have
its native code loaded automatically on the OS X platform, so that end
users do not need to worry about installing anything.

It is also still possible to
[download and install CoreMidi4J](#standalone-coremidi4j) separately,
to use it with applications that did not embed it.

[![License](https://img.shields.io/badge/License-Eclipse%20Public%20License%201.0-blue.svg)](#license)

## Using CoreMidi4J

Once installed, if all you want to do is use the enhanced MIDI devices
provided by CoreMidi4J, all you have to do is use the normal Java MIDI
API, but choose CoreMidi4J&rsquo;s device implementations instead of the
ones provided by the native MIDI SPI. You will be able to identify
them because their names will begin with `CoreMidi4J -`. These devices
will:

* properly support System Exclusive messages,
* provide, translate, and honor CoreMidi timestamps on MIDI events,
  and
* the list of devices available will correctly update even if you
  connect or detach devices after Java is already running.

If you would like to go further and take advantage of
CoreMidi4J&rsquo;s ability to notify your code when the MIDI
environment changes, you will need to access some of its classes
directly. You should use reflection to make sure that CoreMidi4J is
available before trying to do this, however, or your application will
fail to run on machines where CoreMidi4J has not been installed.

Here is an example of how to do that. The first class can safely be
loaded on any system, and will check the environment to see if it is
safe to try and load the second class:

```java
public class Available {

    public static void main(String[] args) throws Exception {

        try {

            Class deviceProviderClass =
                Class.forName("uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider");
            System.out.println("CoreMidi4J Java classes are available.");

            if (Example.isCoreMidiLoaded()) {

                System.out.println("CoreMidi4J native library is running.");

                Example.watchForMidiChanges();
                System.out.println("Watching for MIDI environment changes for thirty seconds.");

                Thread.sleep(30000);
                System.exit(0);

            } else {

                System.out.println("CoreMidi4J native library is not available.");

            }
        } catch (Exception e) {

            System.out.println("CoreMidi4J Java classes are not available.");

        }
    }
}
```

This second class cannot be instantiated on systems which lack the
CoreMidi4J classes, but shows an example of how to check whether the
native library is available, and if it is, to ask to be notified
whenever there is a change in the MIDI environment (in other words, a
new device has become available, or an existing device has been
removed):

```java
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiNotification;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException;

public class Example {

    public static boolean isCoreMidiLoaded() {

        return CoreMidiDeviceProvider.isLibraryLoaded();

    }

    public static void watchForMidiChanges() throws CoreMidiException {

        CoreMidiDeviceProvider.addNotificationListener(new CoreMidiNotification() {

                public void midiSystemUpdated() {

                    System.out.println("The MIDI environment has changed.");

                }
            });
    }
}
```

### Embedding CoreMidi4J

If you want your project's users to be able to rely on a correct MIDI
implementation on Mac OS X without having to install anything, you can
embed CoreMidi4J and thereby make it automatically available. Releases
are available through
[Maven Central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22uk.co.xfactory-librarians%22%20AND%20a%3A%22coremidi4j%22).
[![Maven Central](https://img.shields.io/maven-central/v/uk.co.xfactory-librarians/coremidi4j.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22uk.co.xfactory-librarians%22%20AND%20a%3A%22coremidi4j%22)

> It is safe to embed CoreMidi4J in cross-platform Java projects; the
> native library will be loaded only when needed, on Mac OS X, and the
> Java library will remain inactive on other platforms, and not
> attempt to provide any MIDI devices.

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
embed it, you can download the standalone jar from the
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
