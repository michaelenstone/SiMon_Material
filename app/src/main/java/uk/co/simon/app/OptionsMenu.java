package uk.co.simon.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;

import uk.co.simon.app.filesAndSync.PDFCreator;
import uk.co.simon.app.filesAndSync.ProjectLocationAsync;
import uk.co.simon.app.filesAndSync.UploadReport;
import uk.co.simon.app.sqllite.DataSource;
import uk.co.simon.app.sqllite.DataSourceProjects;
import uk.co.simon.app.sqllite.SQLReport;
import uk.co.simon.app.sqllite.SQLiteHelper;
import uk.co.simon.app.ui.customElements.LoginDialog;

public class OptionsMenu {

	private MenuItem mItem;
	private Context mContext;

	public OptionsMenu (MenuItem item, Context context) {
		mItem = item;
		mContext = context;
	}

	public boolean menuSelect() {
		switch (mItem.getItemId()) {
            case R.id.menuReorder:
                new MaterialDialog.Builder(mContext)
                        .title(R.string.reorderTitle)
                        .items(R.array.reorderArray)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                ActivityReports activity = (ActivityReports) mContext;
                                switch (which) {
                                    case 0:
                                        activity.mAdapter.reOrder(SQLiteHelper.COLUMN_REPORT_DATE + " ASC");
                                        break;
                                    case 1:
                                        activity.mAdapter.reOrder(SQLiteHelper.COLUMN_REPORT_DATE + " DESC");
                                        break;
                                    case 2:
                                        activity.mAdapter.reOrder(SQLiteHelper.COLUMN_PROJECT_NAME + " ASC");
                                        break;
                                    case 3:
                                        activity.mAdapter.reOrder(SQLiteHelper.COLUMN_PROJECT_NAME + " DESC");
                                        break;
                                }
                            }
                        })
                        .show();
                return true;
            case R.id.menuFilter:
                DataSource dataSource = new DataSource(mContext);
                DataSourceProjects dataSourceProjects = new DataSourceProjects(dataSource);
                CharSequence[] projectNames = dataSourceProjects.getProjectNames(true);
                new MaterialDialog.Builder(mContext)
                        .title(R.string.filterTitle)
                        .items(projectNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                ActivityReports activity = (ActivityReports) mContext;
                                activity.mAdapter.filter(text);
                            }
                        })
                        .positiveText(R.string.filterCancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            public void onPositive(MaterialDialog dialog) {
                                ActivityReports activity = (ActivityReports) mContext;
                                activity.mAdapter.reOrder(SQLiteHelper.COLUMN_REPORT_DATE + " ASC");
                            }
                        })
                        .show();
                return true;
			case R.id.menuSettings:
				Intent openSettings = new Intent(mContext, ActivitySettings.class);
				mContext.startActivity(openSettings);
				return true;
			case R.id.menuSync:
				MaterialDialog syncProgress = new MaterialDialog.Builder(mContext)
						.title(R.string.menuSync)
						.content(R.string.messageSync)
						.progress(true, 0)
						.cancelable(false)
						.dismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface dialog) {
								ActivityReports activity = (ActivityReports) mContext;
                                activity.checkProjects();
							}
						})
						.show();
				ProjectLocationAsync mTask = new ProjectLocationAsync(mContext, syncProgress);
				mTask.execute((Void) null);
				return true;
			case R.id.menuLogout:
				SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
				SharedPreferences.Editor editor = mSharedPref.edit();
				editor.putString("PasswordPref", null);
				editor.apply();
				LoginDialog loginDialog = new LoginDialog(mContext);
				loginDialog.showLoginForm(mSharedPref.getString("EmailPref", null), null);
				return true;
			case R.id.reportUpload:
                ActivityReportItems activity = (ActivityReportItems) mContext;
                MaterialDialog uploadProgress = new MaterialDialog.Builder(mContext)
                        .title(R.string.uploadReportDialogTitle)
                        .content(R.string.uploadReportDialogText)
                        .progress(true, 0)
                        .cancelable(false)
                        .show();
                UploadReport reportUpload = new UploadReport(activity.mThisReport,activity,uploadProgress);
                reportUpload.execute();
				return true;
			case R.id.reportPDF:
                ActivityReportItems activityReportItems = (ActivityReportItems) mContext;
                String mPositiveText = mContext.getString(R.string.contextCreatePDF);
                String mNegativeText = mContext.getString(R.string.alert_dialog_cancel);
                final SQLReport thisReport = activityReportItems.mThisReport;
                if (thisReport.hasPDF()) {
                    mPositiveText = mContext.getString(R.string.contextUpdatePDF);
                    mNegativeText = mContext.getString(R.string.contextOpenPDF);
                }
                MaterialDialog mPDFReportDialog = new MaterialDialog.Builder(mContext)
                        .title(mContext.getString(R.string.createPDFDialogTitle))
                        .content(mContext.getString(R.string.createPDFDialogText))
                        .cancelable(true)
                        .positiveText(mPositiveText)
                        .negativeText(mNegativeText)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                if (thisReport.hasPDF()) {
                                    File oldPDF = new File(thisReport.getPDF());
                                    if (!oldPDF.delete()) {
                                        Toast toast = Toast.makeText(mContext, mContext.getString(R.string.errPDFDelete), Toast.LENGTH_SHORT);
                                        toast.show();
                                        thisReport.setPDF(null);
                                    } else {
                                        MaterialDialog progress = new MaterialDialog.Builder(mContext)
                                                .title(R.string.createPDFDialogTitle)
                                                .content(R.string.createPDFDialogText)
                                                .progress(true, 0)
                                                .cancelable(false)
                                                .show();
                                        PDFCreator createPDF = new PDFCreator(thisReport, mContext, progress);
                                        createPDF.execute();
                                    }
                                } else {
                                    MaterialDialog progress = new MaterialDialog.Builder(mContext)
                                            .title(R.string.createPDFDialogTitle)
                                            .content(R.string.createPDFDialogText)
                                            .progress(true, 0)
                                            .cancelable(false)
                                            .show();
                                    PDFCreator createPDF = new PDFCreator(thisReport, mContext, progress);
                                    createPDF.execute();
                                }
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                if (thisReport.hasPDF()) {
                                    try {
                                        File reportPDF = new File(thisReport.getPDF());
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setDataAndType(Uri.fromFile(reportPDF), "application/pdf");
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                        mContext.startActivity(intent);
                                    } catch (Exception e) {
                                        Toast noPDF = Toast.makeText(mContext, R.string.msgNoPDFReader, Toast.LENGTH_SHORT);
                                        noPDF.show();
                                    }
                                }
                            }
                        })
                        .show();
				return true;
		}
		return false;
	}
}
