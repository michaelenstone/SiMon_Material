package uk.co.simon.app.filesAndSync;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.splunk.mint.Mint;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import redstone.xmlrpc.XmlRpcFault;
import uk.co.simon.app.R;
import uk.co.simon.app.sqllite.DataSource;
import uk.co.simon.app.sqllite.DataSourceLocations;
import uk.co.simon.app.sqllite.DataSourcePhotos;
import uk.co.simon.app.sqllite.DataSourceProjects;
import uk.co.simon.app.sqllite.DataSourceReportItems;
import uk.co.simon.app.sqllite.DataSourceReports;
import uk.co.simon.app.sqllite.SQLLocation;
import uk.co.simon.app.sqllite.SQLPhoto;
import uk.co.simon.app.sqllite.SQLProject;
import uk.co.simon.app.sqllite.SQLReport;
import uk.co.simon.app.sqllite.SQLReportItem;
import uk.co.simon.app.wordpress.SiMonWordpress;
import uk.co.simon.app.wordpress.WPLocation;
import uk.co.simon.app.wordpress.WPProject;

public class Sync {

	Context mContext;
	String mEmail;
	String mPassword;
	int mUserID;
	SiMonWordpress mWordPress = null;
	DataSource mDataSource;

	public Sync(Context context) {
		mContext = context;
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		mEmail = sharedPref.getString("EmailPref", null);
		mPassword = sharedPref.getString("PasswordPref", null);
		mUserID = sharedPref.getInt("UserID", 0);
		System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");
		try {
			mWordPress = new SiMonWordpress(mEmail, mPassword, "http://www.simon-app.com/xmlrpc.php");
		} catch (MalformedURLException e) {
			Mint.logEvent(e.toString());
		}
        mDataSource = new DataSource(mContext);
	}

	public boolean projectSync() {

		if(networkIsAvailable()) {
            if (!mDataSource.isOpen()) mDataSource.open();
			DataSourceProjects datasource = new DataSourceProjects(mDataSource);
			try {
				List<WPProject> WPProjects = mWordPress.getProjects();
				if (!WPProjects.get(0).Project.contains("Not Found")) {
					for (WPProject project : WPProjects) {
						SQLProject sqlProject = datasource.getProjectFromCloudId(project.getCloudID());
						if (sqlProject == null) {
							datasource.createProject(project.toSQLProject());
						} else if(!sqlProject.equals(project.toSQLProject())) {
							SQLProject updateProject = project.toSQLProject();
							updateProject.setId(sqlProject.getId());
							datasource.updateProject(updateProject);
						}
					}
				} else {
					datasource.deleteAllProjects();
				}
			} catch (XmlRpcFault e) {
				if (e.getMessage().contains("invalid") && e.getMessage().contains("password")) {
					SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
					SharedPreferences.Editor editor = sharedPref.edit();
					editor.putString("PasswordPref", null);
					editor.apply();
					//Intent login = new Intent(context, DialogFragmentLogin.class);
					//context.startActivity(login);
					return false;
				}
				Mint.logEvent(e.toString());
				return false;
			} catch (Exception e) {
				Mint.logEvent(e.toString());
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString("ErrorPref", mContext.getString(R.string.msgSyncFail) );
				editor.apply();
				return false;
			}
			List<SQLProject> syncedProjects = datasource.getAllProjects(null);
			for (SQLProject project : syncedProjects) {
				locationSync(project);
			}
			mDataSource.close();
			return true;
		} else {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString("ErrorPref", mContext.getString(R.string.msgSyncFail) );
			editor.apply();
			return false;
		}
	}

	public boolean locationSync(SQLProject project) {

		if(networkIsAvailable()) {
            if (!mDataSource.isOpen()) mDataSource.open();
			DataSourceLocations datasource = new DataSourceLocations(mDataSource);
			List<SQLLocation> locations = datasource.getAllProjectLocations(project.getId());
			try {
				List<WPLocation> WPLocations = mWordPress.getLocations(project.getCloudID());
				List<SQLLocation> locationsNotOnServer = new ArrayList<>();
				List<SQLLocation> locationsNotOnDevice = new ArrayList<>();
				locationsNotOnServer.addAll(locations);
				for (WPLocation location : WPLocations) {
					if (location.getCloudID()>-1){
						SQLLocation SQLlocation = datasource.getLocationFromCloudId(Long.parseLong(location.cloudID));
						if (SQLlocation == null) {
                            SQLlocation = location.toSQLLocation();
                            SQLlocation.setLocationProjectId(project.getId());
							locationsNotOnDevice.add(SQLlocation);
						} else {
							locationsNotOnServer.remove(SQLlocation);
						}
					}
				}
				for (SQLLocation location : locationsNotOnDevice) {
					datasource.createLocation(location);
				}
				for (SQLLocation location : locationsNotOnServer) {
					location.setCloudID(mWordPress.uploadLocation(location, project.getCloudID()));
					datasource.updateLocation(location);
				}
			} catch (XmlRpcFault e) {
				if (e.getMessage().contains("invalid") && e.getMessage().contains("password")) {
					SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
					SharedPreferences.Editor editor = sharedPref.edit();
					editor.putString("PasswordPref", null);
					editor.apply();
					return false;
				}
				Mint.logEvent(e.toString());
			}
			mDataSource.close();
			return true;
		} else {
			return false;
		}
	}

