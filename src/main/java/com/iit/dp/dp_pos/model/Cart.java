package com.iit.dp.dp_pos.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Cart {
    private static Cart instance;
    private final Map<Product, Integer> items;

    private Cart() {
        items = new LinkedHashMap<>();
    }

    public static Cart getInstance() {
        if (instance == null) {
            instance = new Cart();
        }
        return instance;
    }

    public void addProduct(Product product) {
        items.put(product, items.getOrDefault(product, 0) + 1);
    }

    public void removeProduct(Product product) {
        items.remove(product);
    }

    public void clear() {
        items.clear();
    }

    public Map<Product, Integer> getItems() {
        return items;
    }
} 