package com.app.deliciaefoco.deliciaefoco.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.deliciaefoco.deliciaefoco.Interfaces.LotProductInterface;
import com.app.deliciaefoco.deliciaefoco.R;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ProductGridViewAdapter extends BaseAdapter implements Filterable {
    private Context context;
    private ArrayList<LotProductInterface> list;
    private ArrayList<LotProductInterface> mList;
    CustomFilter filter;
    private final String baseUrl = "http://portal.deliciaefoco.com.br/api/product/image/";

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
        return this.list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.list.get(position).id;
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
        textView.setText(list.get(position).product.name);
        textPrice.setText(formatMoney(Float.parseFloat(list.get(position).product.price)));
        Picasso.get().load(baseUrl + list.get(position).product.id).into(imageView);

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
                    if (obj.product.name.toLowerCase().contains(constraint)) {
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
    }
}