	public boolean reportSync(SQLReport report) {
		if(networkIsAvailable()) {
            if (!mDataSource.isOpen()) mDataSource.open();
			DataSourceProjects datasource = new DataSourceProjects(mDataSource);
			SQLProject project = datasource.getProject(report.getProjectId());
			try {
				report.setCloudID(mWordPress.uploadReport(report, project.getCloudID()));
				if (report.hasPDF()) {
					uploadPDF(report);
				}
				DataSourceReports datasourceReports = new DataSourceReports(mDataSource);
				datasourceReports.updateReport(report);
			} catch (XmlRpcFault e) {
				if (e.getMessage().contains("invalid") && e.getMessage().contains("password")) {
					SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
					SharedPreferences.Editor editor = sharedPref.edit();
					editor.putString("PasswordPref", null);
					editor.apply();
					return false;
				}
				Mint.logEvent(e.toString());
			}
            mDataSource.close();
			return reportItemsSync(report);
		} else {
			return false;
		}
	}

	public boolean reportItemsSync(SQLReport report) {
		if(networkIsAvailable()) {
            if (!mDataSource.isOpen()) mDataSource.open();
			DataSourceProjects datasource = new DataSourceProjects(mDataSource);
			SQLProject project = datasource.getProject(report.getProjectId());
			DataSourceReportItems datasourceReportItems = new DataSourceReportItems(mDataSource);
			List<SQLReportItem> reportItems = datasourceReportItems.getReportItems(report.getId());
			for (SQLReportItem reportItem : reportItems) {
				try {
					DataSourceLocations datasourceLocations = new DataSourceLocations(mDataSource);
					SQLLocation location = datasourceLocations.getLocation(reportItem.getLocationId());
					reportItem.setCloudID(mWordPress.uploadReportItem(reportItem, project.getCloudID(), report.getCloudID(), location.getCloudID()));
					datasourceReportItems.updateReportItem(reportItem);
					if (!uploadPhotos(reportItem, project.getCloudID(), location.getCloudID())) return false;
				} catch (XmlRpcFault e) {
					if (e.getMessage().contains("invalid") && e.getMessage().contains("password")) {
						SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
						SharedPreferences.Editor editor = sharedPref.edit();
						editor.putString("PasswordPref", null);
						editor.apply();
						return false;
					}
					Mint.logEvent(e.toString());
				}
			}
			mDataSource.close();
			return true;

		} else {
			return false;
		}
	}

	public boolean uploadPhotos(SQLReportItem reportItem, long projectCloudID, long locationCloudID) {
		if(networkIsAvailable()) {
            if (!mDataSource.isOpen()) mDataSource.open();
			DataSourcePhotos datasource = new DataSourcePhotos(mDataSource);
			List<SQLPhoto> photos = datasource.getReportItemPhotos(reportItem.getId());
			for (SQLPhoto photo : photos) {
				File file = new File(Uri.parse(photo.getPhotoPath()).getPath());

				if(file.exists()){ 
					HttpClient httpclient = new DefaultHttpClient();
					HttpPost httppost = new HttpPost(
							"http://www.simon-app.com/wp-content/plugins/SiMon%20Plugin/Upload.php?photo=set");

					try {
						MultipartEntity entity = new MultipartEntity();

						entity.addPart("report_item_id", new StringBody(String.valueOf(reportItem.getCloudID())));
						entity.addPart("project_id", new StringBody(String.valueOf(projectCloudID)));
						entity.addPart("location_id", new StringBody(String.valueOf(locationCloudID)));
						entity.addPart("user_id", new StringBody(String.valueOf(mUserID)));
						entity.addPart("cloud_id", new StringBody(String.valueOf(photo.getCloudID())));

						entity.addPart("image", new FileBody(file,"image/jpeg"));

						httppost.setEntity(entity);
						HttpResponse response = httpclient.execute(httppost);

						HttpEntity resEntity = response.getEntity();

						String content = EntityUtils.toString(resEntity);
						
						if (content.contains("Error:")) {
							SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
							SharedPreferences.Editor editor = sharedPref.edit();
							editor.putString("ErrorPref", content );
							editor.apply();
							return false;
						} else {
							photo.setCloudID(Long.parseLong(content));
							datasource.updatePhoto(photo);
						}
					} catch (IOException e) {
						Mint.logEvent(e.toString());
					}
				}
			}
			mDataSource.close();
			return true;
		} else {
			return false;
		}
	}

	public boolean uploadPDF(SQLReport report) {
		if(networkIsAvailable()) {
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(
					"http://www.simon-app.com/wp-content/plugins/SiMon%20Plugin/Upload.php?pdf=set");

			try {
				MultipartEntity entity = new MultipartEntity();

				entity.addPart("user_id", new StringBody(String.valueOf(mUserID)));
				entity.addPart("report_id", new StringBody(String.valueOf(report.getCloudID())));

				File file = new File(report.getPDF());
				entity.addPart("pdf", new FileBody(file,"application/pdf"));

				httppost.setEntity(entity);
				HttpResponse response = httpclient.execute(httppost);

				HttpEntity resEntity = response.getEntity();

				String content = EntityUtils.toString(resEntity);
				
				if (content.contains("Error:")) {
					SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
					SharedPreferences.Editor editor = sharedPref.edit();
					editor.putString("ErrorPref", content );
					editor.apply();
					return false;
				}
				
			} catch (IOException e) {
				Mint.logEvent(e.toString());
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean networkIsAvailable() {
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}