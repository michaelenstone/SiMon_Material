package uk.co.simon.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.FileNotFoundException;
import java.util.List;

import uk.co.simon.app.R;
import uk.co.simon.app.filesAndSync.FileManager;
import uk.co.simon.app.sqllite.DataSourcePhotos;
import uk.co.simon.app.sqllite.SQLPhoto;

public class AdapterImages extends BaseAdapter {
   
	private static Context mContext;
    private DataSourcePhotos mPhotosDataSource;
	private List<SQLPhoto> mList;
    private static AdapterImages mSelf;

    public AdapterImages(Context context, DataSourcePhotos dataSourcePhotos, long ReportItemId) {
        mContext = context;
        mPhotosDataSource = dataSourcePhotos;
        mList = mPhotosDataSource.getReportItemPhotos(ReportItemId);
        mSelf = this;
    }

    public int getCount() {
        return mList.size();
    }

    public void add(SQLPhoto photo) {
        mList.add(photo);
        notifyDataSetChanged();
    }

    public void add(List<SQLPhoto> photos) {
        mList.addAll(photos);
        notifyDataSetChanged();
    }

    public void remove(SQLPhoto photo) {
        int position = mList.indexOf(photo);
        mList.remove(position);
        mPhotosDataSource.deletePhoto(photo);
        notifyDataSetChanged();
    }

    public SQLPhoto getItem(int position) {
        return mList.get(position);
    }

    public SQLPhoto getSQLPhoto(int position) {
        return mList.get(position);
    }
    
    public long getItemId(int position) {
        return mList.get(position).getId();
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(final int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialise some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(150, 150));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        Bitmap bm = null;
        FileManager fm = new FileManager(mContext);
		try {
			bm = fm.resizeImage(mList.get(position).getPhotoPath(), 150);
		} catch (FileNotFoundException e) {
			Toast toast = Toast.makeText(mContext, mContext.getString(R.string.errFileNotFound), Toast.LENGTH_LONG);
			toast.show();
		}
        imageView.setImageBitmap(bm);
        imageView.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        MaterialDialog mImageOptionsDialog = new MaterialDialog.Builder(mContext)
                                .title(mContext.getString(R.string.photoMenuTitle))
                                .content(mContext.getString(R.string.photoMenuText))
                                .cancelable(true)
                                .positiveText(mContext.getString(R.string.photoMenuView))
                                .negativeText(mContext.getString(R.string.photoMenuDelete))
                                .callback(new MaterialDialog.ButtonCallback() {
                                    public void onPositive(MaterialDialog dialog) {
                                        Intent intent = new Intent();
                                        intent.setAction(Intent.ACTION_VIEW);
                                        intent.setDataAndType(Uri.parse(mList.get(position).getPhotoPath()), "image/*");
                                        mContext.startActivity(intent);
                                    }
                                    public void onNegative(MaterialDialog dialog) {
                                        mSelf.remove(mList.get(position));
                                    }
                                })
                                .show();
                    }
                });
        return imageView;
    }
}
