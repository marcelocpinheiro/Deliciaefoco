package com.app.deliciaefoco.deliciaefoco.Interfaces;

import org.json.JSONArray;

import java.io.Serializable;

/**
 * Created by marcelo on 25/04/18.
 */

public class Product{
    public String name;
    public int id;
    public int product_id;
    public int quantity;
    public double price;
    public int maxQuantity;

    public Product(){}

    public double calculateTotalValue(){
        return this.quantity * this.price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
