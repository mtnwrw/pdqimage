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
package org.mtnwrw.cameraexample.fragments;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.mtnwrw.cameraexample.*;
import org.mtnwrw.cameraexample.acquisition.CameraDriver;
import org.mtnwrw.cameraexample.acquisition.SerialAcquisitionPolicy;
import org.mtnwrw.cameraexample.acquisition.SnapshotEventHandler;
import org.mtnwrw.cameraexample.views.LiveDisplay;
import org.mtnwrw.pdqimg.CompressionService;


/**
 * Fragment to be used to show camera preview images as well as controls to trigger full-frame
 * captures and perform adjustments (like for example setting the image format to be used for
 * full-frame capture).
 * <p>
 * Use the {@link AcquisitionFragment#newInstance} factory method to create an instance of this fragment.
 * </p>
 *
 * @author Martin Wawro
 *
 * @see MainActivity,LiveDisplay
 */
public class AcquisitionFragment extends Fragment implements View.OnClickListener, View.OnTouchListener {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  private static final String LOGTAG="Acquisition";
  private static final String PREFS="AcquisitionPreferences";

  static final int YUV_LOW_QUALITY=0;
  static final int YUV_MEDIUM_QUALITY=1;
  static final int YUV_HIGH_QUALITY=2;
  static final int RAW_LOW_QUALITY=3;
  static final int RAW_MEDIUM_QUALITY=4;
  static final int RAW_HIGH_QUALITY=5;

  //------------------------------------------------------------------------------------------------
  // Member variables
  //------------------------------------------------------------------------------------------------


  private FragmentListener Listener = null;

  /**
   * Reference to camera support interface.
   */
  private CameraDriver Camera = null;


  /**
   * Size of the actual texture display (in pixels)
   */
  private Size DisplaySize;

  /**
   * The display which shows the live camera image.
   *
   * @see LiveDisplay , #onViewCreated
   */
  private LiveDisplay LiveView;


  /**
   * The {@link Surface} where the preview image is to be rendered onto.
   */
  private Surface LiveSurface;


  /**
   * Event handler for snapshot (full-frame acquisition) events.
   */
  private SnapshotEventHandler SnapHandler;

  //------------------------------------------------------------------------------------------------
  // Callback implementations
  //------------------------------------------------------------------------------------------------

