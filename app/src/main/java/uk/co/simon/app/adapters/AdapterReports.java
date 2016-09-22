package uk.co.simon.app.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.splunk.mint.Mint;

import java.util.ArrayList;
import java.util.List;

import uk.co.simon.app.ActivityReportItems;
import uk.co.simon.app.ActivityReports;
import uk.co.simon.app.R;
import uk.co.simon.app.SiMonApplication;
import uk.co.simon.app.sqllite.SQLProject;
import uk.co.simon.app.sqllite.SQLReport;
import uk.co.simon.app.ui.customElements.DialogReportHeader;

public class AdapterReports extends RecyclerView.Adapter<AdapterReports.ViewHolder>  {
	
	private SiMonApplication mSiMonApplication;
    private LayoutInflater mInflater;
	private List<SQLReport> mList = new ArrayList<>();
	private Context mContext;
	private static AdapterReports mSelf;
	private Intent mIntent;
    public boolean isFiltered;
    public boolean isOrdered;

    public AdapterReports(Context context, SiMonApplication application) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mSiMonApplication = application;
        mList = mSiMonApplication.getReportsDatasource().getAllReports(null);
        mSelf = this;
        mIntent = new Intent(mContext, ActivityReportItems.class);
        this.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onChanged(){
                ActivityReports activity = (ActivityReports) mContext;
                activity.invalidateOptionsMenu();
            }
        });
        isFiltered = false;
        isOrdered = false;
    }

	public static class ViewHolder extends RecyclerView.ViewHolder {

		public TextView txtHeader;
		public TextView txtFooter;
		public Button mButton;
		public ImageView mImage;
		public RelativeLayout mLayout;

		public ViewHolder(View v) {
			super(v);
			txtHeader = (TextView) v.findViewById(R.id.firstLine);
			txtFooter = (TextView) v.findViewById(R.id.secondLine);
			mButton = (Button) v.findViewById(R.id.edit_icon);
			mImage = (ImageView) v.findViewById(R.id.arrow_icon);
			mLayout = (RelativeLayout) v.findViewById(R.id.report_row);
		}
	}

	// Create new views (invoked by the layout manager)
	public AdapterReports.ViewHolder onCreateViewHolder(final ViewGroup parent,
												   int viewType) {
		// create a new view
		View v = mInflater.inflate(R.layout.reports_line, parent, false);

		return new ViewHolder(v);
	}

	public void reOrder(String orderBy) {
        mList = mSiMonApplication.getReportsDatasource().getAllReports(orderBy);
        if (orderBy != null) {
            isOrdered = true;
        } else {
            isOrdered = false;
        }
        notifyDataSetChanged();
    }

    public void filter(CharSequence projectName) {
        SQLProject project = mSiMonApplication.getProjectsDatasource().getProject(projectName);
        mList = mSiMonApplication.getReportsDatasource().getAllProjectReports(project.getId());
        isFiltered = true;
        notifyDataSetChanged();
    }

    public void add(int position, SQLReport item) {
		if (position < 0) {
			mList.add(item);
			notifyItemInserted(mList.size());
		} else {
			mList.add(position, item);
			notifyItemInserted(position);
		}
	}

	public void remove(SQLReport item) {
		int position = mList.indexOf(item);
        MaterialDialog progressDialog = new MaterialDialog.Builder(mContext)
                .title(R.string.deleteReportDialogTitle)
                .content(R.string.deleteReportDialogTitle)
                .progress(true, 0)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        ActivityReports activity = (ActivityReports) mContext;
                        activity.checkProjects();
                    }
                })
                .show();
        RemoveItem removeItem = new RemoveItem(progressDialog,item);
        removeItem.execute((Void) null);
		mList.remove(position);
		notifyItemRemoved(position);
	}

    public void removeForUpdate(SQLReport item) {
        int position = mList.indexOf(item);
        mList.remove(position);
        notifyItemRemoved(position);
    }


    public int getItemCount() {
		if (mList == null) {
            return 0;
        } else {
            return mList.size();
        }
	}

	public SQLReport getItem(int position) {
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

		final SQLReport report = mList.get(position);
		holder.txtHeader.setText(report.getReportRef());
		holder.txtHeader.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        mIntent.putExtra("reportId", report.getId());
                        mIntent.putExtra("reportType", report.getReportType());
                        mContext.startActivity(mIntent);
                    }
                });
        SQLProject project = mSiMonApplication.getProjectsDatasource().getProject(report.getProjectId());
		holder.txtFooter.setText(project.getProject() + " - " + report.getReportDate());
		holder.txtFooter.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        mIntent.putExtra("reportId", report.getId());
                        mIntent.putExtra("reportType", report.getReportType());
                        mContext.startActivity(mIntent);
                    }
                });
		holder.mButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        DialogReportHeader dialogReportHeader = new DialogReportHeader(mContext, report, mSiMonApplication, mSelf, position);
                        dialogReportHeader.showHeaderForm();
                    }
                });
        GradientDrawable background = (GradientDrawable) holder.mButton.getBackground();
        background.setColor(project.projectColor);
		holder.mImage.setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View v) {
						mIntent.putExtra("reportId", report.getId());
						mIntent.putExtra("reportType", report.getReportType());
						mContext.startActivity(mIntent);
					}
				});
		holder.mLayout.setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View v) {
						mIntent.putExtra("reportId", report.getId());
						mIntent.putExtra("reportType", report.getReportType());
						mContext.startActivity(mIntent);
					}
				});

	}

    private class RemoveItem extends AsyncTask<Void, Void, Boolean> {

        MaterialDialog mDialog;
        SQLReport mRemoveReport;

        public RemoveItem (MaterialDialog dialog, SQLReport report) {
            mDialog = dialog;
            mRemoveReport = report;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mSiMonApplication.getReportsDatasource().deleteReport(mRemoveReport);
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
                Toast.makeText(mContext, mContext.getString(R.string.msgReportDeleted), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
