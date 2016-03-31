/*
* Copyright (c) 2016 Martin Wawro
*
* Free for non-commercial use. See LICENSE for license details.
*/
package org.mtnwrw.pdqimg;


import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.location.Location;

/**
 * Class to encapsulate information on the photo's location to be fed into the native
 * part of the code.
 *
 * @author Martin Wawro
 */
public class LocationInfo {

  /**
   * Used for storage (marks valid tokens)
   */
  private int TagMask = 0;
  /**
   * The longitude of the geolocation
   */
  private double Longitude;

  /**
   * The latitude of the geolocation
   */
  private double Latitude;

  /**
   * The altitude of the geolocation
   */
  private double Altitude;

  /**
   * Optional accuracy that the device was able to deliver when determining the location
   */
  private float Accuracy;

  /**
   * Local velocity at time the geolocation was acquired
   */
  private float Speed;

  /**
   * Create a geolocation info token to be used for storage in the native code.
   *
   * @param captureInfo A {@link TotalCaptureResult} instance that resulted from the snapshot
   */
  LocationInfo(TotalCaptureResult captureInfo) {
    if (captureInfo != null) {
      Location loc = (Location) captureInfo.get(CaptureResult.JPEG_GPS_LOCATION);
      if (loc != null) {
        Longitude = loc.getLongitude();
        Latitude = loc.getLatitude();
        TagMask |= 1;
        if (loc.hasAltitude()) {
          Altitude = loc.getAltitude();
          TagMask |= 2;
        }
        if (loc.hasAccuracy()) {
          Accuracy = loc.getAccuracy();
          TagMask |= 4;
        }
        if (loc.hasSpeed()) {
          Speed = loc.getSpeed();
          TagMask |= 8;
        }
      }
    }
  }
}

