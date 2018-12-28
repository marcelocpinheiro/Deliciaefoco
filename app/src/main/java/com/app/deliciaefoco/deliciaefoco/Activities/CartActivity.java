package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.lang.reflect.Type;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.deliciaefoco.deliciaefoco.Adapters.CartAdapter;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ConcludeInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.LotProductInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.Product;
import com.app.deliciaefoco.deliciaefoco.R;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.uol.pagseguro.client.plugpag.PlugPag;

public class CartActivity extends AppCompatActivity {
    List<Product>  products;
    ArrayList<LotProductInterface> lots;
    Context context = this;
    private AlertDialog alerta;
    Dialog dialog;
    ProgressDialog progress;
    String cardBrand = null, date = null, time = null;
    private int lastInteractionTime;
    double currentValue;
    private final String baseUrl = "http://portal.deliciaefoco.com.br/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        setContentView(R.layout.activity_cart);
        setTitle("Carrinho - Delícia e foco");
        startUserInactivityDetect();

        progress = new ProgressDialog(context);
        progress.setTitle("Aguardando pagamento...");
        progress.setMessage("Efetue o pagamento na maquina de cartões. Caso esta compra não seja sua, cancele o pagamento na máquina de cartões.");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog

        //startUserInactivityDetect();
        ListView lst = (ListView) findViewById(R.id.listCart);
        Type listType = new TypeToken<ArrayList<Product>>(){}.getType();
        Type lotsType = new TypeToken<ArrayList<LotProductInterface>>(){}.getType();
        products = new Gson().fromJson(getIntent().getStringExtra("CART_ITEMS"), listType);
        lots = new Gson().fromJson(getIntent().getStringExtra("LOTS"), lotsType);
        final TextView txtTotalValue = (TextView) findViewById(R.id.txtTotalValue);
        txtTotalValue.setText(getTotalValue());
        final CartAdapter ca = new CartAdapter(products, this);

