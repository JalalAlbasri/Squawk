package com.jalbasri.squawk;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class TwitterStatusContentProvider extends ContentProvider{
    private static final String TAG = TwitterStatusContentProvider.class.getSimpleName();

    public static final Uri CONTENT_URI =
            Uri.parse("content://com.jalbasri.squawk.twitterstatusprovider/twitteritems");
    public static final String KEY_STATUS_ID = "_id";
    public static final String KEY_STATUS_TEXT = "_status_text";
    public static final String KEY_CREATED_AT = "_created_at";

    public static final String KEY_USER_ID = "_user_id";
    public static final String KEY_USER_NAME = "_user_name";
    public static final String KEY_USER_SCREEN_NAME = "_user_screen_name";
    public static final String KEY_USER_IMAGE = "_user_image";
    public static final String KEY_USER_URL = "_user_url";

    public static final String KEY_LATITUDE = "_latitude";
    public static final String KEY_LONGITUDE = "_longitude";



    private MySQLiteOpenHelper mySQLiteOpenHelper;
    private static final int ALL_ROWS = 1;
    private static final int SINGLE_ROW = 2;
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("com.jalbasri.twitterstatusprovider", "twitteritems", ALL_ROWS);
        uriMatcher.addURI("com.jalbasri.twitterstatusprovider", "twitteritems/#", SINGLE_ROW);
    }

    @Override
    public boolean onCreate() {

        mySQLiteOpenHelper = new MySQLiteOpenHelper(getContext(),
                MySQLiteOpenHelper.DATABASE_NAME, null, MySQLiteOpenHelper.DATABASE_VERSION);

        return false;
    }

    @Override
    public String getType(Uri uri) {
        switch(uriMatcher.match(uri)) {
            case ALL_ROWS: return "vnd.android.cursor.dir/vnd.jalbasri.twitterstatus";
            case SINGLE_ROW: return "vnd.android.cursor.item/vnd.jalbasri.twitterstatus";
            default: throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db;
        try {
            db = mySQLiteOpenHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "Query: Unable to get a writable database, " +
                    "falling back to readable database");
            db = mySQLiteOpenHelper.getReadableDatabase();
        }

        String groupBy = null;
        String having = null;

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(MySQLiteOpenHelper.DATABASE_TABLE);

        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW:
                String rowId = uri.getPathSegments().get(1);
                queryBuilder.appendWhere(KEY_STATUS_ID + "=" + rowId);
            default: break;
        }

        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, groupBy, having, sortOrder);

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        SQLiteDatabase db = mySQLiteOpenHelper.getWritableDatabase();
        String nullColumnHack = null;

        long id = db.insert(MySQLiteOpenHelper.DATABASE_TABLE, nullColumnHack, values);

        if (id > -1) {
            Uri insertedId = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(insertedId, null);
            return insertedId;
        }
        else
            return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        SQLiteDatabase db = mySQLiteOpenHelper.getWritableDatabase();

        switch(uriMatcher.match(uri)) {
            case SINGLE_ROW:
                String rowId= uri.getPathSegments().get(1);
                selection = KEY_STATUS_ID + "=" + rowId
                        + (!TextUtils.isEmpty(selection) ?
                        " AND (" + selection + ")" : "" );
            default: break;
        }

        if (selection == null)
            selection = "1";

        int deleteCount = db.delete(MySQLiteOpenHelper.DATABASE_TABLE, selection, selectionArgs);

        getContext().getContentResolver().notifyChange(uri, null);

        return deleteCount;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        SQLiteDatabase db = mySQLiteOpenHelper.getWritableDatabase();

        switch(uriMatcher.match(uri)) {
            case SINGLE_ROW:
                String rowId= uri.getPathSegments().get(1);
                selection = KEY_STATUS_ID + " = " + rowId
                        + (!TextUtils.isEmpty(selection) ?
                        " AND (" + selection + ")" : "" );
            default: break;
        }

        int updateCount = db.update(MySQLiteOpenHelper.DATABASE_TABLE, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);

        return updateCount;
    }

    private static class MySQLiteOpenHelper extends SQLiteOpenHelper {

        public static final String DATABASE_NAME = "twitterStatusDatabase.db";
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_TABLE = "twitterStatusTable";

        private static final String DATABASE_CREATE = "CREATE TABLE " +
                DATABASE_TABLE + " (" +
                KEY_STATUS_ID + " INTEGER, " +
                KEY_STATUS_TEXT + " TEXT, " +
                KEY_CREATED_AT + " INTEGER, " +
                KEY_USER_ID + " INTEGER, " +
                KEY_USER_NAME + " TEXT, " +
                KEY_USER_SCREEN_NAME + " TEXT, " +
                KEY_USER_IMAGE + " TEXT, " +
                KEY_USER_URL + " TEXT, " +
                KEY_LATITUDE + " REAL, " +
                KEY_LONGITUDE + " REAL " +
                ");" ;

        public MySQLiteOpenHelper(Context context, String name,
                                  CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "Create Databse: " + DATABASE_CREATE);
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading from version " + oldVersion +
                    " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF IT EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }

    }

}

