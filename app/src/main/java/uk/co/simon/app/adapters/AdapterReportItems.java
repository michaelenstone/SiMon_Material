package uk.co.simon.app.adapters;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.splunk.mint.Mint;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import uk.co.simon.app.ActivityReportItems;
import uk.co.simon.app.R;
import uk.co.simon.app.SiMonApplication;
import uk.co.simon.app.filesAndSync.FileManager;
import uk.co.simon.app.sqllite.SQLLocation;
import uk.co.simon.app.sqllite.SQLPhoto;
import uk.co.simon.app.sqllite.SQLReportItem;
import uk.co.simon.app.ui.customElements.DialogReportItem;

public class AdapterReportItems extends RecyclerView.Adapter<AdapterReportItems.ViewHolder>  {
	
	private LayoutInflater mInflater;
	private List<SQLReportItem> mList = new ArrayList<>();
	private static ActivityReportItems mActivity;
	private static SiMonApplication mSiMonApplication;
	private AdapterReportItems mSelf;
	
	// Provide a reference to the views for each data item
	// Complex data items may need more than one view per item, and
	// you provide access to all the views for a data item in a view holder

	public static class ViewHolder extends RecyclerView.ViewHolder {
		// each data item is just a string in this case
		public TextView txtHeader;
		public TextView txtFooter;
		public ImageView mImage;
		public RelativeLayout mLayout;

		public ViewHolder(View v) {
			super(v);
			txtHeader = (TextView) v.findViewById(R.id.item_title);
			txtFooter = (TextView) v.findViewById(R.id.item_subtitle);
			mImage = (ImageView) v.findViewById(R.id.item_photo);
			mLayout = (RelativeLayout) v.findViewById(R.id.item_layout);
		}
	}

	// Create new views (invoked by the layout manager)
	public AdapterReportItems.ViewHolder onCreateViewHolder(final ViewGroup parent,
												   int viewType) {
		// create a new view
		View v = mInflater.inflate(R.layout.report_items_line, parent, false);

		return new ViewHolder(v);
	}

	public void add(int position, SQLReportItem item) {
		if (position < 0) {
			mList.add(item);
			notifyItemInserted(mList.size());
		} else {
			mList.add(position, item);
			notifyItemInserted(position);
		}
	}

	public void remove(SQLReportItem item) {
		int position = mList.indexOf(item);
		mList.remove(position);
		MaterialDialog progressDialog = new MaterialDialog.Builder(mActivity)
				.title(R.string.deleteReportDialogTitle)
				.content(R.string.deleteReportDialogTitle)
				.progress(true, 0)
				.show();
		RemoveItem removeItem = new RemoveItem(progressDialog,item);
		removeItem.execute((Void) null);
		notifyItemRemoved(position);
	}

    public AdapterReportItems(ActivityReportItems activity) {
		mInflater = LayoutInflater.from(activity);
		mActivity = activity;
        mSiMonApplication = activity.mSiMonApplication;
        mList = mSiMonApplication.getReportItemsDatasource().getReportItems(mActivity.mThisReport.getId());
		mSelf = this;
    }

	public int getItemCount() {
		return mList.size();
	}

	public SQLReportItem getItem(int position) {
		return mList.get(position);
	}

	public long getItemId(int position) {
		return mList.get(position).getId();
	}

	// Replace the contents of a view (invoked by the layout manager)
	@Override
	public void onBindViewHolder(final ViewHolder holder, final int position) {
		// - get element from your dataset at this position
		// - replace the contents of the view with that element

		final SQLReportItem reportItem = mList.get(position);
		final List<SQLPhoto> photos = mSiMonApplication.getPhotosDatasource().getReportItemPhotos(reportItem.getId());
		final SQLLocation location = mSiMonApplication.getLocationsDatasource().getLocation(reportItem.getLocationId());
		holder.txtHeader.setText(reportItem.getReportItem() + " - " + location.getLocation());
		holder.txtFooter.setText(reportItem.getDescription());
		Bitmap bm = null;
		FileManager fm = new FileManager(mActivity);
        Log.d("Holder Image", Integer.toString(holder.mImage.getLayoutParams().width));
		if (photos.size()>0) {
			try {
				bm = fm.resizeImage(photos.get(0).getPhotoPath(), holder.mImage.getLayoutParams().width);
			} catch (FileNotFoundException e) {
				Toast toast = Toast.makeText(mActivity, mActivity.getString(R.string.errFileNotFound), Toast.LENGTH_LONG);
				toast.show();
			}
		} else {
			try {
				bm = fm.resizeImage("Default", holder.mImage.getLayoutParams().width);
			} catch (FileNotFoundException e) {
				Toast toast = Toast.makeText(mActivity, mActivity.getString(R.string.errFileNotFound), Toast.LENGTH_LONG);
				toast.show();
			}
		}
		holder.mImage.setImageBitmap(bm);
		holder.mLayout.setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View v) {
						mActivity.mThisReportItem = reportItem;
						DialogReportItem dialogReportItem =
								new DialogReportItem(mActivity,mSiMonApplication,mActivity.mThisReport,mActivity.mThisReportItem,false,mSelf);
						dialogReportItem.showDialog();
					}
				});

	}

	private class RemoveItem extends AsyncTask<Void, Void, Boolean> {

		MaterialDialog mDialog;
		SQLReportItem mRemoveReportItem;

		public RemoveItem (MaterialDialog dialog, SQLReportItem reportItem) {
			mDialog = dialog;
			mRemoveReportItem = reportItem;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
            mSiMonApplication.getReportItemsDatasource().deleteReportItem(mRemoveReportItem);
			return true;
		}

		@Override
		protected  void onPostExecute(final Boolean success) {
			try {
				mDialog.dismiss();
			} catch (Exception e) {
				Mint.logException(e);
			}
			if (success) {
				Toast.makeText(mActivity, mActivity.getString(R.string.msgReportItemDeleted), Toast.LENGTH_SHORT).show();
			}
		}
	}
}
