package uk.co.simon.app.ui.customElements;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.IOException;

import uk.co.simon.app.ActivityReportItems;
import uk.co.simon.app.R;
import uk.co.simon.app.SiMonApplication;
import uk.co.simon.app.adapters.AdapterImages;
import uk.co.simon.app.adapters.AdapterReportItems;
import uk.co.simon.app.filesAndSync.FileManager;
import uk.co.simon.app.sqllite.SQLLocation;
import uk.co.simon.app.sqllite.SQLReport;
import uk.co.simon.app.sqllite.SQLReportItem;

public class DialogReportItem {

	public static final int REQ_CODE_PICK_IMAGE = 100;
	public static final int REQ_CODE_TAKE_IMAGE = 200;

	//DB Interfaces
	private static SiMonApplication mSiMonApplication;
	private SQLReportItem mThisReportItem;
	private SQLLocation mThisLocation;
	private SQLReport mThisReport;

	//UI References
	private static AutoCompleteTextView mLocationText;
	private static EditText mActivityText;
	private static EditText mProgressText;
	private static Spinner mOnTimeSpinner;
	private static EditText mDescriptionText;
	private static LinearLayout mProgress;
	private static Button mTakePhoto;
	private static Button mAttachPhoto;
	private static ExpandableHeightGridView mGridView;

	//Adapters
	ArrayAdapter<SQLLocation> mLocationsAdapter;
	AdapterImages mPhotosAdapter;
    AdapterReportItems mAdapterReportItems;
	
	private static ActivityReportItems mActivity;
	private boolean isNew = false;
	private FileManager mFileManager;
	public Uri mPhotoURI;
    private MaterialDialog mReportItemDialog;
    String mPositiveButtonTitle;
    String mNegatvieButtonTitle;

	public DialogReportItem(ActivityReportItems activity, SiMonApplication application, SQLReport report,
							SQLReportItem reportItem, boolean newOne, AdapterReportItems adapterReportItems) {
		mActivity = activity;
        mSiMonApplication = application;

		mThisReport = report;
		mLocationsAdapter = new ArrayAdapter<>(mActivity, R.layout.spinner_row, mSiMonApplication.getLocationsDatasource().getAllProjectLocations(mThisReport.getProjectId()));

		mThisReportItem = reportItem;

		if (newOne) {
			mPhotosAdapter = new AdapterImages(mActivity, mSiMonApplication.getPhotosDatasource(), mThisReportItem.getId());
			isNew = true;
            mThisLocation = new SQLLocation();
            mPositiveButtonTitle = mActivity.getString(R.string.alert_dialog_ok);
            mNegatvieButtonTitle = mActivity.getString(R.string.alert_dialog_cancel);
		} else {
			mThisLocation = mSiMonApplication.getLocationsDatasource().getLocation(mThisReportItem.getLocationId());
			mPhotosAdapter = new AdapterImages(mActivity, mSiMonApplication.getPhotosDatasource(), mThisReportItem.getId());
			isNew = false;
            mPositiveButtonTitle = mActivity.getString(R.string.dailyProgressSaveChanges);
            mNegatvieButtonTitle = mActivity.getString(R.string.deleteReportItemDialogTitle);
		}
		
		mFileManager = new FileManager(mActivity);

        mAdapterReportItems = adapterReportItems;
	}

