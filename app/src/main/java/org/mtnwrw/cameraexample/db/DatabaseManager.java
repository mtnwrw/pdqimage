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
package org.mtnwrw.cameraexample.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Simple SQLite database manager for managing the acquired images
 *
 * <p>
 * This class is used to manage the acquired images on a 2-level hierarchy based on "camera rolls"
 * which are equivalent to acquisition sessions and images within each of these rolls.
 * </p>
 *
 * @author Martin Wawro
 */
public class DatabaseManager extends SQLiteOpenHelper {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  private static final String LOGTAG="DatabaseManager";
  private static final int DB_VERSION=1;
  private static final String DB_FILENAME="database.db";
  public final static String SETTINGS_TABLE="settings";
  public final static String ROLL_TABLE="rolls";
  public final static String EXPORT_TABLE="exported";
  public final static String ROLL_COLUMN_TIMESTAMP="timestamp";
  public final static String ROLL_COLUMN_NUM_IMAGES ="numimages";
  public final static String ROLL_COLUMN_FORMAT="format";
  public final static String ROLL_COLUMN_FIRST_SERIAL ="firstserial";
  public final static String ROLL_COLUMN_CAPTURE_MODE="capmode";
  public final static String ROLL_COLUMN_CAPTURE_PARAMS="capparams";
  public final static String EXPORT_COLUMN_ROLL="roll";
  public final static String EXPORT_COLUMN_IMAGE="image";
  public final static String COLUMN_PRIMARY="_id";
  public final static String SETTINGS_COLUMN_KEY="key";
  public final static String SETTINGS_COLUMN_VALUE="value";

  //------------------------------------------------------------------------------------------------
  // Member variables
  //------------------------------------------------------------------------------------------------

  private static DatabaseManager Manager=null;

  //------------------------------------------------------------------------------------------------
  // Class implementation
  //------------------------------------------------------------------------------------------------

  /**
   * Create singleton instance and return it
   *
   * @param context Reference to application context where the database should be created under
   *
   * @return Reference to database manager instance
   */
  // TODO (mw) make this thread-safe
  public static DatabaseManager getInstance(Context context) {
    if (Manager == null) {
      Manager = new DatabaseManager(context);
    }
    return Manager;
  }

  public static DatabaseManager getInstance() {
    return Manager;
  }

  /**
   * Constructor
   *
   * <p>
   * Does nothing but calling the superclass.
   * </p>
   *
   * @param context Application context.
   *
   */
  public DatabaseManager(Context context) {
    super(context,DB_FILENAME,null,DB_VERSION);
  }


  /**
   * Callback when the database is created the first time.
   *
   * <p>
   * This function is invoked automatically if there is no database present and a new one should
   * be created.
   * </p>
   *
   * @param db Handle to the database that should be initialized
   */
  @Override
  public void onCreate(SQLiteDatabase db) {
    Log.d(LOGTAG, "Creating new SQL database");
    db.execSQL("CREATE TABLE "+ROLL_TABLE+" ( " +
               COLUMN_PRIMARY+" INTEGER PRIMARY KEY," +
               ROLL_COLUMN_TIMESTAMP+" DATETIME," +
               ROLL_COLUMN_FIRST_SERIAL +" INTEGER," +
               ROLL_COLUMN_NUM_IMAGES +" INTEGER," +
               ROLL_COLUMN_FORMAT+" VARCHAR(8)," +
               ROLL_COLUMN_CAPTURE_MODE+" VARCHAR(16)," +
               ROLL_COLUMN_CAPTURE_PARAMS+" VARCHAR(128))"
    );
    db.execSQL("CREATE TABLE "+EXPORT_TABLE+" ( " +
               COLUMN_PRIMARY+" INTEGER PRIMARY KEY," +
               EXPORT_COLUMN_ROLL+" INTEGER," +
               EXPORT_COLUMN_IMAGE+" INTEGER)");
    db.execSQL("CREATE TABLE "+SETTINGS_TABLE+" ( " +
                COLUMN_PRIMARY+" INTEGER PRIMARY KEY," +
                SETTINGS_COLUMN_KEY+" VARCHAR(128)," +
                SETTINGS_COLUMN_VALUE+" TEXT)");
    db.execSQL("INSERT INTO "+SETTINGS_TABLE+" ("+SETTINGS_COLUMN_KEY+","+SETTINGS_COLUMN_VALUE+") VALUES ('db_version','" + DB_VERSION + "')");
  }



  /**
   * Upgrade-handler function which is invoked once an implicit upgrade of an already-existing
   * database-sche,a to a more recent version is to be made.
   *
   * <p>
   * This function currently does nothing as we only have one database schema as of now.
   * </p>
   *
   * @param db Handle to the database that should be upgraded
   *
   * @param oldVersion Old database version (existing one)
   *
   * @param newVersion The target version to upgrade to
   *
   */
  @Override
  public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
    Log.i(LOGTAG, "Upgrading SQL database from " + oldVersion + " to " + newVersion);
    if (newVersion != DB_VERSION) {
      Log.e(LOGTAG, "Don't know how to upgrade to version " + DB_VERSION);
      // TODO (mw) error stuff here
      return;
    }
    // NOTE (mw) no upgrade yet, still version 1
    //db.execSQL("UPDATE settings SET value='"+DB_VERSION+"' WHERE key='db_version'");
  }


  public Cursor queryAllRolls() {
    return getReadableDatabase().rawQuery("SELECT * FROM "+ROLL_TABLE,null);
  }


  /**
   * Get current date-time as string to be used as timestamp in SQLite database.
   *
   * @return String representation of the current date/time in (nearly) ISO-8601 format.
   */
  public static String getCurrentDateTime() {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    return fmt.format(new Date());
  }


  /**
   * Get string representation of SQL timestamp.
   *
   * @param stamp The timestamp to obtain a string representation from
   *
   * @return String representation of the supplied time-stamp in (nearly) ISO-8601 format, suitable
   *         to use with SQLite database.
   */
  public static String timestampToString(java.sql.Timestamp stamp) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    return fmt.format(stamp);
  }


}
