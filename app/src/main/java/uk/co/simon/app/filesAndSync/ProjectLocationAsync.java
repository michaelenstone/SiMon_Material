package uk.co.simon.app.filesAndSync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import uk.co.simon.app.ActivityReports;
import uk.co.simon.app.R;

public class ProjectLocationAsync extends AsyncTask<Void, Void, Boolean> {

	Context mContext;
	MaterialDialog mProgress;
	
	public ProjectLocationAsync (Context context, MaterialDialog progress) {
		mContext = context;
		mProgress = progress;
	}
	
	@Override
	protected Boolean doInBackground(Void... params) {

		Sync sync = new Sync(mContext);
		return sync.projectSync();
	}

	@Override
	protected void onPostExecute(final Boolean success) {		    
		try {
			mProgress.dismiss();
		} catch (Exception e) {	
		}
		if (success) {
			Toast.makeText(mContext, mContext.getString(R.string.msgSyncSuccess), Toast.LENGTH_SHORT).show();
			ActivityReports activity = (ActivityReports) mContext;
			activity.checkProjects();
		} else {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
			String msg = sharedPref.getString("ErrorPref", mContext.getString(R.string.msgSyncFail));
			Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
			ActivityReports activity = (ActivityReports) mContext;
			activity.checkProjects();
		}
	}

}
