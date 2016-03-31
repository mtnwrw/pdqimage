/*
* Copyright (c) 2016 Martin Wawro
*
* Free for non-commercial use. See LICENSE for license details.
*/
package org.mtnwrw.pdqimg;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

/**
 * Encpasulates basic meta-data that originates from the camera setup, to be fed into the
 * native code.
 *
 * @author Martin Wawro
 */
public class CaptureInfo {
  private float ExposureCompensation=0.0f;
  private float Aperture=0.0f;
  private float FocusDistance=0.0f;
  private float FocalLength=0.0f;
  private int FlashState = CaptureResult.FLASH_STATE_UNAVAILABLE;
  private int Sensitivity = 0;
  private long ExposureTime = 0;
  private int Orientation = 0;

  /**
   * Constructor.
   *
   * <p>
   * This constructor fills the internal fields with some of the information that is conveyed by
   * the provided {@link CameraCharacteristics} and {@link TotalCaptureResult} objects. It's main
   * responsibility is therefore just some unwrapping of the data into PODs for better accessibility
   * by the native code.
   * </p>
   *
   * @param camChar The {@link CameraCharacteristics} object that belongs to the active camera.
   *
   * @param captureInfo {@link TotalCaptureResult} containing the meta-information from the last
   *                                               capture
   */
  CaptureInfo(CameraCharacteristics camChar,TotalCaptureResult captureInfo) {
    if ((camChar != null)&&(captureInfo != null)) {
      if ((captureInfo.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION) != null) &&
          (camChar.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP) != null)) {
        int esteps = (int) captureInfo.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION);
        float eunit = camChar.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP).floatValue();
        ExposureCompensation = eunit * (float) esteps;
      }
      if (captureInfo.get(CaptureResult.FLASH_STATE) != null) {
        FlashState = (int) captureInfo.get(CaptureResult.FLASH_STATE);
      }
      if (captureInfo.get(CaptureResult.LENS_APERTURE) != null) {
        Aperture = (float) captureInfo.get(CaptureResult.LENS_APERTURE);
      }
      if (captureInfo.get(CaptureResult.LENS_FOCUS_DISTANCE) != null) {
        FocusDistance = (float) captureInfo.get(CaptureResult.LENS_FOCUS_DISTANCE);
      }
      if (captureInfo.get(CaptureResult.LENS_FOCAL_LENGTH) != null) {
        FocalLength = (float) captureInfo.get(CaptureResult.LENS_FOCAL_LENGTH);
      }
      if (captureInfo.get(CaptureResult.SENSOR_SENSITIVITY) != null) {
        Sensitivity = (int) captureInfo.get(CaptureResult.SENSOR_SENSITIVITY);
      }
      if (captureInfo.get(CaptureResult.SENSOR_EXPOSURE_TIME) != null) {
        ExposureTime = (long) captureInfo.get(CaptureResult.SENSOR_EXPOSURE_TIME);
      }
      if (captureInfo.get(CaptureResult.JPEG_ORIENTATION) != null) {
        Orientation = (int) captureInfo.get(CaptureResult.JPEG_ORIENTATION);
      }
    }
  }
}
