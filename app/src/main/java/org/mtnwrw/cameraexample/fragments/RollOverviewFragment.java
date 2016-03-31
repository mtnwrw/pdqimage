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

import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.GridView;

import org.mtnwrw.cameraexample.R;
import org.mtnwrw.cameraexample.adapters.RollOverviewAdapter;
import org.mtnwrw.cameraexample.db.DatabaseManager;

/**
 *
 *
 * @author Martin Wawro
 */
public class RollOverviewFragment extends Fragment {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  private static final String LOGTAG="Overview";


  private CursorAdapter RollAdapter = null;
  private FragmentListener Listener = null;

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment RollOverviewFragment.
   */
  public static RollOverviewFragment newInstance(String param1, String param2) {
    RollOverviewFragment fragment = new RollOverviewFragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment RollOverviewFragment.
   */
  public static RollOverviewFragment newInstance() {
    RollOverviewFragment fragment = new RollOverviewFragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }


  /**
   *
   */
  public RollOverviewFragment() {
    // Required empty public constructor
  }


  public void setListener(FragmentListener allEars) {
    Listener = allEars;
  }

  /**
   *
   * @param savedInstanceState
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }


  /**
   *
   * @param inflater
   * @param container
   * @param savedInstanceState
   * @return
   */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_roll_overview, container, false);
  }


  /**
   *
   * @param view
   * @param savedInstanceState
   */
  @Override
  public void onViewCreated(View view,Bundle savedInstanceState) {
    GridView grid = (GridView)view.findViewById(R.id.rollgrid_view);
    if (grid != null) {
      Cursor cursor = DatabaseManager.getInstance().queryAllRolls();
      RollAdapter = new RollOverviewAdapter(getActivity().getApplicationContext(),cursor,0);
      grid.setAdapter(RollAdapter);
    }
  }
}
