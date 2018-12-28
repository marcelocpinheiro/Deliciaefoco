package com.app.deliciaefoco.deliciaefoco.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.app.deliciaefoco.deliciaefoco.Interfaces.SaleOrderInterface;
import com.app.deliciaefoco.deliciaefoco.R;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by marcelo on 09/07/18.
 */

public class SaleListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<SaleOrderInterface> list;

    public SaleListAdapter(Context ctx, ArrayList<SaleOrderInterface> orders){
        this.context = ctx;
        this.list = orders;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(convertView == null){
            convertView = inflater.inflate(R.layout.list_sale, null);
        }

        TextView txtTitle = (TextView) convertView.findViewById(R.id.txtSaleTitle);
        TextView txtValue = (TextView) convertView.findViewById(R.id.txtSaleValue);

        txtTitle.setText("Compra de "+list.get(position).created_at);
        txtValue.setText("Valor total: "+formatMoney(list.get(position).total_value));

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
