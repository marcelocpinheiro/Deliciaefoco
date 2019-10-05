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
import android.view.MenuItem;
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
    ProgressDialog progress;
    private int lastInteractionTime;
    double currentValue;
    Thread detectThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(10000); // checks every 15sec for inactivity
                    setLastInteractionTime(getLastInteractionTime() + 10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(getLastInteractionTime() >= 120000) //SE O USUARIO NÃO MEXE A 1 minutos, Esvazia o carrinho
                {
                    Log.d("inatividade", "StoreActivity");
                    Intent inte = new Intent(context, HomeActivity.class);
                    startActivity(inte);
                }
            }
        }
    });

    public void onDestroy() {
        super.onDestroy();
        detectThread.interrupt();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        setContentView(R.layout.activity_cart);
        setTitle("Carrinho - Delícia e foco");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //detectThread.start();

        progress = new ProgressDialog(context);

        progress.setTitle("Aguardando pagamento...");
        progress.setMessage("Efetue o pagamento na maquina de cartões. Caso esta compra não seja sua, cancele o pagamento na máquina de cartões apertando o botão vermelho.");
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
                                detectThread.interrupt();
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
                detectThread.interrupt();
                returnIntent.putExtra("result",gson.toJson(products));
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
            }
        });

        Button btnPayNow = (Button) findViewById(R.id.btnPaynow);
        btnPayNow.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                selectUser();
            }
        });
    }

    private void selectUser(){
        if(currentValue > 0) {
            Gson gson = new Gson();
            Intent intent = new Intent(getBaseContext(), PayAfterActivity.class);
            intent.putExtra("CART_ITEMS", gson.toJson(products));
            intent.putExtra("LOTS", gson.toJson(lots));
            intent.putExtra("CARTEIRA", 0);
            detectThread.interrupt();
            startActivityForResult(intent, 0);
        }else{
            dialogShow("Compra sem valor. Por favor, insira algum produto.", "Atenção");
        }
    }

    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
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
                    if(detectThread != null) {
                        detectThread.interrupt();
                    }
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

    private String formatMoney(double value){
        BigDecimal valor = new BigDecimal (value);
        Locale ptBr = new Locale("pt", "BR");
        NumberFormat nf = NumberFormat.getCurrencyInstance(ptBr);
        String formatado = nf.format (valor);
        return formatado;
    }
}
