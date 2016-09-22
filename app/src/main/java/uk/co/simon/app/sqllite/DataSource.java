package uk.co.simon.app.sqllite;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DataSource {

	private static SQLiteDatabase mDatabase;
	private SQLiteHelper mDBHelper;

	public DataSource(Context context) {
		mDBHelper = SQLiteHelper.getsInstance(context);
        this.open();
	}

	public SQLiteDatabase getDatabase() {
        this.open();
		return mDatabase;
	}

	public boolean isOpen() {
		return mDBHelper.isOpen();
	}

	public void open() throws SQLException {
		mDatabase = mDBHelper.getWritableDatabase();
	}

	public void close() {
		mDBHelper.close();
	}

}
