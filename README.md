# CoreMidi4J
Core MIDI Service Provider Interface (SPI) for Java 1.7 and above on OS X.

I have created CoreMidi4J as to my knowledge there is currently no SPI under active development that overcomes the inherent Java MIDI SYSEX limitiations, which still have not been fixed in the Java Core software. Hopefully one day these SPIs will not be required, but until then one is needed.

For years I have used MMJ, but that appears to longer be under development and it does not work with later Java Runtimes. After looking around for a replacement, I decided it was necessary to create my own "lightweight" SPI, and that I would make it publicly available for others to contribute to.

The current release (0.3) is a stable pre-release version, which is now considered stable enough for users to experiment with. Feedback on any discovered problems/issues is welcome.

To download and install, navigate to the
[release](https://github.com/DerekCook/CoreMidi4J/releases) page and
follow the instructions for the most recent release.

## Using CoreMidi4J

Once installed, if all you want to do is use the enhanced MIDI devices
provided by CoreMidi4J, all you have to do is use the normal Java MIDI
API, but choose CoreMidi4J's device implementations instead of the
ones provided by the native MIDI SPI. You will be able to identify
them because their names will begin with "CoreMidi4J -". They will
properly support System Exclusive messages and CoreMidi timestamps,
and the list of devices available will properly update even if you
connect or detach devices after Java is already running.

If you would like to take advantage of CoreMidi4J's ability to notify
your code when the MIDI environment changes, you will need to access
some of its classes directly. You should use reflection to make sure
that CoreMidi4J is available before trying to do this, however, or
your application will fail to run on machines where CoreMidi4J has not
been installed.

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

### Depending on CoreMidi4J

If you are building a project with code like these example, the Java
classes are available through
[Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22coremidi4j%22).

If your project is only conditionally dependent on CoreMidi4J (you
plan to only use it when you find that it has been installed as an
extension on the system where you are running), then you should
declare it as a dependency in the `provided` scope. That will allow
your code to compile, but will not try to copy the CoreMidi4J Java
classes into your project. This is likely the most appropriate
approach, because you will need to provide some sort of installer for
the native library if you want to distribute CoreMidi4J with your
project in any case.
