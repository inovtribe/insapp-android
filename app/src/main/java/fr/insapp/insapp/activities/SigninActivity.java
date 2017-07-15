package fr.insapp.insapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import fr.insapp.insapp.R;
import fr.insapp.insapp.http.ServiceGenerator;
import fr.insapp.insapp.models.credentials.LoginCredentials;
import fr.insapp.insapp.models.credentials.SessionCredentials;
import fr.insapp.insapp.models.credentials.SigninCredentials;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SigninActivity extends AppCompatActivity {

    private final String CAS_URL = "https://cas.insa-rennes.fr/cas/login?service=https://insapp.fr/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_sign_in);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeSessionCookie();

        final WebView webView = (WebView) findViewById(R.id.webview_conditions);

        webView.loadUrl(CAS_URL);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                final int id = url.lastIndexOf("?ticket=");
                if (url.contains("?ticket=")) {
                    final String ticket = url.substring(id + "?ticket=".length(), url.length());

                    System.out.println("URL: " + url);
                    System.out.println("Ticket: " + ticket);

                    signin(ticket);
                    webView.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void signin(final String ticket) {
        SigninCredentials signinCredentials = new SigninCredentials(Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID));

        Call<LoginCredentials> call = ServiceGenerator.create().signUser(ticket, signinCredentials);
        call.enqueue(new Callback<LoginCredentials>() {
            @Override
            public void onResponse(Call<LoginCredentials> call, Response<LoginCredentials> response) {
                if (response.isSuccessful()) {
                    login(response.body());
                }
                else {
                    Toast.makeText(SigninActivity.this, "SigninActivity", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginCredentials> call, Throwable t) {
                Toast.makeText(SigninActivity.this, "SigninActivity", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void login(LoginCredentials loginCredentials) {
        Call<SessionCredentials> call = ServiceGenerator.create().logUser(loginCredentials);
        call.enqueue(new Callback<SessionCredentials>() {
            @Override
            public void onResponse(Call<SessionCredentials> call, Response<SessionCredentials> response) {
                if (response.isSuccessful()) {
                    startActivity(new Intent(SigninActivity.this, MainActivity.class));
                    finish();
                }
                else {
                    Toast.makeText(SigninActivity.this, "SigninActivity", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SessionCredentials> call, Throwable t) {
                Toast.makeText(SigninActivity.this, "SigninActivity", Toast.LENGTH_LONG).show();
            }
        });
    }
}