package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.Window;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.deliciaefoco.deliciaefoco.Activities.StoreActivity;
import com.app.deliciaefoco.deliciaefoco.Adapters.EmployeeGridViewAdapter;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ConcludeInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.EmployeeInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.LotProductInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.Product;
import com.app.deliciaefoco.deliciaefoco.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.uol.pagseguro.client.plugpag.PlugPag;

public class ConcludeSale extends AppCompatActivity {
    EmployeeInterface selectedEmployee;
    List<Product> produtos;
    int carteira = 0;
    double saldo = 0;
    Context context = this;
    private final String baseUrl = "http://portal.deliciaefoco.com.br/api";
    private AlertDialog alerta;
    ArrayList<LotProductInterface> lots;
    TextView valueTxt;
    Dialog dialog;
    ProgressDialog progress;
    boolean dinheiro = false;
    String cardBrand = null, date = null, time = null;
    int user_id_buyer = 6;
    PlugPag plugPag;
    int sale_id;

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
        this.carteira = getIntent().getIntExtra("CARTEIRA", 0);

        TextView txtNameEmployee = (TextView) findViewById(R.id.txtNameEmployee);
        txtNameEmployee.setText(selectedEmployee.nome);

        valueTxt = (TextView) findViewById(R.id.txtValue);
        valueTxt.setText("Caso escolha pagar depois, será adicionada uma compra no valor de "+this.getTotalValue()+" em sua conta, que deverá ser paga em até 7 dias.");

        if(this.carteira == 1){
            this.getSaldo();
        }

