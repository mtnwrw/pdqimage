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
package org.mtnwrw.cameraexample;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;

import org.mtnwrw.cameraexample.fragments.AcquisitionFragment;
import org.mtnwrw.cameraexample.fragments.FragmentListener;
import org.mtnwrw.cameraexample.fragments.RollOverviewFragment;


/**
 * Main (default) application activity.
 *
 * @author Martin Wawro
 */
public class MainActivity extends Activity implements FragmentListener {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  private static final String LOGTAG="Main";

  //------------------------------------------------------------------------------------------------
  // Member variables
  //------------------------------------------------------------------------------------------------

  /**
   * Fragment that displays the acquisition (capturing) view.
   */
  AcquisitionFragment AcquisitionFrag = null;

  /**
   * Fragment that displays an overview over the acquired image rolls.
   */
  RollOverviewFragment OverviewFrag = null;

  //------------------------------------------------------------------------------------------------
  // Class implementation
  //------------------------------------------------------------------------------------------------

  /**
   * Invoked on init of the activity. This function will initialize the image-acquisition fragment
   * to be displayed and places it into the central view area of the main activity.
   *
   * @param savedInstanceState Optional bundle data originating from suspended instance on resume
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    if (savedInstanceState == null) {
      FragmentManager mgr = getFragmentManager();
      AcquisitionFrag  = AcquisitionFragment.newInstance();
      AcquisitionFrag.setListener(this);
      mgr.beginTransaction().replace(R.id.central,AcquisitionFrag).commit();
    }
  }


  /**
   * Fragment signal handler. Responsible for switching between fragments.
   *
   * @param sender The object that sent the signal
   *
   * @param signalName Signal identifier, controls which fragment will be displayed next.
   */
  @Override
  public void onFragmentSignal(Fragment sender, String signalName) {
    FragmentManager mgr = getFragmentManager();
    if (signalName.equals("OVERVIEW")) {
      if (OverviewFrag == null) {
        OverviewFrag = RollOverviewFragment.newInstance();
        OverviewFrag.setListener(this);
      }
      mgr.beginTransaction().replace(R.id.central,OverviewFrag).addToBackStack("acquisition").commit();
    }
  }
}