  /**
   * Listener which is attached to the SurfaceTexture that is used for the display of preview
   * images.
   * <p>
   * The callbacks in this instance handle availability and size-changes of the underlying texture
   * for the preview and are triggering the camera setup as well as adjustments to the viewing
   * transform of the texture.
   * </p>
   */
  private TextureView.SurfaceTextureListener SurfaceListener = new TextureView.SurfaceTextureListener() {
    /**
     * Handler when texture has been created for use with the camera
     *
     * @param surfaceTexture The SurfaceTexture instance which will serve as the target for the
     *                       camera preview output.
     * @param width The width (in pixels/texels) of the texture
     *
     * @param height The height (in pixels/texels) of the texture
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
      DisplaySize = new Size(width,height);
      //--------------------------------------------------------
      // Perform camera initialization and setup...
      //--------------------------------------------------------
      String id = Camera.setupCamera(DisplaySize);
      if (id != null) {
        LiveSurface = LiveView.setPreviewSize(Camera.getLivePreviewSize(),Camera.getNativeRotation(),Camera.getSensorRotation());
        Camera.setLiveSurface(LiveSurface);
        LiveView.setTransform(LiveView.getSensorTransform());
        if (!Camera.openCamera(id)) id = null;
      }
      if (id == null) {
        ((ImageButton)(getActivity().findViewById(R.id.button_quality))).setEnabled(false);
      }
    }



    /**
     * Handler for size-changes in the preview display.
     *
     * <p>
     * Usually this callback is invoked whenever the {@link LiveDisplay} changes its size.
     * </p>
     *
     * @param surfaceTexture The SurfaceTexture instance which will serve as the target for the
     *                       camera preview output.
     * @param newWidth The width (in pixels/texels) of the texture
     * @param newHeight The height (in pixels/texels) of the texture
     *
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int newWidth, int newHeight) {
      DisplaySize = new Size(newWidth,newHeight);
      LiveView.setTransform(LiveView.getSensorTransform());
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
      return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
  };

  //------------------------------------------------------------------------------------------------
  // Class implementation
  //------------------------------------------------------------------------------------------------

  /**
   * Use this factory method to create a new instance of this fragment using the provided parameters.
   *
   * @return A new instance of fragment AcquisitionFragment.
   */
  public static AcquisitionFragment newInstance() {
    AcquisitionFragment fragment = new AcquisitionFragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  /**
   * Default constructor. Currently idle.
   */
  public AcquisitionFragment() {
    // Required empty public constructor
  }


  public void setListener(FragmentListener allEars) {
    Listener = allEars;
  }

  /**
   * Event handler which is invoked after instantiation of this instance.
   *
   * @param savedInstanceState Optional bundle data that contains the state of the previously
   *                           running instance on resumes.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }


  /**
   *
   * @param view
   * @param savedInstanceState
   */
  @Override
  public void onViewCreated(View view,Bundle savedInstanceState) {
    //-------------------------------------------------
    // Fetch some parts from the layout as shortcuts...
    //-------------------------------------------------
    LiveView = (LiveDisplay)view.findViewById(R.id.live);
    //-------------------------------------------------
    // ...set up connections to controls...
    //-------------------------------------------------
    ((ImageButton)view.findViewById(R.id.button_record)).setOnTouchListener(this);
    ((ImageButton)view.findViewById(R.id.button_quality)).setOnClickListener(this);
    ((ImageButton)view.findViewById(R.id.button_switch_to_overview)).setOnClickListener(this);
    ((TextureView)view.findViewById(R.id.live)).setOnTouchListener(this);
    adjustQualitySelector(R.id.button_yuv_low,YUV_LOW_QUALITY);
    adjustQualitySelector(R.id.button_yuv_medium,YUV_MEDIUM_QUALITY);
    adjustQualitySelector(R.id.button_yuv_high,YUV_HIGH_QUALITY);
    adjustQualitySelector(R.id.button_raw_low,RAW_LOW_QUALITY);
    adjustQualitySelector(R.id.button_raw_medium,RAW_MEDIUM_QUALITY);
    adjustQualitySelector(R.id.button_raw_high,RAW_HIGH_QUALITY);
  }


  /**
   *
   * @param view
   * @param event
   * @return
   */
  @Override
  public boolean onTouch(View view,MotionEvent event) {
    boolean rc=false;
    switch (view.getId()) {
      case R.id.button_record:
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          if (Camera != null) Camera.startSnapshots();
          rc=true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
          if (Camera != null) Camera.stopSnapshots();
          rc=true;
        }
        break;
      case R.id.live: {
        if (Camera != null) {
          float xm = (float) view.getWidth() / 2.0f;
          float ym = (float) view.getHeight() / 2.0f;
          float rsqr = (float) (Math.pow(event.getX() - xm, 2) + Math.pow(event.getY() - ym, 2));
          if (rsqr < 300f * 300f) {
            if (!Camera.isRecording()) Camera.perform3A();
          }
        }
        break;
      }
    }
    return rc;
  }

  /**
   * Callback which is invoked when the view should be created.
   *
   * @param inflater Instance to layout inflater to be used to inflate a layout from XML resources.
   * @param container The target container to place this view into
   * @param savedInstanceState Optional saved state resulting from a pause/resume operation
   *
   * @return The created View instance
   */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_acquisition, container, false);
  }


  /**
   * Callback handler which is invoked on resume/init of the fragment.
   *
   */
  @Override
  public void onResume() {
    super.onResume();
    //------------------------------------------------
    // Set up camera event loop...
    //------------------------------------------------
    if (SnapHandler == null) SnapHandler = new SnapshotEventHandler(getActivity().getApplicationContext());
    if (Camera == null) Camera = new CameraDriver(getActivity(),SnapHandler);
    if (CompressionService.getInstance() == null) CompressionService.initialize(1);
    Camera.init();
    LiveView.setSurfaceTextureListener(SurfaceListener);
    //------------------------------------------------
    // ...adjust state of button(s)...
    //------------------------------------------------
    int quality = 0;
    switch (SnapHandler.getQuality()) {
      case QUALITY_MEDIUM:
        quality = 1;
        break;
      case QUALITY_HIGH:
        quality = 2;
        break;
    }
    if (Camera.getCaptureFormat() == ImageFormat.RAW_SENSOR) quality+=3;
    ((ImageButton)getActivity().findViewById(R.id.button_quality)).setImageLevel(quality);
    //------------------------------------------------
    // Enter (currently hardcoded) acquisition policy
    //------------------------------------------------
    Camera.setSnapshotPolicy(new SerialAcquisitionPolicy());
  }


  /**
   * Callback handler when fragment has been removed from view.
   *
   */
  @Override
  public void onPause() {
    if (Camera != null) Camera.shutdown();
    Camera = null;
    if (SnapHandler != null) SnapHandler.shutdown();
    SnapHandler = null;
    super.onPause();
  }



  /**
   * Click response handler
   *
   * @param view The view (button) which is responsible for emitting the click event
   */
  @Override
  public void onClick(View view) {
    if ((Camera != null) && (!Camera.isRecording())) {
      switch (view.getId()) {
        case R.id.button_yuv_low:
        case R.id.button_yuv_medium:
        case R.id.button_yuv_high:
        case R.id.button_raw_low:
        case R.id.button_raw_medium:
        case R.id.button_raw_high:
          selectQualityAndFormat(view);
          break;
        case R.id.button_quality:
          toggleQualityAndFormatSelector(view);
          break;
        case R.id.button_switch_to_overview:
          if (Listener != null) Listener.onFragmentSignal(this,"OVERVIEW");
          break;
        default:
          Log.w(LOGTAG, "Click event from unknown/unhandled source");
          break;
      }
    }
  }



  /**
   * Adjust the image-level of the quality/format selection context-menu buttons
   *
   * <p>
   * The context-menu of the quality/format selector on the top-left of the screen uses a bunch
   * of simple ImageButton objects that represent the individual quality/image-format combinations.
   * The image-data itself is defined in a LevelListDrawable, where each level represents a unique
   * quality/format combination.
   * </p>
   *
   * @param id The ID of the ImageButton instance to adjust the image-level
   *
   * @param level The image-level to use
   *
   * @see org.mtnwrw.cameraexample.R.drawable#qualities
   */
  private void adjustQualitySelector(int id,int level) {
    ImageButton button = (ImageButton)(getView().findViewById(id));
    if (button != null) {
      button.setImageLevel(level);
      button.setOnClickListener(this);
    }
  }


  /**
   * Toggle context-menu of the quality/format selector button
   *
   * @param view The ImageButton instance that represents the quality & format selector/indicator
   *             button on the top-left of the screen
   */
  private void toggleQualityAndFormatSelector(View view) {
    ImageButton button = (ImageButton)view;
    if (button.isSelected()) {
      deselectQualityButton(button);
    } else {
      button.setSelected(true);
      getActivity().findViewById(R.id.layout_yuv_qualities).setVisibility(View.VISIBLE);
      getActivity().findViewById(R.id.layout_raw_qualities).setVisibility(View.VISIBLE);
    }
  }


  /**
   *
   * @param view
   */
  private void selectQualityAndFormat(View view) {
    ImageButton qualitybutton = (ImageButton)(getActivity().findViewById(R.id.button_quality));
    if (qualitybutton != null) {
      switch (view.getId()) {
        case R.id.button_yuv_low:
          setQualityAndFormat(qualitybutton, YUV_LOW_QUALITY);
          break;
        case R.id.button_yuv_medium:
          setQualityAndFormat(qualitybutton, YUV_MEDIUM_QUALITY);
          break;
        case R.id.button_yuv_high:
          setQualityAndFormat(qualitybutton, YUV_HIGH_QUALITY);
          break;
        case R.id.button_raw_low:
          setQualityAndFormat(qualitybutton, RAW_LOW_QUALITY);
          break;
        case R.id.button_raw_medium:
          setQualityAndFormat(qualitybutton, RAW_MEDIUM_QUALITY);
          break;
        case R.id.button_raw_high:
          setQualityAndFormat(qualitybutton, RAW_HIGH_QUALITY);
          break;
      }
      qualitybutton.setSelected(false);

    }
    deselectQualityButton(qualitybutton);
  }


  /**
   * Adjust compression quality and output format (YUV or raw)
   *
   * @param qualityButton ImageButton instance that serves as quality selector
   *
   * @param imageLevel The image level to display in that button (preselects the right icon
   *                   that indicates the selected quality)
   */
  private void setQualityAndFormat(ImageButton qualityButton, int imageLevel) {
    CompressionService.quality quality = CompressionService.quality.QUALITY_LOW;
    int format=(imageLevel>=3) ? ImageFormat.RAW_SENSOR : ImageFormat.YUV_420_888;
    switch (imageLevel % 3) {
      case 0:
        // NOTE (mw) the default will suffice here
        break;
      case 1:
        quality = CompressionService.quality.QUALITY_MEDIUM;
        break;
      case 2:
        quality = CompressionService.quality.QUALITY_HIGH;
        break;
    }
    if (SnapHandler != null) SnapHandler.setQuality(quality);
    if (Camera != null) Camera.setCaptureFormat(format);
    qualityButton.setImageLevel(imageLevel);
  }


  /**
   * Helper function which executes deselection (context-menu-hiding) routine for the quality
   * indicator button in the upper-left corner.
   *
   * @param button The quality button which is to change its selected state to <em>deselected</em>.
   */
  private void deselectQualityButton(ImageButton button) {
    button.setSelected(false);
    getActivity().findViewById(R.id.layout_yuv_qualities).setVisibility(View.INVISIBLE);
    getActivity().findViewById(R.id.layout_raw_qualities).setVisibility(View.INVISIBLE);
  }


}
