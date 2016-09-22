package uk.co.simon.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.software.shell.fab.ActionButton;
import com.splunk.mint.Mint;

import uk.co.simon.app.adapters.AdapterReports;
import uk.co.simon.app.filesAndSync.ProjectLocationAsync;
import uk.co.simon.app.ui.customElements.DialogReportHeader;
import uk.co.simon.app.ui.customElements.LoginDialog;

public class ActivityReports extends AppCompatActivity {

	//Database References
	private SiMonApplication mSiMonApplication;

	// UI references.
	ProgressDialog uploadProgress = null;
	private RecyclerView mReportsList;
	public AdapterReports mAdapter;
	private static LoginDialog mLoginDialog;
    public static ActionButton mActionButton;
    private static Menu mMenu;

    private Boolean hasProjects = false;
    public ActivityReports mActivityReports;

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mint.initAndStartSession(ActivityReports.this, "6c6b0664");
        setContentView(R.layout.activity_reports);
        setTitle(R.string.title_activity_reports);

        mSiMonApplication = SiMonApplication.getInstance();
        mActivityReports = this;

        mReportsList = (RecyclerView) findViewById(R.id.reportsList);
        mActionButton = (ActionButton) findViewById(R.id.action_button);
        mAdapter = new AdapterReports(this, mSiMonApplication);

        mLoginDialog = new LoginDialog(this);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.reports_toolbar);
        setSupportActionBar(mToolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        checkLogin();
	}

	/**
	 * Checks stored login credentials and shows login Form if required
	 */
	private void checkLogin(){

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String mEmail = sharedPref.getString("EmailPref", null);
		String mPassword = sharedPref.getString("PasswordPref", null);

		String loginMessage = mLoginDialog.verifyEmailPassword(mEmail, mPassword);

		if (loginMessage != null) {
			mLoginDialog.showLoginForm(mEmail, loginMessage);
		} else {
            checkProjects();
        }
	}

	public void onClickReports(View view) {
		switch (view.getId()) {
			case R.id.action_button:
                if (hasProjects) {
                    DialogReportHeader mHeaderDialog = new DialogReportHeader(this, null, mSiMonApplication, mAdapter, 0);
                    mHeaderDialog.showHeaderForm();
                } else {
                    this.syncProjects();
                }
				break;
		}
	}

    public void syncProjects() {
        MaterialDialog syncProgress = new MaterialDialog.Builder(this)
                .title(R.string.menuSync)
                .content(R.string.messageSync)
                .progress(true, 0)
                .cancelable(false)
                .show();
        ProjectLocationAsync mTask = new ProjectLocationAsync(this, syncProgress);
        mTask.execute((Void) null);
    }

    public void checkProjects() {
		/*if (!mProjectsDatasource.mDatabase.isOpen()) {
            mDataSource.open();
            mProjectsDatasource = new DataSourceProjects(mDataSource);
        }*/
        if (mSiMonApplication.getProjectsDatasource().getAllProjects(null)!=null) {
            hasProjects = true;
            mActionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.fab_plus_icon));
            PopulateList(mReportsList);
        } else {
            hasProjects = false;
            mActionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_action_refresh));
            MaterialDialog mNoProjectDialog = new MaterialDialog.Builder(this)
                    .title(R.string.menuNoProjects)
                    .content(R.string.msgAddProjects)
                    .positiveText(R.string.menuGoToWeb)
                    .negativeText(R.string.menuSync)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            String url = "http://www.simon-app.com/wp-login.php";
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            mActivityReports.startActivity(i);
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            mActivityReports.syncProjects();
                        }
                    })
                    .cancelable(false)
                    .show();
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.reports_menu, menu);
        if (mAdapter.getItemCount()>2 || mAdapter.isOrdered || mAdapter.isFiltered) {
            menu.findItem(R.id.menuFilter).setVisible(true);
            menu.findItem(R.id.menuReorder).setVisible(true);
        } else {
            menu.findItem(R.id.menuFilter).setVisible(false);
            menu.findItem(R.id.menuReorder).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		OptionsMenu options = new OptionsMenu(item,this);
		options.menuSelect();
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
        mSiMonApplication.open();
		if (mLoginDialog.onReg) {
			checkLogin();
		} else {
            checkProjects();
        }
	}

	@Override
	protected void onPause() {
		if (uploadProgress != null) {
			uploadProgress.dismiss();
		}
        mSiMonApplication.close();
		super.onPause();
	}

    private void PopulateList(RecyclerView recyclerView) {

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new AdapterReports(this, mSiMonApplication);
        invalidateOptionsMenu();
        recyclerView.setAdapter(mAdapter);
    }
}
