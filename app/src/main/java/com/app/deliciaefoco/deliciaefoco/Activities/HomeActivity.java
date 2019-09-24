package com.app.deliciaefoco.deliciaefoco.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.app.deliciaefoco.deliciaefoco.R;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class HomeActivity extends AppCompatActivity {

    private final String baseUrl = "http://portal.deliciaefoco.com.br/api/enterprise/";
    String FILENAME = "DEFAULT_COMPANY";
    Context context = this;
    ImageView imageView;

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


        setContentView(R.layout.activity_home);

        imageView = (ImageView) findViewById(R.id.home_image);
        try {
            Picasso.get().load(baseUrl + this.getEnterpriseId() + "/image").into(imageView);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Button btnBuy = (Button) findViewById(R.id.btnBuy);
        btnBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent inte = new Intent(context, StoreActivity.class);
                startActivityForResult(inte, 0);
            }
        });

        Button btnPagar = (Button) findViewById(R.id.btnPagarCarrinhos);
        btnPagar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent inte = new Intent(context, SelectEmployeeActivity.class);
                inte.putExtra("action", "pay");
                startActivityForResult(inte, 0);
            }
        });

        Button btnProblem = (Button) findViewById(R.id.btnProblems);
        btnProblem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent inte = new Intent(context, ProblemActivity.class);
                startActivityForResult(inte, 0);
            }
        });

        Button btnCadastrar = (Button) findViewById(R.id.btnCadastrar);
        btnCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent inte = new Intent(context, SignUpActivity.class);
                startActivityForResult(inte, 0);
            }
        });
    }

    @Override
    protected void onResume (){
        super.onResume();
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
        try {
            Picasso.get().load(baseUrl + this.getEnterpriseId() + "/image").into(imageView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private int getEnterpriseId() throws IOException {
        SharedPreferences settings = getSharedPreferences(FILENAME, 0);
        return settings.getInt("enterprise_id", 0);
    }
}