        ca.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                currentValue = ca.getTotalPrice();
                products = ca.getProducts();
                txtTotalValue.setText(formatMoney(currentValue));
            }
        });

        lst.setAdapter(ca);


        Button btnCancelCart = (Button) findViewById(R.id.btnCancelCart);
        btnCancelCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder
                        .setMessage("Deseja realmente cancelar esta compra?")
                        .setPositiveButton("Sim",  new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                products.clear();
                                lots.clear();

                                Intent intent = new Intent(context, HomeActivity.class);
                                Gson gson = new Gson();
                                startActivityForResult(intent, 0);
                                finish();
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


        Button btnKeepBuying = (Button) findViewById(R.id.btnBack);
        btnKeepBuying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //RETORNA OS PRODUTOS DO CARRINHO ATUALIZADOS
                Intent returnIntent = new Intent();
                Gson gson = new Gson();
                returnIntent.putExtra("result",gson.toJson(products));
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
            }
        });

        Button btnPayAfter = (Button) findViewById(R.id.btnPayAfter);
        btnPayAfter.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(currentValue > 0) {
                    Gson gson = new Gson();
                    Intent intent = new Intent(getBaseContext(), PayAfterActivity.class);
                    intent.putExtra("CART_ITEMS", gson.toJson(products));
                    intent.putExtra("LOTS", gson.toJson(lots));

                    startActivityForResult(intent, 0);
                }else{
                    dialogShow("Compra sem valor. Por favor, insira algum produto.", "Atenção");
                }
            }
        });

        Button btnPayNow = (Button) findViewById(R.id.btnPaynow);
        btnPayNow.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                if(currentValue > 0){
                    dialog = new Dialog(context);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.payment_dialog);
                    dialog.show();
                    Button btnDebit = (Button) dialog.findViewById(R.id.btnDebit);
                    Button btnCredit = (Button) dialog.findViewById(R.id.btnCredit);
                    Button btnVoucher = (Button) dialog.findViewById(R.id.btnVoucher);

                    btnDebit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            payNow(PlugPag.DEBIT);
                        }
                    });


                    btnCredit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            payNow(PlugPag.CREDIT);
                        }
                    });


                    btnVoucher.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View v){
                            dialog.dismiss();
                            payNow(3); //VOUCHER
                        }
                    });
                }else{
                    dialogShow("Compra sem valor. Por favor, insira algum produto.", "Atenção");
                }
            }
        });
    }

    public void startUserInactivityDetect(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(10000); // checks every 15sec for inactivity
                        setLastInteractionTime(getLastInteractionTime() + 10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(getLastInteractionTime() >= 60000) //SE O USUARIO NÃO MEXE A 1 minuto, REINICIA O APLICATIVO
                    {
                        Intent intent = new Intent(context, HomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intent);
                        Runtime.getRuntime().exit(0);
                    }
                }
            }
        }).start();
    }

    @Override
    public void onUserInteraction() {
        // TODO Auto-generated method stub
        super.onUserInteraction();
        setLastInteractionTime(0);
    }

    public int getLastInteractionTime() {
        return lastInteractionTime;
    }

    public void setLastInteractionTime(int lastInteractionTime) {
        this.lastInteractionTime = lastInteractionTime;
    }

    private void payNow(int payment){
        progress.show();
        final int paymentMethod = payment;


        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int result = msg.getData().getInt("what");
                progress.dismiss();
                try {
                    handlePaymentResult(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                int installmentType = PlugPag.A_VISTA;
                int installment = 1;
                int method = paymentMethod;
                String codigoVenda = "CODIGVENDA";
                PlugPag plugPag = new PlugPag();
                plugPag.InitBTConnection();
                plugPag.SetVersionName("DelíciaeFoco", "R001");

                int ret = plugPag.SimplePaymentTransaction(
                        method,
                        installmentType,
                        installment,
                        pagSeguroValue(),
                        codigoVenda);

                setLastTransactionValues(plugPag.getDate(), plugPag.getTime(), plugPag.getCardBrand());
                handler.sendEmptyMessage(ret);
            }
        }).start();

    }

    private void setLastTransactionValues(String date, String time, String cardBrand){
        this.date = date;
        this.time = time;
        this.cardBrand = cardBrand;
    }

    private void handlePaymentResult(int paymentResult) throws JSONException {
        String message = "";
        String title = "";
        Log.d("Mensagem de retorno", paymentResult + "");
        switch (paymentResult){
            case 0:
                if(cardBrand != null){
                    message = "Compra concluída. Muito obrigado por comprar conosco!";
                    title = "Sucesso";
                    concludeSale();
                }else{
                    message = "Houve um erro. Por favor, tente novamente!";
                    title = "Falha";
                }
                break;

            case -1003:
                message = "Houve um erro. Por favor, tente novamente!";
                title = "Falha";
                break;

            case -1004:
                message = "Transação Negada";
                title = "Falha";
                break;

            case -1005:
                message = "Houve um erro. Por favor, tente novamente!";
                title = "Falha";
                break;

            case -1018:
                message = "Houve um erro. Por favor, tente novamente!";
                title = "Falha";
                break;

            case -2004:
                message = "Houve um erro. Por favor, tente novamente!";
                title = "Falha";
                break;

            default:
                message = "Houve um erro interno. Por favor, informe ao RH";
                title = "Erro Interno";
                break;


        }

        if(title != "Sucesso"){
            dialogShow(message, title);
        }
    }

    private void concludeSale() throws JSONException {
        final RequestQueue requestQueue = Volley.newRequestQueue(context);
        ArrayList<ConcludeInterface> arrayConclude = new ArrayList<>();
        for (int i = 0; i < products.size(); i++){
            ConcludeInterface ci = new ConcludeInterface();
            ci.price = products.get(i).getPrice();
            ci.quantity = products.get(i).getQuantity();
            for(int n = 0; n < lots.size(); n++){
                if(lots.get(i).product.id == products.get(i).product_id){
                    ci.id = lots.get(i).id;
                    Log.d("ID DO PRODUTO", ci.id + "");
                    break;
                }
            }

            arrayConclude.add(ci);
        }

        EditText cpf = (EditText) findViewById(R.id.txtCpf);
        String textCpf = cpf.getText().toString();
        Gson gson = new Gson();
        String json = "{\"user_id\":\"6\", \"products\":"+gson.toJson(arrayConclude)+", \"paid\":1, \"cpf\":\""+textCpf+"\"}";
        Log.d("Request", json);
        final JSONObject compraRequestBody = new JSONObject(json);


        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, baseUrl + "/saveSale", compraRequestBody, new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response) {
                progress.dismiss();
                try {
                    if(response.getString("status").contains("success")){
                        dialogShow("Compra concluída. Obrigado por comprar conosco!", "Sucesso");
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
                dialogShow("Não foi possível concluir a compra. Por favor, tente novamente e se o problema persistir, informe ao RH o seguinte erro: " + error.networkResponse.statusCode, "Falha!");
                progress.dismiss();
            }
        });
        requestQueue.add(jor);
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
                    Intent intent = new Intent(context, StoreActivity.class);
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
        for(final Product product: products){
            ret += product.calculateTotalValue();
        }
        currentValue = ret;
        return this.formatMoney(currentValue);
    }

    private String pagSeguroValue(){
        double ret = 0.0;

        for(final Product product: products){
            ret += product.calculateTotalValue();
        }

        DecimalFormat df = new DecimalFormat("#.00");
        String formatted = df.format(ret);
        return  formatted.replace(".", "").replace(",", "");

    }

    private String formatMoney(double value){
        BigDecimal valor = new BigDecimal (value);
        Locale ptBr = new Locale("pt", "BR");
        NumberFormat nf = NumberFormat.getCurrencyInstance(ptBr);
        String formatado = nf.format (valor);
        return formatado;
    }
}
