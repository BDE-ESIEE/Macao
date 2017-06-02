package fr.esiee.bde.macao;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;
import fr.esiee.bde.macao.Fragments.AnnalesFragment;
import fr.esiee.bde.macao.Fragments.CalendarFragment;
import fr.esiee.bde.macao.Fragments.EventsFragment;
import fr.esiee.bde.macao.Fragments.RoomsFragment;
import fr.esiee.bde.macao.Fragments.SignInFragment;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, GoogleApiClient.OnConnectionFailedListener, CalendarFragment.OnFragmentInteractionListener, SignInFragment.OnFragmentInteractionListener, RoomsFragment.OnFragmentInteractionListener, EventsFragment.OnFragmentInteractionListener, AnnalesFragment.OnFragmentInteractionListener{

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private GoogleApiClient mGoogleApiClient;
    private ProgressDialog mProgressDialog;

    private boolean isSignedIn = false;

    private String username = "";
    private String firstname = "";
    private String lastname = "";
    private String mail = "";
    private String idToken = "";
    private String id = "";
    private String authCode = "";

    TextView nameDrawer;
    TextView mailDrawer;
    ImageView pictureDrawer;
    ImageView backgroundDrawer;

    private View mainView;

    private Fragment currentFragment = null;

    private Bundle savedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        nameDrawer = (TextView) headerView.findViewById(R.id.nameDrawer);
        mailDrawer = (TextView) headerView.findViewById(R.id.mailDrawer);
        pictureDrawer = (ImageView) headerView.findViewById(R.id.imageDrawer);
        backgroundDrawer = (ImageView) headerView.findViewById(R.id.backgroundDrawer);

        navigationView.setNavigationItemSelectedListener(this);

        mainView = findViewById(R.id.content_main);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                //.requestIdToken(getString(R.string.annales_client_id))
                //.requestServerAuthCode("557464199167-4lbgvd3o6c6qjtqitqf1h8vkl9017csl.apps.googleusercontent.com", false)
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        this.savedInstanceState = savedInstanceState;
    }


    @Override
    public void onStart() {
        super.onStart();

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }
        if (savedInstanceState == null) {
            // The Activity is NOT being re-created so we can instantiate a new Fragment
            // and add it to the Activity
            Fragment fragment = new CalendarFragment();

            switchFragment(fragment);

        } else {
            // The Activity IS being re-created so we don't need to instantiate the Fragment or add it,
            // but if we need a reference to it, we can use the tag we passed to .replace
            Fragment fragment = (Fragment) getSupportFragmentManager().findFragmentByTag("FragmentSaved");
            switchFragment(fragment);
        }
        //switchFragment(((Fragment) new CalendarFragment()));
    }

    @Override
    public void onStop(){
        super.onStop();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        hideProgressDialog();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = null;

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
            fragment = new CalendarFragment();
        } else if (id == R.id.nav_gallery) {
            fragment = new SignInFragment();

        } else if (id == R.id.nav_slideshow) {
            fragment = new RoomsFragment();

        } else if (id == R.id.nav_manage) {
            fragment = new EventsFragment();

        } else if (id == R.id.nav_share) {
            fragment = new AnnalesFragment();
        } else if (id == R.id.nav_send) {

        }

        switchFragment(fragment);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void switchFragment(Fragment fragment){
        currentFragment = fragment;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.content_main, fragment, "FragmentSaved");
        transaction.commit();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            default:
                break;
        }
    }

    public void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        nameDrawer.setText("Macao");
                        mailDrawer.setText("");
                        updateUI(false);
                        // [END_EXCLUDE]
                    }
                });
    }

    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        updateUI(false);
                        // [END_EXCLUDE]
                    }
                });
    }

    public void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            //mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getEmail()));

            String email = acct.getEmail();
            //Log.d("MAIN", acct.getServerAuthCode());
            //String token = acct.getIdToken();
            //Log.d("MAIN", token);
            this.idToken = acct.getIdToken();
            this.id = acct.getId();
            //this.authCode = acct.getServerAuthCode();

            AccountManager am = AccountManager.get(this);
            Bundle options = new Bundle();

            am.getAuthToken(
                    acct.getAccount(),                     // Account retrieved using getAccountsByType()
                    "Manage your tasks",            // Auth scope
                    options,                        // Authenticator-specific options
                    this,                           // Your activity
                    new OnTokenAcquired(),          // Callback called when a token is successfully acquired
                    null);    // Callback called if an error occurs


            if (email.substring(email.indexOf("@")).equals("@edu.esiee.fr")) {
                String firstname = email.substring(0, email.indexOf("."));
                String lastname = email.substring(email.indexOf(".") + 1, email.indexOf("@"));
                String username;
                if (lastname.length() >= 7) {
                    username = lastname.substring(0, 7) + firstname.substring(0, 1);
                } else {
                    username = lastname + firstname.substring(0, 1);
                }
                this.username = username;
                this.firstname = firstname;
                this.lastname = lastname;
                this.mail = email;
                this.isSignedIn = true;

                this.nameDrawer.setText(username);
                this.mailDrawer.setText(mail);
                Uri uri = acct.getPhotoUrl();
                String pictureUrl = null;
                if (uri != null) {
                    pictureUrl = uri.toString();
                }
                if (pictureUrl != null) {
                    Picasso.with(this).load(pictureUrl).into(pictureDrawer);
                } else {
                    pictureDrawer.setImageResource(R.mipmap.ic_launcher);
                }

                //mStatusTextView.setText(username);

                updateUI(true);
            } else {
                signOut();
                makeSnackBar("Veuillez vous connecter avec un compte ESIEE");
            }
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            // Get the result of the operation from the AccountManagerFuture.
            Bundle bundle = null;
            try {
                bundle = result.getResult();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            }

            // The token is a named value in the bundle. The name of the value
            // is stored in the constant AccountManager.KEY_AUTHTOKEN.
            idToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            Log.d("IDDDD", idToken);
        }
    }

    public String getId() {
        return id;
    }

    public String getAuthCode() {
        return authCode;
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    private void updateUI(boolean signedIn) {
        this.isSignedIn = signedIn;

        if(currentFragment instanceof SignInFragment) {
            ((SignInFragment) currentFragment).connectUser(signedIn);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void makeSnackBar(String text){
        Snackbar snackbar = Snackbar
                .make(mainView, text, Snackbar.LENGTH_LONG);

        if(Objects.equals(text, "Connectez vous d'abord")){
            snackbar.setAction("Ici", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bde.esiee.fr/aurion/agenda"));
                    startActivity(browserIntent);
                }
            });
        }

        snackbar.show();
    }

    public boolean isSignedIn() {
        return isSignedIn;
    }

    public void setSignedIn(boolean signedIn) {
        isSignedIn = signedIn;
    }

    public String getUsername() {
        return username;
    }

    public String getIdToken() {
        return idToken;
    }

}