	public void showDialog() {

		mReportItemDialog = new MaterialDialog.Builder(mActivity)
				.customView(R.layout.dialog_fragment_report_item, true)
				.positiveText(mPositiveButtonTitle)
				.negativeText(mNegatvieButtonTitle)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						commitReportItem();
					}
				})
				.onNegative(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (!isNew) {
                            MaterialDialog mDeleteReportDialog = new MaterialDialog.Builder(mActivity)
                                    .title(mActivity.getString(R.string.deleteReportItemDialogTitle))
                                    .content(mActivity.getString(R.string.deleteReportItemDialogText))
                                    .cancelable(true)
                                    .positiveText(mActivity.getString(R.string.contextDelete))
                                    .negativeText(mActivity.getString(R.string.alert_dialog_cancel))
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            mAdapterReportItems.remove(mThisReportItem);
                                            mAdapterReportItems.notifyDataSetChanged();
                                        }
                                    })
                                    .onNegative(null)
                                    .show();
                        }
                    }
                })
				.cancelable(true)
				.show();
		View mCustomDialogView = mReportItemDialog.getCustomView();

		assert mCustomDialogView != null;
		mLocationText = (AutoCompleteTextView) mCustomDialogView.findViewById(R.id.dailyProgressLocationEditText);
		mActivityText = (EditText) mCustomDialogView.findViewById(R.id.dailyProgressActivityEditText);
		mProgressText = (EditText) mCustomDialogView.findViewById(R.id.dailyProgressProgressEditText);
		mOnTimeSpinner = (Spinner) mCustomDialogView.findViewById(R.id.dailyProgressOnTimeSpinner);
		mDescriptionText = (EditText) mCustomDialogView.findViewById(R.id.dailyProgressDescriptionEditText);
		mGridView = (ExpandableHeightGridView) mCustomDialogView.findViewById(R.id.dailyProgressPhotosGrid);

		if (mThisReport.getReportType()) {
			mProgress = (LinearLayout) mCustomDialogView.findViewById(R.id.dailyProgressProgressLayout);
			mProgress.setVisibility(View.GONE);
		}

		mTakePhoto = (Button) mCustomDialogView.findViewById(R.id.dailyProgressTakePhotoButton);
		mAttachPhoto = (Button) mCustomDialogView.findViewById(R.id.dailyProgressAttachPhotoButton);

		mLocationText.setAdapter(mLocationsAdapter);

		if (!isNew) {

			mActivityText.setText(mThisReportItem.getReportItem());
			mProgressText.setText(Float.toString(mThisReportItem.getProgress()));
			mDescriptionText.setText(mThisReportItem.getDescription());
            mDescriptionText.requestFocus();
			mOnTimeSpinner.setSelection(getIndex(mOnTimeSpinner, mThisReportItem.getOnTIme()));
			mLocationText.setText(mThisLocation.getLocation());

			mGridView.setAdapter(mPhotosAdapter);
			mGridView.setExpanded(true);

		}

		mTakePhoto.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
                commitReportItem();
                mReportItemDialog.dismiss();
                mActivity.mThisReportItem = mThisReportItem;
                dispatchTakePictureIntent();
			}
		});

		mAttachPhoto.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
                commitReportItem();
                mReportItemDialog.dismiss();
                mActivity.mThisReportItem = mThisReportItem;
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                mActivity.startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), REQ_CODE_PICK_IMAGE);
			}
		});

		mReportItemDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		mReportItemDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	}

	private void dispatchTakePictureIntent() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
			// Create the File where the photo should go
			File photoFile = null;
			try {
				photoFile = mFileManager.createImageFile();
			} catch (IOException ex) {
				
			}
			// Continue only if the File was successfully created
			if (photoFile != null) {
				mPhotoURI = Uri.fromFile(photoFile);
                mActivity.mPhotoUri = mPhotoURI;
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoURI);
				mActivity.startActivityForResult(takePictureIntent, REQ_CODE_TAKE_IMAGE);
			}
		}
	}

	private void commitReportItem() {

		mThisLocation.setLocation(mLocationText.getText().toString());
		try {
			mThisLocation = mSiMonApplication.getLocationsDatasource().findLocationId(mThisLocation);
		} catch (Exception e) {
			mThisLocation = mSiMonApplication.getLocationsDatasource().createLocation(mThisLocation);
		}
        mThisReportItem = packageReportItem();
		if (isNew) {
			mThisReportItem = mSiMonApplication.getReportItemsDatasource().createReportItem(mThisReportItem);
            mAdapterReportItems.add(0, mThisReportItem);
		} else mThisReportItem = mSiMonApplication.getReportItemsDatasource().updateReportItem(mThisReportItem);
        mAdapterReportItems.notifyDataSetChanged();
	}

	public SQLReportItem packageReportItem() {

		mThisReportItem.setLocationId(mThisLocation.getId());
		mThisReportItem.setReportItem(mActivityText.getText().toString());
		try  {
			mThisReportItem.setProgress(Float.parseFloat(mProgressText.getText().toString()));
		} catch( Exception e ) {
			mThisReportItem.setProgress(0);
		}
		mThisReportItem.setOnTIme(mOnTimeSpinner.getSelectedItem().toString());
		mThisReportItem.setDescription(mDescriptionText.getText().toString());
        mThisReportItem.setReportId(mThisReport.getId());
		return mThisReportItem;
	}

	private int getIndex(Spinner spinner, String myString){

		int index = 0;

		for (int i=0;i<spinner.getCount();i++){
			if (spinner.getItemAtPosition(i).equals(myString)){
				index = i;
			}
		}
		return index;
	}
}