/*
* Copyright (c) 2016 Martin Wawro
*
* Free for non-commercial use. See LICENSE for license details.
*/
package org.mtnwrw.pdqimg;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;

import java.nio.ByteBuffer;

/**
 * Service class that provides basic conversion functionality for {@link PDQImage} instances in
 * order to yield output formats that are either more suitable for display, processing or
 * data exchange.
 *
 * @author Martin Wawro
 */

public class ConversionService {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  private static final String LOGTAG = "Conv";

  //------------------------------------------------------------------------------------------------
  // Native code declarations
  //------------------------------------------------------------------------------------------------

  private static native Bitmap convertPDQImageToBitmapNative(PDQImage img,boolean highQuality);
  private static native ByteBuffer convertPDQImageToDNGNative(PDQImage img);

  //------------------------------------------------------------------------------------------------
  // Class implementation
  //------------------------------------------------------------------------------------------------

  static {
    try {
      System.loadLibrary("pdqimg");
    } catch (UnsatisfiedLinkError ex) {
      ex.printStackTrace();
    } catch (NoSuchMethodError ex) {
      ex.printStackTrace();
    }
  }


  /**
   * Convert PDQImage instance to displayable bitmap.
   *
   * @param img A {@link PDQImage} object that encapsulates an image which was processed by the
   *            libpdq image library.
   *
   * @return {@link Bitmap} instance that contains an easily-displayable version of the image
   *                        that was encapsulated by the provided {@link PDQImage} object.
   */
  public static Bitmap convertPDQImageToBitmap(PDQImage img,boolean highQuality) {
    return convertPDQImageToBitmapNative(img,highQuality);
  }


  /**
   * Convert a RAW-subtype PDQImage to a DNG bytestream for export.
   *
   * <p>
   * This function generates a ready-to-write DNG image in RAM and returns a buffer encapsulating
   * that image. The buffer can be written into any output file or transferred via a socket
   * in order to move it to its designated destination.
   * </p>
   *
   * <p>
   * The current implementation is rather rudimentary and many tags are not written. It is planned
   * to improve on that function in the future.
   * </p>
   *
   * @param img A {@link PDQImage} object (of RAW subtype) that encapsulates an image which was
   *            processed by the libpdq image library.
   *
   * @return A filled {@link ByteBuffer} instance (direct kind) that contains the data to be
   *         written as DNG file.
   *
   */
  public static ByteBuffer convertPDQImageToDNG(PDQImage img) throws IllegalArgumentException {
    if (img.getFormat() == ImageFormat.RAW_SENSOR) {
      return convertPDQImageToDNGNative(img);
    } else {
      throw new IllegalArgumentException();
    }
  }

}
