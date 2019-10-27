package com.app.deliciaefoco.deliciaefoco.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.app.deliciaefoco.deliciaefoco.Providers.ApiProvider;
import com.app.deliciaefoco.deliciaefoco.Interfaces.LotProductInterface;
import com.app.deliciaefoco.deliciaefoco.NumberPickerDialog;
import com.app.deliciaefoco.deliciaefoco.Interfaces.Product;
import com.app.deliciaefoco.deliciaefoco.Adapters.ProductGridViewAdapter;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ProductInterface;
import com.app.deliciaefoco.deliciaefoco.Providers.DialogProvider;
import com.app.deliciaefoco.deliciaefoco.Providers.UtilitiesProvider;
import com.app.deliciaefoco.deliciaefoco.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class StoreActivity extends AppCompatActivity implements NumberPicker.OnValueChangeListener {
    private AlertDialog alerta;
    Context context = this;
    List<Product> cart;
    Product currentProduct;
    HashMap<String ,Integer> hmLang;
    JSONArray products;
    String FILENAME = "DEFAULT_COMPANY";
    private Integer enterpriseId = 0;
    private int lastInteractionTime;
    ProductGridViewAdapter adapter;
    ArrayList<LotProductInterface> lots, lotsToSend;
    EditText txtSearch;
    Button btnCart, btnEsvaziar;
    private DialogProvider dialog;
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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


        dialog = new DialogProvider(this);
        SharedPreferences settings = getSharedPreferences(FILENAME, 0);
        this.enterpriseId = settings.getInt("enterprise_id", 0);


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

        btnEsvaziar = (Button) findViewById(R.id.btnEsvaziar);
        btnEsvaziar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cart.clear();
                btnCart.setText("Carrinho");
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

    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }

    @Override
    protected void onResume (){
        super.onResume();
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
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


    private void getProducts(){
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Carregando ...");
        progress.setMessage("Aguarde enquanto carregamos os produtos");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        ApiProvider api = new ApiProvider(this);
        api.getEnterpriseMenu(enterpriseId, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    distributeProducts(response);
                    progress.dismiss();
                } catch (JSONException e) {
                    UtilitiesProvider.trackException(e);
                    dialog.dialogShow("Erro", "Falha ao buscar produtos. Por favor, tente novamente.", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            UtilitiesProvider.backToHome(context);
                        }
                    });
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                dialog.dialogShow("Erro", "Falha ao buscar produtos \n" + error.getMessage(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UtilitiesProvider.backToHome(context);
                    }
                });
                progress.dismiss();
            }
        });
    }

    private void distributeProducts(JSONArray products) throws JSONException{
        GridView gv = (GridView) findViewById(R.id.gridViewStore);
        final ArrayList<LotProductInterface> array = new ArrayList<LotProductInterface>();
        for(int i = 0; i < products.length(); i++){
            LotProductInterface obj = new LotProductInterface();
            ProductInterface productInterface = new ProductInterface();


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
