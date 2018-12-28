package com.app.deliciaefoco.deliciaefoco.Adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.app.deliciaefoco.deliciaefoco.Interfaces.Product;
import com.app.deliciaefoco.deliciaefoco.R;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Created by marcelo on 25/04/18.
 */

public class CartAdapter extends BaseAdapter {
    private final List<Product> produtos;
    private final Activity act;

    public CartAdapter(List<Product> produtos, Activity ac){
        this.produtos = produtos;
        this.act = ac;
    }

    @Override
    public int getCount() {
        return produtos.size();
    }

    @Override
    public Object getItem(int position) {
        return produtos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return produtos.get(position).getId();
    }

    public float getTotalPrice(){
        float ret = 0;
        for(int i = 0; i < produtos.size(); i++){
            ret += (produtos.get(i).price * produtos.get(i).getQuantity());
        }
        return ret;
    }

    public List<Product> getProducts(){
        return this.produtos;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ArrayAdapter<Product> adapter = new ArrayAdapter<Product>(act, R.layout.cart_item, produtos);

        if(convertView == null){
            convertView = act.getLayoutInflater()
                    .inflate(R.layout.cart_item, parent, false);
        }

        final int index = position;
        TextView txtNome = (TextView) convertView.findViewById(R.id.lista_produto_nome);
        TextView txtDesc1 = (TextView) convertView.findViewById(R.id.lista_produto_linha1);
        TextView txtDesc2 = (TextView) convertView.findViewById(R.id.lista_produto_linha2);
        Button removeItem = (Button) convertView.findViewById(R.id.btnRemoveItem);
        Button btnIncrease = (Button) convertView.findViewById(R.id.btnIncreaseQuantity);
        Button btnDecrease = (Button) convertView.findViewById(R.id.btnDecreaseQuantity);

        txtNome.setText(produtos.get(index).getQuantity() + " X " + produtos.get(index).getName());
        txtDesc1.setText("Valor Unitário: " + formatMoney(produtos.get(index).getPrice()));
        txtDesc2.setText("Valor Total: " + formatMoney(produtos.get(index).calculateTotalValue()));

        btnIncrease.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(produtos.get(index).getQuantity() < produtos.get(index).maxQuantity){
                    produtos.get(index).setQuantity(produtos.get(index).getQuantity() + 1);
                    notifyDataSetChanged();
                    notifyDataSetInvalidated();
                }
            }
        });

        btnDecrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(produtos.get(index).getQuantity() > 1){
                    produtos.get(index).setQuantity(produtos.get(index).getQuantity() - 1);
                    notifyDataSetChanged();
                    notifyDataSetInvalidated();
                }
            }
        });

        removeItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(act);

                builder
                        .setMessage("Deseja realmente remover o produto "+produtos.get(index).getName()+" ?")
                        .setPositiveButton("Sim",  new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                produtos.remove(index);
                                notifyDataSetChanged();
                                notifyDataSetInvalidated();
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


        return convertView;
    }




    private String formatMoney(double value){
        BigDecimal valor = new BigDecimal (value);
        Locale ptBr = new Locale("pt", "BR");
        NumberFormat nf = NumberFormat.getCurrencyInstance(ptBr);
        String formatado = nf.format (valor);
        return formatado;
    }

    @Nullable
    @Override
    public CharSequence[] getAutofillOptions() {
        return new CharSequence[0];
    }
}
