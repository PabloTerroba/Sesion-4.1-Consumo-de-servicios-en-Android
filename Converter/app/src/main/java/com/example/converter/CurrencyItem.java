package com.example.converter;

public class CurrencyItem {
    private String code;      // Código ISO de la moneda (ej: USD)
    private int flagResId;    // ID del recurso de la bandera

    public CurrencyItem(String code, int flagResId) {
        this.code = code;
        this.flagResId = flagResId;
    }

    public String getCode() {
        return code;
    }

    public int getFlagResId() {
        return flagResId;
    }
}








