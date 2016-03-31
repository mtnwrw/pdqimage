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

import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;

import org.mtnwrw.pdqimg.CompressionService;

/**
 * Interface for implementing snapshot-handler objects which can be connected to the {@link CameraDriver}
 * class.
 *
 * @author Martin Wawro
 */
public interface SnapshotEventInterface {
  /**
   * Callback function which is invoked from the camera driver whenever an image is available.
   *
   * <p>
   * The {@link CameraDriver} class invokes this function when an image has become available
   * and matching capture results have been found.
   * </p>
   *
   * <p>
   * When subclassing and implementing this function, make sure that the supplied image is
   * closed once it is not required anymore (in the good case as well as in the error case).
   * </p>
   *
   * <p>
   * Important: the {@link CameraDriver} features a resource counter in {@link CameraDriver#AvailableBuffers}
   * which has to be (atomically) <em>increased</em> whenever the image is closed. Failure to do so
   * will lock the driver once the resource counter is depleted.
   * </p>
   *
   * @param driver The {@link CameraDriver} instance that invoked this function
   *
   * @param image The {@link Image} instance that contains the camera picture
   *
   * @param result The meta-data ({@link TotalCaptureResult}) for the supplied image.
   */
  void onImageAvailable(CameraDriver driver,Image image, TotalCaptureResult result);

  /**
   *
   * @return
   */
  CompressionService.quality getQuality();


  /**
   *
   */
  void setQuality(CompressionService.quality quality);

  /**
   * Indicate that a new camera-roll (acquisition session) is about to be started
   *
   * @return {@code true} in case the handler implementation is ready to process a new roll,
   *         {@code false} otherwise
   *
   */
  boolean newRoll();

  /**
   * Indicate that the current roll is done from the acquisition point-of-view.
   */
  void rollDone();

  /**
   * Wait until the current roll has been completely written/processed.
   */
  void waitRoll();

  /**
   * Deallocate all unused buffers.
   */
  void cleanUp();

  /**
   * Completely shuts down the event handler.
   */
  void shutdown();
}
