package uk.co.simon.app.ui.customElements;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.net.MalformedURLException;

import redstone.xmlrpc.XmlRpcFault;
import uk.co.simon.app.ActivityReports;
import uk.co.simon.app.R;
import uk.co.simon.app.wordpress.SiMonUser;
import uk.co.simon.app.wordpress.SiMonWordpress;

public class LoginDialog {

    private UserLoginTask mAuthTask = null;

    // Values
    String mEmail;
    String mPassword;
    Context mContext;
    public Boolean onReg = true;
    private Boolean formVissible = false;

    //Preferences
    SharedPreferences mSharedPref;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;

    public LoginDialog(Context context) {
        mContext = context;
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    /**
     * Generates Material style login form based on parameters
     * @param email email address to show
     * @param message message to show above login form
     */
    public void showLoginForm(String email, String message) {
        if (!formVissible) {
            MaterialDialog mLoginDialog = new MaterialDialog.Builder(mContext)
                    .title(R.string.title_activity_activity_login)
                    .customView(R.layout.dialog_fragment_login, true)
                    .positiveText(R.string.action_sign_in)
                    .negativeText(R.string.action_register)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            View view = dialog.getCustomView();
                            assert view != null;
                            mEmailView = (EditText) view.findViewById(R.id.email);
                            mPasswordView = (EditText) view.findViewById(R.id.password);
                            attemptLogin(mEmailView.getText().toString(), mPasswordView.getText().toString());
                            formVissible = false;
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            String url = "http://www.simon-app.com/wp-login.php?action=register";
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            mContext.startActivity(i);
                            formVissible = false;
                        }
                    })
                    .cancelable(false)
                    .show();
            formVissible = true;
            View view = mLoginDialog.getCustomView();
            assert view != null;
            if (message != null) {
                TextView mLoginMessage = (TextView) view.findViewById(R.id.login_message);
                mLoginMessage.setText(message);
                mLoginMessage.setTextColor(mContext.getResources().getColor(R.color.red));
            }
            mEmailView = (EditText) view.findViewById(R.id.email);
            if (email != null && email.length() > 4) {
                mEmailView.setText(email);
                mPasswordView = (EditText) view.findViewById(R.id.password);
                mPasswordView.requestFocus();
            } else {
                mEmailView.requestFocus();
            }
        }
    }

    /**
     * Function to verify that email and password are acceptable
     * @param email email address
     * @param password password
     * @return null if no issues else returns error message
     */
    public String verifyEmailPassword(String email, String password) {
        String loginMessage = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            loginMessage = mContext.getString(R.string.error_password_field_required);
        } else if (password.length() < 4) {
            loginMessage = mContext.getString(R.string.error_invalid_password);
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            if (loginMessage != null) {
                if (loginMessage.length() > 4) {
                    loginMessage = loginMessage + ". " + mContext.getString(R.string.error_email_field_required);
                } else {
                    loginMessage = mContext.getString(R.string.error_email_field_required);
                }
            }
        } else if (!email.contains("@")) {
            if (loginMessage != null) {
                if (loginMessage.length() > 4) {
                    loginMessage = loginMessage + ". " + mContext.getString(R.string.error_invalid_email);
                } else {
                    loginMessage = mContext.getString(R.string.error_invalid_email);
                }
            }
        }

        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(password)) {
            loginMessage = mContext.getString(R.string.login_request);
        }

        if (loginMessage == null) {
            mEmail = email;
            mPassword = password;
        }

        return loginMessage;
    }

    /**
     * verify email and password before creating progress dialog
     * and logging in over the internet in background thread
     * @param email email
     * @param password password
     */
    public void attemptLogin(String email, String password) {

        String loginMessage = verifyEmailPassword(email, password);

        if (loginMessage!=null) {

            showLoginForm(email, loginMessage);

        } else {

            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.

            MaterialDialog mLoginStatusView = new MaterialDialog.Builder(mContext)
                    .title(R.string.login_progress_signing_in)
                    .content(R.string.login_progress_signing_in)
                    .progress(true, 0)
                    .show();

            mAuthTask = new UserLoginTask(mLoginStatusView);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        MaterialDialog progressDialog;

        public UserLoginTask (MaterialDialog dialog) {
            progressDialog = dialog;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            SiMonWordpress wp;
            SiMonUser user;
            System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");

            try {
                wp = new SiMonWordpress(mEmail, mPassword, "http://www.simon-app.com/xmlrpc.php");
            } catch (MalformedURLException e) {
                return false;
            }
            try {
                user = wp.getSimonUserInfo();
            } catch (XmlRpcFault e) {
                return false;
            }
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString("EmailPref", mEmail);
            editor.putString("PasswordPref", mPassword);
            editor.putInt("UserID", user.getUserid());
            editor.putInt("PhotoStorage", user.getPhotoStorage());
            editor.putString("NamePref", user.getFirstname() + " " + user.getLastname());
            editor.apply();
            return true;
        }

        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            onReg = false;
            progressDialog.dismiss();
            if (!success) {
                showLoginForm(mEmail, mContext.getString(R.string.error_incorrect_password));
            } else {
                ActivityReports dialogContext = (ActivityReports) mContext;
                dialogContext.checkProjects();
            }
        }
    }

}
