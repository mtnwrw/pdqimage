/*
* Copyright (c) 2016 Martin Wawro
*
* Free for non-commercial use. See LICENSE for license details.
*/
package org.mtnwrw.pdqimg;

import android.os.Build;

/**
 * Class to encapsulate general device information (manufacturer and model name) to pass into
 * the native code.
 *
 * @author Martin Wawro
 */
public class DeviceInfo {

  private String Manufacturer;
  private String Model;

  DeviceInfo() {
    Manufacturer = Build.MANUFACTURER;
    Model = Build.MODEL;
  }
}

