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
package org.mtnwrw.cameraexample.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import org.mtnwrw.cameraexample.R;
import org.mtnwrw.cameraexample.db.DatabaseManager;
import org.mtnwrw.pdqimg.ConversionService;
import org.mtnwrw.pdqimg.DecompressionService;
import org.mtnwrw.pdqimg.PDQImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;

/**
 * Simple list view adapter that relates view items to database entries (using a cursor).
 *
 * @author Martin Wawro
 */
public class RollOverviewAdapter extends CursorAdapter {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  private static final String LOGTAG="ROAdpt";

  //------------------------------------------------------------------------------------------------
  // Nested classes
  //------------------------------------------------------------------------------------------------

  /**
   * Internal representation of a single database item.
   */
  public class RollItem {
    public long DBID;
    public long FirstSerial;
    public int NumImages;
    public int ImageFormat;
    public String Timestamp;
  }


  //------------------------------------------------------------------------------------------------
  // Member variables
  //------------------------------------------------------------------------------------------------

  /**
   * Bitmaps that have (already) been loaded/decompressed into memory and may be used for
   * displaying thumbnail images in the individual view items.
   */
  protected HashMap<Long,Bitmap> Bitmaps = new HashMap<Long,Bitmap>();


  //------------------------------------------------------------------------------------------------
  // Class implementation
  //------------------------------------------------------------------------------------------------

  /**
   * Constructor
   *
   * @param ctx The application context
   *
   * @param cs The database cursor to use within this adapter
   *
   * @param flags
   */
  public RollOverviewAdapter(Context ctx,Cursor cs,int flags) {
    super(ctx,cs,flags);
  }


  /**
   * Obtain information in roll database at specific position.
   *
   * @param i An index within the model that this adapter acts on that should be read
   *
   * @return A {@link RollItem} instance that contains the information within the database at the
   *         supplied position
   */
  @Override
  public Object getItem(int i) {
    Cursor cs = getCursor();
    if ((cs != null)&&(cs.moveToPosition(i))) {
      RollItem result = new RollItem();
      result.DBID = cs.getLong(cs.getColumnIndex(DatabaseManager.COLUMN_PRIMARY));
      result.Timestamp = cs.getString(cs.getColumnIndex(DatabaseManager.ROLL_COLUMN_TIMESTAMP));
      result.FirstSerial = cs.getLong(cs.getColumnIndex(DatabaseManager.ROLL_COLUMN_FIRST_SERIAL));
      result.NumImages = cs.getInt(cs.getColumnIndex(DatabaseManager.ROLL_COLUMN_NUM_IMAGES));
      return result;
    }
    return null;
  }


  /**
   * (Re-)populate a view item which was created from the item_roll XML template.
   *
   * @param view The view to populate
   *
   * @param context Application context
   *
   * @param cursor Database cursor
   */
  @Override
  public void bindView(View view,Context context,Cursor cursor) {
    ImageView iview = (ImageView)view.findViewById(R.id.imgview);
    if (iview != null) {
      long dbid = cursor.getLong(cursor.getColumnIndex(DatabaseManager.COLUMN_PRIMARY));
      if (!Bitmaps.containsKey(dbid)) {
        File file = findFirstFile(context,cursor);
        if (file != null) {
          Bitmap bm = readAndDecompressThumbnail(file);
          if (bm != null) {
            iview.setImageBitmap(bm);
            Bitmaps.put(dbid, bm);
          }
        }
      } else {
        iview.setImageBitmap(Bitmaps.get(dbid));
      }
    }
  }


  /**
   * Create a new {@link View} instance from the item_roll XML layout which can be populated in
   * {@link #bindView(View, Context, Cursor)}.
   *
   * @param context Application context
   *
   * @param cursor Database cursor
   *
   * @param parent Parent view group for the new view to be created
   *
   * @return Newly-created view (from item roll template) that can be populated with images/data.
   */
  @Override
  public View newView(Context context,Cursor cursor,ViewGroup parent) {
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    return inflater.inflate(R.layout.item_roll,parent,false);
  }


  /**
   * Read and extract a thumbnail-sized image from a file.
   *
   * @param file Input file to read the data from
   *
   * @return Bitmap that contains a thumbnail version of the image referred to by the fileName
   *         or null on failure.
   */
  private Bitmap readAndDecompressThumbnail(File file) {
    try {
      if (file.length() > 0) {
        FileInputStream instream = new FileInputStream(file);
        ReadableByteChannel bytechan = Channels.newChannel(instream);
        ByteBuffer buff = ByteBuffer.allocateDirect((int) file.length());
        if (buff != null) {
          bytechan.read(buff);
          instream.close();
          buff.rewind();
          PDQImage pthumb = DecompressionService.decompressThumbnailImage(buff);
          if (pthumb != null) {
            Bitmap bm = ConversionService.convertPDQImageToBitmap(pthumb, false);
            pthumb.close();
            return bm;
          }
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return null;
  }


  /**
   * Find the first (readable) file belonging to the roll that the cursor points to.
   *
   * @param context Application context
   *
   * @param cs The database cursor which points to the roll in question
   *
   * @return The first readable file that was found in the supplied camera roll
   */
  private File findFirstFile(Context context,Cursor cs) {
    long first = cs.getLong(cs.getColumnIndex(DatabaseManager.ROLL_COLUMN_FIRST_SERIAL));
    int numimages = cs.getInt(cs.getColumnIndex(DatabaseManager.ROLL_COLUMN_NUM_IMAGES));
    for (long i=first;i<(first+numimages);i++) {
      String filename = String.format("%08d.iff",i);
      File file = new File(context.getFilesDir(),filename);
      if (file.exists()) return file;
    }
    return null;
  }

}
