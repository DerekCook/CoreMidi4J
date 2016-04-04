package uk.co.xfactorylibrarians.coremidi4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Loads the native library when we are running on a Mac. If necessary, extracts a copy
 * of the library from our jar file to a temporary directory, to save the user the trouble
 * of having to install it on their system. Arranges for that directory to be deleted when
 * we exit.
 *
 * Inspired by the techniques used by usb4java, and the loader written by Klaus Raimer, k@ailis.de
 *
 * @author James Elliott
 */
public class Loader {

  /**
   * How large a buffer should be used for copying the dynamic library out of the jar.
   */
  private static final int BUFFER_SIZE = 8192;

  /**
   * The file name of our native library.
   */
  public static final String NATIVE_LIBRARY_NAME = "libCoreMidi4J.dylib";

  /**
   * Prevent instantiation.
   */
  private Loader() {
    // Nothing to do here
  }

  /**
   * Check that we are running on Mac OS X.
   */
  private static boolean isMacOSX() {
    final String os = System.getProperty("os.name").toLowerCase().replace(" ", "");
    return (os.equals("osx") || os.equals("macosx"));
  }

  /**
   * The temporary directory we will use to extract the native library.
   */
  private static File tempDir;

  /**
   * Indicates whether we have tried to load the library.
   */
  private static boolean loaded = false;

  /**
   * Indicates whether the library was successfully loaded, which implies we are on a Mac and ready to operate.
   */
  private static boolean available = false;

  /**
   * Creates the temporary directory used for unpacking the native library.
   * This directory is marked to be deleted when the JVM exits.
   *
   * @return The temporary directory for the native library.
   */
  private static File createTempDirectory() throws CoreMidiException {

    if (tempDir != null) {

      // We have already created it, so simply return it
      return tempDir;

    }

    try
    {

      tempDir = File.createTempFile("coreMidi4J", null);

      if (!tempDir.delete()) {

        throw new IOException("Unable to delete temporary file " + tempDir);

      } if (!tempDir.mkdirs()) {

        throw new IOException("Unable to create temporary directory " + tempDir);

      }

      tempDir.deleteOnExit();
      return tempDir;

    } catch (final IOException e) {

      throw new CoreMidiException("Unable to create temporary directory for CoreMidi4J library: " + e, e);

    }
  }

  /**
   * Copies the specified input stream to the specified output file.
   *
   * @param input   The input stream to be copied.
   * @param output  The output file to which the stream should be copied.
   *
   * @throws IOException If the copy failed.
   */
  private static void copy(final InputStream input, final File output) throws IOException {

    final byte[] buffer = new byte[BUFFER_SIZE];
    final FileOutputStream stream = new FileOutputStream(output);

    try {

      int read;

      while ((read = input.read(buffer)) != -1) {

        stream.write(buffer, 0, read);

      }

    } finally {

      stream.close();

    }
  }

  /**
   * Locates the native library, extracting a temporary copy from our jar it does not already exist in the filesystem.
   *
   * @return The absolute path to the existing or extracted library.
   */
  private static String locateLibrary() throws CoreMidiException {

    // Check if native library is present
    final String source = "/" + NATIVE_LIBRARY_NAME;
    final URL url = Loader.class.getResource(source);

    if (url == null) {

      throw new CoreMidiException("Native library " + source + " not found in classpath.");

    }

    // If the native library was found as an actual file, there is no need to extract a copy from our jar.
    if ("file".equals(url.getProtocol())) {

      try {

        return new File(url.toURI()).getAbsolutePath();

      } catch (final URISyntaxException e) {

        // In theory this can't happen because we are not constructing the URI manually.
        // But even if it happens, we can fall back to extracting the library.
        System.err.println("Problem trying to obtain File from dynamic library: " + e);
      }
    }

    // Extract the library and return the path to the extracted file.
    final File dest = new File(createTempDirectory(), NATIVE_LIBRARY_NAME);

    try {

      final InputStream stream = Loader.class.getResourceAsStream(source);
      if (stream == null) {

        throw new CoreMidiException("Unable to find " + source + " in the classpath");

      }

      try {

        copy(stream, dest);

      } finally {

        stream.close();

      }

    } catch (final IOException e) {

      throw new CoreMidiException("Unable to extract native library " + source + " to " + dest + ": " + e, e);

    }

    // Arrange for the copied library to be deleted when the JVM exits
    dest.deleteOnExit();

    return dest.getAbsolutePath();
  }

  /**
   * Tries to load our native library. Can be safely called multiple times; duplicate attemtps are ignored.
   * This method is automatically called whenever any CoreMidi4J class that relies on JNI is loaded. If you
   * need to do it earlier (to catch exceptions for example, or check whether the native library can be used),
   * simply call this method manually.
   *
   * @throws CoreMidiException if something unexpected happens trying to load the native library on a Mac OS X system.
   */
  public static synchronized void load() throws CoreMidiException {

    // Do nothing if this is a redundant call
    if (loaded) {

      return;

    }

    loaded = true;

    if (isMacOSX()) {

      System.load(locateLibrary());
      available = true;

    }

  }

  /**
   * Checks whether CoreMidi4J is available on the current system, in other words whether it is a Mac OS X system
   * and the native library was loaded successfully. Will attempt to perform that load if it has not yet occurred.
   *
   * @return true if this is a Mac OS X system and the native library has been loaded successfully.
   *
   * @throws CoreMidiException if something unexpected happens trying to load the native library on a Mac OS X system.
   */
  public static synchronized boolean isAvailable() throws CoreMidiException {

    load();
    return available;

  }

}
