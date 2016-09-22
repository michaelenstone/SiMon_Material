package uk.co.simon.app.filesAndSync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import uk.co.simon.app.R;
import uk.co.simon.app.sqllite.DataSource;
import uk.co.simon.app.sqllite.DataSourceProjects;
import uk.co.simon.app.sqllite.SQLProject;
import uk.co.simon.app.sqllite.SQLReport;

public class UploadReport extends AsyncTask<Void, Void, Boolean> {

	SQLReport mThisReport;
	Context mContext;
	MaterialDialog mProgress;
	DataSource mDataSource;

	public UploadReport(SQLReport report, Context context, MaterialDialog uploadProgress) {
		mThisReport = report;
		mContext = context;
		mProgress = uploadProgress;
		mDataSource = new DataSource(mContext);
	}

	protected Boolean doInBackground(Void... params) {
		Sync sync = new Sync(mContext);
		if (sync.networkIsAvailable()) {
			DataSourceProjects datasource = new DataSourceProjects(mDataSource);
			SQLProject project = datasource.getProject(mThisReport.getProjectId());
			if (sync.locationSync(project)){
				return sync.reportSync(mThisReport);
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	protected void onPostExecute(Boolean result) {		    
		if (result) {
			Toast.makeText(mContext, mContext.getString(R.string.msgUploadSuccess), Toast.LENGTH_SHORT).show();
		} else {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
			String msg = sharedPref.getString("ErrorPref", mContext.getString(R.string.msgUploadFail) );
			Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
		}
        mProgress.dismiss();
        mDataSource.close();
	}

}
