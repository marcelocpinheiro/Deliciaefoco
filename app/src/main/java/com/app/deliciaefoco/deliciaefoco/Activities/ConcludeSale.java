package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.deliciaefoco.deliciaefoco.Activities.StoreActivity;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ConcludeInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.EmployeeInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.LotProductInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.Product;
import com.app.deliciaefoco.deliciaefoco.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConcludeSale extends AppCompatActivity {
    EmployeeInterface selectedEmployee;
    List<Product> produtos;
    Context context = this;
    private final String baseUrl = "http://portal.deliciaefoco.com.br/api";
    private AlertDialog alerta;
    ArrayList<LotProductInterface> lots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conclude_sale);
        setTitle("Concluir Compra - Delícia e Foco");

        Type productsType = new TypeToken<List<Product>>(){}.getType();
        final Type employee = new TypeToken<EmployeeInterface>(){}.getType();
        Type lotsType = new TypeToken<ArrayList<LotProductInterface>>(){}.getType();

        produtos = new Gson().fromJson(getIntent().getStringExtra("PRODUTOS"), productsType);
        selectedEmployee = new Gson().fromJson(getIntent().getStringExtra("EMPLOYEE"), employee);
        lots = new Gson().fromJson(getIntent().getStringExtra("LOTS"), lotsType);

        TextView txtNameEmployee = (TextView) findViewById(R.id.txtNameEmployee);
        txtNameEmployee.setText(selectedEmployee.nome);

        TextView valueTxt = (TextView) findViewById(R.id.txtValue);
        valueTxt.setText("Será adicionada uma compra no valor de "+this.getTotalValue()+" em sua conta, que deverá ser paga em até 7 dias.");

        Button btnCancelar = (Button) findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final EditText passwordText = (EditText) findViewById(R.id.passwordField) ;


        Button btnFinalizar = (Button) findViewById(R.id.btnFinalizarCompra);
        btnFinalizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final RequestQueue requestQueue = Volley.newRequestQueue(context);
                final ProgressDialog progress = new ProgressDialog(context);
                progress.setTitle("Aguarde ...");
                progress.setMessage("Verificando credenciais ...");
                progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                progress.show();

                try {
                    final JSONObject jsonBody = new JSONObject("{\"email\":\""+selectedEmployee.email+"\", \"password\":\""+passwordText.getText()+"\"}");
                    JsonObjectRequest jar = new JsonObjectRequest(Request.Method.POST, baseUrl + "/checkuser", jsonBody, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if(response.getString("status").contains("success")){
                                    progress.dismiss();
                                    progress.setTitle("Aguarde ...");
                                    progress.setMessage("Concluindo Compra ...");
                                    progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                                    progress.show();

                                    ArrayList<ConcludeInterface> arrayConclude = new ArrayList<>();
                                    for (int i = 0; i < produtos.size(); i++){
                                        ConcludeInterface ci = new ConcludeInterface();
                                        ci.price = produtos.get(i).getPrice();
                                        ci.quantity = produtos.get(i).getQuantity();
                                        for(int n = 0; n < lots.size(); n++){
                                            if(lots.get(n).product.id == produtos.get(i).product_id){
                                                ci.id = lots.get(n).id;
                                                break;
                                            }
                                        }

                                        Log.d("Id ci", ci.id+"");

                                        arrayConclude.add(ci);
                                    }

                                    Log.d("Debugando", arrayConclude.toString());


                                    int user_id = response.getInt("user_id");
                                    Gson gson = new Gson();
                                    final JSONObject compraRequestBody = new JSONObject("{\"user_id\":\""+user_id+"\", \"products\":"+gson.toJson(arrayConclude)+"}");
                                    JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, baseUrl + "/saveSale", compraRequestBody, new Response.Listener<JSONObject>(){
                                        @Override
                                        public void onResponse(JSONObject response) {
                                            progress.dismiss();
                                            try {
                                                if(response.getString("status").contains("success")){
                                                    dialogShow("Compra concluída. Obrigado por comprar conosco, "+selectedEmployee.nome+"!", "Sucesso");
                                                }else{
                                                    dialogShow("Falha!", response.getString("message"));
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }, new Response.ErrorListener(){
                                        @Override
                                        public void onErrorResponse(VolleyError error){
                                            Log.d("DeliciaEFoco", "Falha ao concluir compra");
                                            dialogShow(new String(error.networkResponse.data, StandardCharsets.UTF_8), "Falha!");
                                            progress.dismiss();
                                        }
                                    });

                                    requestQueue.add(jor);
                                }else{
                                    dialogShow(new Gson().toJson(response), "Falha Fatal");
                                    progress.dismiss();
                                }
                            } catch (JSONException e) {
                                dialogShow("Falha ao processar pedido. Por favor, Informe este problema ao RH" + e.getMessage(), "Falha");
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

    private String getTotalValue(){
        double ret = 0.0;
        for(final Product product: produtos){
            ret += product.calculateTotalValue();
        }
        return this.formatMoney(ret);
    }

    private String formatMoney(double value){
        BigDecimal valor = new BigDecimal (value);
        Locale ptBr = new Locale("pt", "BR");
        NumberFormat nf = NumberFormat.getCurrencyInstance(ptBr);
        String formatado = nf.format (valor);
        return formatado;
    }

}
