package com.cajama.background;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper{
	 
    //The Android's default system path of your application database.
	private static String TAG = "DatabaseHelper"; 
	private static String DB_PATH = Environment.getExternalStorageDirectory() + "/Android/data/com.cajama.malarialite/files/";
    private static String DB_NAME = "db.db";
    private static String TABLE_NAME = "user";
    private static String USER_COLUMN = "username";
    
    private SQLiteDatabase myDataBase;
    private final Context myContext;
 
    public DataBaseHelper(Context context) {
    	super(context, DB_NAME, null, 1);
        this.myContext = context;
    }	
    
    public void createDataBase() throws IOException{
    	boolean dbExist = checkDataBase();
    	if (dbExist) {
    		//do nothing - database already exist
    	}
    	else {
        	this.getReadableDatabase();
        	try {
    			copyDataBase();
    		} catch (IOException e) {
        		throw new Error("Error copying database");
        	}
    	}
 
    }
 
    private boolean checkDataBase() {
 
    	SQLiteDatabase checkDB = null;
    	try {
    		String myPath = DB_PATH + DB_NAME;
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    	} catch (SQLiteException e) {
    		
    	}
 
    	if (checkDB != null) {
    		checkDB.close();
    	}
 
    	return checkDB != null ? true : false;
    }
 
    private void copyDataBase() throws IOException {
    	
    	InputStream myInput = myContext.getAssets().open(DB_NAME);
    	String outFileName = DB_PATH + DB_NAME;
    	OutputStream myOutput = new FileOutputStream(outFileName);
 
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = myInput.read(buffer)) > 0) {
    		myOutput.write(buffer, 0, length);
    	}
 
    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
    }
 
    public void openDataBase() throws SQLException {
 
        String myPath = DB_PATH + DB_NAME;
    	myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    }
 
    @Override
	public synchronized void close() {
 
    	    if(myDataBase != null)
    		    myDataBase.close();
 
    	    super.close();
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) {
 
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
 
	}
	
	public Cursor getPair(String user) {
		//SQLiteDatabase db = this.getReadableDatabase();
		/*Cursor cursor = db.rawQuery("select 1 from "
	            + TABLE_NAME + " where " + USER_COLUMN + "=?",
	            new String[] {user});*/
		
		String[] tableColumns = new String[] {"username", "password"};
		String whereClause = "username = ?";
		String[] whereArgs = new String[] {user};
		Cursor cursor = myDataBase.query("user", tableColumns, whereClause, whereArgs,
		        null, null, null, null);
		
		if (cursor.getCount()==0) return null;
		return cursor;
	}
	
	public Cursor showAllTables() {
        String mySql = " SELECT name FROM sqlite_master " + " WHERE type='table'             ";
        return myDataBase.rawQuery(mySql, null);
    }
 
        // Add your public helper methods to access and get content from the database.
       // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
       // to you to create adapters for your views.
 
}