package uk.co.simon.app.ui.customElements;

import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import uk.co.simon.app.R;
import uk.co.simon.app.SiMonApplication;
import uk.co.simon.app.adapters.AdapterReports;
import uk.co.simon.app.adapters.SpinnerAdapterProjects;
import uk.co.simon.app.sqllite.SQLProject;
import uk.co.simon.app.sqllite.SQLReport;

import static com.afollestad.materialdialogs.MaterialDialog.Builder;

public class DialogReportHeader implements OnDateSetListener {

	private Context mContext;

	SiMonApplication mSiMonApplication;
	SQLReport mThisReport;
	SQLProject mThisProject = null;

	//UI Interfaces
	private Spinner mProjectsSpinner;
	private EditText mRefText;
	private Button mDateButton;
	private EditText mWeatherText;
	private EditText mTempText;
	private Spinner mTempTypeSpinner;
	private EditText mSupervisorText;
	private AdapterReports mAdapterReports;

	//Values
	private boolean isNew;
	Calendar now = Calendar.getInstance();
	String mPositiveButtonTitle;
	String mNegatvieButtonTitle;
	private int mPosition;

	/**
	 * Initialise class
	 * @param context Required context in which to show dialog
	 * @param report Optional SQLReport, if provided dialog will be pre populated with values otherwise a new report will be generated
	 * @param application Application.
	 * @param adapterReports reports adapter required to add report to list and/or update.
	 * @param position position in adapter when updating report header, can be null
	 */
	public DialogReportHeader(Context context, SQLReport report, SiMonApplication application, AdapterReports adapterReports, int position) {
        mContext = context;
		mAdapterReports = adapterReports;
		mPosition = position;
		if (report == null) {
			mThisReport = new SQLReport();
			isNew = true;
			mPositiveButtonTitle = mContext.getString(R.string.title_activity_site_visit_report);
			mNegatvieButtonTitle = mContext.getString(R.string.title_activity_progress_report);
		} else {
			mThisReport = report;
			isNew = false;
			mPositiveButtonTitle = mContext.getString(R.string.dailyProgressSaveChanges);
			mNegatvieButtonTitle = mContext.getString(R.string.deleteReportDialogTitle);
		}

        mSiMonApplication = application;

	}

