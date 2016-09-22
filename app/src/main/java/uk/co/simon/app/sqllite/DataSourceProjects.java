package uk.co.simon.app.sqllite;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class DataSourceProjects {

    public DataSource mDataSource;
    private String[] allColumns = { SQLiteHelper.COLUMN_ID,
            SQLiteHelper.COLUMN_PROJECT_NAME,
            SQLiteHelper.COLUMN_PROJECT_NO,
            SQLiteHelper.COLUMN_CLOUD_ID  };

    public DataSourceProjects(DataSource dataSource) {
        mDataSource = dataSource;
    }

    public SQLProject createProject(SQLProject project) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_PROJECT_NAME, project.getProject());
        values.put(SQLiteHelper.COLUMN_PROJECT_NO, project.getProjectNumber());
        values.put(SQLiteHelper.COLUMN_CLOUD_ID, project.getCloudID());
        long insertId = mDataSource.getDatabase().insert(SQLiteHelper.PROJECTS_TABLE_NAME, null, values);
        Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.PROJECTS_TABLE_NAME,
                allColumns, SQLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        SQLProject newProject = cursorToProject(cursor);
        cursor.close();
        return newProject;
    }

    public SQLProject updateProject(SQLProject project) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_PROJECT_NAME, project.getProject());
        values.put(SQLiteHelper.COLUMN_PROJECT_NO, project.getProjectNumber());
        values.put(SQLiteHelper.COLUMN_CLOUD_ID, project.getCloudID());
        mDataSource.getDatabase().update(SQLiteHelper.PROJECTS_TABLE_NAME,
                values,
                SQLiteHelper.COLUMN_ID + " = " + project.getId(),
                null);
        Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.PROJECTS_TABLE_NAME,
                allColumns, SQLiteHelper.COLUMN_ID + " = " + project.getId(), null,
                null, null, null);
        cursor.moveToFirst();
        SQLProject updProject = cursorToProject(cursor);
        cursor.close();
        return updProject;
    }

    public void deleteProject(SQLProject project) {
        mDataSource.getDatabase().delete(SQLiteHelper.PROJECTS_TABLE_NAME,
                SQLiteHelper.COLUMN_ID + " = " + project.getId(), null);
    }

    public void deleteAllProjects() {
        List<SQLProject> projects = getAllProjects(null);
        for (SQLProject project : projects) {
            deleteProject(project);
        }
    }

    public List<SQLProject> getAllProjects(String orderBy) {
        List<SQLProject> projects = new ArrayList<>();
        String order;

        if (orderBy == null) {
            order = SQLiteHelper.COLUMN_PROJECT_NAME + " ASC";
        } else {
            order = orderBy;
        }

        Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.PROJECTS_TABLE_NAME,
                allColumns, null, null, null, null, order);

        cursor.moveToFirst();

        if (cursor!=null && cursor.getCount()>0) {

            while (!cursor.isAfterLast()) {
                SQLProject project = cursorToProject(cursor);
                projects.add(project);
                cursor.moveToNext();
            }

        } else {
            return null;
        }
        // Make sure to close the cursor
        cursor.close();
        return projects;
    }

    public CharSequence[] getProjectNames(boolean withReports) {
        List<String> projects = new ArrayList<>();

        Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.PROJECTS_TABLE_NAME,
                allColumns, null, null, null, null, SQLiteHelper.COLUMN_PROJECT_NAME + " ASC");

        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            SQLProject project = cursorToProject(cursor);
            if (withReports) {
                DataSourceReports dataSourceReports = new DataSourceReports(mDataSource);
                List<SQLReport> reports = dataSourceReports.getAllProjectReports(project.getId());
                if (reports != null) {
                    if (reports.size() > 0) {
                        projects.add(project.getProject());
                    }
                }
            } else {
                projects.add(project.getProject());
            }
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();

        return projects.toArray(new CharSequence[projects.size()]);
    }

    public SQLProject getProject(long id) {

        Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.PROJECTS_TABLE_NAME,
                allColumns, SQLiteHelper.COLUMN_ID + " = " + id, null, null, null, null);

        cursor.moveToFirst();
        SQLProject project = null;
        while (!cursor.isAfterLast()) {
            project = cursorToProject(cursor);
            cursor.moveToNext();
        }
        cursor.close();
        return project;
    }

    public SQLProject getProject(CharSequence text) {

        String projectName = TextUtils.concat("\"", text, "\"").toString();

        Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.PROJECTS_TABLE_NAME,
                allColumns, SQLiteHelper.COLUMN_PROJECT_NAME + " = " + projectName, null, null, null, null);

        cursor.moveToFirst();
        SQLProject project = null;
        while (!cursor.isAfterLast()) {
            project = cursorToProject(cursor);
            cursor.moveToNext();
        }
        cursor.close();
        return project;
    }

    public SQLProject getProjectFromCloudId(long CloudId) {

        Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.PROJECTS_TABLE_NAME,
                allColumns, SQLiteHelper.COLUMN_CLOUD_ID + " = " + CloudId, null, null, null, null);

        cursor.moveToFirst();
        SQLProject project = null;
        while (!cursor.isAfterLast()) {
            project = cursorToProject(cursor);
            cursor.moveToNext();
        }
        cursor.close();
        return project;
    }

    public long getProjectId(String projectName, String projectNumber){
        String where = SQLiteHelper.COLUMN_PROJECT_NAME + " = ? AND " + SQLiteHelper.COLUMN_PROJECT_NO + " = ?";
        String[] whereArgs = {projectName,projectNumber};
        Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.PROJECTS_TABLE_NAME, allColumns, where, whereArgs, null, null, null);
        cursor.moveToFirst();
        long result = cursor.getLong(0);
        cursor.close();
        return result;
    }

    private SQLProject cursorToProject(Cursor cursor) {
        SQLProject project = new SQLProject();
        project.setId(cursor.getLong(0));
        project.setProject(cursor.getString(1));
        project.setProjectNumber(cursor.getString(2));
        project.setCloudID(cursor.getLong(3));
        return project;
    }
}
