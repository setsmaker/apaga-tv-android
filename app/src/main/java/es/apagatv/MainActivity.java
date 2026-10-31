package es.apagatv;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Un botón envía los códigos de todas las marcas seleccionadas. */
public final class MainActivity extends Activity {
    private static final String[] BRANDS = {"Samsung", "LG", "Sony", "Philips", "Xiaomi"};
    private static final int BG = Color.rgb(12, 20, 34);
    private static final int PANEL = Color.rgb(26, 38, 57);
    private static final int WHITE = Color.rgb(234, 240, 248);
    private static final int GRAY = Color.rgb(166, 183, 201);
    private static final int RED = Color.rgb(237, 80, 97);
    private static final int GREEN = Color.rgb(126, 222, 173);
    private ConsumerIrManager ir;
    private SharedPreferences prefs;
    private final Map<String, CheckBox> selected = new LinkedHashMap<>();
    private Button mainButton;
    private TextView status;
    private TextView detail;
    private boolean sending = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        ir = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        prefs = getSharedPreferences("tv_brands", Context.MODE_PRIVATE);
        boolean ready = ir != null && ir.hasIrEmitter();
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(26), dp(22), dp(25));
        scroll.addView(root);

        add(root, label("APAGA TV  ·  IR UNIVERSAL", 13, GREEN, Typeface.BOLD), 15);
        add(root, label("Un botón.\nCinco marcas.", 32, WHITE, Typeface.BOLD), 9);
        add(root, label("Apunta el móvil hacia la televisión y pulsa el botón.", 15, GRAY, 0), 25);

        mainButton = new Button(this);
        mainButton.setText("⏻  APAGAR TODAS");
        mainButton.setTextColor(Color.WHITE);
        mainButton.setTextSize(21);
        mainButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mainButton.setAllCaps(false);
        mainButton.setBackground(bg(RED, 22));
        mainButton.setElevation(dp(6));
        mainButton.setEnabled(ready);
        addHeight(root, mainButton, 106, 18);

        status = label(ready ? "Emisor de infrarrojos disponible" :
                "Android no detecta un emisor infrarrojo compatible", 14,
                ready ? GREEN : RED, 0);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        add(root, status, 5);
        detail = label("", 13, GRAY, 0);
        detail.setGravity(Gravity.CENTER_HORIZONTAL);
        add(root, detail, 26);

        add(root, label("MARCAS", 13, GRAY, Typeface.BOLD), 10);
        add(root, label("Activa las que quieras. La selección se guarda en el móvil.",
                14, GRAY, 0), 11);
        for (String brand : BRANDS) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(12), dp(8), dp(12), dp(8));
            box.setBackground(bg(PANEL, 13));
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            box.addView(row);
            CheckBox checkbox = new CheckBox(this);
            checkbox.setText(brand);
            checkbox.setTextColor(WHITE);
            checkbox.setTextSize(18);
            checkbox.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));
            checkbox.setChecked(prefs.getBoolean(brand, !"Xiaomi".equals(brand)));
            checkbox.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(brand, checked).apply());
            row.addView(checkbox, new LinearLayout.LayoutParams(0, dp(52), 1f));
            selected.put(brand, checkbox);

            Button test = new Button(this);
            test.setText("Probar");
            test.setAllCaps(false);
            test.setTextColor(Color.WHITE);
            test.setBackground(bg(Color.rgb(58, 73, 95), 11));
            test.setEnabled(ready);
            row.addView(test, new LinearLayout.LayoutParams(dp(88), dp(42)));
            test.setOnClickListener(v -> {
                ArrayList<String> one = new ArrayList<>();
                one.add(brand); send(one);
            });
            if ("Xiaomi".equals(brand)) {
                add(box, label("Desmarcada inicialmente: POWER puede encender una TV apagada.",
                        12, GRAY, 0), 2);
            } else if ("Philips".equals(brand)) {
                add(box, label("Depende del modelo de televisión.", 12, GRAY, 0), 2);
            }
            add(root, box, 8);
        }
        add(root, label("ATENCIÓN", 13, GREEN, Typeface.BOLD), 8);
        add(root, label(
            "Primero prueba cada marca por separado. Xiaomi puede utilizar un código " +
            "que alterna encendido y apagado; solo actívala si has comprobado que te interesa. " +
            "La aplicación no sabe si una TV está encendida. Los infrarrojos requieren " +
            "apuntar hacia el receptor y no atraviesan paredes.",
            13, GRAY, 0), 8);

        mainButton.setOnClickListener(v -> {
            List<String> names = new ArrayList<>();
            for (String brand : BRANDS)
                if (selected.get(brand).isChecked()) names.add(brand);
            send(names);
        });
        setContentView(scroll);
    }

    private void send(List<String> brands) {
        if (sending) return;
        if (brands.isEmpty()) {
            status.setTextColor(RED);
            status.setText("Selecciona al menos una marca.");
            return;
        }
        if (ir == null || !ir.hasIrEmitter()) {
            status.setText("No se ha detectado el infrarrojo del móvil.");
            return;
        }
        final List<IrCodes.Signal> signals = new ArrayList<>();
        for (String name : brands) signals.addAll(IrCodes.forBrand(name));
        sending = true;
        mainButton.setEnabled(false);
        status.setTextColor(GREEN);
        status.setText("Enviando… mantén el móvil apuntando");
        detail.setText("");
        new Thread(() -> {
            int sent = 0;
            for (IrCodes.Signal sig : signals) {
                try { ir.transmit(frequency(sig.hertz), sig.pattern); sent++; }
                catch (RuntimeException ignored) { }
                try { Thread.sleep(95); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
            }
            final int successful = sent;
            runOnUiThread(() -> {
                sending = false;
                mainButton.setEnabled(ir != null && ir.hasIrEmitter());
                status.setText(successful == signals.size() ?
                        "Señales enviadas" : "No se pudieron enviar todas las señales");
                status.setTextColor(successful == signals.size() ? GREEN : RED);
                detail.setText(successful + " de " + signals.size() +
                        " señales enviadas. Comprueba la televisión.");
            });
        }, "infrared-sender").start();
    }

    private int frequency(int desired) {
        try {
            ConsumerIrManager.CarrierFrequencyRange[] ranges = ir.getCarrierFrequencies();
            if (ranges == null || ranges.length == 0) return desired;
            int best = desired, delta = Integer.MAX_VALUE;
            for (ConsumerIrManager.CarrierFrequencyRange r : ranges) {
                int selected = Math.max(r.getMinFrequency(), Math.min(r.getMaxFrequency(), desired));
                if (Math.abs(selected - desired) < delta) {
                    best = selected; delta = Math.abs(selected - desired);
                }
            }
            return best;
        } catch (RuntimeException ex) {
            return desired;
        }
    }
    private TextView label(String s, int size, int color, int style) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, style);
        return v;
    }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        return d;
    }
    private void add(LinearLayout root, View view, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(margin);
        root.addView(view, p);
    }
    private void addHeight(LinearLayout root, View view, int height, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(height));
        p.bottomMargin = dp(margin);
        root.addView(view, p);
    }
    private int dp(int n) {
        return (int) (getResources().getDisplayMetrics().density * n + .5f);
    }
}