        Button btnEsqueci = (Button) findViewById(R.id.btnEsqueciSenha);
        if(selectedEmployee.id == 11){
            btnEsqueci.setVisibility(View.INVISIBLE);
        }
        btnEsqueci.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogShow("Acesse o link \"portal.deliciaefoco.com.br/password/reset\" \n" +
                        "e altere a sua senha. Após isto, basta inserir aqui e concluir sua compra!", "Alterar sua senha");
            }
        });

        Button btnCancelar = (Button) findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final EditText passwordText = (EditText) findViewById(R.id.passwordField);
        if(selectedEmployee.id == 11){
            passwordText.setVisibility(View.INVISIBLE);
        }

        Button btnFinalizarAgora = (Button) findViewById(R.id.btnFinalizarCompraAgora);
        btnFinalizarAgora.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("password", passwordText.getText().toString());
                if(passwordText.getText().equals("")){
                    dialogShow("Por favor, digite a sua senha! \n" +
                            "Caso tenha esquecido, clique em \"Esqueci minha senha\" para alterá-la", "Insira sua senha");
                    return;
                }

                if(selectedEmployee.id == 11){
                    payAsConvidado();
                }else{
                    final RequestQueue requestQueue = Volley.newRequestQueue(context);
                    final ProgressDialog progress = new ProgressDialog(context);
                    progress.setTitle("Aguarde ...");
                    progress.setMessage("Verificando credenciais ...");
                    progress.setCancelable(false); // disable dismiss by tapping outside of the dialog

                    try{
                        progress.show();
                    }catch(Exception e) {
                        Log.d("Falha", e.getMessage());
                    }



                    try {
                        final JSONObject jsonBody = new JSONObject("{\"email\":\""+selectedEmployee.email+"\", \"password\":\""+passwordText.getText()+"\"}");
                        JsonObjectRequest jar = new JsonObjectRequest(Request.Method.POST, baseUrl + "/checkuser", jsonBody, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    if(response.getString("status").contains("success")){
                                        user_id_buyer = response.getInt("user_id");

                                        dialog = new Dialog(context);
                                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                        dialog.setContentView(R.layout.payment_dialog);
                                        dialog.show();
                                        Button btnDebit = (Button) dialog.findViewById(R.id.btnDebit);
                                        Button btnCredit = (Button) dialog.findViewById(R.id.btnCredit);
                                        Button btnVoucher = (Button) dialog.findViewById(R.id.btnVoucher);
                                        Button btnCarteira = (Button) dialog.findViewById(R.id.btnCarteira);
                                        Button btnDinheiro = (Button) dialog.findViewById(R.id.btnDinheiro);
                                        btnDinheiro.setText("Dinheiro / Transferência");

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

                                        btnCarteira.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Gson gson = new Gson();
                                                Intent intent = new Intent(getBaseContext(), PayAfterActivity.class);
                                                intent.putExtra("CART_ITEMS", gson.toJson(produtos));
                                                intent.putExtra("LOTS", gson.toJson(lots));
                                                intent.putExtra("CARTEIRA", 1);

                                                startActivityForResult(intent, 0);
                                            }
                                        });

                                        btnDinheiro.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {

                                                            concludeSale(user_id_buyer, true);
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }).start();


                                            }
                                        });
                                    }else{
                                        dialogShow("Por favor, verifique se digitou a sua senha corretamente.", "Acesso negado");
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
                        progress.dismiss();
                        dialogShow("Falha ao processar pedido. Por favor, Informe este problema ao RH" + e.getMessage(), "Falha");
                        Log.d("Falha", e.getMessage());
                    }
                }
            }
        });



        Button btnFinalizar = (Button) findViewById(R.id.btnFinalizarCompra);
        if(selectedEmployee.id == 11){
            btnFinalizar.setVisibility(View.INVISIBLE);
        }
        btnFinalizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("password", passwordText.getText().toString());
                if(passwordText.getText().equals("")){
                    dialogShow("Por favor, digite a sua senha! \n" +
                            "Caso tenha esquecido, clique em \"Esqueci minha senha\" para alterá-la", "Insira sua senha");
                    return;
                }

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

                                    int user_id = response.getInt("user_id");
                                    Gson gson = new Gson();

                                    double ret = 0.0;
                                    for(final Product product: produtos){
                                        ret += product.calculateTotalValue();
                                    }


                                    final JSONObject compraRequestBody;
                                    if(saldo > 0 && saldo >= ret){
                                        compraRequestBody = new JSONObject("{\"user_id\":\""+user_id+"\", \"carteira\": \"1\",  \"products\":"+gson.toJson(arrayConclude)+"}");
                                    }else{
                                        compraRequestBody = new JSONObject("{\"user_id\":\""+user_id+"\", \"products\":"+gson.toJson(arrayConclude)+"}");
                                    }

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
                                            dialogShow(new String(error.networkResponse.data), "Falha!");
                                            progress.dismiss();
                                        }
                                    });

                                    requestQueue.add(jor);
                                }else{
                                    dialogShow("Por favor, verifique se digitou a sua senha corretamente.", "Acesso negado");
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
                    progress.dismiss();
                    dialogShow("Falha ao processar pedido. Por favor, Informe este problema ao RH" + e.getMessage(), "Falha");
                    Log.d("Falha", e.getMessage());
                }
            }
        });

    }

    private void prePayment(int userId, final int paymentMethod){
        final RequestQueue requestQueue = Volley.newRequestQueue(context);
        Log.d("prepayment", "++++++++++++++++++++ METODO "+ paymentMethod +"++++++++++++++++");

        try {
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

            int user_id = userId;
            Gson gson = new Gson();

            double ret = 0.0;
            for(final Product product: produtos){
                ret += product.calculateTotalValue();
            }


            final JSONObject compraRequestBody;
            compraRequestBody = new JSONObject("{\"user_id\":\""+user_id+"\", \"products\":"+gson.toJson(arrayConclude)+", \"method\": "+paymentMethod+"}");

            JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, baseUrl + "/savePreSale", compraRequestBody, new Response.Listener<JSONObject>(){
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if(response.getString("status").contains("success")){
                            sale_id = response.getInt("sale_id");
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
                    dialogShow(new String(error.networkResponse.data), "Falha!");
                    progress.dismiss();
                }
            });

            requestQueue.add(jor);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void payAsConvidado(){
        user_id_buyer = 6;
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.payment_dialog);
        dialog.show();
        Button btnDebit = (Button) dialog.findViewById(R.id.btnDebit);
        Button btnCredit = (Button) dialog.findViewById(R.id.btnCredit);
        Button btnVoucher = (Button) dialog.findViewById(R.id.btnVoucher);
        Button btnCarteira = (Button) dialog.findViewById(R.id.btnCarteira);
        Button btnDinheiro = (Button) dialog.findViewById(R.id.btnDinheiro);
        btnDinheiro.setText("Dinheiro");

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

        btnCarteira.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Gson gson = new Gson();
                Intent intent = new Intent(getBaseContext(), PayAfterActivity.class);
                intent.putExtra("CART_ITEMS", gson.toJson(produtos));
                intent.putExtra("LOTS", gson.toJson(lots));
                intent.putExtra("CARTEIRA", 1);

                startActivityForResult(intent, 0);
            }
        });

        btnDinheiro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            concludeSale(user_id_buyer);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();


            }
        });
    }

    private void payNow(int payment){

        prePayment(user_id_buyer, payment);
        Log.d("DeliciaEFoco", payment + "");
        final ProgressDialog progress = new ProgressDialog(context);
        progress.setTitle("Aguardando Pagamento ...");
        progress.setMessage("Siga as instruções da máquina de pagamento ...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar Compra", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                if(plugPag != null){
                    //plugPag.CancelTransaction();
                }
            }
        });
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
                plugPag = new PlugPag();
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
                    concludeSale(user_id_buyer);
                }else{
                    message = "Houve um erro. Por favor, tente novamente!";
                    title = "Falha";
                }
                break;

            case -1003:
                message = "Transação Cancelada!";
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
    }

    private void concludeSale(int user_id, boolean money) throws JSONException{
        Log.d("DEBUG", "+++++++++++++++++++++++++ CONCLUDE SALE MONEY CHAMADO +++++++++++++++++=");
        final RequestQueue requestQueue = Volley.newRequestQueue(context);

        try {
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

            Gson gson = new Gson();

            double ret = 0.0;
            for(final Product product: produtos){
                ret += product.calculateTotalValue();
            }


            final JSONObject compraRequestBody;
            compraRequestBody = new JSONObject("{\"user_id\":\""+user_id+"\", \"products\":"+gson.toJson(arrayConclude)+", \"method\": 4}");

            JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, baseUrl + "/savePreSale", compraRequestBody, new Response.Listener<JSONObject>(){
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if(response.getString("status").contains("success")){
                            sale_id = response.getInt("sale_id");


                            String json = "{\"sale_order_ids\":["+sale_id+"], \"money\": true}";
                            final JSONObject compraRequestBody = new JSONObject(json);
                            JsonObjectRequest jor_2 = new JsonObjectRequest(Request.Method.POST, baseUrl + "/payment", compraRequestBody, new Response.Listener<JSONObject>(){
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        if(progress != null) {
                                            progress.dismiss();
                                        }
                                        if(response.getString("status").contains("success")){
                                            dialogShow("Compra concluída. Obrigado por comprar conosco! \n " +
                                                    "Caso seja transferencia, por favor, envie o comprovante para contato@deliciaefoco.com.br \n" +
                                                    "Os dados da conta são: \n" +
                                                    "Banco Itaú \n" +
                                                    "Ag.: 6393\n" +
                                                    "Cc.: 20429-9\n" +
                                                    "CPF. 277.848.938-09\n" +
                                                    "Os dados também estão disponíveis na página inicial", "Sucesso");
                                        }else{
                                            dialogShow(response.getString("message"), "Falha!");
                                        }
                                    } catch (JSONException e) {
                                        Log.d("DeliciaeFoco", e.getMessage() + "");
                                    }
                                }
                            }, new Response.ErrorListener(){
                                @Override
                                public void onErrorResponse(VolleyError error){
                                    if(progress != null) {
                                        progress.dismiss();
                                    }
                                    Log.d("DeliciaeFoco", error.networkResponse  +  "");
                                    dialogShow("Por favor, guarde o seu comprovante e nos envie por e-mail (contato@deliciaefoco.com.br) para podermos fazer a baixa. Pedimos perdão pelo transtorno.", "Não foi possível concluir o pagamento");
                                }
                            });
                            requestQueue.add(jor_2);

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
                    dialogShow(new String(error.networkResponse.data), "Falha!");
                    progress.dismiss();
                }
            });

            requestQueue.add(jor);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void concludeSale(int user_id) throws JSONException {
        final RequestQueue requestQueue = Volley.newRequestQueue(context);
        String json = "{\"sale_order_ids\":["+sale_id+"]}";
        final JSONObject compraRequestBody = new JSONObject(json);
        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, baseUrl + "/payment", compraRequestBody, new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(progress != null) {
                        progress.dismiss();
                    }
                    if(response.getString("status").contains("success")){
                        dialogShow("Compra concluída. Obrigado por comprar conosco!", "Sucesso");
                    }else{
                        dialogShow("Falha!", response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("DeliciaeFoco", e.getMessage() + "");
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                if(progress != null) {
                    progress.dismiss();
                }
                dialogShow("Por favor, guarde o seu comprovante e nos envie por e-mail (contato@deliciaefoco.com.br) para podermos fazer a baixa. Pedimos perdão pelo transtorno.", "Não foi possível concluir o pagamento");
            }
        });
        requestQueue.add(jor);

        /*


        ArrayList<ConcludeInterface> arrayConclude = new ArrayList<>();
        for (int i = 0; i < produtos.size(); i++){
            ConcludeInterface ci = new ConcludeInterface();
            ci.price = produtos.get(i).getPrice();
            ci.quantity = produtos.get(i).getQuantity();
            for(int n = 0; n < lots.size(); n++){
                if(lots.get(i).product.id == produtos.get(i).product_id){
                    ci.id = lots.get(i).id;
                    break;
                }
            }

            arrayConclude.add(ci);
        }

        String textCpf = "";
        Gson gson = new Gson();
        String json = "{\"user_id\":\""+user_id+"\", \"products\":"+gson.toJson(arrayConclude)+", \"paid\":1, \"cpf\":\""+textCpf+"\"}";
        Log.d("Request", json);
        final JSONObject compraRequestBody = new JSONObject(json);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, baseUrl + "/saveSale", compraRequestBody, new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response) {
                if(progress != null){
                    progress.dismiss();
                }

                try {
                    if(response.getString("status").contains("success")){
                        if(dinheiro){
                            dialogShow("Por favor, insira o valor em dinheiro na urna ao lado. Compra concluída, Obrigado por comprar conosco!", "Sucesso");
                        }else{
                            dialogShow("Compra concluída. Obrigado por comprar conosco!", "Sucesso");
                        }
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
        */
    }

    private String pagSeguroValue(){
        double ret = 0.0;

        for(final Product product: produtos){
            ret += product.calculateTotalValue();
        }

        DecimalFormat df = new DecimalFormat("#.00");
        String formatted = df.format(ret);
        return  formatted.replace(".", "").replace(",", "");

    }

    private void getSaldo(){
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Carregando ...");
        progress.setMessage("Aguarde enquanto buscamos seu saldo");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        JsonObjectRequest jar = new JsonObjectRequest(Request.Method.GET, this.baseUrl + "/employee/"+selectedEmployee.id+"/saldo", null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    double ret = 0.0;
                    for(final Product product: produtos){
                        ret += product.calculateTotalValue();
                    }

                    saldo = response.getDouble("saldo");
                    Log.d("DeliciaEFoco", saldo + "");

                    if(saldo < ret){
                        dialogShow("Saldo insuficiente para esta compra. A compra sera marcada para ser paga depois.",  "Saldo insuficiente");
                    }else{
                        valueTxt.setText("Seu saldo é de "+saldo+", O valor de "+getTotalValue()+" será abatido dele.");
                    }

                    progress.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
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
