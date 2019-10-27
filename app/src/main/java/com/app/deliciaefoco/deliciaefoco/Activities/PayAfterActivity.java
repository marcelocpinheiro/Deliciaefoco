package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.app.deliciaefoco.deliciaefoco.Adapters.EmployeeGridViewAdapter;
import com.app.deliciaefoco.deliciaefoco.Interfaces.EmployeeInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.LotProductInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.Product;
import com.app.deliciaefoco.deliciaefoco.Providers.ApiProvider;
import com.app.deliciaefoco.deliciaefoco.Providers.DialogProvider;
import com.app.deliciaefoco.deliciaefoco.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    ApiProvider api;
    ProgressDialog progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = new ApiProvider(this);
        setContentView(R.layout.activity_pay_after);
        setTitle(R.string.title_activity_payafter);
        //startUserInactivityDetect();
        Type listType = new TypeToken<ArrayList<Product>>(){}.getType();
        products = new Gson().fromJson(getIntent().getStringExtra("CART_ITEMS"), listType);
        Type lotsType = new TypeToken<ArrayList<LotProductInterface>>(){}.getType();
        lots = new Gson().fromJson(getIntent().getStringExtra("LOTS"), lotsType);
        gv = (GridView) findViewById(R.id.gridViewClients);
        progress  = new ProgressDialog(context);

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

        Button btnGuest = (Button) findViewById(R.id.btnGuest);
        btnGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Gson gson = new Gson();
                Intent intent = new Intent(getBaseContext(), ConcludeSale.class);
                intent.putExtra("PRODUTOS", gson.toJson(products));
                intent.putExtra("LOTS", gson.toJson(lots));
                intent.putExtra("GUEST", true);
                startActivityForResult(intent, 0);
            }
        });

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final EmployeeInterface employee = (EmployeeInterface) adapter.getItem(position);
                final DialogProvider dp = new DialogProvider(context);
                dp.promptPasswordDialogShow("Por favor " + employee.nome + ", " +
                        "Digite sua senha",
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progress.setTitle("Aguarde ...");
                        progress.setMessage("Verificando credenciais ...");
                        progress.show();

                        String password = dp.getInputValue();
                        try{
                            api.authenticateUser(employee.email, password, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try{
                                        if(response.getString("status").contains("success")){
                                            progress.dismiss();
                                            Gson gson = new Gson();
                                            Intent intent = new Intent(getBaseContext(), ConcludeSale.class);
                                            intent.putExtra("ID_USUARIO", response.getInt("user_id"));
                                            intent.putExtra("PRODUTOS", gson.toJson(products));
                                            intent.putExtra("EMPLOYEE", gson.toJson(employee));
                                            intent.putExtra("LOTS", gson.toJson(lots));
                                            intent.putExtra("CARTEIRA", getIntent().getIntExtra("CARTEIRA", 0));
                                            startActivityForResult(intent, 0);
                                        }else{
                                            progress.dismiss();
                                            dp.dialogShow("Credenciais inválidas", "Por favor, tente novamente", null);
                                        }
                                    }catch(JSONException e){
                                        dp.dialogShow("Falha ao buscar usuário", "Por favor, tente novamente", null);
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    dp.dialogShow("Falha ao buscar usuário", "Por favor, tente novamente", null);
                                }
                            });
                        }catch(JSONException e){
                            dp.dialogShow("Falha ao buscar usuário", "Por favor, tente novamente", null);
                        }
                    }
                });
            }
        });
    }

    private void getEmployees(){
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Carregando ...");
        progress.setMessage("Aguarde enquanto carregamos os funcionários");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();


        api.getEmployees(enterpriseId, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    distributeEmployees(response);
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
    }

    private void distributeEmployees(JSONArray employees) throws JSONException{
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
    }

    private String getBaseUrl(){
        SharedPreferences settings = getSharedPreferences(FILENAME, 0);
        return settings.getString("base_url", "");
    }
}