	public void showHeaderForm() {

		MaterialDialog mReportHeaderDialog = new Builder(mContext)
				.title(R.string.newReportDialogTitle)
				.customView(R.layout.dialog_fragment_report_header, true)
				.positiveText(mPositiveButtonTitle)
				.negativeText(mNegatvieButtonTitle)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (isNew) {
                            mThisReport.setReportType(true);
                        }
                        commitReport();
                    }
                })
				.onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						if (isNew) {
							mThisReport.setReportType(false);
							commitReport();
                        } else {
                            new MaterialDialog.Builder(mContext)
                                    .title(mContext.getString(R.string.deleteReportDialogTitle))
                                    .content(mContext.getString(R.string.deleteReportDialogText))
                                    .cancelable(true)
                                    .positiveText(mContext.getString(R.string.contextDelete))
                                    .negativeText(mContext.getString(R.string.alert_dialog_cancel))
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            mAdapterReports.remove(mThisReport);
                                            mAdapterReports.notifyDataSetChanged();
                                        }
                                    })
                                    .onNegative(null)
                                    .show();
                        }
					}
				})
				.cancelable(true)
				.show();
		View mCustomDialogView = mReportHeaderDialog.getCustomView();

		assert mCustomDialogView != null;
		mProjectsSpinner = (Spinner) mCustomDialogView.findViewById(R.id.dailyProgressProjectSpinner);
		mRefText = (EditText) mCustomDialogView.findViewById(R.id.dailyProgressReportRefEditText);
		mRefText.requestFocus();
		mDateButton = (Button) mCustomDialogView.findViewById(R.id.dailyProgressDateButton);

		updateSpinner(mProjectsSpinner);

		mSupervisorText = (EditText) mCustomDialogView.findViewById(R.id.dailyProgressSupervisor);
		mWeatherText = (EditText) mCustomDialogView.findViewById(R.id.dailyProgressWeatherEditText);
		mTempText = (EditText) mCustomDialogView.findViewById(R.id.dailyProgressTempEditText);
		mTempTypeSpinner = (Spinner) mCustomDialogView.findViewById(R.id.dailyProgressTempSpinner);

		if (!isNew) {

			mDateButton.setText(mThisReport.getReportDate());
			mSupervisorText.setText(mThisReport.getSupervisor());
			mWeatherText.setText(mThisReport.getWeather());
			mTempText.setText(mThisReport.getTemp());
			mRefText.setText(mThisReport.getReportRef());
			mTempTypeSpinner.setSelection((int) mThisReport.getTempType());

		} else {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
			String name = sharedPref.getString("NamePref", "xyd324");
			assert name != null;
			if (!name.equals("xyd324")) {
				mThisReport.setSupervisor(name);
				mSupervisorText.setText(mThisReport.getSupervisor());
			}
		}

		mProjectsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
				if (position != 0) {
					mThisProject = (SQLProject) mProjectsSpinner.getAdapter().getItem(position);
					mThisReport.setProjectId(mThisProject.getId());
				}
			}

			public void onNothingSelected(AdapterView<?> parentView) {

			}
		});

		mDateButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showDatePickerDialog();
			}
		});

	}
	
	public void showDatePickerDialog() {
		try {
			FragmentManager fragmentManager = ((FragmentActivity) mContext).getSupportFragmentManager();
			DialogFragment newFragment = new DialogFragmentDate(this, now);
			newFragment.show(fragmentManager,"datePicker");
		} catch (ClassCastException ignored) {
		}
	}

	public void updateSpinner(Spinner projectsSpinner) {

		List<SQLProject> projects;
		projects = mSiMonApplication.getProjectsDatasource().getAllProjects(null);
		List<SQLProject> values = new ArrayList<>();
		SQLProject firstRow = new SQLProject();
		if (isNew) {
			firstRow.setId(-1);
			firstRow.setProject(mContext.getString(R.string.dailyProgressSelectProject));
			firstRow.setProjectNumber(mContext.getString(R.string.dailyProgressClickHere));
		} else {
			firstRow = mSiMonApplication.getProjectsDatasource().getProject(mThisReport.getProjectId());
		}
		values.add(firstRow);
		values.addAll(projects);
		SpinnerAdapterProjects mSpinnerAdapter = new SpinnerAdapterProjects(mContext, values, android.R.layout.simple_list_item_2);
		projectsSpinner.setAdapter(mSpinnerAdapter);
	}

	public void commitReport() {
		if (isNew) {
			packageReport();
			mThisReport = mSiMonApplication.getReportsDatasource().createReport(mThisReport);
			mAdapterReports.add(-1, mThisReport);
		} else {
			mAdapterReports.removeForUpdate(mThisReport);
			packageReport();
			mThisReport = mSiMonApplication.getReportsDatasource().updateReport(mThisReport);
			mAdapterReports.add(mPosition,mThisReport);
		}
		mAdapterReports.notifyDataSetChanged();
	}

	public void packageReport() {

		SpinnerAdapter adapter = mProjectsSpinner.getAdapter();
		SQLProject project = (SQLProject) adapter.getItem(mProjectsSpinner.getSelectedItemPosition());

		mThisReport.setProjectId(project.getId());
		mThisReport.setSupervisor(mSupervisorText.getText().toString());
		try  {  
			mThisReport.setTemp(mTempText.getText().toString());
		} catch( Exception e ) {  			  
			mThisReport.setTemp("0");
		} 
		mThisReport.setTempType(mTempTypeSpinner.getSelectedItemPosition());
		mThisReport.setWeather(mWeatherText.getText().toString());
		mThisReport.setReportRef(mRefText.getText().toString());
	}

	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		String date = String.valueOf(dayOfMonth)+"/"+String.valueOf(monthOfYear+1)+"/"+String.valueOf(year);
		mDateButton.setText(date);
		now.set(year, monthOfYear, dayOfMonth);
		mThisReport.setReportDate(date);
		if(mThisProject!=null) {
			String refString = mThisProject.getProjectNumber() + "/" + mThisReport.getReportDateDB();
			mRefText.setText(refString);
			mWeatherText.requestFocus();
			InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.showSoftInput(mWeatherText, InputMethodManager.SHOW_IMPLICIT);
		}
	}

}
