package uk.co.simon.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import uk.co.simon.app.sqllite.SQLProject;

public class SpinnerAdapterProjects extends ArrayAdapter {

	private LayoutInflater mInflater;
	private List<SQLProject> mProjects = new ArrayList<>();
	private Context mContext;

	public SpinnerAdapterProjects(Context context, List<SQLProject> projects, int textViewResourceID) {

		super (context, textViewResourceID);
		mContext = context;
		mProjects = projects;
		mInflater = LayoutInflater.from(mContext);

	}


	public int getCount() {
		return mProjects.size();
	}

	public SQLProject getItem(int position) {
		return mProjects.get(position);
	}

	public long getItemId(int position) {
		return mProjects.get(position).getId();
	}

	public View getCustomView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater.inflate(android.R.layout.simple_list_item_2, parent, false);

			holder = new ViewHolder();
			holder.title = (TextView) convertView.findViewById(android.R.id.text1);
			holder.sub = (TextView) convertView.findViewById(android.R.id.text2);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		SQLProject project = mProjects.get(position);
		holder.title.setText(project.getProject());
		holder.sub.setText(project.getProjectNumber());
		return convertView;

	}

	public View getDropDownView(int position, View convertView,
								ViewGroup parent) {
		return getCustomView(position, convertView, parent);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		return getCustomView(position, convertView, parent);
	}

	static class ViewHolder {
		TextView title;
		TextView sub;
	}
}
