package uk.co.simon.app.ui.customElements;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import java.util.Calendar;

@SuppressLint("ValidFragment")
public class DialogFragmentDate extends DialogFragment {

	static int mYear;
	static int mMonth;
	static int mDay;
	private DialogReportHeader mDialog;
	private Calendar c;
	
	public DialogFragmentDate(DialogReportHeader callback, Calendar cal) {
		mDialog = callback;
		this.c = cal;
	}
	
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);
		
		return new DatePickerDialog(getActivity(), (OnDateSetListener) mDialog, mYear, mMonth, mDay);
	}
}