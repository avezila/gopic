package com.avesila.gopic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import auth.AuthGrpc;
import auth.AuthOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 * //todo verify for errors etc.
 * //todo may be split to different classes
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private LoginTask mLoginTask = null;

    // UI references.
    private EditText mLoginView;
    private EditText mPasswordView;
    private AutoCompleteTextView mEmailView;
    private View mProgressView;
    private View mLoginFormView;
    private Button mSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();
        mLoginView = (EditText) findViewById ( R.id.login );

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mSignInButton = (Button) findViewById( R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button mRegisterButton = (Button) findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( mEmailView.getVisibility () == View.VISIBLE ) {
                    attemptLogin ();
                } else {//todo make material animation
                    
                    int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

                    mSignInButton.setVisibility(View.GONE);
                    mSignInButton.animate()
                                 .setDuration(shortAnimTime)
                                 .alpha( 0 ).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mSignInButton.setVisibility(View.GONE);
                        }
                    });

                    mEmailView.setVisibility(View.VISIBLE);
                    mEmailView.animate()
                              .setDuration(shortAnimTime)
                              .alpha(1)
                              .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mEmailView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mLoginTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String loginName = mLoginView.getText ().toString ();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (mEmailView.isShown ()) {
            if ( TextUtils.isEmpty ( email ) ) {
                mEmailView.setError ( getString ( R.string.error_field_required ) );
                focusView = mEmailView;
                cancel = true;
            } else if ( !isEmailValid ( email ) ) {
                mEmailView.setError ( getString ( R.string.error_invalid_email ) );
                focusView = mEmailView;
                cancel = true;
            }
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mLoginTask = new LoginTask (loginName, password);
            if (mEmailView.getVisibility () == View.VISIBLE) {
                mLoginTask.register ( email );
            }
            mLoginTask.execute();
        }
    }
    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                                                                     .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }


    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    /**
     * Created by vlad on 05.07.16.//todo change
     */
    public class LoginTask extends AsyncTask<Void, Void, Boolean> {

        private ManagedChannel            channel;
        private AuthGrpc.AuthBlockingStub blockingStub;
        private String                    loginName, password, email, session;
        private List<AuthOuterClass.Error> errorList;

        public LoginTask ( String loginName, String password ) {
            channel = ManagedChannelBuilder
                    .forAddress ( "gopic.ru", 5353 )
                    .usePlaintext ( true )
                    .build ();

            blockingStub = AuthGrpc.newBlockingStub ( channel );

            this.loginName = loginName;
            this.password = password;
        }

        public LoginTask register ( String email ) {
            this.email = email;

            return this;
        }

        @Override
        protected Boolean doInBackground ( Void... voids ) {

            if ( email != null ) {
                AuthOuterClass.RegisterReq registerReq = AuthOuterClass.RegisterReq
                        .newBuilder ()
                        .setLogin ( loginName )
                        .setPassword ( password )
                        .setEmail ( email )
                        .build ();

                AuthOuterClass.RegisterRes registerRes = blockingStub.register ( registerReq );

                errorList = registerRes.getErrorList ();
                session = registerRes.getSession ();

            }else{
                AuthOuterClass.LoginReq loginReq = AuthOuterClass.LoginReq
                        .newBuilder ()
                        .setLogin ( loginName )
                        .setPassword ( password )
                        .build ();

                AuthOuterClass.LoginRes  loginRes = blockingStub.login ( loginReq );

                errorList = loginRes.getErrorList ();
                session = loginRes.getSession ();

            }

            return errorList.isEmpty ();
        }

        @Override
        protected void onPostExecute ( Boolean successful ) {
            super.onPostExecute ( successful );

            mLoginTask = null;
            showProgress( false );

            if (successful) {
                finish();
            } else {
                for ( AuthOuterClass.Error curError : errorList) 
                {
                    switch ( curError ){
                        case LOGIN_EXISTS:
                            mLoginView.setError ( getString ( R.string.error_login_exists ) );
                            mLoginView.requestFocus();
                            break;
                        case EMAIL_EXISTS:
                            mEmailView.setError ( getString ( R.string.error_email_exists ) );
                            mEmailView.requestFocus();
                            break;
                        case BAD_PASSWORD:
                            mPasswordView.setError(getString(R.string.error_incorrect_password));
                            mPasswordView.requestFocus();
                            break;
                        case BAD_LOGIN:
                            mLoginView.setError ( getString ( R.string.error_invalid_login ) );
                            mLoginView.requestFocus();
                            break;
                        case BAD_EMAIL:
                            mEmailView.setError ( getString ( R.string.error_invalid_email ) );
                            mEmailView.requestFocus();
                            break;
                        case LOGIN_IS_NOT_EXISTS:
                            mLoginView.setError ( getString ( R.string.error_login_is_not_exists ));
                            mLoginView.requestFocus();
                            break;
                        case BAD_SESSION://that shouldn't happen
                        case INTERNAL:
                        default:
                            Toast.makeText ( LoginActivity.this
                                    , "Something went wrong"
                                    , Toast.LENGTH_SHORT )
                                 .show ();
                    }
                }
            }

        }

        @Override
        protected void onCancelled() {
            mLoginTask = null;
            showProgress(false);
        }
    }

    
}

