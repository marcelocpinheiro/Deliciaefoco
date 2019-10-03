package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ConcludeInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.Product;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ProductInterface;
import com.app.deliciaefoco.deliciaefoco.Adapters.ProductListAdapter;
import com.app.deliciaefoco.deliciaefoco.R;
import com.app.deliciaefoco.deliciaefoco.Adapters.SaleListAdapter;
import com.app.deliciaefoco.deliciaefoco.Interfaces.SaleOrderInterface;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import br.uol.pagseguro.client.plugpag.PlugPag;

public class GiveBackActivity extends AppCompatActivity {

    Context context;
    Dialog dialog;
    ProgressDialog progress;
    private AlertDialog alerta;
    private final String baseUrl = "http://portal.deliciaefoco.com.br/api/";
    JSONArray sales, products;
    ArrayList<SaleOrderInterface> soi;
    SaleOrderInterface soiPayment;
    ArrayList<ProductInterface> piArray;
    SaleListAdapter adapter;
    ProductListAdapter productAdapter;
    int employee_id, saleOrderId;
    String employee_name;
    ListView gv, lv;
    Button btnPay, btnPayTotalDebt;
    String cardBrand = "", date = "", time = "";
    ArrayList<ProductInterface> arrayAll;
    double valorTotal = 0.0;
    double totalDebt = 0.0;
    boolean isTotal = false, money = false;
    ArrayList<Integer> selectedSois;
    boolean foundClass = false;
    TextView lblTotalDebt;
    int metodo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_give_back);
        context = this;
        employee_id = getIntent().getIntExtra("EMPLOYEE_ID", 0);
        employee_name = getIntent().getStringExtra("EMPLOYEE_NAME");

        btnPay = (Button) findViewById(R.id.btnPayWithCard);
        btnPay.setVisibility(View.GONE);

        btnPayTotalDebt = (Button) findViewById(R.id.btnPayTotalDebt);


        arrayAll = new ArrayList<>();
        selectedSois = new ArrayList<>();

        lblTotalDebt = (TextView) findViewById(R.id.txtTotalDebtValue);

        progress = new ProgressDialog(context);
        progress.setTitle("Aguardando pagamento...");
        progress.setMessage("Efetue o pagamento na maquina de cartões. Caso esta compra não seja sua, cancele o pagamento na máquina apertando o botão vermelho.");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog

        Button btnCancelar =  (Button) findViewById(R.id.btnCancelarGiveBack);
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder
                        .setMessage("Deseja realmente cancelar esta operação?")
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


        if(getIntent().getStringExtra("action").equals("pay")){
            btnPayTotalDebt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    valorTotal = totalDebt;
                    isTotal = true;
                    showPaymentDialog();
                }
            });


            btnPay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPaymentDialog();
                }
            });

        }else{
            btnPayTotalDebt.setVisibility(View.GONE);
        }

        setTitle("8 Compras mais antigas de " + employee_name);
        getSales();
        gv = (ListView) findViewById(R.id.lstSales);
        lv = (ListView) findViewById(R.id.lstProducts);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SaleOrderInterface soi = (SaleOrderInterface) parent.getItemAtPosition(position);
                soiPayment = soi;
                saleOrderId = soi.id;
                getProducts(soi.id);
                if(getIntent().getStringExtra("action").equals("pay")){
                    saleOrderInfos(soi, view);
                }
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!(getIntent().getStringExtra("action").equals("pay"))){
                    ProductInterface pi = (ProductInterface) parent.getItemAtPosition(position);
                    Gson gson = new Gson();
                    Log.d("PI", gson.toJson(pi));
                    Intent intent = new Intent(context, FinishGiveBackActivity.class);
                    intent.putExtra("EMPLOYEE_ID", employee_id);
                    intent.putExtra("SALEORDER_ID", saleOrderId);
                    intent.putExtra("EMPLOYEE_MAIL", getIntent().getStringExtra("EMPLOYEE_MAIL"));
                    intent.putExtra("PRODUCT", gson.toJson(pi));
                    startActivityForResult(intent, 0);
                }
            }
        });
    }

    private void showPaymentDialog(){
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.payment_dialog);
        dialog.show();
        Button btnDebit = (Button) dialog.findViewById(R.id.btnDebit);
        Button btnCredit = (Button) dialog.findViewById(R.id.btnCredit);
        Button btnVoucher = (Button) dialog.findViewById(R.id.btnVoucher);
        Button btnDinheiro = (Button) dialog.findViewById(R.id.btnDinheiro);

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

        btnDinheiro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            money = true;
                            concludePayment(soiPayment.id);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
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
                    Log.d("DeliciaeFoco", e.getMessage());
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                int installmentType = PlugPag.A_VISTA;
                int installment = 1;
                int method = paymentMethod;
                metodo = method;
                String codigoVenda = "CODIGVENDA";
                PlugPag plugPag = new PlugPag();
                plugPag.InitBTConnection();
                plugPag.SetVersionName("DelíciaeFoco", "R001");

                int ret = plugPag.SimplePaymentTransaction(
                        method,
                        installmentType,
                        installment,
                        pagSeguroValue(valorTotal),
                        codigoVenda);

                setLastTransactionValues(plugPag.getDate(), plugPag.getTime(), plugPag.getCardBrand());
                handler.sendEmptyMessage(ret);
            }
        }).start();
    }

    private String pagSeguroValue(double value){
        DecimalFormat df = new DecimalFormat("#.00");
        String formatted = df.format(value);
        return  formatted.replace(".", "").replace(",", "");
    }

    private void setLastTransactionValues(String date, String time, String cardBrand){
        this.date = date;
        this.time = time;
        this.cardBrand = cardBrand;
    }

    private void handlePaymentResult(int paymentResult) throws JSONException {

        String message = "";
        String title = "";
        switch (paymentResult){
            case 0:
                if(cardBrand != null){
                    message = "Pagamento realizado. Muito obrigado por comprar conosco!";
                    title = "Sucesso";
                    if(isTotal){
                        concludePayment();
                    }else{
                        concludePayment(soiPayment.id);
                    }
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

            default:
                message = "Houve um erro interno. Por favor, informe ao RH";
                title = "Erro Interno";
                break;


        }

        dialogShow(message, title);
    }

    private void concludePayment() throws JSONException {

        String jsonBody = "{\"user_id\": \""+employee_id+"\", \"money\": "+money+", \"payment_method\": "+metodo+", \"date\": \""+this.date+"\", \"time\": \""+this.time+"\", \"brand\": \""+this.cardBrand+"\"}";

        final RequestQueue rq = Volley.newRequestQueue(context);
        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, baseUrl + "payAll", new JSONObject(jsonBody), new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response){
                progress.dismiss();
                try {
                    if(response.getString("status").contains("success")){
                        dialogShow(response.getString("message"), "Sucesso");
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
                Log.d("DeliciaeFoco", error.networkResponse  +  "");
                dialogShow("Não foi possível concluir o pagamento. Por favor, guarde o seu comprovante e nos envie por e-mail (contato@deliciaefoco.com.br) para podermos fazer a baixa. Pedimos perdão pelo transtorno.", "Falha!");
                progress.dismiss();
            }
        });
        rq.add(jor);
    }

    private void concludePayment(int soi_id) throws JSONException {
        final RequestQueue requestQueue = Volley.newRequestQueue(context);
        String soiPays = "";
        for(int i = 0; i < selectedSois.size(); i++){
            if(soiPays.equals("")){
                soiPays += ""+selectedSois.get(i);
            }else{
                soiPays += ","+selectedSois.get(i);
            }
        }

        String json = "{\"sale_order_ids\":["+soiPays+"], \"date\": \""+this.date+"\", \"time\": \""+this.time+"\", \"brand\": \""+this.cardBrand+"\"}";
        final JSONObject compraRequestBody = new JSONObject(json);
        Log.d("DeliciaeFoco", soiPays + "");

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, baseUrl + "payment", compraRequestBody, new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response) {
                progress.dismiss();
                try {
                    if(response.getString("status").contains("success")){
                        dialogShow(response.getString("message"), "Sucesso");
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
                Log.d("DeliciaeFoco", error.networkResponse  +  "");
                dialogShow("Não foi possível concluir o pagamento. Por favor, guarde o seu comprovante e nos envie por e-mail (contato@deliciaefoco.com.br) para podermos fazer a baixa. Pedimos perdão pelo transtorno.", "Falha!");
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

    private void saleOrderInfos(SaleOrderInterface soi, View view){
        TextView txtPayTitle = (TextView) findViewById(R.id.txtPayTitle);
        TextView txtPayValue = (TextView) findViewById(R.id.txtPayValue);
        btnPay.setVisibility(View.VISIBLE);


        boolean found = false;
        int index = 0;
        for(int i = 0; i < this.selectedSois.size(); i++){
            if(this.selectedSois.get(i).equals(soi.id)){
                found = true;
                index = i;
                break;
            }
        }

        if (found) {
            this.selectedSois.remove(index);
            valorTotal -= soi.total_value;
            view.setBackgroundColor(Color.WHITE);
        }else{
            this.selectedSois.add(soi.id);
            valorTotal += soi.total_value;
            view.setBackgroundColor(Color.YELLOW);
        }


        txtPayTitle.setText("Efetuar pagamento");
        txtPayValue.setText("Valor compras selecionadas: " + formatMoney(valorTotal));
    }

    private void getSales(){
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Carregando ...");
        progress.setMessage("Aguarde enquanto carregamos suas compras");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        String baseurl = "";
        if(getIntent().getStringExtra("action").equals("pay")){
            baseurl = this.baseUrl + "employee/"+employee_id+"/saleOrders/pay";
        }else{
            baseurl = this.baseUrl + "employee/"+employee_id+"/saleOrders";
        }

        JsonArrayRequest jar = new JsonArrayRequest(Request.Method.GET, baseurl, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    sales = response;
                    ArrayList<SaleOrderInterface> array = new ArrayList<>();
                    for(int i = 0; i < sales.length(); i++){
                        SaleOrderInterface obj = new SaleOrderInterface();



                        obj.id = sales.getJSONObject(i)
                                .getInt("id");

                        obj.created_at = sales.getJSONObject(i)
                                .getString("created_at");

                        obj.employee_id = sales.getJSONObject(i)
                                .getInt("employee_id");

                        obj.total_value = sales.getJSONObject(i)
                                .getDouble("total_value");

                        obj.paid = sales.getJSONObject(i)
                                .getInt("paid");

                        obj.updated_at = sales.getJSONObject(i)
                                .getString("updated_at");


                        totalDebt = totalDebt + obj.total_value;

                        if(array.size() < 8){
                            array.add(obj);
                        }
                    }

                    soi = array;
                    adapter = new SaleListAdapter(context, array);


                    lblTotalDebt.setText("Valor Total: " + formatMoney(totalDebt));


                    gv.setAdapter(adapter);
                    progress.dismiss();

                } catch (JSONException e) {
                    Log.d("DeliciaEFoco", "Falha ao buscar compras" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                Log.d("DeliciaEFoco", "Falha ao buscar compras" + error.getMessage() + error.networkResponse);
                progress.dismiss();
            }
        });

        requestQueue.add(jar);
    }

    private String formatMoney(double value){
        BigDecimal valor = new BigDecimal (value);
        Locale ptBr = new Locale("pt", "BR");
        NumberFormat nf = NumberFormat.getCurrencyInstance(ptBr);
        String formatado = nf.format (valor);
        return formatado;
    }

    private void getProducts(int sale_order_id){
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Carregando ...");
        progress.setMessage("Aguarde enquanto carregamos os produtos");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        for(int i = 0; i < this.selectedSois.size(); i++){
            if(this.selectedSois.get(i).equals(sale_order_id)){
                foundClass = true;
                break;
            }
        }


        JsonArrayRequest jar = new JsonArrayRequest(Request.Method.GET, this.baseUrl + "saleOrder/"+sale_order_id+"/products", null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {


                try {
                    products = response;
                    //ArrayList<ProductInterface> array = new ArrayList<>();
                    for(int i = 0; i < products.length(); i++){
                        ProductInterface productInterface = new ProductInterface();

                        //instancia o produto

                        productInterface.id = products.getJSONObject(i)
                                .getInt("id");

                        productInterface.name = products.getJSONObject(i)
                                .getString("name");

                        productInterface.description = products.getJSONObject(i)
                                .getString("description");

                        productInterface.category_id = products.getJSONObject(i)
                                .getString("category_id");

                        productInterface.image = products.getJSONObject(i)
                                .getString("image");

                        productInterface.price = products.getJSONObject(i)
                                .getString("price");

                        productInterface.created_at = products.getJSONObject(i)
                                .getString("created_at");

                        productInterface.quantity = products.getJSONObject(i)
                                .getInt("quantity");

                        productInterface.product_menu_id = products.getJSONObject(i)
                                .getInt("product_menu_id");

                        productInterface.updated_at = products.getJSONObject(i)
                                .getString("updated_at");


                        if(foundClass){
                            for(int n = 0; n < arrayAll.size(); n++){
                                ProductInterface currentPi = arrayAll.get(n);
                                if(currentPi.product_menu_id == productInterface.product_menu_id &&
                                                currentPi.price.equals(productInterface.price) &&
                                                currentPi.quantity == productInterface.quantity &&
                                                currentPi.id == productInterface.id &&
                                                currentPi.created_at.equals(productInterface.created_at)){
                                    arrayAll.remove(n);
                                }
                            }
                        }else{
                            arrayAll.add(productInterface);
                        }

                    }

                    foundClass = false;
                    piArray = arrayAll;
                    productAdapter = new ProductListAdapter(arrayAll, context);
                    lv.setAdapter(productAdapter);
                    progress.dismiss();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                Log.d("DeliciaEFoco", "Falha ao buscar compras");
                progress.dismiss();
            }
        });

        requestQueue.add(jar);
    }
}
