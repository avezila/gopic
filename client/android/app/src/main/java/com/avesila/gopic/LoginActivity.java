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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import auth.AuthGrpc;
import auth.AuthOuterClass;
import auth.AuthOuterClass.LoginReq;
import auth.AuthOuterClass.LoginRes;
import auth.AuthOuterClass.RegisterReq;
import auth.AuthOuterClass.RegisterRes;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 * //todo verify for errors etc.
 * //todo SPLIT to different classes!!!!
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
    private EditText mAddressView;
    private AutoCompleteTextView mEmailView;
    private View mProgressView;
    private View mLoginFormView;
    private Button mSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        mAddressView = (EditText) findViewById ( R.id.address );
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
        
        mEmailView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                } else {//todo may be change this animation
                    
                    mPasswordView.setImeActionLabel (
                            getString ( R.string.action_register )
                            , R.id.login );
                    
                    final Animation animEmailView = AnimationUtils.makeInAnimation (
                            LoginActivity.this, true );
                    animEmailView.setDuration ( 500 );
                    animEmailView.setInterpolator( new DecelerateInterpolator () );
                
                    Animation animSignInButton = AnimationUtils.makeOutAnimation ( 
                            LoginActivity.this, true );
                    animSignInButton.setDuration ( 500 );
                    animSignInButton.setInterpolator( new AccelerateInterpolator () );
                    animSignInButton.setAnimationListener ( new Animation.AnimationListener () {

                        @Override
                        public void onAnimationStart ( Animation animation ) {
                            //nothing to do
                        }

                        @Override
                        public void onAnimationEnd ( Animation animation ) {
                            mSignInButton.setVisibility ( View.GONE );
                            mEmailView.setVisibility ( View.VISIBLE );
                            mEmailView.startAnimation ( animEmailView );
                        }

                        @Override
                        public void onAnimationRepeat ( Animation animation ) {
                            //it would not repeat
                        }
                    } );
                    mSignInButton.startAnimation ( animSignInButton );
                    
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
        String address = mAddressView.getText ().toString ();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String username = mLoginView.getText ().toString ();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if ( TextUtils.isEmpty ( password ) ) {
            mPasswordView.setError ( getString ( R.string.error_field_required ) );
            focusView = mPasswordView;
            cancel = true;
        } else if ( !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if ( mEmailView.getVisibility () == View.VISIBLE ) {
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

        if ( !address.contains ( ":" ) ){
            address = address + ":" + getString ( R.string.standard_port );
        }
        if ( TextUtils.isEmpty ( address ) ) {
            mAddressView.setError ( getString ( R.string.error_field_required ) );
            focusView = mAddressView;
            cancel = true;
        } else if ( !isAddressValid ( address ) ) {
            mAddressView.setError ( getString ( R.string.error_incorrect_address ) );
            focusView = mAddressView;
            cancel = true;
        }
        
        if ( TextUtils.isEmpty ( username ) ) {
            mLoginView.setError ( getString ( R.string.error_field_required ) );
            focusView = mLoginView;
            cancel = true;
        } else if ( !isUsernameValid ( username ) ) {
            mLoginView.setError ( getString ( R.string.error_invalid_login ) );
            focusView = mLoginView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mLoginTask = new LoginTask (address, username, password);
            if (mEmailView.getVisibility () == View.VISIBLE) {
                mLoginTask.register ( email );
            }
            mLoginTask.execute();
        }
    }
    private boolean isUsernameValid(String username) {
        //TODO: Replace this with your own logic
        return true;
    }
    
    private boolean isAddressValid( String address){
        boolean valid = true;
        String [] addressArr = TextUtils.split ( address, ":" );
        if (addressArr.length != 2) {
            valid = false;
        }else if (!addressArr[0].matches(
                "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}$" )//domain 
                  // todo make local symbols 
            && !addressArr[0].matches ( 
                "((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)" ) //ipv4
            && !addressArr[0].matches ( "((^|:)([0-9a-fA-F]{0,4})){1,8}$" )) {//ipv6
            valid = false;
        }
        return valid;
    }
    
    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.matches ( "^[-\\w.]+@([A-z0-9][-A-z0-9]+\\.)+[A-z]{2,4}$" );
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

        public LoginTask ( String address, String loginName, String password ) {
            String [] addressArr = address.split ( ":" );
            channel = ManagedChannelBuilder
                    .forAddress ( addressArr [ 0 ], Integer.valueOf ( addressArr [ 1 ] ) )
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
            try {
                
                if ( email != null ) {
                    RegisterReq registerReq = AuthOuterClass.RegisterReq
                            .newBuilder ()
                            .setLogin ( loginName )
                            .setPassword ( password )
                            .setEmail ( email )
                            .build ();

                    RegisterRes registerRes = blockingStub
                            .withDeadlineAfter ( 5, TimeUnit.SECONDS )
                            .register ( registerReq );

                    errorList = registerRes.getErrorList ();
                    session = registerRes.getSession ();

                } else {
                    
                    LoginReq loginReq = AuthOuterClass.LoginReq
                            .newBuilder ()
                            .setLogin ( loginName )
                            .setPassword ( password )
                            .build ();

                    LoginRes loginRes = blockingStub
                            .withDeadlineAfter ( 5, TimeUnit.SECONDS  )
                            .login ( loginReq );
                    Log.w( "loginRes initialized", loginRes.isInitialized () + "" );
                    errorList = loginRes.getErrorList ();
                    session = loginRes.getSession ();
                }
            } catch ( StatusRuntimeException e ) {
                
                
            }
            
            catch ( Exception e ){
                e.printStackTrace ( System.err );
                errorList = new ArrayList<> ();
                errorList.add ( AuthOuterClass.Error.INTERNAL );
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
                                    , getString ( R.string.something_went_wrong )
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

