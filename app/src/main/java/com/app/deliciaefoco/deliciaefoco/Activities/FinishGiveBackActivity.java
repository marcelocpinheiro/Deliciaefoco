package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ConcludeInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ProductInterface;
import com.app.deliciaefoco.deliciaefoco.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FinishGiveBackActivity extends AppCompatActivity {
    int employee_id, saleOrderId, quantity = 1;
    String employee_mail;
    ProductInterface product;
    Context context;
    private final String baseUrl = "http://portal.deliciaefoco.com.br/api/product/image/";
    TextView txtProdData;
    EditText txtPassword;
    private final String baseUrlPasswd = "http://portal.deliciaefoco.com.br/api";
    private AlertDialog alerta;
    ProgressDialog progress;
    HashMap<String, String> hmLang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Status", "Iniciando Finishgivebackactivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_give_back);
        context = this;
        progress  = new ProgressDialog(context);
        employee_id = getIntent().getIntExtra("EMPLOYEE_ID", 0);
        saleOrderId = getIntent().getIntExtra("SALEORDER_ID", 0);
        employee_mail = getIntent().getStringExtra("EMPLOYEE_MAIL");
        Type productType = new TypeToken<ProductInterface>(){}.getType();
        product = new Gson().fromJson(getIntent().getStringExtra("PRODUCT"), productType);

        TextView txtProdName = (TextView) findViewById(R.id.txtProdName);
        txtProdData = (TextView) findViewById(R.id.txtProdData);
        ImageView imgProd = (ImageView) findViewById(R.id.imgProd);
        txtPassword = (EditText) findViewById(R.id.txtPasswordGiveBack);
        txtProdName.setText(product.name);
        txtProdData.setText("Devolver " + quantity + " Unidade(s)");

        Spinner spnMotivos = (Spinner) findViewById(R.id.spinerGiveBack);
        List<String> spinnerArray =  new ArrayList<String>();

        spinnerArray.add("Produto vencido");
        spinnerArray.add("Produto violado");
        spinnerArray.add("Produto em más condições");
        spinnerArray.add("Não é o produto que eu queria");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnMotivos.setAdapter(adapter);


        Picasso.get().load(baseUrl + product.id).into(imgProd);

        Button btnPlus = (Button) findViewById(R.id.buttonPlus);
        Button btnMinus = (Button) findViewById(R.id.buttonMinus);
        Button btnFinish = (Button) findViewById(R.id.btnFinish);
        Button btnGoBack = (Button) findViewById(R.id.btnGoBack);

        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder
                        .setMessage("Deseja realmente cancelar esta devolução?")
                        .setPositiveButton("Sim",  new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(context, HomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                context.startActivity(intent);
                                Runtime.getRuntime().exit(0);
                            }
                        })
                        .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        })
                        .show();
            }
        });

        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(quantity < product.quantity){
                    quantity++;
                }
                txtProdData.setText("Devolver " + quantity + " Unidade(s)");
            }
        });

        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(quantity > 1){
                    quantity--;
                }
                txtProdData.setText("Devolver " + quantity + " Unidade(s)");
            }
        });

        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final RequestQueue requestQueue = Volley.newRequestQueue(context);

                progress.setTitle("Aguarde ...");
                progress.setMessage("Verificando credenciais ...");
                progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                progress.show();

                try {
                    final JSONObject jsonBody = new JSONObject("{\"email\":\""+employee_mail+"\", \"password\":\""+txtPassword.getText()+"\"}");
                    JsonObjectRequest jar = new JsonObjectRequest(Request.Method.POST, baseUrlPasswd + "/checkuser", jsonBody, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if(response.getString("status").contains("success")){
                                    progress.dismiss();
                                    progress.setTitle("Aguarde ...");
                                    progress.setMessage("Efetuando a devolução ...");
                                    progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                                    progress.show();
                                    giveBackCall();
                                }else{
                                    dialogShow("Acesso Negado", "Falha");
                                }
                            } catch (JSONException e) {
                                dialogShow("Falha ao processar pedido. Por favor, Informe este problema ao RH: " + e.getMessage(), "Falha");
                                Log.d("Falha", e.getMessage());
                                progress.dismiss();
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
                    e.printStackTrace();
                }


            }
        });


    }

    private void giveBackCall(){
        final RequestQueue requestQueue = Volley.newRequestQueue(context);
        final JSONObject jsonBodyGiveBack;
        try {
            jsonBodyGiveBack = new JSONObject("{\"sale_order_id\":\""+saleOrderId+"\", \"product_menu_id\":\""+product.product_menu_id+"\", \"quantity\":"+quantity+", \"price\": "+product.price+"}");
            JsonObjectRequest jarGiveBack = new JsonObjectRequest(Request.Method.POST, baseUrlPasswd  + "/giveBack", jsonBodyGiveBack, new Response.Listener<JSONObject>(){
                @Override
                public void onResponse(JSONObject res){
                    progress.dismiss();
                    try {
                        if(res.getString("status").contains("success")){
                            dialogShow("Devolução realizada com sucesso. Por favor, insira o produto na urna para avaliação.", "Sucesso");
                        }else{
                            dialogShow("Falha ao devolver produto. Por favor, insira manualmente a sua devolução.", "Falha");
                        }
                    } catch (JSONException e) {
                        dialogShow("Falha ao devolver produto. Por favor, insira manualmente a sua devolução.", "Falha");
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener(){
                @Override
                public void onErrorResponse(VolleyError error){
                    Log.d("DeliciaEFoco", "Falha ao devolver produto");
                    dialogShow("Falha ao devolver produto. Por favor, insira manualmente a sua devolução.", "Falha");
                }
            });

            requestQueue.add(jarGiveBack);
        } catch (JSONException e) {
            e.printStackTrace();
            dialogShow("Falha ao devolver produto. Por favor, insira manualmente a sua devolução.", "Falha");
        }
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
}
