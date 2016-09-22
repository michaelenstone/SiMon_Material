package uk.co.simon.app.sqllite;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class DataSourceLocations {

	private DataSource mDataSource;
	private String[] allColumns = { SQLiteHelper.COLUMN_ID,
			SQLiteHelper.COLUMN_PROJECT_ID,
			SQLiteHelper.COLUMN_LOCATION_NAME,
			SQLiteHelper.COLUMN_CLOUD_ID };

    /**
     * Initialize class using DataSource to get database
     * @param dataSource provide DataSource object
     */
	public DataSourceLocations(DataSource dataSource) {
		mDataSource = dataSource;
	}

    /**
     * Add a new location to the locations table in the database
     * @param location valid location object
     * @return location with id field set from database
     */
	public SQLLocation createLocation(SQLLocation location) {
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COLUMN_LOCATION_NAME, location.getLocation());
		values.put(SQLiteHelper.COLUMN_PROJECT_ID, location.getProjectId());
		values.put(SQLiteHelper.COLUMN_CLOUD_ID, location.getCloudID());
		long insertId = mDataSource.getDatabase().insert(SQLiteHelper.LOCATIONS_TABLE_NAME, null,
				values);
		Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.LOCATIONS_TABLE_NAME,
				allColumns, SQLiteHelper.COLUMN_ID + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		SQLLocation newLocation = cursorToLocation(cursor);
		cursor.close();
		return newLocation;
	}

    /**
     * Update a location in database
     * @param location valid location object
     * @return updated location read back from database
     */
	public SQLLocation updateLocation(SQLLocation location) {
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COLUMN_LOCATION_NAME, location.getLocation());
		values.put(SQLiteHelper.COLUMN_PROJECT_ID, location.getProjectId());
		values.put(SQLiteHelper.COLUMN_CLOUD_ID, location.getCloudID());
		mDataSource.getDatabase().update(SQLiteHelper.LOCATIONS_TABLE_NAME,
				values,
				SQLiteHelper.COLUMN_ID + " = " + location.getId(),
				null);
		Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.LOCATIONS_TABLE_NAME,
				allColumns, SQLiteHelper.COLUMN_ID + " = " + location.getId(), null,
				null, null, null);
		cursor.moveToFirst();
		SQLLocation updLocation = cursorToLocation(cursor);
		cursor.close();
		return updLocation;
	}

    /**
     * Provide ordered list of locations from project based on the projectId
     * @param ProjectId project id as Long
     * @return list of location objects
     */
	public List<SQLLocation> getAllProjectLocations(Long ProjectId) {
		List<SQLLocation> locations = new ArrayList<>();

		Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.LOCATIONS_TABLE_NAME,
				allColumns, SQLiteHelper.COLUMN_PROJECT_ID + " = " + ProjectId, null,
				null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			SQLLocation location = cursorToLocation(cursor);
			locations.add(location);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return locations;
	}

    /**
     * Provide ordered list of locations from project based on the project object
     * @param project project object
     * @return list of location objects
     */
    public List<SQLLocation> getAllProjectLocations(SQLProject project) {
        return getAllProjectLocations(project.getId());
    }

    /**
     * Get a location on the basis of local DB location ID
     * @param locationId long of local DB location ID
     * @return location object
     */
	public SQLLocation getLocation(Long locationId) {

		Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.LOCATIONS_TABLE_NAME,
				allColumns, SQLiteHelper.COLUMN_ID + " = " + locationId, null,
				null, null, null);

		cursor.moveToFirst();
		SQLLocation location = null;
		while (!cursor.isAfterLast()) {
			location = cursorToLocation(cursor);
			cursor.moveToNext();
		}
		cursor.close();
		return location;
	}

    /**
     * Get a location on the basis of remote DB location ID
     * @param cloudId long of remote DB location ID
     * @return location object
     */
	public SQLLocation getLocationFromCloudId(long cloudId) {

		Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.LOCATIONS_TABLE_NAME,
				allColumns, SQLiteHelper.COLUMN_CLOUD_ID + " = " + cloudId, null,
				null, null, null);

		cursor.moveToFirst();
		SQLLocation location = null;
		while (!cursor.isAfterLast()) {
			location = cursorToLocation(cursor);
			cursor.moveToNext();
		}
		cursor.close();
		return location;
	}

    /**
     * Get a location on the basis of a location Object mostly in order to get the local or remote DB ID
     * @param location location object with project ID and location name fields set
     * @return location object from DB with local and remote DB IDs set
     */
	public SQLLocation findLocationId(SQLLocation location) {
		String where = SQLiteHelper.COLUMN_PROJECT_ID + " = ? AND " + SQLiteHelper.COLUMN_LOCATION_NAME + " = ?";
		String[] whereArgs = {Long.toString(location.getProjectId()), location.getLocation()};
		Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.LOCATIONS_TABLE_NAME, allColumns, where, whereArgs, null, null, null);
		cursor.moveToFirst();
		SQLLocation newLocation = cursorToLocation(cursor);
		// Make sure to close the cursor
		cursor.close();
		return newLocation;		
	}

	private SQLLocation cursorToLocation(Cursor cursor) {
		SQLLocation location = new SQLLocation();
		location.setId(cursor.getLong(0));
		location.setLocationProjectId(cursor.getLong(1));
		location.setLocation(cursor.getString(2));
		location.setCloudID(cursor.getLong(3));
		return location;
	}





}
