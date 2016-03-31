/*
* Copyright (c) 2016, Martin Wawro
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice, this
*    list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
* ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
* ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
* The views and conclusions contained in the software and documentation are those
* of the authors and should not be interpreted as representing official policies,
* either expressed or implied, of the FreeBSD Project.
*/
package org.mtnwrw.cameraexample.views;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

/**
 * View class to show the live camera image.
 *
 * <p>
 * This class is used to show the "preview" image which is currently captured by the camera
 * sensor. It computes the necessary adjustments with respect to the screen resolution and
 * orientation to perform an aspect-ratio-preserving display of the images.
 * </p>
 *
 * @author Martin Wawro
 */
public class LiveDisplay extends TextureView {
  private final String LOGTAG="LiveView";

  private Size DisplaySize = null;

  private Size SensorSize = null;
  /**
   * Native (default) device orientation. Usually 90 degrees for mobile phones and
   * 0 degrees for tablets.
   */
  private int NativeRotation=0;

  /**
   * Sensor orientation, already corrected to be relative to the same fixed coordinate frame
   * as the native rotation (i.e. if {@link #NativeRotation} and {@link #SensorRotation} are
   * the same, sensor is aligned to the native rotation.
   */
  private int SensorRotation=0;


  /**
   * Indicator whether or not the image axes of the preview images have to be swapped (i.e. the
   * camera image has to be rotates by 90 degrees CCW.
   */
  private boolean SwapAxes=false;

  /**
   * Constructor.
   *
   * @param context The application environment
   */
  public LiveDisplay(Context context) {
    super(context);
  }

  /**
   * Constructor.
   *
   * @param context The application context that the app is running under.
   *
   * @param attrs
   */
  public LiveDisplay(Context context, AttributeSet attrs) {
    super(context,attrs);
  }

  /**
   * Another constructor.
   *
   * @param context The application context that the app is running under.
   *
   * @param attrs
   *
   * @param defaultStyle
   */
  public LiveDisplay(Context context, AttributeSet attrs, int defaultStyle) {
    super(context,attrs,defaultStyle);
  }


  /**
   * Set the size of the preview image to be display.
   *
   * @param sensorSize The size (width x height) of the live images acquired from the camera
   *
   * @param nativeRotation The default rotation angle (in degrees) of the device that this app
   *                       is running on.
   *
   * @param sensorRotation The rotation angle (in degrees) of the camera sensor with respect to
   *                       the native rotation.
   *
   * @return A SurfaceTexture instance which should be the target buffer for the camera stack.
   */
  public Surface setPreviewSize(Size sensorSize,int nativeRotation,int sensorRotation) {
    SurfaceTexture tex = getSurfaceTexture();
    tex.setDefaultBufferSize(sensorSize.getWidth(), sensorSize.getHeight());
    SensorSize = sensorSize;
    SensorRotation = sensorRotation;
    NativeRotation = nativeRotation;
    Surface drawto = new Surface(tex);
    requestLayout();
    return drawto;
  }


  /**
   * Retrieve transformation matrix for sensor image
   *
   * <p>
   * This function computes a transformation matrix (2x2) for transforming the image obtained
   * from the sensor to fit into this view.
   * </p>
   *
   * @return A 2x2 transformation matrix that should be applied as coordinate transform to this
   *         view
   */
  public Matrix getSensorTransform() {
    Matrix T = new Matrix();
    if (SwapAxes) {
      T.setRectToRect(new RectF(0, 0, SensorSize.getWidth(), SensorSize.getHeight()), new RectF(0, 0, SensorSize.getHeight(), SensorSize.getWidth()), Matrix.ScaleToFit.FILL);
      T.postRotate(-90.0f);
      T.postTranslate(0.0f,DisplaySize.getHeight());
    }
    return T;
  }


  /**
   * Adjust the layouting parameters to render correct image aspect ratio
   *
   * <p>
   *
   * </p>
   *
   * @param widthMeasureSpec
   *
   * @param heightMeasureSpec
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);
    if (SensorSize != null) {
      boolean lsdisplay = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
      boolean lsnative = ((NativeRotation == 0)||(NativeRotation == 180));
      boolean lssensor = (!lsnative)^((SensorRotation == 0)||(SensorRotation == 180));
      //--------------------------------------------------------------------
      // Get actual preview texture dimensions. The texture is automatically
      // rotated to match the native device orientation (at least it is on
      // the devices that were tested so far).
      //--------------------------------------------------------------------
      SwapAxes = !(lsnative^lssensor^lsdisplay);
      int pwidth = (SwapAxes) ? SensorSize.getWidth() : SensorSize.getHeight();
      int pheight = (SwapAxes) ? SensorSize.getHeight() : SensorSize.getWidth();
      //--------------------------------------------------------------------
      // Aspect-ratio-aware scaling to best-fit...
      //--------------------------------------------------------------------
      float xscale = (float)width/(float)pwidth;
      float yscale = (float)height/(float)pheight;
      float scale = (xscale < yscale) ? xscale : yscale;
      width=(int)(scale*(float)pwidth);
      height=(int)(scale*(float)pheight);
      DisplaySize = new Size(width,height);
      setMeasuredDimension(width, height);
    }
  }
}
