package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.app.deliciaefoco.deliciaefoco.Interfaces.LotProductInterface;
import com.app.deliciaefoco.deliciaefoco.NumberPickerDialog;
import com.app.deliciaefoco.deliciaefoco.Interfaces.Product;
import com.app.deliciaefoco.deliciaefoco.Adapters.ProductGridViewAdapter;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ProductInterface;
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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class StoreActivity extends AppCompatActivity implements NumberPicker.OnValueChangeListener {
    private final String baseUrl = "http://portal.deliciaefoco.com.br/api";
    private AlertDialog alerta;
    Context context = this;
    List<Product> cart;
    Product currentProduct;
    HashMap<String ,Integer> hmLang;
    JSONArray products;
    String FILENAME = "enterprise_file.txt";
    private Integer enterpriseId = 0;
    private int lastInteractionTime;
    ProductGridViewAdapter adapter;
    ArrayList<LotProductInterface> lots, lotsToSend;
    Thread detectThread;
    EditText txtSearch;
    Button btnCart;
    //atributo da classe.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        lotsToSend = new ArrayList<>();
        setContentView(R.layout.activity_store);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cart = new ArrayList<Product>();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
        });

        try{
            FileInputStream in = openFileInput(FILENAME);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            this.enterpriseId = Integer.parseInt(sb.toString());


            txtSearch = (EditText) findViewById(R.id.editTextSearch);
            txtSearch.addTextChangedListener(new TextWatcher() {
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




            this.getProducts();
        }catch(FileNotFoundException e){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } catch (IOException e) {
            e.printStackTrace();
        }


        btnCart = (Button) findViewById(R.id.btnCart);
        btnCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Gson gson = new Gson();
                Intent intent = new Intent(getBaseContext(), CartActivity.class);
                intent.putExtra("CART_ITEMS", gson.toJson(cart));
                intent.putExtra("LOTS", gson.toJson(lotsToSend));

                startActivityForResult(intent, 1);
            }
        });

        GridView gv = (GridView) findViewById(R.id.gridViewStore);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LotProductInterface product = adapter.getItem(position);

                lotsToSend.add(adapter.getItem(position));
                currentProduct = new Product();
                currentProduct.name = product.product.name;
                currentProduct.id = product.id;
                currentProduct.product_id = product.product.id;
                currentProduct.price = Double.parseDouble(product.product.price);
                currentProduct.maxQuantity = product.quantity;

                //exemplo_simples("Item clicado: " + product.getInt("id") +" - "+ product.getString("name"));
                showNumberPicker(currentProduct.name, currentProduct.price, product.quantity);
                txtSearch.setText("");


            }
        });
    }

    @Override
    protected void onResume (){
        super.onResume();
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
    }
    

    protected void onWindowFocusChanged(Boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                Type listType = new TypeToken<ArrayList<Product>>(){}.getType();
                this.cart = new Gson().fromJson(data.getStringExtra("result"), listType);
                btnCart.setText("Carrinho ("+cart.size()+" Items)");
            }
        }
    }

    @Override
    public void onValueChange(NumberPicker numberPicker, int i, int i1) {
        currentProduct.quantity = numberPicker.getValue();
        cart.add(currentProduct);
        btnCart.setText("Carrinho ("+cart.size()+" Items)");

        Toast.makeText(this,
                "Foram adicionados " + currentProduct.quantity + " unidades de " + currentProduct.name + " ao carrinho.", Toast.LENGTH_SHORT).show();

    }


    public void showNumberPicker(String productName, double productPrice, int maxQuantity){
        NumberPickerDialog newFragment = new NumberPickerDialog(productName + " - "+formatMoney(productPrice), "Selecione a quantidade do produto que deseja adicionar ao carrinho", "Adicionar ao carrinho", "Cancelar", 1, maxQuantity);
        newFragment.setValueChangeListener(this);
        newFragment.show(getFragmentManager(), "Number Picker");
    }

    private void exemplo_simples(String text, String title) {
        //Cria o gerador do AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //define o titulo
        builder.setTitle(title);
        //define a mensagem
        builder.setMessage(text);
        //define um botão como positivo
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });
        //cria o AlertDialog
        alerta = builder.create();
        //Exibe
        alerta.show();
    }

    private void getProducts(){
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Carregando ...");
        progress.setMessage("Aguarde enquanto carregamos os produtos");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        JsonArrayRequest jar = new JsonArrayRequest(Request.Method.GET, this.baseUrl + "/enterprise/"+enterpriseId+"/products", null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    GridView gv = (GridView) findViewById(R.id.gridViewStore);
                    products = response;
                    ArrayList<LotProductInterface> array = new ArrayList<LotProductInterface>();
                    for(int i = 0; i < products.length(); i++){
                        LotProductInterface obj = new LotProductInterface();
                        ProductInterface productInterface = new ProductInterface();

                        //instancia o produto

                        productInterface.id = products.getJSONObject(i)
                                .getJSONObject("product")
                                .getInt("id");

                        productInterface.name = products.getJSONObject(i)
                                .getJSONObject("product")
                                .getString("name");

                        productInterface.description = products.getJSONObject(i)
                                .getJSONObject("product")
                                .getString("description");

                        productInterface.category_id = products.getJSONObject(i)
                                .getJSONObject("product")
                                .getString("category_id");

                        productInterface.image = products.getJSONObject(i)
                                .getJSONObject("product")
                                .getString("image");

                        productInterface.price = products.getJSONObject(i)
                                .getJSONObject("product")
                                .getString("price");

                        productInterface.created_at = products.getJSONObject(i)
                                .getJSONObject("product")
                                .getString("created_at");

                        productInterface.updated_at = products.getJSONObject(i)
                                .getJSONObject("product")
                                .getString("updated_at");


                        obj.product = productInterface;

                        obj.id = products.getJSONObject(i)
                                .getInt("id");

                        obj.created_at = products.getJSONObject(i)
                                .getString("created_at");

                        obj.enterprise_id = products.getJSONObject(i)
                                .getString("enterprise_id");

                        obj.price = products.getJSONObject(i)
                                .getString("price");

                        obj.quantity = products.getJSONObject(i)
                                .getInt("quantity");

                        obj.product_id = products.getJSONObject(i)
                                .getString("product_id");

                        obj.updated_at = products.getJSONObject(i)
                                .getString("updated_at");

                        array.add(obj);
                    }

                    lots = array;
                    adapter = new ProductGridViewAdapter(array, context);
                    gv.setAdapter(adapter);
                    progress.dismiss();

                } catch (JSONException e) {
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

    private String formatMoney(double value){
        BigDecimal valor = new BigDecimal (value);
        Locale ptBr = new Locale("pt", "BR");
        NumberFormat nf = NumberFormat.getCurrencyInstance(ptBr);
        String formatado = nf.format (valor);
        return formatado;
    }

    public void startUserInactivityDetect(){
        detectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(10000); // checks every 15sec for inactivity
                        setLastInteractionTime(getLastInteractionTime() + 10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(getLastInteractionTime() >= 60000) //SE O USUARIO NÃO MEXE A 1 minutos, Esvazia o carrinho
                    {
                        Intent inte = new Intent(context, HomeActivity.class);
                        startActivity(inte);
                    }
                }
            }
        });

        detectThread.start();
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

}
