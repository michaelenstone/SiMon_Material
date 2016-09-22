package uk.co.simon.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.software.shell.fab.ActionButton;
import com.splunk.mint.Mint;

import java.io.File;
import java.io.IOException;

import uk.co.simon.app.adapters.AdapterReportItems;
import uk.co.simon.app.filesAndSync.FileManager;
import uk.co.simon.app.sqllite.SQLPhoto;
import uk.co.simon.app.sqllite.SQLProject;
import uk.co.simon.app.sqllite.SQLReport;
import uk.co.simon.app.sqllite.SQLReportItem;
import uk.co.simon.app.ui.customElements.DialogReportItem;

public class ActivityReportItems extends AppCompatActivity {

	//DB Interfaces
    public static SiMonApplication mSiMonApplication;
    public static SQLReport mThisReport;
	public static SQLReportItem mThisReportItem;

	//UI Interfaces
	private static RecyclerView mReportItemsList;
    private static AdapterReportItems mAdapterReportItems;
    public Uri mPhotoUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		Mint.initAndStartSession(ActivityReportItems.this, "6c6b0664");
        setContentView(R.layout.activity_report_items);

        mSiMonApplication = SiMonApplication.getInstance();

        Bundle extras = getIntent().getExtras();

        try {
            mThisReport = mSiMonApplication.getReportsDatasource().getReport(extras.getLong("reportId"));

	    if (mThisReport.getReportType()){
	    	setTitle(R.string.title_activity_site_visit_report);
	    } else {
	    	setTitle(R.string.title_activity_progress_report);
	    }

		Toolbar mToolbar = (Toolbar) findViewById(R.id.report_items_toolbar);

        SQLProject project = mSiMonApplication.getProjectsDatasource().getProject(mThisReport.getProjectId());

        mToolbar.setBackgroundColor(project.projectColor);

        TextView mainTitle = (TextView) mToolbar.findViewById(R.id.mainTitle);
        mainTitle.setText(mThisReport.getReportRef());

        TextView subTitle = (TextView) mToolbar.findViewById(R.id.subTitle);
        subTitle.setText(project.getProject() + " - " + mThisReport.getReportDate());

        setSupportActionBar(mToolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
		getSupportActionBar().setDisplayShowTitleEnabled(false);

        ActionButton actionButton = (ActionButton) findViewById(R.id.action_button);
        actionButton.setButtonColor(project.projectColor);

		mReportItemsList = (RecyclerView)findViewById(R.id.reportItemsList);
        mAdapterReportItems = new AdapterReportItems(this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mReportItemsList.setLayoutManager(layoutManager);
        mReportItemsList.setAdapter(mAdapterReportItems);

        } catch (Exception e) {
            Intent mIntent = new Intent(ActivityReportItems.this, ActivityReports.class);
            this.startActivity(mIntent);
            Log.e("bundle error", "bundle not present");
        }
    }
	
    @Override
    protected void onResume() {
    	super.onResume();
        mSiMonApplication.open();
    }

	@Override
    protected void onPause() {
        super.onPause();
        mSiMonApplication.close();
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.report_items_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		OptionsMenu options = new OptionsMenu(item,this);
		options.menuSelect();
        return super.onOptionsItemSelected(item);
    }

	public void onClickReportItems(View view) {
        mThisReportItem = new SQLReportItem();
        DialogReportItem dialogReportItem = new DialogReportItem(this,mSiMonApplication,mThisReport,mThisReportItem,true,mAdapterReportItems);
        dialogReportItem.showDialog();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
		if (resultCode == Activity.RESULT_OK) {
            SQLPhoto photo = new SQLPhoto();
            photo.setReportItemId(mThisReportItem.getId());
            photo.setLocationId(mThisReportItem.getLocationId());
            FileManager fm = new FileManager(this);
            switch (requestCode) {
                case DialogReportItem.REQ_CODE_TAKE_IMAGE:
                    photo.setPhoto(mPhotoUri.getPath());
                    break;
                case DialogReportItem.REQ_CODE_PICK_IMAGE:
                    String realPath = fm.getRealPath(imageReturnedIntent.getData());
                    File imagefile = new File(realPath);
                    try {
                        File importedImage = fm.importImageFile(imagefile);
                        photo.setPhoto(importedImage.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }

            mSiMonApplication.getPhotosDatasource().createPhoto(photo);

            mReportItemsList = (RecyclerView)findViewById(R.id.reportItemsList);
            mAdapterReportItems = new AdapterReportItems(this);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            mReportItemsList.setLayoutManager(layoutManager);
            mReportItemsList.setAdapter(mAdapterReportItems);

            DialogReportItem dialogReportItem = new DialogReportItem(this, mSiMonApplication, mThisReport, mThisReportItem, false, mAdapterReportItems);
            dialogReportItem.showDialog();
		}
	}
}
