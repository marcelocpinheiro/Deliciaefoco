package com.app.deliciaefoco.deliciaefoco.Adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.deliciaefoco.deliciaefoco.Interfaces.LotProductInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ProductInterface;
import com.app.deliciaefoco.deliciaefoco.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by marcelo on 09/07/18.
 */

public class ProductListAdapter extends BaseAdapter {
    ArrayList<ProductInterface> products;
    Context context;
    String FILENAME = "DEFAULT_COMPANY";

    public ProductListAdapter(ArrayList<ProductInterface> prod, Context ctx ){
        this.products = prod;
        this.context = ctx;
    }

    @Override
    public int getCount() {
        return products.size();
    }

    private String getBaseUrl(){
        SharedPreferences settings = context.getSharedPreferences(FILENAME, 0);
        return settings.getString("base_url", "") + "/product/image/";
    }

    @Override
    public Object getItem(int position) {
        return products.get(position);
    }

    @Override
    public long getItemId(int position) {
        return products.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(convertView == null){
            convertView = inflater.inflate(R.layout.grid_single, null);
        }

        final View convertViewfinal = inflater.inflate(R.layout.grid_single, null);
        final ProductInterface productFinal = products.get(position);

        TextView textView = (TextView) convertView.findViewById(R.id.grid_text);
        ImageView imageView = (ImageView)convertView.findViewById(R.id.grid_image);
        TextView textPrice = (TextView) convertView.findViewById(R.id.grid_price);
        textView.setText(productFinal.name);
        textPrice.setText(formatMoney(Float.parseFloat(productFinal.price)) + " X " + productFinal.quantity);
        Picasso.with(context)
                .load(this.getBaseUrl() + productFinal.id)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError() {
                        ImageView imageView = (ImageView)convertViewfinal.findViewById(R.id.grid_image);
                        Picasso.with(context)
                                .load(getBaseUrl() + productFinal.id)
                                .into(imageView, new Callback() {
                                    @Override
                                    public void onSuccess() {

                                    }

                                    @Override
                                    public void onError() {
                                        Log.e("ERROR_PICASSO", "Falha ao carregar imagem do produto " + productFinal.name);
                                    }
                                });
                    }
                });

        return convertView;
    }

    private String formatMoney(double value){
        BigDecimal valor = new BigDecimal (value);
        Locale ptBr = new Locale("pt", "BR");
        NumberFormat nf = NumberFormat.getCurrencyInstance(ptBr);
        String formatado = nf.format (valor);
        return formatado;
    }
}
