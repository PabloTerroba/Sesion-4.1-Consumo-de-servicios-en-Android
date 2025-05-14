package com.example.converter;

// Importaciones necesarias
import android.os.Bundle;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.List;
import java.util.ArrayList;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    // Variable para el ratio de conversión actual
    private Double currentRate = 1.0;
    // Monedas seleccionadas por el usuario (por defecto EUR a USD)
    private String fromCurrency = "EUR";
    private String toCurrency = "USD";
    // Guarda el último ratio obtenido para mostrarlo en pantalla
    private String lastRatioText = "Ratio: --";

    // API_KEY de Fixer API, en nuestro caso
    private static final String API_KEY = "c69f7935bce32153c283e23df65dc05e";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Siempre primero: carga el layout correcto
        setContentView(R.layout.activity_main);

        // 2. Referencias a los Spinners del layout
        Spinner spinnerFrom = findViewById(R.id.spinnerFrom);
        Spinner spinnerTo = findViewById(R.id.spinnerTo);

        // 3. Crea la lista de monedas con sus banderas (debes tener las imágenes en res/drawable/)
        List<CurrencyItem> currencyItems = new ArrayList<>();
        currencyItems.add(new CurrencyItem("EUR", R.drawable.eu));
        currencyItems.add(new CurrencyItem("USD", R.drawable.us));
        currencyItems.add(new CurrencyItem("GBP", R.drawable.gb));
        currencyItems.add(new CurrencyItem("JPY", R.drawable.jp));
        currencyItems.add(new CurrencyItem("AUD", R.drawable.au));
        currencyItems.add(new CurrencyItem("CAD", R.drawable.ca));
        currencyItems.add(new CurrencyItem("CHF", R.drawable.ch));
        currencyItems.add(new CurrencyItem("CNY", R.drawable.cn));
        currencyItems.add(new CurrencyItem("MXN", R.drawable.mx));
        // ...añade más si quieres

        // 4. Crea el adapter personalizado para mostrar bandera y código
        CurrencyAdapter currencyAdapter = new CurrencyAdapter(this, currencyItems);

        // 5. Asigna el adapter a ambos Spinners
        spinnerFrom.setAdapter(currencyAdapter);
        spinnerTo.setAdapter(currencyAdapter);

        // 6. Variables para guardar las monedas seleccionadas
        // (deberían ser atributos de la clase para usarlas en otros métodos)
        fromCurrency = currencyItems.get(0).getCode(); // Por defecto la primera
        toCurrency = currencyItems.get(1).getCode();   // Por defecto la segunda

        // 7. Listeners para detectar cambios y actualizar el ratio
        spinnerFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fromCurrency = currencyItems.get(position).getCode();
                updateRate();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerTo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                toCurrency = currencyItems.get(position).getCode();
                updateRate();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 8. Botón para actualizar el ratio manualmente
        Button buttonUpdateRatio = findViewById(R.id.buttonUpdateRatio);
        buttonUpdateRatio.setOnClickListener(v -> updateRate());

        // 9. Botón para convertir la cantidad
        Button buttonConvert = findViewById(R.id.buttonConvert);
        buttonConvert.setOnClickListener(v -> convertCurrency());

        // 10. Inicializa el ratio al arrancar
        updateRate();
    }

    /**
     * Actualiza el ratio de conversión usando la API de Fixer.
     * Debido a la limitación del plan gratuito, siempre se usa base=EUR.
     * Se obtienen los ratios de ambas monedas respecto a EUR y se calcula el ratio entre ellas.
     */
    private void updateRate() {
        if (isNetworkAvailable()) {
            // Se piden los ratios de ambas monedas respecto a EUR
            String url = "http://data.fixer.io/api/latest?access_key=" + API_KEY +
                    "&symbols=" + fromCurrency + "," + toCurrency;
            new UpdateRateTask().execute(url);
        } else {
            Toast.makeText(this, "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
            ((TextView) findViewById(R.id.textViewRatio)).setText("Ratio: --");
        }
    }

    /**
     * Convierte la cantidad introducida usando el ratio actual y muestra el resultado.
     */
    private void convertCurrency() {
        EditText editTextAmount = findViewById(R.id.editTextAmount);
        TextView textViewResult = findViewById(R.id.textViewResult);
        String amountStr = editTextAmount.getText().toString();
        if (!amountStr.isEmpty()) {
            double amount = Double.parseDouble(amountStr);
            double result = amount * currentRate;
            textViewResult.setText(String.format("%.2f %s", result, toCurrency));
        } else {
            Toast.makeText(this, "Introduce una cantidad", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Comprueba si hay conexión a Internet.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Tarea asíncrona para obtener el ratio de conversión entre dos monedas usando Fixer API.
     * Debido a la limitación del plan gratuito, siempre se usa base=EUR.
     * Se calcula el ratio entre las monedas seleccionadas como rate_to / rate_from.
     */
    private class UpdateRateTask extends AsyncTask<String, Void, Double> {
        @Override
        protected Double doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    JSONObject json = new JSONObject(result.toString());

                    // Comprueba si la consulta fue exitosa
                    if (!json.getBoolean("success")) {
                        return null;
                    }

                    JSONObject rates = json.getJSONObject("rates");
                    // Obtenemos los ratios de ambas monedas respecto a EUR
                    double rateFrom = rates.getDouble(fromCurrency);
                    double rateTo = rates.getDouble(toCurrency);

                    // Calculamos el ratio entre las monedas seleccionadas
                    return rateTo / rateFrom;
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Double rate) {
            TextView textViewRatio = findViewById(R.id.textViewRatio);
            if (rate != null) {
                currentRate = rate;
                lastRatioText = String.format("Ratio: 1 %s = %.4f %s", fromCurrency, rate, toCurrency);
                textViewRatio.setText(lastRatioText);
                Toast.makeText(MainActivity.this, "Ratio actualizado: " + rate, Toast.LENGTH_SHORT).show();
            } else {
                lastRatioText = "Ratio: --";
                textViewRatio.setText(lastRatioText);
                Toast.makeText(MainActivity.this, "Error al obtener el ratio", Toast.LENGTH_SHORT).show();
            }
        }
    }
}



/*
package com.example.converter;

// Importaciones necesarias para Android y para la conexión HTTP/JSON
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Button;

import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class MainActivity extends AppCompatActivity {
    private Double currentRate = 1.0;
    private String fromCurrency = "EUR";
    private String toCurrency = "USD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinnerFrom = findViewById(R.id.spinnerFrom);
        Spinner spinnerTo = findViewById(R.id.spinnerTo);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.currencies, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        // Listeners para actualizar la moneda seleccionada y el ratio
        spinnerFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fromCurrency = parent.getItemAtPosition(position).toString();
                updateRate();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerTo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                toCurrency = parent.getItemAtPosition(position).toString();
                updateRate();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Botón para actualizar el ratio manualmente
        Button buttonUpdateRatio = findViewById(R.id.buttonUpdateRatio);
        buttonUpdateRatio.setOnClickListener(v -> updateRate());

        // Botón para convertir
        Button buttonConvert = findViewById(R.id.buttonConvert);
        buttonConvert.setOnClickListener(v -> convertCurrency());

        // Inicializa el ratio al arrancar
        updateRate();
    }

    // Método para actualizar el ratio
    private void updateRate() {
        if (isNetworkAvailable()) {
            String url = "http://data.fixer.io/api/latest?access_key=TU_API_KEY&symbols=" + toCurrency + "&base=" + fromCurrency;
            new UpdateRateTask().execute(url);
        } else {
            Toast.makeText(this, "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
            ((TextView) findViewById(R.id.textViewRatio)).setText("Ratio: --");
        }
    }

    // Método para convertir la moneda
    private void convertCurrency() {
        EditText editTextAmount = findViewById(R.id.editTextAmount);
        TextView textViewResult = findViewById(R.id.textViewResult);
        String amountStr = editTextAmount.getText().toString();
        if (!amountStr.isEmpty()) {
            double amount = Double.parseDouble(amountStr);
            double result = amount * currentRate;
            textViewResult.setText(String.format("%.2f %s", result, toCurrency));
        } else {
            Toast.makeText(this, "Introduce una cantidad", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para comprobar si hay conexión a Internet
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Método asociado al botón "A euros" mediante android:onClick en el layout
    public void onClickToEuros(View view) {
        EditText editTextDollars = findViewById(R.id.editTextDollars);
        EditText editTextEuros = findViewById(R.id.editTextEuros);

        String dollarsStr = editTextDollars.getText().toString();

        if (!dollarsStr.isEmpty()) {
            double dollars = Double.parseDouble(dollarsStr);
            // Convierte dólares a euros usando el ratio actualizado
            double euros = dollars / currentRate;
            editTextEuros.setText(String.format("%.2f", euros));
        } else {
            Toast.makeText(this, "Introduce una cantidad en dólares", Toast.LENGTH_SHORT).show();
        }
    }

    // Método asociado al botón "A dólares" mediante android:onClick en el layout
    public void onClickToDollars(View view) {
        EditText editTextEuros = findViewById(R.id.editTextEuros);
        EditText editTextDollars = findViewById(R.id.editTextDollars);

        String eurosStr = editTextEuros.getText().toString();

        if (!eurosStr.isEmpty()) {
            double euros = Double.parseDouble(eurosStr);
            // Convierte euros a dólares usando el ratio actualizado
            double dollars = euros * currentRate;
            editTextDollars.setText(String.format("%.2f", dollars));
        } else {
            Toast.makeText(this, "Introduce una cantidad en euros", Toast.LENGTH_SHORT).show();
        }
    }

    // Clase interna para realizar la consulta del ratio de conversión en segundo plano
    public class UpdateRateTask extends AsyncTask<String, Void, Double> {
        @Override
        protected Double doInBackground(String... urls) {
            try {
                // Crea la conexión HTTP a la URL proporcionada
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    // Lee la respuesta del servidor línea a línea
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    // Parsea la respuesta JSON
                    JSONObject json = new JSONObject(result.toString());
                    // Extrae el objeto "rates" y el valor de "USD"
                    JSONObject rates = json.getJSONObject("rates");
                    return rates.getDouble("USD");
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                // Si ocurre un error (red, parseo, etc.), lo imprime en el log y retorna null
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Double rate) {
            TextView textViewRatio = findViewById(R.id.textViewRatio);
            if (rate != null) {
                // Si se obtuvo el ratio correctamente, lo guarda y actualiza la interfaz
                MainActivity.this.currentRate = rate;
                textViewRatio.setText("Ratio: " + rate);
                Toast.makeText(MainActivity.this, "Ratio actualizado: " + rate, Toast.LENGTH_SHORT).show();
            } else {
                // Si hubo error, muestra un mensaje de error y actualiza la interfaz
                textViewRatio.setText("Ratio: --");
                Toast.makeText(MainActivity.this, "Error al obtener el ratio", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
*/