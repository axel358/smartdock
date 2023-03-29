package cu.axel.smartdock.db;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;
import cu.axel.smartdock.db.DatabaseContract.*;
import android.content.ContentValues;
import android.database.Cursor;

public class DBHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "smartdock.db";
	private static final String SQL_CREATE_MODES_ENTRIES = "CREATE TABLE " + LaunchModesTable.TABLE_NAME + " ("
			+ LaunchModesTable._ID + " INTEGER PRIMARY KEY NOT NULL," + LaunchModesTable.COLUMN_PACKAGE_NAME
			+ " TEXT NOT NULL," + LaunchModesTable.COLUMN_LAUNCH_MODE + " TEXT NOT NULL )";

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase p1) {
		p1.execSQL(SQL_CREATE_MODES_ENTRIES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase p1, int p2, int p3) {
	}

	public void saveLaunchMode(String app, String mode) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(LaunchModesTable.COLUMN_PACKAGE_NAME, app);
			values.put(LaunchModesTable.COLUMN_LAUNCH_MODE, mode);

			String selection = LaunchModesTable.COLUMN_PACKAGE_NAME + " = ?";
			String[] selectionArgs = { app };

			Cursor c = db.query(LaunchModesTable.TABLE_NAME, null, selection, selectionArgs, null, null, null);

			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				db.update(LaunchModesTable.TABLE_NAME, values, selection, selectionArgs);
			} else {
				db.insert(LaunchModesTable.TABLE_NAME, null, values);
			}
			c.close();
		} catch (Exception e) {
		}
	}

	public String getLaunchMode(String app) {
		String mode = null;
		try {
			SQLiteDatabase db = getReadableDatabase();
			String selection = LaunchModesTable.COLUMN_PACKAGE_NAME + " = ?";
			String[] selectionArgs = { app };

			Cursor c = db.query(LaunchModesTable.TABLE_NAME, null, selection, selectionArgs, null, null, null);

			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				mode = c.getString(c.getColumnIndex(LaunchModesTable.COLUMN_LAUNCH_MODE));
				c.close();
			}
		} catch (Exception e) {
		}
		return mode;

	}

}
