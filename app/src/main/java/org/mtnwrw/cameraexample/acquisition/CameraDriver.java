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
package org.mtnwrw.cameraexample.acquisition;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import org.mtnwrw.cameraexample.util.BinarySemaphore;
import org.mtnwrw.pdqimg.PDQImage;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Support class that interacts with Android's camera interface.
 *
 * <p>
 * This class aggregates basic function blocks that are required to interact with Android's
 * camera stack.
 * </p>
 *
 * @author Martin Wawro
 */
public class CameraDriver {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------
  private static final String LOGTAG = "CamDrv";
  private static final String PREFS = "AcquisitionPreferences";
  private static final int MAX_WAIT_COMPLETION = 5000;

  /**
   * The number of buffers which should be used (internally) by the ImageReader instance.
   *
   * @see #CaptureReader
   */
  // TODO (mw) find something better than a mere constant here
  private static final int NumInternalBuffers = 20;


  enum PreviewState {
    PREVIEW,
    FOCUS,
    PRECAPTURE,
    PRECAPTURE_FLASH,
    AAA_COMPLETE
  };

  //------------------------------------------------------------------------------------------------
  // Member variables
  //------------------------------------------------------------------------------------------------


  /**
   * The active snapshot policy. This basically controls how the "full frame" images are to be
   * shot.
   *
   * @see {@link AcquisitionPolicy}, {@link SerialAcquisitionPolicy}
   */
  protected AcquisitionPolicy SnapshotPolicy = null;

  /**
   * Instance of render surface where the camera live image should be placed on.
   */
  protected Surface LiveSurface = null;

  /**
   * Storage for capture results of full-frame captures (to be matched with images).
   */
  protected LinkedList<TotalCaptureResult> CaptureResults = new LinkedList<TotalCaptureResult>();


  protected LinkedList<Image> PendingImages = new LinkedList<Image>();

  /**
   * Event handler which is to be used when capturing (actual snapshots, not live previews)
   */
  protected SnapshotEventInterface SnapHandler = null;

  /**
   * The controlling activity which is currently running
   */
  protected Activity CurrentActivity;

  /**
   * An instance of ImageReader which is to be used for processing the captures (snapshots)
   *
   * @see #NumInternalBuffers
   */
  protected ImageReader CaptureReader;

  /**
   * Used for bookkeeping on image acquisition an to check whether or not all frames have
   * made it to the image-output-queue.
   */
  protected AtomicLong ImagedFrame = new AtomicLong(-1);

  /**
   * Used for bookkeeping (contains the frame number of the last started capturerequest)
   */
  protected AtomicLong StartedFrame = new AtomicLong(-1);


  /**
   * Used for bookkeeping on image acquisition and to check whether or not all frames
   * have made it to the image-output-queue.
   */
  protected AtomicLong CompletedFrame = new AtomicLong(-1);

  /**
   * The image format to use for captures (snapshots). Should be either {@code YUV_420_888} or
   * {@code RAW_SENSOR}.
   */
  int CaptureFormat = ImageFormat.YUV_420_888;

  /**
   * Resource counter that tracks available buffers for capture compression.
   */
  Semaphore AvailableBuffers = null;

  /**
   * Semaphore that provides a stat variable whether or not we are in a recording session.
   * May be used to wait/sleep on it.
   */
  BinarySemaphore InSession = new BinarySemaphore(true);

  /**
   * Handler thread where all the camera/picture acquisition takes place.
   */
  protected HandlerThread CameraThread;

  /**
   * The handler that runs in the {@link #CameraThread}. Takes care of processing camera
   * events.
   */
  protected Handler CameraHandler;

  /**
   * Reference to actual camera device.
   */
  protected CameraDevice CameraDev;

  /**
   * Request builder for preview requests.
   */
  protected CaptureRequest.Builder PreviewBuilder;

  /**
   * Capture request used for preview images
   */
  protected CaptureRequest PreviewRequest;

  /**
   * Capture session that manages the camera requests.
   */
  protected CameraCaptureSession CaptureSession;

  /**
   * CFA (Bayer) pattern used by the camera sensor (for raw images)
   */
  protected PDQImage.cfapattern CFAPattern;

  /**
   * The (used) bit depth for the raw images acquired by the sensor.
   */
  protected int RawBitDepth = 8;

  /**
   * The rotation angle (in degrees) of the camera sensor with respect to the native orientation
   * of the device.
   *
   * @see #NativeRotation
   */
  protected int SensorRotation;

