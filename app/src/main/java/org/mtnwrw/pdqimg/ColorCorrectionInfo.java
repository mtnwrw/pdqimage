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

/**
 * Class that encapsulates information about (RAW) color correction values, to be fed into
 * the native code. Color correction includes white-balancing gains as well as transform
 * matrices from XYZ to sensor RGB space. In addition the black- and white levels for the
 * sensor are also stored here as well as an optional shading map that corrects for vignetting.
 *
 * @author Martin Wawro
 */
public class ColorCorrectionInfo {

  /**
   * White-balancing gains for each CFA component (in sensor order)
   */
  private float[] Gains = new float[4];
  /**
   * Calibration matrix from reference sensor to actual sensor for reference illuminant #1
   */
  private float[] Calibration1 = new float[9];
  /**
   * Calibration matrix from reference sensor to actual sensor for reference illuminant #2
   */
  private float[] Calibration2 = new float[9];
  /**
   * Transform matrix that maps XYZ (CIE) to sensor RGB for reference illuminant #1
   */
  private float[] Transform1 = new float[9];
  /**
   * Transform matrix that maps XYZ (CIE) to sensor RGB for reference illuminant #2
   */
  private float[] Transform2 = new float[9];
  /**
   * The black-levels for each of the CFA elements (in sensor order)
   */
  private int[] BlackLevels = new int[4];
  /**
   * The ID of reference illuminant #1
   */
  private int ReferenceIllum1 = 0;
  /**
   * The ID of reference illuminant #2
   */
  private int ReferenceIllum2 = 0;
  /**
   * The white-level (saturation-level) for each sensor element.
   */
  private int WhiteLevel;
  /**
   * In presence of a shading map, this provides the number of rows for that map.
   */
  private int ShadingRows = 0;
  /**
   * In presence of a shading map, this provides the number of columns for that map.*
   */
  private int ShadingCols = 0;
  /**
   * Optional shading map.
   */
  private float[] ShadingMap = null;


  /**
   * Create a color correction information token to be used from inside the native code.
   *
   * @param camChar The {@link CameraCharacteristics} of the used camera
   *
   * @param captureInfo The {@link TotalCaptureResult} of the snapshot
   */
  ColorCorrectionInfo(CameraCharacteristics camChar,TotalCaptureResult captureInfo) {
    if (captureInfo != null) {
      RggbChannelVector gains = (RggbChannelVector) captureInfo.get(CaptureResult.COLOR_CORRECTION_GAINS);
      Gains[0] = gains.getBlue();
      Gains[1] = gains.getRed();
      Gains[2] = gains.getGreenOdd();
      Gains[3] = gains.getGreenEven();
      if (camChar.get(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1) != null) ReferenceIllum1 = camChar.get(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1);
      if (camChar.get(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT2) != null) ReferenceIllum2 = camChar.get(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT2);
      copyTransformMatrix(camChar.get(CameraCharacteristics.SENSOR_COLOR_TRANSFORM1),Transform1);
      copyTransformMatrix(camChar.get(CameraCharacteristics.SENSOR_COLOR_TRANSFORM2), Transform2);
      copyCalibrationMatrix(camChar.get(CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM1),Calibration1);
      copyCalibrationMatrix(camChar.get(CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM2),Calibration2);
      if (camChar.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN) != null) {
        BlackLevelPattern bp = camChar.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN);
        BlackLevels[0] = bp.getOffsetForIndex(0,0);
        BlackLevels[1] = bp.getOffsetForIndex(0,1);
        BlackLevels[2] = bp.getOffsetForIndex(1,0);
        BlackLevels[3] = bp.getOffsetForIndex(1,1);
      }
      if (camChar.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL) != null) WhiteLevel = camChar.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL);
      LensShadingMap sm = captureInfo.get(CaptureResult.STATISTICS_LENS_SHADING_CORRECTION_MAP);
      if (sm != null) {
        ShadingRows = sm.getRowCount();
        ShadingCols = sm.getColumnCount();
        ShadingMap = new float[ShadingRows*ShadingCols*4];
        sm.copyGainFactors(ShadingMap,0);
      }
    }
  }

  /**
   * Copies a color-transform matrx into a float array for native processing.
   * 
   * @param src The source transformation matrix
   *
   * @param dst The target float array
   */
  private void copyTransformMatrix(ColorSpaceTransform src,float [] dst) {
    if (src != null) {
      for (int r = 0; r < 3; r++) {
        for (int c = 0; c < 3; c++) {
          dst[r * 3 + c] = src.getElement(c, r).floatValue();
        }
      }
    }
  }

  /**
   * Copies a calibration matrix into a float array for native processing.
   * <p>
   * In case the provided calibration matrix is null, the target is set to the identity matrix.
   * </p>
   *
   * @param src The source calibration matrix
   *
   * @param dst The target float array
   */
  private void copyCalibrationMatrix(ColorSpaceTransform src,float [] dst) {
    if (src != null) {
      for (int r = 0; r < 3; r++) {
        for (int c = 0; c < 3; c++) {
          dst[r * 3 + c] = src.getElement(c, r).floatValue();
        }
      }
    } else {
      for (int r=0;r<3;r++) dst[r*3+r]=1.0f;
    }
  }

}
