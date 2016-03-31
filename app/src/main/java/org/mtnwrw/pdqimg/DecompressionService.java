/*
* Copyright (c) 2016 Martin Wawro
*
* Free for non-commercial use. See LICENSE for license details.
*/
package org.mtnwrw.pdqimg;

import java.nio.ByteBuffer;

/**
 * Service class that provides decompression functionality for PDQ-compressed IFF images.
 *
 * <p>
 * Use this service to decompress images that were compressed with the libpdq using the
 * {@link CompressionService}. This class exclusively works on {@link ByteBuffer} instances and
 * not on streams and/or files. It is up to the caller to make sure that the data is loaded
 * from whatever device the compressed file was residing on.
 * </p>
 *
 * @author Martin Wawro
 */
public class DecompressionService {

  //------------------------------------------------------------------------------------------------
  // Native code declarations
  //------------------------------------------------------------------------------------------------

  private static native PDQImage decompressImageNative(ByteBuffer inputStream);
  private static native PDQImage decompressThumbnailNative(ByteBuffer inputStream);
  private static native PDQImage decompressPreviewNative(ByteBuffer inputStream);
  private static native ImageInformation getImageInformationNative(ByteBuffer inputStream);

  //------------------------------------------------------------------------------------------------
  // Nested class definitions
  //------------------------------------------------------------------------------------------------
  /**
   * Container for basic information about a compressed image. It can be filled by using the
   * DecompressionService.getImageInformation() method.
   *
   * @author Martin Wawro
   */
  static public class ImageInformation {
    /**
     * The width of the image (measured in pixels)
     */
    public int Width;
    /**
     * The height fo the image (measured in pixels)
     */
    public int Height;
    /**
     * The used depth (in bits) of the image. For all image types, except for raw images,
     * this is set to 8. For raw images, this is set to the number of bits that were regarded
     * by the compressor. The pixel size for the RAW_SENSOR format is fixed at 2 bytes per pixel,
     * this number here provides the <e>used bits</e> of that 16 bits.
     */
    public int UsedBits;
    /**
     * The image type, as can be found in android.media.ImageFormat
     */
    public int ImageType;
    /**
     * The used compression quality for this image.
     */
    public int Quality;
  }
  //------------------------------------------------------------------------------------------------
  // Implementation
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
   * Retrieve essential information about a (compressed) image without decompressing it.
   *
   * @param data The compressed byte-stream that represents the (full) image.
   *
   * @return {@link ImageInformation} object that summarizes the basic information (like resolution
   *         and (internal) format) of the compressed image in the data stream.
   */
  static public ImageInformation getImageInformation(ByteBuffer data) {
    return getImageInformationNative(data);
  }


  /**
   * Decompress a libpdq-compressed image.
   *
   * @param data The compressed byte-stream that represents the (full) image.
   *
   * @return Instance of {@link PDQImage} which contains a decompressed version of the supplied
   *         image.
   */
  static public PDQImage decompressImage(ByteBuffer data) {
    return decompressImageNative(data);
  }


  /**
   * Decompress a libpdq-compressed image to a size which is suitable for displaying a preview.
   *
   * @param data The compressed byte-stream that represents the (full) image.
   *
   * @return Instance of {@link PDQImage} which contains a downsized and decompressed version of the
   *         supplied image, suitable for display as preview image.
   */
  static public PDQImage decompressPreviewImage(ByteBuffer data) {
    return decompressPreviewNative(data);
  }

  /**
   * Decompress a libpdq-compressed image to a size which is suitable for displaying a thumbnail.
   *
   * @param data The compressed byte-stream that represents the (full) image.
   *
   * @return Instance of {@link PDQImage} which contains a downsized and decompressed version of the
   *         supplied image, suitable for display as thumbnail image.
   */
  static public PDQImage decompressThumbnailImage(ByteBuffer data) {
    return decompressThumbnailNative(data);
  }


}
