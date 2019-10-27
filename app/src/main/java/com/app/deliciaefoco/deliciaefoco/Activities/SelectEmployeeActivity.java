package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.deliciaefoco.deliciaefoco.Adapters.EmployeeGridViewAdapter;
import com.app.deliciaefoco.deliciaefoco.Interfaces.EmployeeInterface;
import com.app.deliciaefoco.deliciaefoco.Providers.UtilitiesProvider;
import com.app.deliciaefoco.deliciaefoco.R;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by marcelo on 09/07/18.
 */

public class SelectEmployeeActivity extends AppCompatActivity {
    JSONArray employees;
    String FILENAME = "DEFAULT_COMPANY";
    private Integer enterpriseId = 0;
    EmployeeGridViewAdapter adapter;
    GridView gv;
    Context context = this;
    private int lastInteractionTime;
    EmployeeInterface employee;
    Intent intent;
    ProgressDialog progress;
    private AlertDialog alerta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_after);
        setTitle(R.string.title_activity_payafter);
        gv = (GridView) findViewById(R.id.gridViewClients);
        progress  = new ProgressDialog(context);

        Button btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

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

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                employee = (EmployeeInterface) adapter.getItem(position);
                Gson gson = new Gson();



                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Por favor, "+employee.nome+", insira a sua senha: ");

                // Set up the input
                final EditText input = new EditText(context);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final JSONObject jsonBody;
                        try {
                            final RequestQueue requestQueue = Volley.newRequestQueue(context);

                            progress.setTitle("Aguarde ...");
                            progress.setMessage("Verificando credenciais ...");
                            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                            progress.show();

                            jsonBody = new JSONObject("{\"email\":\""+employee.email+"\", \"password\":\""+input.getText().toString()+"\"}");
                            JsonObjectRequest jar = new JsonObjectRequest(Request.Method.POST, getBaseUrl()+ "/checkuser", jsonBody, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        if(response.getString("status").contains("success")){
                                            progress.dismiss();
                                            if(getIntent().getStringExtra("action").equals("pay")){
                                                intent = new Intent(getBaseContext(), GiveBackActivity.class);
                                                intent.putExtra("EMPLOYEE_ID", employee.id);
                                                intent.putExtra("EMPLOYEE_NAME", employee.nome);
                                                intent.putExtra("EMPLOYEE_MAIL", employee.email);
                                                intent.putExtra("action", getIntent().getStringExtra("action"));
                                                startActivityForResult(intent, 0);
                                            }else if(getIntent().getStringExtra("action").equals("giveback")){
                                                intent = new Intent(getBaseContext(), GiveBackActivity.class);
                                                intent.putExtra("EMPLOYEE_ID", employee.id);
                                                intent.putExtra("EMPLOYEE_NAME", employee.nome);
                                                intent.putExtra("EMPLOYEE_MAIL", employee.email);
                                                intent.putExtra("action", getIntent().getStringExtra("action"));
                                                startActivityForResult(intent, 0);
                                            }
                                        }else{
                                            progress.dismiss();
                                            dialogShow("Acesso Negado", "Falha");
                                        }
                                    } catch (JSONException e) {
                                        UtilitiesProvider.trackException(e);
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener(){
                                @Override
                                public void onErrorResponse(VolleyError error){
                                    Log.d("DeliciaEFoco", "Falha ao buscar empresas");
                                    progress.dismiss();
                                }
                            });
                            requestQueue.add(jar);
                        } catch (JSONException e) {
                            UtilitiesProvider.trackException(e);
                            e.printStackTrace();
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

    }

    private void dialogShow(String text, String title) {
        //Cria o gerador do AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //define o titulo
        builder.setTitle(title);
        //define a mensagem
        builder.setMessage(text);
        //define um botão como positivo
        if(title == "Sucesso"){
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    Intent intent = new Intent(context, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                    Runtime.getRuntime().exit(0);
                }
            });
        }else{
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {

                }
            });
        }

        //cria o AlertDialog
        alerta = builder.create();
        //Exibe
        alerta.show();
    }

    private void getEmployees(){
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Carregando ...");
        progress.setMessage("Aguarde enquanto carregamos os funcionários");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();


        JsonArrayRequest jar = new JsonArrayRequest(Request.Method.GET, getBaseUrl() + "/enterprise/"+enterpriseId+"/employees", null, new Response.Listener<JSONArray>() {
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
                    UtilitiesProvider.trackException(e);
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                Log.d("DeliciaEFoco", "Falha ao buscar empresas");
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
