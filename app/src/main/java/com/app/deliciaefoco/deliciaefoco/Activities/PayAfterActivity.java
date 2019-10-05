package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.app.deliciaefoco.deliciaefoco.Adapters.EmployeeGridViewAdapter;
import com.app.deliciaefoco.deliciaefoco.Interfaces.EmployeeInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.LotProductInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.Product;
import com.app.deliciaefoco.deliciaefoco.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PayAfterActivity extends AppCompatActivity {
    Context context = this;
    private int lastInteractionTime;
    List<Product> products;
    ArrayList<LotProductInterface> lots;
    JSONArray employees;
    String FILENAME = "DEFAULT_COMPANY";
    private Integer enterpriseId = 0;
    EmployeeGridViewAdapter adapter;
    GridView gv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_after);
        setTitle(R.string.title_activity_payafter);
        //startUserInactivityDetect();
        Type listType = new TypeToken<ArrayList<Product>>(){}.getType();
        products = new Gson().fromJson(getIntent().getStringExtra("CART_ITEMS"), listType);
        Type lotsType = new TypeToken<ArrayList<LotProductInterface>>(){}.getType();
        lots = new Gson().fromJson(getIntent().getStringExtra("LOTS"), lotsType);
        gv = (GridView) findViewById(R.id.gridViewClients);

        //busca o id da empresa salvo no arquivo
        SharedPreferences settings = getSharedPreferences(FILENAME, 0);
        this.enterpriseId = settings.getInt("enterprise_id", 0);
        this.getEmployees();

        EditText txtFilter = (EditText) findViewById(R.id.txtSearchEmployee);
        txtFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        Button btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EmployeeInterface employee = (EmployeeInterface) adapter.getItem(position);
                Gson gson = new Gson();
                Intent intent = new Intent(getBaseContext(), ConcludeSale.class);
                intent.putExtra("PRODUTOS", gson.toJson(products));
                intent.putExtra("EMPLOYEE", gson.toJson(employee));
                intent.putExtra("LOTS", gson.toJson(lots));
                intent.putExtra("CARTEIRA", getIntent().getIntExtra("CARTEIRA", 0));
                startActivityForResult(intent, 0);
            }
        });




    }

    private void getEmployees(){
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Carregando ...");
        progress.setMessage("Aguarde enquanto carregamos os funcionários");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();


        JsonArrayRequest jar = new JsonArrayRequest(Request.Method.GET, this.getBaseUrl() + "/enterprise/"+enterpriseId+"/employees", null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {

                    employees = response;
                    ArrayList<EmployeeInterface> array = new ArrayList<>();
                    for(int i = 0; i < employees.length(); i++) {
                        EmployeeInterface obj = new EmployeeInterface();

                        obj.id = employees.getJSONObject(i)
                                .getInt("id");

                        obj.cpf = employees.getJSONObject(i)
                               .getString("cpf");

                        obj.email = employees.getJSONObject(i)
                                .getString("email");

                        obj.nome = employees.getJSONObject(i)
                                .getString("nome");

                        obj.telefone = employees.getJSONObject(i)
                                .getString("telefone");

                        obj.endereco = employees.getJSONObject(i)
                                .getString("endereco");

                        obj.user_id = employees.getJSONObject(i)
                                .getString("user_id");

                        array.add(obj);

                    }
                    adapter = new EmployeeGridViewAdapter(array, context);
                    gv.setAdapter(adapter);
                    progress.dismiss();

                } catch (JSONException e) {
                    Log.d("DeliciaEFoco", e.getMessage());
                    progress.dismiss();
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                Log.d("DeliciaEFoco", error.getMessage());
                progress.dismiss();
            }
        });

        requestQueue.add(jar);

    }

    private String getBaseUrl(){
        SharedPreferences settings = getSharedPreferences(FILENAME, 0);
        return settings.getString("base_url", "");
    }
}
