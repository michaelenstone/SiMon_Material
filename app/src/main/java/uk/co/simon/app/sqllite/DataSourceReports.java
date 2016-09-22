package uk.co.simon.app.sqllite;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class DataSourceReports {

	private DataSource mDataSource;
	private String[] allColumns = { SQLiteHelper.COLUMN_ID,
			SQLiteHelper.COLUMN_PROJECT_ID,
			SQLiteHelper.COLUMN_REPORT_DATE,
			SQLiteHelper.COLUMN_REPORT_TYPE,
			SQLiteHelper.COLUMN_REPORT_SUPERVISOR,
			SQLiteHelper.COLUMN_REPORT_REF,
			SQLiteHelper.COLUMN_REPORT_WEATHER,
			SQLiteHelper.COLUMN_REPORT_TEMP,
			SQLiteHelper.COLUMN_REPORT_TEMP_TYPE,
			SQLiteHelper.COLUMN_REPORT_PDF,
			SQLiteHelper.COLUMN_CLOUD_ID };

	public DataSourceReports(DataSource dataSource) {
		mDataSource = dataSource;
	}

    public SQLReport createReport(SQLReport report) {

		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COLUMN_PROJECT_ID, report.getProjectId());
		values.put(SQLiteHelper.COLUMN_REPORT_DATE, report.getReportDateDB());
		values.put(SQLiteHelper.COLUMN_REPORT_TYPE, report.getReportType());
		values.put(SQLiteHelper.COLUMN_REPORT_SUPERVISOR, report.getSupervisor());
		values.put(SQLiteHelper.COLUMN_REPORT_REF, report.getReportRef());
		values.put(SQLiteHelper.COLUMN_REPORT_WEATHER, report.getWeather());
		values.put(SQLiteHelper.COLUMN_REPORT_TEMP, report.getTemp());
		values.put(SQLiteHelper.COLUMN_REPORT_TEMP_TYPE, report.getTempType());
		values.put(SQLiteHelper.COLUMN_REPORT_PDF, report.getPDF());
		values.put(SQLiteHelper.COLUMN_CLOUD_ID, report.getCloudID());
		long insertId = mDataSource.getDatabase().insert(SQLiteHelper.REPORTS_TABLE_NAME, null,
				values);
		Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.REPORTS_TABLE_NAME,
				allColumns, SQLiteHelper.COLUMN_ID + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		SQLReport newReport = cursorToReport(cursor);
		cursor.close();
		return newReport;
	}

	public SQLReport updateReport(SQLReport report) {
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COLUMN_PROJECT_ID, report.getProjectId());
		values.put(SQLiteHelper.COLUMN_REPORT_DATE, report.getReportDateDB());
		values.put(SQLiteHelper.COLUMN_REPORT_TYPE, report.getReportType());
		values.put(SQLiteHelper.COLUMN_REPORT_SUPERVISOR, report.getSupervisor());
		values.put(SQLiteHelper.COLUMN_REPORT_REF, report.getReportRef());
		values.put(SQLiteHelper.COLUMN_REPORT_WEATHER, report.getWeather());
		values.put(SQLiteHelper.COLUMN_REPORT_TEMP, report.getTemp());
		values.put(SQLiteHelper.COLUMN_REPORT_TEMP_TYPE, report.getTempType());
		values.put(SQLiteHelper.COLUMN_REPORT_PDF, report.getPDF());
		values.put(SQLiteHelper.COLUMN_CLOUD_ID, report.getCloudID());
		mDataSource.getDatabase().update(SQLiteHelper.REPORTS_TABLE_NAME,
				values,
				SQLiteHelper.COLUMN_ID + " = " + report.getId(),
				null);
		Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.REPORTS_TABLE_NAME,
				allColumns, SQLiteHelper.COLUMN_ID + " = " + report.getId(), null,
				null, null, null);
		cursor.moveToFirst();
		SQLReport updReport = cursorToReport(cursor);
		cursor.close();
		return updReport;
	}

	public void deleteReport(SQLReport report) {
		DataSourceReportItems datasourceReportItems = new DataSourceReportItems(mDataSource);
		datasourceReportItems.deleteReportItems(report);
		mDataSource.getDatabase().delete(SQLiteHelper.REPORTS_TABLE_NAME,
				SQLiteHelper.COLUMN_ID + " = " + report.getId(), null);
	}

	public List<SQLReport> getAllReports(String orderBy) {
		List<SQLReport> reports = new ArrayList<>();
		String order;

        if (orderBy == null) {
            order = SQLiteHelper.COLUMN_REPORT_DATE + " DESC";
        } else {
            order = orderBy;
        }

        if (order.equals(SQLiteHelper.COLUMN_PROJECT_NAME + " ASC") || order.equals(SQLiteHelper.COLUMN_PROJECT_NAME + " DESC")) {

            DataSourceProjects dataSourceProjects = new DataSourceProjects(mDataSource);
            List<SQLProject> projects = dataSourceProjects.getAllProjects(orderBy);

            for (SQLProject project: projects) {
                List<SQLReport> projectReports = getAllProjectReports(project.getId());
                if (projectReports != null) {
                    reports.addAll(projectReports);
                }
            }

        } else {

            Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.REPORTS_TABLE_NAME,
					allColumns, null, null, null, null, order);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                SQLReport report = cursorToReport(cursor);
                reports.add(report);
                cursor.moveToNext();
            }
            // Make sure to close the cursor
            cursor.close();
        }

		return reports;
	}

    public void upgradeAllReports() {
        List<SQLReport> reports = new ArrayList<>();

        Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.REPORTS_TABLE_NAME,
				allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            SQLReport report = upgradeCursorToReport(cursor);
            reports.add(report);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();

        for (SQLReport report:reports) {
            updateReport(report);
        }
    }

	public List<SQLReport> getAllProjectReports(long projectId) {
		List<SQLReport> reports = new ArrayList<>();

		Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.REPORTS_TABLE_NAME,
				allColumns, SQLiteHelper.COLUMN_PROJECT_ID + " = " + projectId, null,
				null, null, SQLiteHelper.COLUMN_ID + " DESC");
		
		if (!(cursor.moveToFirst()) || cursor.getCount() ==0) {
			cursor.close();
			return null;
		} else {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				SQLReport report = cursorToReport(cursor);
				reports.add(report);
				cursor.moveToNext();
			}
			// Make sure to close the cursor
			cursor.close();
			return reports;
		}
	}

	public SQLReport getReport(long reportId) {
		Cursor cursor = mDataSource.getDatabase().query(SQLiteHelper.REPORTS_TABLE_NAME,
				allColumns, SQLiteHelper.COLUMN_ID + " = " + reportId, null,
				null, null, null);
		cursor.moveToFirst();
		SQLReport Report = cursorToReport(cursor);
		cursor.close();
		return Report;
	}

	private SQLReport cursorToReport(Cursor cursor) {

		SQLReport report = new SQLReport();
		report.setId(cursor.getLong(0));
		report.setProjectId(cursor.getLong(1));
		report.setReportDateDB(cursor.getString(2));
		if (cursor.getLong(3)>0) {
			report.setReportType(true);
		} else {
			report.setReportType(false);
		}
		report.setSupervisor(cursor.getString(4));
		report.setReportRef(cursor.getString(5));
		report.setWeather(cursor.getString(6));
		report.setTemp(cursor.getString(7));
		report.setTempType(cursor.getLong(8));
		report.setPDF(cursor.getString(9));
		report.setCloudID(cursor.getLong(10));
		return report;
	}

    private SQLReport upgradeCursorToReport(Cursor cursor) {

        SQLReport report = new SQLReport();
        report.setId(cursor.getLong(0));
        report.setProjectId(cursor.getLong(1));
        report.setReportDate(cursor.getString(2));
        if (cursor.getLong(3)>0) {
            report.setReportType(true);
        } else {
            report.setReportType(false);
        }
        report.setSupervisor(cursor.getString(4));
        report.setReportRef(cursor.getString(5));
        report.setWeather(cursor.getString(6));
        report.setTemp(cursor.getString(7));
        report.setTempType(cursor.getLong(8));
        report.setPDF(cursor.getString(9));
        report.setCloudID(cursor.getLong(10));
        return report;
    }

}
