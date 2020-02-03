package com.app.deliciaefoco.deliciaefoco.Adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.deliciaefoco.deliciaefoco.Interfaces.LotProductInterface;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ProductInterface;
import com.app.deliciaefoco.deliciaefoco.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ProductGridViewAdapter extends BaseAdapter implements Filterable {
    private Context context;
    private ArrayList<LotProductInterface> list;
    private ArrayList<LotProductInterface> mList;
    CustomFilter filter;
    private int currentPosition = 0;
    String FILENAME = "DEFAULT_COMPANY";

    public ProductGridViewAdapter(ArrayList<LotProductInterface> list, Context context){
        this.context = context;
        this.list = list;
        this.mList = list;
    }

    @Override
    public int getCount() {
        return this.list.size();
    }

    @Override
    public LotProductInterface getItem(int position) {
        if(!this.list.isEmpty()) return this.list.get(position);
        return null;
    }

    @Override
    public long getItemId(int position) {
        return this.list.get(position).id;
    }

    private String getBaseUrl(){
        SharedPreferences settings = context.getSharedPreferences(FILENAME, 0);
        return settings.getString("base_url", "") + "/product/image/";
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        currentPosition = position;

        if(convertView == null){
            convertView = inflater.inflate(R.layout.grid_single, null);
        }

        final View convertViewfinal = inflater.inflate(R.layout.grid_single, null);
        final LotProductInterface productFinal = list.get(position);

        TextView textView = (TextView) convertView.findViewById(R.id.grid_text);
        ImageView imageView = (ImageView)convertView.findViewById(R.id.grid_image);
        TextView textPrice = (TextView) convertView.findViewById(R.id.grid_price);
        textView.setText(productFinal.product.name);

        textPrice.setText(formatMoney(Float.parseFloat(productFinal.product.price)));

        Picasso.with(context)
                .load(this.getBaseUrl() + productFinal.product.id)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError() {
                        ImageView imageView = (ImageView)convertViewfinal.findViewById(R.id.grid_image);
                        Picasso.with(context)
                                .load(getBaseUrl() + productFinal.product.id)
                                .into(imageView, new Callback() {
                                    @Override
                                    public void onSuccess() {

                                    }

                                    @Override
                                    public void onError() {
                                        Log.e("ERROR_PICASSO", "Falha ao carregar imagem do produto " + productFinal.product.name);
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

    @Override
    public Filter getFilter() {
        if(filter == null){
            filter = new CustomFilter();
        }

        return filter;
    }

    class CustomFilter extends Filter{

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            FilterResults results = new FilterResults();

            if(constraint != null && constraint.length() > 0){
                constraint = constraint.toString().toLowerCase();
                ArrayList<LotProductInterface> filters = new ArrayList<>();

                for(int i = 0; i < mList.size(); i++){
                    LotProductInterface obj = mList.get(i);

                    if (obj.product.name.toLowerCase().contains(constraint) ||
                            this.removerAcentos(obj.product.name.toLowerCase()).contains(constraint)) {
                        filters.add(obj);
                    }

                    Log.d("Pesquisa", constraint + " / " + obj.product.barcode);
                    if (obj.product.barcode.toLowerCase().equals(constraint)){
                        filters.add(obj);
                    }
                }

                results.count = filters.size();
                results.values = filters;
            }else{
                results.count = mList.size();
                results.values = mList;
            }


            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            list = (ArrayList<LotProductInterface>) results.values;
            notifyDataSetChanged();
        }

        private String removerAcentos(String str) {
            return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        }
    }
}