package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.app.deliciaefoco.deliciaefoco.Providers.ApiProvider;
import com.app.deliciaefoco.deliciaefoco.Providers.DialogProvider;
import com.app.deliciaefoco.deliciaefoco.Providers.UtilitiesProvider;
import com.app.deliciaefoco.deliciaefoco.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
    private AlertDialog alerta;
    ArrayList<LotProductInterface> lots;
    TextView valueTxt;
    Dialog dialog;
    ProgressDialog progress;
    String cardBrand = null, date = null, time = null;
    int user_id_buyer = 6;
    PlugPag plugPag;
    ApiProvider api;
    int sale_id;
    boolean guest = false;
    String FILENAME = "DEFAULT_COMPANY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conclude_sale);
        setTitle("Concluir Compra - Delícia e Foco");

        Type productsType = new TypeToken<List<Product>>(){}.getType();
        final Type employee = new TypeToken<EmployeeInterface>(){}.getType();
        Type lotsType = new TypeToken<ArrayList<LotProductInterface>>(){}.getType();
        this.api = new ApiProvider(this);

        produtos = new Gson().fromJson(getIntent().getStringExtra("PRODUTOS"), productsType);
        lots = new Gson().fromJson(getIntent().getStringExtra("LOTS"), lotsType);
        guest = getIntent().getBooleanExtra("GUEST", false);
        if (!guest) {
            selectedEmployee = new Gson().fromJson(getIntent().getStringExtra("EMPLOYEE"), employee);
            this.carteira = getIntent().getIntExtra("CARTEIRA", 0);
        }

        TextView txtNameEmployee = (TextView) findViewById(R.id.txtNameEmployee);
        valueTxt = (TextView) findViewById(R.id.txtValue);
        if (guest) {
            txtNameEmployee.setText("Convidado");
            valueTxt.setText("Valor total: " + this.getTotalValue());
        } else {
            txtNameEmployee.setText(selectedEmployee.nome);
            valueTxt.setText("Caso escolha pagar depois, será adicionada uma compra no valor de " + this.getTotalValue() + " em sua conta, que deverá ser paga em até 7 dias.");
        }


        if(this.carteira == 1){
            this.getSaldo();
        }

        Button btnCancelar = (Button) findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button btnFinalizarAgora = (Button) findViewById(R.id.btnFinalizarCompraAgora);
        btnFinalizarAgora.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (guest) {
                    payAsConvidado();
                }else{
                    user_id_buyer = getIntent().getIntExtra("ID_USUARIO", 0);
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
                                        UtilitiesProvider.trackException(e);
                                    } catch (IOException e) {
                                        UtilitiesProvider.trackException(e);
                                    }
                                }
                            }).start();
                        }
                    });
                }
            }
        });



        Button btnFinalizar = (Button) findViewById(R.id.btnFinalizarCompra);
        if (guest) {
            btnFinalizar.setVisibility(View.INVISIBLE);
        }
        btnFinalizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog progress = new ProgressDialog(context);

                progress.dismiss();
                progress.setTitle("Aguarde ...");
                progress.setMessage("Concluindo Compra ...");
                progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                progress.show();

                ArrayList<ConcludeInterface> arrayConclude = arrayConcludeCreator();

                int user_id = getIntent().getIntExtra("ID_USUARIO", 0);
                double ret = 0.0;
                for(final Product product: produtos){
                    ret += product.calculateTotalValue();
                }

                try{
                    api.payOff(user_id, (saldo > 0 && saldo >= ret), arrayConclude, new Response.Listener<JSONObject>(){
                        @Override
                        public void onResponse(JSONObject response) {
                            progress.dismiss();
                            try {
                                if(response.getString("status").contains("success")){
                                    new DialogProvider(context).dialogShow("Sucesso", "Compra concluída. Obrigado por comprar conosco, " + selectedEmployee.nome + "!", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            UtilitiesProvider.backToHome(context);
                                        }
                                    });
                                }else{
                                    new DialogProvider(context).dialogShow("Falha ao salvar compra 1", "Por favor, tente novamente mais tarde", null);
                                }
                            } catch (JSONException e) {
                                UtilitiesProvider.trackException(e);
                                new DialogProvider(context).dialogShow("Falha ao salvar compra 2", "Por favor, tente novamente mais tarde", null);
                            }
                        }
                    }, new Response.ErrorListener(){
                        @Override
                        public void onErrorResponse(VolleyError error){

                            new DialogProvider(context).dialogShow("Falha ao salvar compra 3", "Por favor, tente novamente mais tarde", null);
                            progress.dismiss();
                        }
                    });
                }catch(JSONException e){
                    UtilitiesProvider.trackException(e);
                    new DialogProvider(context).dialogShow("Falha ao salvar compra 4", "Por favor, tente novamente mais tarde", null);
                }
            }
        });

    }

    private void setSaleId(int saleid, int payment){
        sale_id = saleid;

        //armazena o ultimo id nas configurações
        SharedPreferences settings = getSharedPreferences(FILENAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("last_sale_id", saleid);
        editor.commit();
        continuePayment(payment);
    }

    private void prePayment(int userId, final int paymentMethod) throws IOException {
        try {
            ArrayList<ConcludeInterface> arrayConclude = this.arrayConcludeCreator();
            api.preSale(userId, arrayConclude, paymentMethod, new Response.Listener<JSONObject>(){
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        Log.d("response", response.toString());
                        if(response.getString("status").contains("success")){
                            setSaleId(response.getInt("sale_id"), paymentMethod);
                        }else{
                            new DialogProvider(context).dialogShow("Falha", response.getString("message"), null);
                            progress.dismiss();
                        }
                    } catch (JSONException e) {
                        UtilitiesProvider.trackException(e);
                        new DialogProvider(context).dialogShow("Falha", "Não foi possível salvar sua compra. Por favor, tente novamente.", null);
                        progress.dismiss();
                    }
                }
            }, new Response.ErrorListener(){
                @Override
                public void onErrorResponse(VolleyError error){
                    new DialogProvider(context).dialogShow("Falha", "Não foi possível salvar sua compra. Por favor, tente novamente.", null);
                    progress.dismiss();
                }
            });
        } catch (JSONException e) {
            UtilitiesProvider.trackException(e);
            new DialogProvider(context).dialogShow("Falha", "Não foi possível salvar sua compra. Por favor, tente novamente.", null);
            progress.dismiss();
        }

    }

    private void payAsConvidado(){
        user_id_buyer = 0;
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

                            concludeSale(user_id_buyer, true);
                        } catch (JSONException e) {
                            UtilitiesProvider.trackException(e);
                            e.printStackTrace();
                        } catch (IOException e) {
                            UtilitiesProvider.trackException(e);
                            e.printStackTrace();
                        }
                    }
                }).start();


            }
        });
    }

    private void continuePayment(int payment){
        Log.d("DeliciaEFoco", payment + " " + sale_id);
        final ProgressDialog progress = new ProgressDialog(context);
        progress.setTitle("Aguardando Pagamento ...");
        progress.setMessage("Siga as instruções da máquina de pagamento ... \n" +
                "Para cancelar, aperte o botão vermelho na maquininha");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        final int paymentMethod = payment;


        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int result = msg.getData().getInt("retorno");
                progress.dismiss();
                try {
                    handlePaymentResult(result);
                } catch (JSONException e) {
                    UtilitiesProvider.trackException(e);
                    Log.d("Teste_Mensagem", e.getMessage());
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

                try{
                    double value = 0.0;
                    for (final Product product : produtos) {
                        value += product.calculateTotalValue();
                    }


                    int ret = plugPag.SimplePaymentTransaction(
                            method,
                            installmentType,
                            installment,
                            UtilitiesProvider.pagSeguroValue(value, context),
                            codigoVenda);



                    setLastTransactionValues(plugPag.getDate(), plugPag.getTime(), plugPag.getCardBrand());
                    Bundle b = new Bundle();
                    b.putInt("retorno", ret);
                    Message m = new Message();
                    m.setData(b);
                    handler.sendMessage(m);
                }catch(Exception e){
                    UtilitiesProvider.trackException(e);
                    Log.d("Teste_Mensagem", e.getMessage() + "+++++++++++++++++++");
                }
            }
        }).start();
    }

    private void payNow(int payment){
        try {
            prePayment(user_id_buyer, payment);
        } catch (IOException e) {
            UtilitiesProvider.trackException(e);
            e.printStackTrace();
        }
    }

    private void setLastTransactionValues(String date, String time, String cardBrand){
        this.date = date;
        this.time = time;
        this.cardBrand = cardBrand;
    }

    private ArrayList<ConcludeInterface> arrayConcludeCreator(){
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
            arrayConclude.add(ci);
        }
        return arrayConclude;
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
                    cancelSale("Não foi possível completar a transação.");
                }
                break;

            case -1003:
                cancelSale("Compra cancelada pelo usuário.");
                break;

            case -1004:
                cancelSale("Compra cancelada pelo usuário / Transação negada pelo terminal.");
                break;

            case -2023:
                cancelSale("Não há conexão com o terminal de pagamento. Por favor, pague em dinheiro ou marque a sua compra");
                break;

            default:
                cancelSale("Não foi possível concluir a sua compra. Por favor, tente novamente.");
                break;
        }
    }

    private void cancelSale(String motive){
        SharedPreferences settings = getSharedPreferences(FILENAME, 0);
        final String textMotive = motive;

        if(sale_id == 0){
            sale_id = settings.getInt("last_sale_id", 0);
        }

        try {
            api.cancelSale(sale_id, new Response.Listener<JSONObject>(){
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if(response.getString("status").contains("success")){
                            dialogShow(textMotive, "Compra cancelada");
                        }else{
                            dialogShow("Falha!", response.getString("message"));
                        }
                    } catch (JSONException e) {
                        UtilitiesProvider.trackException(e);
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener(){
                @Override
                public void onErrorResponse(VolleyError error){
                    Log.d("DeliciaEFoco", "Falha ao cance");
                    dialogShow("Falha ao cancelar transação", "Falha!");
                    progress.dismiss();
                }
            });
        } catch (JSONException e) {
            UtilitiesProvider.trackException(e);
            e.printStackTrace();
        }
    }

    private void concludeSale(int user_id, boolean money) throws JSONException, IOException {


        Log.d("DEBUG", "+++++++++++++++++++++++++ CONCLUDE SALE MONEY CHAMADO +++++++++++++++++=");
        final RequestQueue requestQueue = Volley.newRequestQueue(context);

        try {
            ArrayList<ConcludeInterface> arrayConclude = this.arrayConcludeCreator();

            Gson gson = new Gson();

            double ret = 0.0;
            for(final Product product: produtos){
                ret += product.calculateTotalValue();
            }

            api.preSale(user_id, arrayConclude, 4, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if(response.getString("status").contains("success")){
                            sale_id = response.getInt("sale_id");
                            api.paymentMoney(new int[]{sale_id}, new Response.Listener<JSONObject>() {
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
                                        UtilitiesProvider.trackException(e);
                                        Log.d("DeliciaeFoco", e.getMessage() + "");
                                    }
                                }
                            }, new Response.ErrorListener(){
                                @Override
                                public void onErrorResponse(VolleyError error){
                                    if(progress != null) {
                                        progress.dismiss();
                                    }
                                    Log.e("DeliciaeFoco", "ERRO ===============================================================");
                                    Log.e("DeliciaeFoco", error.getMessage() + "");
                                    dialogShow("Por favor, guarde o seu comprovante e nos envie por e-mail (contato@deliciaefoco.com.br) para podermos fazer a baixa. " +
                                                    "Pedimos perdão pelo transtorno.",
                                            "Não foi possível concluir o pagamento em dinheiro");
                                }
                            });
                        }else{
                            dialogShow("Falha!", response.getString("message"));
                        }
                    } catch (JSONException e) {
                        UtilitiesProvider.trackException(e);
                    }
                }
            }, new Response.ErrorListener(){
                @Override
                public void onErrorResponse(VolleyError error){
                    dialogShow(new String(error.networkResponse.data), "Falha!");
                    if (progress != null) progress.dismiss();
                }
            });
        } catch (JSONException e) {
            UtilitiesProvider.trackException(e);
        }
    }

    private void concludeSale(int user_id) throws JSONException {
        api.payment(new int[]{sale_id}, this.date, this.time, this.cardBrand, new Response.Listener<JSONObject>() {
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
                    UtilitiesProvider.trackException(e);
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                if(progress != null) {
                    progress.dismiss();
                }
                Log.e("Erro ===========", error.getStackTrace().toString());
                dialogShow("Por favor, guarde o seu comprovante e nos envie por e-mail (contato@deliciaefoco.com.br) " +
                                "para podermos fazer a baixa. Pedimos perdão pelo transtorno.",
                        "Não foi possível concluir o pagamento");
            }
        });
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
        //TODO: PASSAR A BUSCA DE SALDO PARA USAR O API PROVIDER
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Carregando ...");
        progress.setMessage("Aguarde enquanto buscamos seu saldo");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        JsonObjectRequest jar = new JsonObjectRequest(Request.Method.GET, getBaseUrl() + "/employee/"+selectedEmployee.id+"/saldo", null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    double ret = 0.0;
                    for(final Product product: produtos){
                        ret += product.calculateTotalValue();
                    }

                    saldo = response.getDouble("saldo");

                    if(saldo < ret){
                        dialogShow("Saldo insuficiente para esta compra. A compra sera marcada para ser paga depois.",  "Saldo insuficiente");
                    }else{
                        valueTxt.setText("Seu saldo é de "+saldo+", O valor de "+getTotalValue()+" será abatido dele.");
                    }

                    progress.dismiss();
                } catch (Exception e) {
                    UtilitiesProvider.trackException(e);
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
        //TODO: UTILIZAR APENAS O DIALOG PROVIDER
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

    private String getBaseUrl(){
        SharedPreferences settings = getSharedPreferences(FILENAME, 0);
        return settings.getString("base_url", "");
    }

}
