/*
* Copyright (c) 2016 Martin Wawro
*
* Free for non-commercial use. See LICENSE for license details.
*/
package org.mtnwrw.pdqimg;

import java.nio.ByteBuffer;

/**
 * Image wrapper for images processed by the PDQ image library.
 *
 * <p>
 * This class encapsulates an image after decompression by the {@link DecompressionService}.
 * It may be used as input for functions in the {@link ConversionService} or as means for direct
 * image access (comparable to android.media.Image).
 * </p>
 *
 * <p>
 * Important:<br/>
 * In order to free the resources taken up by an image, the image was be closed using the
 * close() method. The Java garbage collector alone is not sufficient to free all the resources.
 * </p>
 *
 * @author Martin Wawro
 */
public class PDQImage {

  //------------------------------------------------------------------------------------------------
  // Native code declarations
  //------------------------------------------------------------------------------------------------

  private native void internalClose();

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  /**
   * Enumerator for the Bayer raw pattern (cfa = color filter array) which is substructuring the
   * camera sensor.
   */
  public enum cfapattern {
    UNKNOWN,
    RGGB,
    BGGR,
    GBRG,
    GRBG
  }

  //------------------------------------------------------------------------------------------------
  // Nested class definitions
  //------------------------------------------------------------------------------------------------

  /**
   * Representation of a single image plane. An image can consist of 1 to 3 planes, depending
   * on the type of image. Every plane features a buffer that contains the actual pixel data.
   */
  class Plane {
    private ByteBuffer Buffer=null;
    private int PixelStride;
    private int RowStride;
    public ByteBuffer getBuffer() {
      return Buffer;
    }
    public int getRowStride() {
      return RowStride;
    }
    public int getPixelStride() {
      return PixelStride;
    }
  }

  //------------------------------------------------------------------------------------------------
  // Member variables
  //------------------------------------------------------------------------------------------------

  /**
   * The width of the image (measured in pixels).
   */
  private int Width;

  /**
   * The height of the image (measured in pixels).
   */
  private int Height;

  /**
   * The image format, as it is defined in android.graphics.ImageFormat
   */
  private int Format;

  /**
   * The internal image format (from the JNI side, for internal bookkeeping purpose, not used on
   * the Java side)
   */
  private int InternalFormat;

  /**
   * Data planes used for this image.
   */
  private Plane[] Planes;


  /**
   * Member for internal bookkeeping from the native side, do not access from Java.
   */
  private long InternalData = 0;


  /**
   * For raw images, defines the allocated bits per pixel (the store bits is implicitly assumed
   * to always be 16).
   */
  int RawBitsUsed;

  //------------------------------------------------------------------------------------------------
  // Class implementation
  //------------------------------------------------------------------------------------------------


  /**
   * Close image instance.
   *
   * <p>
   * Close the image instance and free up all internal resources that were used by the image.
   * Do not access the image after calling this function as pretty much all data will be
   * invalid.
   * </p>
   */
  public void close() {
    internalClose();
    for (int i=0;i<Planes.length;i++) Planes[i]=null;
    Width=0;
    Height=0;
  }

  /**
   * Retrieve the (Android) image format of the encapsulated image.
   *
   * @return The image format was defined in {@link android.graphics.ImageFormat}
   */
  public int getFormat() {
    return Format;
  }

  /**
   * Retrieve image width.
   *
   * @return Width of the image in pixels
   */
  public int getWidth() {
    return Width;
  }


  /**
   * Retrieve image height.
   *
   * @return Height of the image in pixels
   */
  public int getHeight() {
    return Height;
  }


  /**
   * Constructor.
   *
   * The constructor is for private-use only (invoked by native code during creation).
   *
   * @param numPlanes The number of planes for this image
   *
   * @param width The target width for this image (in pixels)
   *
   * @param height The target height for this image (in pixels)
   *
   * @param format The image format (as can be found in android.media.ImageFormat)
   *
   * @param internalFormat Internal format (associated with native code) for this image
   */
  private PDQImage(int numPlanes,int width,int height,int format,int internalFormat) {
    Planes = new Plane[numPlanes];
    Width=width;
    Height=height;
    Format=format;
    RawBitsUsed=8;
    InternalFormat=internalFormat;
  }

}