  /**
   * The rotation angle (in degrees) of the handheld device. A value of 0 refers to landscape
   * orientation.
   *
   * @see #SensorRotation
   */
  protected int NativeRotation;

  /**
   * The characteristics of the currently-used camera.
   */
  protected CameraCharacteristics Characteristics;

  /**
   * Indicator whether or not this instance is currently performing full-frame recording.
   */
  boolean Recording = false;


  PreviewState PreState = PreviewState.PREVIEW;

  /**
   * Size for full-frame capture.
   *
   * @see #findLargestSize
   */
  Size CaptureSize;

  /**
   * Size for the live-preview image.
   *
   * @see #findMatchingSize
   */
  Size LivePreviewSize;

  //------------------------------------------------------------------------------------------------
  // Callback implementations
  //------------------------------------------------------------------------------------------------

  /**
   * Callback implementation for capture-status for snapshots/captures.
   */
  protected CameraCaptureSession.CaptureCallback FullFrameCallback = new CameraCaptureSession.CaptureCallback() {

    @Override
    public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
      InSession.release();
    }

    @Override
    public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
      InSession.release();
    }

    /**
     * Callback handler which is invoked when capture meta-data is available.
     *
     * <p>
     * This function is invoked by the {@link CameraDriver} when the capture has been completed (but
     * the image is not completely processed/available yet). It provides meta-data obtained during
     * the image capture in the parameters. This implementation simply appends the provided
     * capture results to an internal list of capture results to be matched later in conjunction with
     * the onImageAvailable() function. It also increments the {@link CameraDriver#CompletedFrame}
     * counter to match the frame-number that has been processed by this function.
     * </p>
     *
     * @param session The currently active {@link CameraCaptureSession}
     *
     * @param request The {@link CaptureRequest} that caused the invokation of this function
     *
     * @param result The collected capture results ({@link TotalCaptureResult}) for this frame (the
     *               "meta-data" for the corresponding image).
     *
     */
    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
      long myframe = result.getFrameNumber();
      long curr = CompletedFrame.get();
      while (curr < myframe) {
        if (CompletedFrame.compareAndSet(curr, myframe)) break;
        curr = CompletedFrame.get();
      }
      synchronized (CaptureResults) {
        CaptureResults.addLast(result);
      }
      synchronized (PendingImages) {
        if (!PendingImages.isEmpty()) processPendingImages();
      }
    }

    /**
     * Handler that is invoked when a full-frame capture has been started
     *
     * @param session The currently active {@link CameraCaptureSession}
     *
     * @param request The {@link CaptureRequest} that caused the invokation of this function
     *
     * @param timestamp Timestamp which signifies the beginning of the acquisition (will match with
     *                  the sensor timestamp in the {@link Image})
     *
     * @param frameNumber The frame number (i.e. an image sequence number) that has been started.
     *                    Will match frame numbers in the completion handler.
     */
    @Override
    public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
      long curr = StartedFrame.get();
      while (curr < frameNumber) {
        if (StartedFrame.compareAndSet(curr, frameNumber)) break;
        curr = StartedFrame.get();
      }
    }

    @Override
    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
      Log.w(LOGTAG, "onCaptureFailed() " + failure.getFrameNumber());
    }

  };


  /**
   * Callback implementation for capture-status of the preview channel.
   */
  protected CameraCaptureSession.CaptureCallback PreviewCallback = new CameraCaptureSession.CaptureCallback() {

    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
      switch (PreState) {
        case FOCUS:
          if (result.get(CaptureResult.CONTROL_AF_STATE) == null) PreState = PreviewState.AAA_COMPLETE;
          else {
            int focus = (int) result.get(CaptureResult.CONTROL_AF_STATE);
            if ((focus == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) || (focus == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)) {
              if (result.get(CaptureResult.CONTROL_AE_STATE) != null) {
                PreState=PreviewState.PRECAPTURE;
                PreviewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                try {
                  session.capture(PreviewBuilder.build(), PreviewCallback, CameraHandler);
                } catch (CameraAccessException ex) {
                  ex.printStackTrace();
                  PreState=PreviewState.AAA_COMPLETE;
                }
              } else {
                PreState=PreviewState.AAA_COMPLETE;
              }
            }
          }
          break;
        case PRECAPTURE:
          if (result.get(CaptureResult.CONTROL_AE_STATE) == null) PreState = PreviewState.AAA_COMPLETE;
          else {
            int aestate = (int)result.get(CaptureResult.CONTROL_AE_STATE);
            if (aestate == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
              PreState = PreviewState.PRECAPTURE_FLASH;
            } else if (aestate != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
              PreState = PreviewState.AAA_COMPLETE;
            }
          }
          break;
        case PRECAPTURE_FLASH:
          if (result.get(CaptureResult.CONTROL_AE_STATE) == null) PreState = PreviewState.AAA_COMPLETE;
          else {
            int aestate = (int) result.get(CaptureResult.CONTROL_AE_STATE);
            if (aestate != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
              PreState = PreviewState.AAA_COMPLETE;
            }
          }
          break;
        case AAA_COMPLETE:
          if (request.get(CaptureRequest.CONTROL_AF_TRIGGER)!=null) {
            if (request.get(CaptureRequest.CONTROL_AF_TRIGGER) == CameraMetadata.CONTROL_AF_TRIGGER_CANCEL) {
              PreState=PreviewState.PREVIEW;
            }
          }
          break;
        default:
          break;
      }
    }
  };


  /**
   * Callback implementation for image-readout of the snapshot/capture channel.
   */
  protected ImageReader.OnImageAvailableListener CaptureListener = new ImageReader.OnImageAvailableListener() {

    /**
     * Callback which is triggered when a captured image is available.
     *
     * <p>
     * This function is invoked from the camera subsystem everytime an image is available
     * from the camera. It is (usually) invoked after the meta-data has been propagated in
     * the capture results and in this function the image is matched with its meta-data based
     * on the time-stamp in this function.
     *
     * After matching the image with its metadata, the combination is queued to the event handler.
     * </p>
     *
     * @param imageReader The ImageReader which contains the image available for readout
     */
    @Override
    public void onImageAvailable(ImageReader imageReader) {
      if ((Recording) && (AvailableBuffers.tryAcquire())) {
        Image latest = imageReader.acquireLatestImage();
        if (latest == null) {
          AvailableBuffers.release();
          return;
        }
        if (!processIncomingImage(latest)) {
          synchronized (PendingImages) {
            if (Recording) PendingImages.addFirst(latest);
          }
        }
      } else {
        //--------------------------------------------------
        // Cannot process the image, just make sure it gets
        //  released again...
        //--------------------------------------------------
        Image latest = imageReader.acquireLatestImage();
        if (latest != null) latest.close();
        AvailableBuffers.release();
      }
    }
  };


  /**
   *
   */
  protected CameraDevice.StateCallback CameraStatusCB = new CameraDevice.StateCallback() {

    @Override
    public void onOpened(CameraDevice cameraDevice) {
      setupCaptureSession(cameraDevice);
      CameraDev = cameraDevice;
    }

    @Override
    public void onDisconnected(CameraDevice cameraDevice) {
      shutdown();
      cameraDevice.close();
      CameraDev = null;
    }

    @Override
    public void onError(CameraDevice cameraDevice, int errCode) {
      Log.e(LOGTAG, "Camera reported error code " + errCode);
    }
  };


  //------------------------------------------------------------------------------------------------
  // Class implementation
  //------------------------------------------------------------------------------------------------

  /**
   * Simple constructor
   *
   * @param currentActivity The Activity that this driver is running under
   * @param handler         The handler to be used for snapshot (full-frame) images. Must implement the
   *                        {@link SnapshotEventInterface}
   */
  public CameraDriver(Activity currentActivity, SnapshotEventInterface handler) {
    CurrentActivity = currentActivity;
    SnapHandler = handler;
  }


  /**
   * Initialize driver instance by starting a handler thread and an event loop which is to be
   * used for processing preview/capture events.
   *
   * @see #CameraThread,#CameraHandler
   */
  public void init() {
    CameraThread = new HandlerThread("Camera Thread");
    CameraThread.start();
    CameraHandler = new Handler(CameraThread.getLooper());
  }


  /**
   * Obtain image size (in pixels) for preview images
   *
   * @return Pixel size for preview images.
   */
  public Size getLivePreviewSize() {
    return LivePreviewSize;
  }


  /**
   * Get image size (in pixels) for full-frame captures.
   *
   * @return Pixel size of the full-frame capture images
   */
  public Size getCaptureSize() {
    return CaptureSize;
  }


  /**
   * Obtain sensor rotation w.r.t. native device orientation
   *
   * @return The rotation of the sensor w.r.t. the native orientation of the device (in degrees)
   */
  public int getSensorRotation() {
    return SensorRotation;
  }


  /**
   * Retrieve the native device orientation in degrees.
   * <p/>
   * <p>
   * This function returns the native device orientation in degrees. A value of 0 degrees
   * indicates a landscape orientation (e.g. width > height) whereas a value of
   * </p>
   *
   * @return
   */
  public int getNativeRotation() {
    return NativeRotation;
  }


  /**
   * Set the policy class to be used for obtaining full-frame captures.
   *
   * @param policy Instance that must implement the {@link AcquisitionPolicy} interface and provides
   *               functionality to create and start full-frame captures.
   */
  public void setSnapshotPolicy(AcquisitionPolicy policy) {
    SnapshotPolicy = policy;
    if (SnapshotPolicy != null) {
      if (CameraDev != null) {
        try {
          SnapshotPolicy.apply(CameraDev, CaptureReader.getSurface());
        } catch (CameraAccessException ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  /**
   * Adjust the output format for full frame captures.
   *
   * @param format The format to use for full-frame captures (currently only YUV 420 and RAW
   *               formats are supported)
   *
   * @see {@link ImageFormat}
   */
  public void setCaptureFormat(int format) {
    if ((CaptureFormat != format)&&(!isRecording())) {
      CaptureFormat = format;
      if (CameraDev != null) setupCaptureSession(CameraDev);
    }
  }


  /**
   * Retrieve the currently selected capture image format
   *
   * @return The image format to use for capturing full-frame images.
   */
  public int getCaptureFormat() {
    return CaptureFormat;
  }


  /**
   * Check if an actual (full frame) snapshot session is in progress.
   *
   * @return true if camera is currently recording snapshots, false otherwise.
   */
  public boolean isRecording() {
    return Recording;
  }


  /**
   * Shuts down the camera driver.
   * <p/>
   * <p>
   * This function stops all camera handler thread(s) and closes the camera device. This function
   * does not wait for any pending sessions to complete.
   * </p>
   */
  public void shutdown() {
    if (CaptureSession != null) try {
      CaptureSession.stopRepeating();
    } catch (CameraAccessException ex) {
      ex.printStackTrace();
    }
    if (CameraDev != null) {
      try {
        CameraDev.close();
      } catch (IllegalStateException ex) {
        Log.i(LOGTAG, "Camera was already closed");
      }
      CameraDev = null;
    }
    CameraThread.quitSafely();
    try {
      CameraThread.join();
      CameraHandler = null;
      CameraThread = null;
    } catch (InterruptedException ex) {
      ex.printStackTrace();
    }
    Recording = false;
  }


  /**
   * Set the surface where the live-preview image is to be rendered onto.
   *
   * @param live The surface which should be used for rendering the live image on
   * @see #LiveSurface
   */
  public void setLiveSurface(Surface live) {
    LiveSurface = live;
  }


  /**
   * Start actual full-frame image capture (snapshots).
   *
   * <p>
   * This function stops the (supposedly running) live stream and activates the capture
   * session.
   * </p>
   *
   * @return true if capturing was started successfully, false otherwise
   */
  public boolean startSnapshots() {
    if ((CaptureSession == null) || (SnapHandler == null) || (SnapshotPolicy == null)) return false;
    try {
      //--------------------------------------------------------
      // Check if we are still in an acquisition session and
      // bail out if this is the case...
      //--------------------------------------------------------
      if (Recording) return false;
      if (!InSession.tryAcquire()) {
        Log.w(LOGTAG,"Still in session");
        return false;
      }
      CaptureResults.clear();
      StartedFrame.set(-1);
      CompletedFrame.set(-1);
      ImagedFrame.set(-1);
      //--------------------------------------------------------
      // Stop preview capture and start the full-frame capture
      //--------------------------------------------------------
      CaptureSession.stopRepeating();
      SnapHandler.newRoll();
      Recording = true;
      SnapshotPolicy.activate(CaptureSession, FullFrameCallback, CameraHandler);
      return true;
    } catch (CameraAccessException ex) {
      ex.printStackTrace();
    }
    return false;
  }


  /**
   * Stop the recording of snapshots and wait until all images have been saved.
   */
  public void stopSnapshots() {
    //-------------------------------------------------
    // First stop the full-frame capture...
    //-------------------------------------------------
    try {
      if (CaptureSession != null) CaptureSession.stopRepeating();
    } catch (CameraAccessException ex) {
      ex.printStackTrace();
    }
    //-------------------------------------------------
    // Make sure that all outstanding images have been
    // finalized and reset the recording state...
    //-------------------------------------------------
    try {
      if ((Recording) && (CaptureSession != null)) {
        InSession.acquire();
      }
    } catch (InterruptedException ex) {
      ex.printStackTrace();
    }
    //-------------------------------------------------
    // Make sure that the snapshot handler is done with
    // the roll..
    //-------------------------------------------------
    if (SnapHandler != null) {
      //-----------------------------------------------
      // Wait until we reached the last completed frame
      // with our imagereader pipeline. Or bail out
      // trying...
      //-----------------------------------------------
      try {
        long complete = CompletedFrame.get();
        int delay = 0;
        while (ImagedFrame.get() < complete) {
          synchronized (CaptureResults) {
            if (CaptureResults.size() == 0) break;
          }
          synchronized (this) {
            wait(250);                                  // NOTE (mw) a condition would be nicer here
            delay+=250;
          }
          if (delay > MAX_WAIT_COMPLETION) {
            Log.w(LOGTAG,"Waited for "+delay+"ms for completion. Getting impatient and bailing out.");
            break;
          }
        }
      } catch (InterruptedException ex) {
      }
      SnapHandler.rollDone();
      SnapHandler.waitRoll();
      SnapHandler.cleanUp();
    }
    //-------------------------------------------------
    // Clean up pending images and capture results...
    //-------------------------------------------------
    synchronized (CaptureResults) {
      CaptureResults.clear();
    }
    Recording = false;
    synchronized (PendingImages) {
      processPendingImages();
    }
    InSession.release();
    //-------------------------------------------------
    // Finally re-enable the preview again...
    //-------------------------------------------------
    try {
      if (CaptureSession != null) {
        if (PreState == PreviewState.AAA_COMPLETE) {
          PreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
          PreState = PreviewState.PREVIEW;
          CaptureSession.capture(PreviewBuilder.build(), PreviewCallback, CameraHandler);
        }
        CaptureSession.setRepeatingRequest(PreviewRequest, PreviewCallback, CameraHandler);
      }
    } catch (CameraAccessException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Retrieve the color-filter-array pattern (Bayer pattern) for raw images.
   *
   * @return The Bayer CFA pattern for raw images
   */
  public PDQImage.cfapattern getCFAPattern() {
    return CFAPattern;
  }


  /**
   * Retrieve maximum bit-depth for any raw color channel.
   *
   * @return The maximum number of bits per color channel in raw images.
   */
  public int getRawBits() {
    return RawBitDepth;
  }


  /**
   * Retrieve {@link CameraCharacteristics} of the currently used camera
   *
   * @return The characteristics of the currently used camera.
   */
  public CameraCharacteristics getCameraCharacteristics() {
    return Characteristics;
  }


  /**
   * Setup capture/live-preview sessions with the back-facing camera
   * <p>
   * This function tries to open the back-facing camera of the device and determines the
   * resolutions to be used, which are stored in {@link #CaptureSize} and {@link #LivePreviewSize}.
   * </p>
   *
   * @param displaySize The size of the display which is to be used to show live-preview images. It
   *                    does not have to be exact, but it must be possible to tell the rotation
   *                    (portrait or landscape) from the aspect ratio.
   * @return A string containing the camera ID to use see {@link #openCamera(String)} or null on
   *         failure
   *
   */
  public String setupCamera(Size displaySize) {
    int imgFormat = CaptureFormat;
    CameraManager mgr = (CameraManager) CurrentActivity.getSystemService(Context.CAMERA_SERVICE);
    String useCamID = null;
    //--------------------------------------------------------
    // First find the (first) back facing camera...
    //--------------------------------------------------------
    try {
      for (String camID : mgr.getCameraIdList()) {
        Characteristics = mgr.getCameraCharacteristics(camID);
        if (Characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
          useCamID = camID;
          break;
        }
      }
      //--------------------------------------------------------
      // ...if we found a usable camera, select a good output-
      // and a matching preview-size.
      //--------------------------------------------------------
      if (useCamID != null) {
        SensorRotation = mgr.getCameraCharacteristics(useCamID).get(CameraCharacteristics.SENSOR_ORIENTATION);
        Size matchsize;
        int deforient = getDeviceDefaultOrientation();
        switch (deforient) {
          case Surface.ROTATION_0:
            NativeRotation = 0;
            break;
          case Surface.ROTATION_90:
            NativeRotation = 90;
            break;
          case Surface.ROTATION_180:
            NativeRotation = 180;
            break;
          case Surface.ROTATION_270:
            NativeRotation = 270;
            break;
        }
        //--------------------------------------------------------
        // See if we need to look for the right resolution using
        // a rotated version of the target resolution (assuming
        // that sensor resolution is always provided in landscape)
        //--------------------------------------------------------
        if (displaySize.getWidth() <= displaySize.getHeight()) {
          matchsize = new Size(displaySize.getHeight(), displaySize.getWidth());
        } else {
          matchsize = new Size(displaySize.getWidth(), displaySize.getHeight());
        }
        //--------------------------------------------------------
        // Determine Bayer CFA pattern for raw mode...
        //--------------------------------------------------------
        if (Characteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT) != null) {
          switch (Characteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)) {
            case CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB:
              CFAPattern = PDQImage.cfapattern.RGGB;
              break;
            case CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR:
              CFAPattern = PDQImage.cfapattern.BGGR;
              break;
            case CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG:
              CFAPattern = PDQImage.cfapattern.GBRG;
              break;
            case CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG:
              CFAPattern = PDQImage.cfapattern.GRBG;
              break;
            default:
              CFAPattern = PDQImage.cfapattern.UNKNOWN;
              break;
          }
        } else CFAPattern = PDQImage.cfapattern.UNKNOWN;
        //--------------------------------------------------------
        // Determine bit-depth for RAW data
        //--------------------------------------------------------
        RawBitDepth = 14;
        if (Characteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL) != null) {
          int maxval = (int) Characteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL);
          if (maxval > 0) {
            RawBitDepth = Math.min(14, (int) Math.ceil(Math.log((double) maxval) / Math.log(2.0d)));
          }
        }
        //--------------------------------------------------------
        // Determine appropriate preview/capture sizes...
        //--------------------------------------------------------
        CaptureSize = findLargestSize(mgr, useCamID, imgFormat);
        float targetar = (float) CaptureSize.getWidth() / (float) CaptureSize.getHeight();
        LivePreviewSize = findMatchingSize(mgr, useCamID, SurfaceTexture.class, matchsize, targetar);
        if ((CaptureSize != null) && (LivePreviewSize != null)) {
          return useCamID;
        }
      }
    } catch (CameraAccessException acc) {
      acc.printStackTrace();
    }
    return null;
  }


  /**
   * Open camera device by its ID
   *
   * @param cameraID The ID of the camera which should be opened
   */
  public boolean openCamera(String cameraID) {
    CameraManager mgr = (CameraManager) CurrentActivity.getSystemService(Context.CAMERA_SERVICE);
    try {
      //------------------------------------------------------
      // Open the camera device. On success, the callback
      // will create preview and full-frame capture reqs for
      // later use.
      //------------------------------------------------------
      mgr.openCamera(cameraID, CameraStatusCB, CameraHandler);
      return true;
    } catch (CameraAccessException ex) {
      ex.printStackTrace();
    } catch (SecurityException ex) {
      ex.printStackTrace();
    }
    return false;
  }

  /**
   *
   */
  public void perform3A() {
    if ((PreState == PreviewState.PREVIEW) || (PreState == PreviewState.AAA_COMPLETE)) {
      try {
        if (PreState != PreviewState.PREVIEW) {
          PreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
          PreviewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
          CaptureSession.capture(PreviewBuilder.build(), PreviewCallback, CameraHandler);
          try {
            while (PreState != PreviewState.PREVIEW) Thread.sleep(100);
          } catch (InterruptedException ex) {}
          CaptureSession.stopRepeating();
          CaptureSession.setRepeatingRequest(PreviewRequest, PreviewCallback, CameraHandler);
        }
        PreState = PreviewState.FOCUS;
        PreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        CaptureSession.capture(PreviewBuilder.build(), PreviewCallback, CameraHandler);
      } catch (CameraAccessException ex) {
        ex.printStackTrace();
      }
    }
  }


  /**
   * Create capture session (incl. preview)
   *
   * <p>
   * This function sets up a (new) capture session and makes sure that the appropriate
   * target surfaces (preview and capture) are registered with the session and the corresponding
   * request builders.
   * </p>
   *
   * @param cameraDevice The currently selected camera device to use for capturing/live-display
   */
  protected void setupCaptureSession(CameraDevice cameraDevice) {
    if (isRecording()) return;
    try {
      if (CaptureSession != null) {
        CaptureSession.stopRepeating();
        CaptureSession.close();
        CaptureSession = null;
      }
      if (CaptureReader != null) {
        CaptureReader.setOnImageAvailableListener(null,null);
        CaptureReader = null;
      }
      CaptureReader = ImageReader.newInstance(CaptureSize.getWidth(), CaptureSize.getHeight(), CaptureFormat, NumInternalBuffers);
      if (PreviewBuilder == null) {
        PreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        PreviewBuilder.addTarget(LiveSurface);
        PreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        PreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
      }
      CaptureReader.setOnImageAvailableListener(CaptureListener, CameraHandler);
      AvailableBuffers = new Semaphore(NumInternalBuffers - 1);
      cameraDevice.createCaptureSession(Arrays.asList(LiveSurface, CaptureReader.getSurface()), new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
          CaptureSession = cameraCaptureSession;
          try {
            PreviewRequest = PreviewBuilder.build();
            cameraCaptureSession.setRepeatingRequest(PreviewRequest, PreviewCallback, CameraHandler);
          } catch (CameraAccessException ex) {
            ex.printStackTrace();
          }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
          Log.e(LOGTAG, "Unable to create camera capture session");
        }
      }, null);
    } catch (CameraAccessException ex) {
      ex.printStackTrace();
    }
    if (SnapshotPolicy != null) {
      try {
        SnapshotPolicy.apply(cameraDevice, CaptureReader.getSurface());
      } catch (CameraAccessException ex) {
        ex.printStackTrace();
      }
    }
  }


  /**
   * Obtain the default orientation of the current device.
   *
   * <p>
   * As different device-types (e.g. tables vs. phones) may have different "default" orientations
   * which should be taken into account for a proper display, this function tries to determine
   * what is the device's default orientation.
   * <p/>
   *
   * <p>
   * The code here is borrowed and adjusted from StackExchange
   * </p>
   *
   * @return The default orientation of the device
   */
  protected int getDeviceDefaultOrientation() {
    Configuration config = CurrentActivity.getResources().getConfiguration();
    int rotation = CurrentActivity.getWindowManager().getDefaultDisplay().getRotation();
    if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
      config.orientation == Configuration.ORIENTATION_LANDSCAPE)
      || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
      config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
      return Configuration.ORIENTATION_LANDSCAPE;
    } else {
      return Configuration.ORIENTATION_PORTRAIT;
    }
  }


  /**
   * Find the best-suited size that is close to the supplied reference size (and its aspect ratio)
   * which the camera can deliver for the selected image format.
   *
   * @param mgr      Camera manager instance for this device
   *
   * @param camID    The ID of the camera to use
   *
   * @param tClass   The class of the Surface we want to use with the camera
   *
   * @param refSize  The reference size to match with the camera
   *
   * @param targetAR The aspect ratio to match (should coincide with the AR of the full-frame format to deliver good previews)
   *
   * @return The size which might be best suited to match the given reference size
   *
   * @throws CameraAccessException
   */
  protected Size findMatchingSize(CameraManager mgr, String camID, Class tClass, Size refSize, float targetAR) throws CameraAccessException {
    CameraCharacteristics camtype = mgr.getCameraCharacteristics(camID);
    StreamConfigurationMap streams = camtype.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    if ((streams != null) && (streams.getOutputSizes(tClass) != null)) {
      Size bestmatch = null;
      float penalty = Float.MAX_VALUE;
      for (Size size : streams.getOutputSizes(tClass)) {
        float ar = (float) size.getWidth() / (float) size.getHeight();
        float wd = (float) (size.getWidth() - refSize.getWidth()) / (float) refSize.getWidth();
        float hd = (float) (size.getHeight() - refSize.getHeight()) / (float) refSize.getHeight();
        wd = (wd < 0) ? (-2.0f * wd) : wd;
        hd = (hd < 0) ? (-2.0f * hd) : hd;
        float par = (Math.abs(ar - targetAR) < 1.0E-3f) ? 0.0f : 10.0f;
        float p = par + wd + hd;
        if (p < penalty) {
          bestmatch = size;
          penalty = p;
        }
      }
      return bestmatch;
    }
    return null;
  }


  /**
   * Find and return the largest available size (in terms of total area) that the selected camera
   * can output for the specified image format.
   *
   * @param mgr   Camera manager instance for this device
   *
   * @param camID The ID of the camera to use
   *
   * @param fmt   The image format we want to use with the camera
   *
   * @return The maximum size that the camera can deliver for the supplied image format
   *
   * @throws CameraAccessException
   */
  protected Size findLargestSize(CameraManager mgr, String camID, int fmt) throws CameraAccessException {
    CameraCharacteristics camtype = mgr.getCameraCharacteristics(camID);
    StreamConfigurationMap streams = camtype.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    if ((streams != null) && (streams.getOutputSizes(fmt) != null)) {
      int largestarea = 0;
      Size fullsize = null;
      for (Size size : streams.getOutputSizes(fmt)) {
        if (largestarea > 0) {
          int area = size.getWidth() * size.getHeight();
          if (largestarea < area) {
            largestarea = area;
            fullsize = size;
          }
        } else {
          fullsize = size;
          largestarea = size.getWidth() * size.getHeight();
        }
      }
      return fullsize;
    }
    return null;
  }


  /**
   * Process a single incoming image for full-frame processing (i.e. compression)
   *
   * <p>
   * This function tries to match-up the image with its meta-data (by using the timestamps of both).
   * In case a corresponding meta-data entry has been found, this function schedules the pair for
   * processing by the {@link #SnapHandler} which does all the rest of the work.
   * </p>
   *
   * @param img The {@link android.media.Image} instance (more or less fresh from the camera) that
   *            is to be processed
   *
   * @return true in case the image was processed (and closed or scheduled to be closed) successfully,
   *              or false if there was no matching meta-data and this function is not sure if
   *              this image may even be obsolete.
   */
  protected boolean processIncomingImage(Image img) {
    boolean imageobsolete=false;
    TotalCaptureResult match = null;
    long imagets = img.getTimestamp();
    //--------------------------------------------------------
    // Use the timestamp on the incoming image to look for a
    // corresponding capture result (meta-data)...
    //--------------------------------------------------------
    synchronized (CaptureResults) {
      ListIterator<TotalCaptureResult> li = CaptureResults.listIterator(0);
      while (li.hasNext()) {
        TotalCaptureResult cap = li.next();
        long ts = cap.get(CaptureResult.SENSOR_TIMESTAMP);
        if (ts == imagets) {
          match = cap;
          li.remove();
          break;
        } else if (ts > imagets) {
          imageobsolete = true;
        }
      }
    }
    //--------------------------------------------------------
    // If capture result was found, invoke the snapshot handler
    //--------------------------------------------------------
    if (match != null) {
      long myframe = match.getFrameNumber();
      if (SnapHandler != null) {
        SnapHandler.onImageAvailable(CameraDriver.this, img, match);    // NOTE (mw) it is up to the event handler to close the image once done
      } else {
        Log.w(LOGTAG,"No snapshot handler installed.");
        img.close();
        AvailableBuffers.release();
      }
      //--------------------------------------------------------
      // Adjust the imaged frame number...
      //--------------------------------------------------------
      long curr = ImagedFrame.get();
      while (curr < myframe) {
        if (ImagedFrame.compareAndSet(curr, myframe)) break;
        curr = ImagedFrame.get();
      }
      return true;
    } else if (imageobsolete) {
      //--------------------------------------------------------
      // No matching capture result was found and at least one
      // capture result with a more recent timestamp than the
      // image was found. Image is obsolete (meta-data lost ?),
      // so kill it...
      //--------------------------------------------------------
      Log.w(LOGTAG,"Removing obsolete pending image (assuming capture results are monotonous");
      img.close();
      AvailableBuffers.release();
      return true;
    }
    return false;
  }


  /**
   * Process pending images (i.e. images which could not be matched with meta-data on arrival).
   *
   * <p>
   * This function processed the images found in the {@link #PendingImages} list and tries to
   * match them up with meta-data again. If the {@link #Recording} flag is false, then all images
   * in the {@link #PendingImages} list are closed and the resource counter in {@link #AvailableBuffers}
   * is incremented again.
   * </p>
   *
   * @see {@link #CaptureListener}, {@link #PendingImages}
   */
  protected void processPendingImages() {
    if (Recording) {
      ListIterator<Image> ii = PendingImages.listIterator(0);
      while (ii.hasNext()) {
        Image img = ii.next();
        if (processIncomingImage(img)) {
          ii.remove();
        }
      }
    } else {
      ListIterator<Image> ii = PendingImages.listIterator(0);
      while (ii.hasNext()) {
        Image img = ii.next();
        img.close();
        AvailableBuffers.release();
      }
      PendingImages.clear();
    }
  }

}
