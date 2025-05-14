package com.example.converter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class CurrencyAdapter extends ArrayAdapter<CurrencyItem> {
    private LayoutInflater inflater;

    public CurrencyAdapter(Context context, List<CurrencyItem> items) {
        super(context, 0, items);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.spinner_item_currency, parent, false);
        ImageView imageViewFlag = view.findViewById(R.id.imageViewFlag);
        TextView textViewCode = view.findViewById(R.id.textViewCode);

        CurrencyItem item = getItem(position);
        imageViewFlag.setImageResource(item.getFlagResId());
        textViewCode.setText(item.getCode());

        return view;
    }
}








