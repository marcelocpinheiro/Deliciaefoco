package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.app.deliciaefoco.deliciaefoco.Providers.UtilitiesProvider;
import com.app.deliciaefoco.deliciaefoco.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;

public class SignUpActivity extends AppCompatActivity {
    private WebView webView;
    String FILENAME = "DEFAULT_COMPANY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Bem vindo à Delícia e Foco");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
        });

        setContentView(R.layout.activity_sign_up);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);



        webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(false);
        try {
            webView.loadUrl("http://portal.deliciaefoco.com.br/cadastro/" + getEnterpriseId());
        } catch (IOException e) {
            UtilitiesProvider.trackException(e);
            e.printStackTrace();
        }
        WebViewClient wvc = new WebViewClient();
        webView.setWebViewClient(wvc);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }

    private int getEnterpriseId() throws IOException {
        SharedPreferences settings = getSharedPreferences(FILENAME, 0);
        return settings.getInt("enterprise_id", 0);
    }

    @Override
    protected void onResume (){
        super.onResume();
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
    }
}
