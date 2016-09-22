package uk.co.simon.app;

import android.app.Application;
import android.content.res.Configuration;

import uk.co.simon.app.sqllite.DataSource;
import uk.co.simon.app.sqllite.DataSourceLocations;
import uk.co.simon.app.sqllite.DataSourcePhotos;
import uk.co.simon.app.sqllite.DataSourceProjects;
import uk.co.simon.app.sqllite.DataSourceReportItems;
import uk.co.simon.app.sqllite.DataSourceReports;

public class SiMonApplication extends Application {

    private static SiMonApplication singleton;

    //DB Interfaces
    private static DataSourceProjects mProjectsDatasource;
    private static DataSourceReports mReportsDatasource;
    private static DataSourceReportItems mReportItemsDatasource;
    private static DataSourcePhotos mPhotosDatasource;
    private static DataSourceLocations mLocationsDatasource;
    private static DataSource mDatasource;

    public static SiMonApplication getInstance(){
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        mDatasource = new DataSource(getApplicationContext());
        mProjectsDatasource = new DataSourceProjects(mDatasource);
        mReportsDatasource = new DataSourceReports(mDatasource);
        mReportItemsDatasource = new DataSourceReportItems(mDatasource);
        mPhotosDatasource = new DataSourcePhotos(mDatasource);
        mLocationsDatasource = new DataSourceLocations(mDatasource);
    }

    public DataSourceProjects getProjectsDatasource() {
        open();
        return mProjectsDatasource;
    }

    public DataSourceReports getReportsDatasource() {
        open();
        return mReportsDatasource;
    }

    public DataSourceReportItems getReportItemsDatasource() {
        open();
        return mReportItemsDatasource;
    }

    public DataSourcePhotos getPhotosDatasource() {
        open();
        return mPhotosDatasource;
    }

    public DataSourceLocations getLocationsDatasource() {
        open();
        return mLocationsDatasource;
    }

    public void open() {
        if (!mDatasource.isOpen()) {
            mDatasource.open();
        }
    }

    public void close() {
        if (mDatasource.isOpen()) {
            mDatasource.close();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

}
