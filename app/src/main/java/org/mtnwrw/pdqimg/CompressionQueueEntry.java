/*
* Copyright (c) 2016 Martin Wawro
*
* Free for non-commercial use. See LICENSE for license details.
*/
package org.mtnwrw.pdqimg;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.BlackLevelPattern;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.LensShadingMap;
import android.hardware.camera2.params.RggbChannelVector;
import android.media.Image;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Wrapper for image input and output data to be submitted to the compression queue.
 *
 * @author Martin Wawro
 */
public abstract class CompressionQueueEntry {

  //------------------------------------------------------------------------------------------------
  // Member variables
  //------------------------------------------------------------------------------------------------

  /**
   * Used by the native code. Do not touch.
   */
  private long InternalEntry = 0;


  /**
   * The quality to use for the compression of the associated image.
   */
  protected CompressionService.quality Quality;

  /**
   * Capture results that were communicated via CameraCaptureSession.CaptureCallback.
   */
  protected TotalCaptureResult CaptureMetaData;

  /**
   * Image acquired by the camera and communicated via the ImageReader
   */
  protected Image ImageData;

  /**
   * A native byte-buffer that will hold the compressed image data.
   */
  protected ByteBuffer OutputStream = null;

  /**
   * The number of bytes in the {@link #OutputStream}
   */
  protected int OutputSize = 0;

  /**
   * Defines the Bayer pattern of the camera sensor for RAW images.
   */
  protected PDQImage.cfapattern CFAPattern = PDQImage.cfapattern.UNKNOWN;


  /**
   * Defines the maximum bit-depth per color-component in RAW mode. Note that the current
   * version of the compressor cannot go deeper than 14 bits.
   */
  protected int RawBitDepth = 8;

  /**
   * Stores information about color correction for RAW images extracted in the constructor.
   */
  protected ColorCorrectionInfo ColorCorrection = null;

  /**
   * Stores basic capture meta-data which is extracted in the constructor.
   */
  protected CaptureInfo BasicMetaData = null;

  /**
   * Store geolocation information which is extracted in the constructor.
   */
  protected LocationInfo Location = null;

  /**
   * Stores information about the capture device extracted in the constructor.
   */
  protected DeviceInfo DeviceData = null;

  //------------------------------------------------------------------------------------------------
  // Class implementation
  //------------------------------------------------------------------------------------------------

  /**
   * Constructor.
   *
   * Simple constructor for a single compression queue entry. Please note that the
   * supplied outputStream must not be touched once this queue entry is submitted. It is
   * possible that the native implementation has to adjust the supplied ByteBuffer. Once the
   * finalization callback (see {@link #compressionDone(boolean)}) is invoked, it is safe to re-use
   * the ByteBuffer instance again.
   *
   * @param meta Image meta data originating from the capture
   *
   * @param image The actual image data from the capture
   *
   * @param outputStream A direct-memory ByteBuffer instance where to store the stream to. This
   *                     buffer must have been created by using the CompressionService.createOutputBuffer()
   *                     method
   *
   * @see {@link CompressionService#createOutputBuffer}
   */
  public CompressionQueueEntry(CameraCharacteristics camChar,TotalCaptureResult meta,Image image, CompressionService.quality quality, ByteBuffer outputStream) {
    CaptureMetaData = meta;
    ImageData = image;
    OutputStream = outputStream;
    Quality = quality;
    BasicMetaData = new CaptureInfo(camChar,meta);
    Location = new LocationInfo(meta);
    ColorCorrection = new ColorCorrectionInfo(camChar,meta);
    DeviceData = new DeviceInfo();
  }


  /**
   * Set the Bayer pattern which is used by the camera sensor (required for RAW images).
   *
   * @param pattern The {@link org.mtnwrw.pdqimg.PDQImage.cfapattern} enumerator that reflects
   *                the camera-sensor pattern.
   */
  public void setCFAPattern(PDQImage.cfapattern pattern) {
    CFAPattern = pattern;
  }


  /**
   * Release the resources taken-up by this instance.
   */
  void release() {
    CaptureMetaData=null;
    if (ImageData != null) ImageData.close();
    ImageData=null;
    OutputStream=null;
    OutputSize=0;
  }

  /**
   * Get compression quality for the associated image of this entry.
   *
   * @return Compression quality for the associated image of this entry.
   */
  public CompressionService.quality getQuality() {
    return Quality;
  }


  /**
   * Callback function which is invoked when the associated image has been compressed.
   *
   * <p>
   * Overide this callback function in order to define post-compression handling for the image
   * data which is associated to this queue entry. The callback itself is invoked from a background
   * thread from the native side and should return as soon as possible. Please use a queueing
   * mechanism in case heavy and/or slow data processing is to be done here.
   * </p>
   * <p>
   * In case this function returns without an error, the {@link #OutputStream} will contain a
   * compressed data stream which can be written to any destination (file, network, etc.).
   * </p>
   *
   * @param error Flag that indicates whether or not an error occured during the processing. If
   *              the error flag is set, the contents of the {@link #OutputStream} are undefined.
   */
  public abstract void compressionDone(boolean error);

}
