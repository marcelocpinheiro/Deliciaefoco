package com.app.deliciaefoco.deliciaefoco.Providers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.deliciaefoco.deliciaefoco.Interfaces.ConcludeInterface;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;


public class ApiProvider {
    private RequestQueue requestQueue;
    private Context context;
    private final String FILENAME = "DEFAULT_COMPANY";
    Gson gson;

    public ApiProvider(Context context){
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(this.context);
        this.gson = new Gson();
    }

    public void getEnterpriseMenu(int enterpriseId, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener){
        JsonArrayRequest jar = new JsonArrayRequest(Request.Method.GET,
                this.getBaseUrl() + "/enterprise/"+enterpriseId+"/products",
                null,
                listener,
                errorListener);
        this.requestQueue.add(jar);
    }

    public void getEmployees(int enterpriseId, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener){
        JsonArrayRequest jar = new JsonArrayRequest(Request.Method.GET,
                this.getBaseUrl() + "/enterprise/"+enterpriseId+"/employees",
                null,
                listener,
                errorListener);
        this.requestQueue.add(jar);
    }

    public void authenticateUser(String email, String password, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) throws JSONException {
        final JSONObject jsonBody = new JSONObject("{\"email\":\""+email+"\", \"password\":\""+password+"\"}");
        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST,
                this.getBaseUrl()+ "/checkuser",
                jsonBody,
                listener,
                errorListener);
        this.requestQueue.add(jor);
    }

    public void payOff(int userId, boolean isCarteira, ArrayList<ConcludeInterface> arrayConclude, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) throws JSONException{
        String body = "{\"user_id\":\"" + userId + "\", \"carteira\": \"" + isCarteira + "\",  \"products\":" + gson.toJson(arrayConclude) + "}";
        Log.d("Body", body);
        final JSONObject compraRequestBody = new JSONObject(body);
        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST,
                this.getBaseUrl() + "/saveSale",
                compraRequestBody,
                listener,
                errorListener);
        this.requestQueue.add(jor);
    }

    public void preSale(int userId, ArrayList<ConcludeInterface> arrayConclude, int method, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) throws JSONException, IOException {
        JSONObject compraRequestBody = new JSONObject("{\"user_id\":\"" + userId + "\", \"products\":" + gson.toJson(arrayConclude) + ", \"method\": " + method + ", \"enterprise_id\": " + this.getEnterpriseId() + "}");
        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST,
                this.getBaseUrl() + "/savePreSale",
                compraRequestBody,
                listener,
                errorListener);
        this.requestQueue.add(jor);
    }

    public void cancelSale(int saleId, Response.Listener<JSONObject> listener, Response.ErrorListener eListener) throws JSONException{
        JSONObject compraRequestBody = new JSONObject("{\"sale_id\":\""+saleId+"\"}");
        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST,
                this.getBaseUrl() + "/deletePreSale",
                compraRequestBody,
                listener,
                eListener);
        this.requestQueue.add(jor);

    }

    public void payment(int[] saleOrders, String date, String time, String brand, Response.Listener<JSONObject> listener, Response.ErrorListener eListener) throws JSONException {
        String saleOrdersArray = "[";
        for (int i = 0; i < saleOrders.length; i++) {
            if (saleOrdersArray.equals("[")) {
                saleOrdersArray = saleOrdersArray + saleOrders[i];
            } else {
                saleOrdersArray += saleOrdersArray + ", " + saleOrders[i];
            }
        }
        saleOrdersArray = saleOrdersArray + "]";
        String json = "{\"sale_order_ids\":" + saleOrdersArray + ", \"date\": \"" + date + "\", \"time\": \"" + time + "\", \"brand\": \"" + brand + "\"}";
        JSONObject compraRequestBody = new JSONObject(json);
        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST,
                this.getBaseUrl() + "/payment",
                compraRequestBody,
                listener,
                eListener);
        this.requestQueue.add(jor);
    }

    public void paymentMoney(int[] saleOrders, Response.Listener<JSONObject> listener, Response.ErrorListener eListener) throws JSONException {
        String saleOrdersArray = "[";
        for (int i = 0; i < saleOrders.length; i++) {
            if (saleOrdersArray.equals("[")) {
                saleOrdersArray = saleOrdersArray + saleOrders[i];
            } else {
                saleOrdersArray += saleOrdersArray + ", " + saleOrders[i];
            }
        }
        saleOrdersArray = saleOrdersArray + "]";
        String json = "{\"sale_order_ids\":" + saleOrdersArray + ", \"money\": true}";
        JSONObject compraRequestBody = new JSONObject(json);
        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST,
                this.getBaseUrl() + "/payment",
                compraRequestBody,
                listener,
                eListener);
        this.requestQueue.add(jor);
    }

    private String getBaseUrl(){
        SharedPreferences settings = this.context.getSharedPreferences(FILENAME, 0);
        return settings.getString("base_url", "");
    }

    private int getEnterpriseId() throws IOException {
        SharedPreferences settings = this.context.getSharedPreferences(FILENAME, 0);
        return settings.getInt("enterprise_id", 0);
    }



}
