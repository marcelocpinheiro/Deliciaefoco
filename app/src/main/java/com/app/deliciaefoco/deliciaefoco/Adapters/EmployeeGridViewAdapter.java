package com.app.deliciaefoco.deliciaefoco.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.app.deliciaefoco.deliciaefoco.Interfaces.EmployeeInterface;
import com.app.deliciaefoco.deliciaefoco.R;

import java.util.ArrayList;

/**
 * Created by marcelo on 23/06/18.
 */

public class EmployeeGridViewAdapter extends BaseAdapter implements Filterable {
    private Context context;
    private ArrayList<EmployeeInterface> employees;
    private ArrayList<EmployeeInterface> employees_filter;
    CustomFilterEmployee filter;

    public EmployeeGridViewAdapter(ArrayList<EmployeeInterface> comingEmployees, Context ctx){
        this.employees = comingEmployees;
        this.employees_filter = comingEmployees;
        this.context = ctx;
    }


    @Override
    public int getCount() {
        return this.employees.size();
    }

    @Override
    public Object getItem(int position) {
        return this.employees.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.employees.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(convertView == null){
            convertView = inflater.inflate(R.layout.employee_grid, null);
        }

        TextView txtName = (TextView) convertView.findViewById(R.id.txtEmployeeName);
        TextView txtEmail = (TextView) convertView.findViewById(R.id.txtEmployeeEmail);

        txtName.setText(this.employees.get(position).nome);
        txtEmail.setText(this.employees.get(position).email);

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if(filter == null){
            filter = new CustomFilterEmployee();
        }

        return filter;
    }

    class CustomFilterEmployee extends Filter{

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            ArrayList<EmployeeInterface> filters = new ArrayList<>();
            if(constraint != null && constraint.length() > 0) {
                constraint = constraint.toString().toLowerCase();
                for(int i = 0; i < employees_filter.size(); i++){
                    EmployeeInterface obj = employees_filter.get(i);
                    if(obj.nome.toLowerCase().contains(constraint) || obj.email.toLowerCase().contains(constraint)){
                        filters.add(obj);
                    }
                }

                results.count = filters.size();
                results.values = filters;
            }else{
                results.count = employees_filter.size();
                results.values = employees_filter;
            }


            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            employees = (ArrayList<EmployeeInterface>) results.values;
            notifyDataSetChanged();
        }
    }
}
