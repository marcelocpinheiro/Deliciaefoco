package com.app.deliciaefoco.deliciaefoco.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.deliciaefoco.deliciaefoco.Interfaces.ProductInterface;
import com.app.deliciaefoco.deliciaefoco.R;
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
    private final String baseUrl = "http://portal.deliciaefoco.com.br/api/product/image/";

    public ProductListAdapter(ArrayList<ProductInterface> prod, Context ctx ){
        this.products = prod;
        this.context = ctx;
    }

    @Override
    public int getCount() {
        return products.size();
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

        TextView textView = (TextView) convertView.findViewById(R.id.grid_text);
        ImageView imageView = (ImageView)convertView.findViewById(R.id.grid_image);
        TextView textPrice = (TextView) convertView.findViewById(R.id.grid_price);
        textView.setText(products.get(position).name);
        textPrice.setText(formatMoney(Float.parseFloat(products.get(position).price)) + " X " + products.get(position).quantity);
        Picasso.get().load(baseUrl + products.get(position).id).into(imageView);

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
