package es.apagatv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String[] HOME = {"Samsung", "LG", "Sony", "Xiaomi", "Philips"};
    private static final int BG = Color.rgb(11, 19, 32);
    private static final int PANEL = Color.rgb(29, 42, 61);
    private static final int WHITE = Color.rgb(237, 241, 249);
    private static final int DIM = Color.rgb(167, 184, 200);
    private static final int RED = Color.rgb(237, 80, 97);
    private static final int GREEN = Color.rgb(131, 223, 172);
    private SharedPreferences prefs;
    private ConsumerIrManager ir;
    private IrCatalog catalog;
    private final Map<String, CheckBox> checked = new LinkedHashMap<>();
    private Button bigButton;
    private TextView status;
    private TextView stats;
    private LinearLayout otherBrands;
    private boolean sending;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("settings_v2", Context.MODE_PRIVATE);
        ir = (ConsumerIrManager)getSystemService(CONSUMER_IR_SERVICE);
        catalog = new IrCatalog(this);
        boolean ready = ir != null && ir.hasIrEmitter();
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(34));
        sv.addView(root);

        add(root, label("APAGA TV   ·   INFRARROJOS", 13, GREEN, true), 11);
        add(root, label("Un botón. Muchas teles.", 29, WHITE, true), 8);
        add(root, label("Envía códigos de apagado exclusivos de las marcas seleccionadas. No hace falta elegir un mando.", 15, DIM, false), 19);
        bigButton = button("⏻   APAGAR TODAS", RED, 21);
        bigButton.setEnabled(ready);
        addHeight(root, bigButton, 100, 12);
        status = label(ready ? "Emisor infrarrojo disponible" : "Este móvil no expone el emisor infrarrojo a Android", 14, ready ? GREEN : RED, false);
        status.setGravity(Gravity.CENTER);
        add(root, status, 7);
        stats = label("Catálogo: " + catalog.offTotal() + " apagados específicos · " +
                catalog.toggleTotal() + " botones POWER para prueba", 13, DIM, false);
        stats.setGravity(Gravity.CENTER);
        add(root, stats, 23);
        add(root, label("TUS MARCAS", 13, GREEN, true), 8);
        add(root, label("Selecciona las que quieres incluir en el botón universal.", 14, DIM, false), 10);
        for (String brand : HOME) {
            addBrand(root, brand, ready);
        }
        add(root, label("OTRAS MARCAS", 13, GREEN, true), 9);
        Button expand = button("Ver más marcas ▼", PANEL, 16);
        addHeight(root, expand, 52, 10);
        otherBrands = new LinearLayout(this);
        otherBrands.setOrientation(LinearLayout.VERTICAL);
        otherBrands.setVisibility(View.GONE);
        add(root, otherBrands, 12);
        for (String brand : catalog.brands()) {
            if (!Arrays.asList(HOME).contains(brand)) addBrand(otherBrands, brand, ready);
        }
        expand.setOnClickListener(v -> {
            if (otherBrands.getVisibility() == View.GONE) {
                otherBrands.setVisibility(View.VISIBLE);
                expand.setText("Ocultar otras marcas ▲");
            } else {
                otherBrands.setVisibility(View.GONE);
                expand.setText("Ver más marcas ▼");
            }
        });
        add(root, label("CÓMO FUNCIONA", 13, GREEN, true), 9);
        add(root, label("El botón universal solo envía códigos identificados como OFF. " +
                "Los códigos POWER que alternan encendido y apagado se pueden probar por separado, " +
                "pero nunca se envían automáticamente.\n\n" +
                "Pulsa «Probar» en una marca y recorre sus códigos. Si uno apaga tu televisor, " +
                "puedes guardarlo para que el botón principal lo priorice. Puedes guardar " +
                "más de uno para televisores diferentes de la misma marca.\n\n" +
                "La señal infrarroja necesita apuntar al receptor. No atraviesa paredes. " +
                "La compatibilidad depende del modelo. Fuentes: Flipper-IRDB (CC0), " +
                "Tasmota, RemoteCentral y códigos LG webOS publicados.", 13, DIM, false), 10);
        bigButton.setOnClickListener(v -> sendAll());
        setContentView(sv);
    }

    private void addBrand(LinearLayout root, String brand, boolean ready) {
        int nOff = catalog.count(brand, false);
        int nToggle = catalog.count(brand, true);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(7), dp(13), dp(10));
        card.setBackground(shape(PANEL, 14));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(row);
        CheckBox cb = new CheckBox(this);
        cb.setText(brand);
        cb.setTextColor(WHITE);
        cb.setTextSize(18);
        cb.setButtonTintList(ColorStateList.valueOf(GREEN));
        boolean own = Arrays.asList(HOME).contains(brand);
        cb.setChecked(prefs.getBoolean("brand:"+brand, own && nOff > 0));
        cb.setOnCheckedChangeListener((b, value) -> prefs.edit().putBoolean("brand:"+brand, value).apply());
        row.addView(cb, new LinearLayout.LayoutParams(0, dp(51), 1f));
        checked.put(brand, cb);
        Button test = button("Probar", Color.rgb(60, 74, 97), 14);
        test.setEnabled(ready && (nOff+nToggle>0));
        row.addView(test, new LinearLayout.LayoutParams(dp(92), dp(43)));
        test.setOnClickListener(v -> openTester(brand));
        TextView sub = label(nOff + " apagados específicos · " + nToggle + " POWER (alternativos)", 12, DIM, false);
        add(card, sub, 2);
        if (nOff == 0) {
            add(card, label("Sin OFF independiente: no se enviará desde «Apagar todas».", 12, RED, false), 3);
        }
        add(root, card, 9);
    }

    private void openTester(String brand) {
        List<IrCatalog.Entry> discrete = catalog.get(brand, false);
        List<IrCatalog.Entry> power = catalog.get(brand, true);
        final boolean[] isPower = {false};
        final int[] index = {0};
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(10));
        TextView desc = label("", 15, Color.DKGRAY, false);
        add(box, desc, 13);
        Button send = button("Enviar código", RED, 16);
        addHeight(box, send, 56, 11);
        Button next = button("Siguiente código →", PANEL, 16);
        addHeight(box, next, 53, 12);
        Button save = button("✓ Guardar este OFF", Color.rgb(26, 128, 84), 15);
        addHeight(box, save, 53, 12);
        Button switchMode = button("Ver botones POWER alternativos", PANEL, 14);
        addHeight(box, switchMode, 53, 8);
        final Runnable[] redraw = new Runnable[1];
        redraw[0] = () -> {
            List<IrCatalog.Entry> list = isPower[0] ? power : discrete;
            if (index[0] >= list.size()) index[0] = 0;
            if (list.isEmpty()) {
                desc.setText(isPower[0] ? "No hay códigos POWER registrados." :
                        "No hay códigos OFF específicos. Prueba los POWER por separado, con la TV encendida.");
            } else {
                IrCatalog.Entry e = list.get(index[0]);
                desc.setText((isPower[0] ? "⚠ POWER: puede encender la televisión" : "APAGADO INDEPENDIENTE") +
                        "\nCódigo " + (index[0]+1) + " de " + list.size() +
                        "\nOrigen/modelo: " + e.model +
                        (isSaved(brand, e) ? "\n✓ Guardado" : ""));
            }
            send.setEnabled(!list.isEmpty());
            next.setEnabled(list.size() > 1);
            save.setVisibility(!isPower[0] && !list.isEmpty() ? View.VISIBLE : View.GONE);
            switchMode.setText(isPower[0] ? "Volver a apagados independientes" : "Ver POWER alternativos (con riesgo)");
        };
        send.setOnClickListener(v -> {
            List<IrCatalog.Entry> list = isPower[0] ? power : discrete;
            if (list.isEmpty()) return;
            IrCatalog.Entry e = list.get(index[0]);
            sendOne(e);
            desc.setText(desc.getText() + "\n→ Señal enviada; comprueba la TV.");
        });
        next.setOnClickListener(v -> {
            List<IrCatalog.Entry> list = isPower[0] ? power : discrete;
            if (!list.isEmpty()) index[0] = (index[0]+1)%list.size();
            redraw[0].run();
        });
        save.setOnClickListener(v -> {
            List<IrCatalog.Entry> list = discrete;
            if (list.isEmpty()) return;
            saveWorking(brand,list.get(index[0]));
            redraw[0].run();
        });
        switchMode.setOnClickListener(v -> {
            isPower[0] = !isPower[0];
            index[0] = 0;
            redraw[0].run();
        });
        redraw[0].run();
        new AlertDialog.Builder(this).setTitle("Probar " + brand)
            .setView(box).setPositiveButton("Cerrar", null).show();
    }

    private String prefKey(String brand) { return "working_off:" + brand; }
    private boolean isSaved(String brand, IrCatalog.Entry e) {
        return prefs.getStringSet(prefKey(brand), Collections.emptySet()).contains(e.key);
    }
    private void saveWorking(String brand, IrCatalog.Entry e) {
        Set<String> keys = new LinkedHashSet<>(prefs.getStringSet(prefKey(brand), Collections.emptySet()));
        keys.add(e.key);
        prefs.edit().putStringSet(prefKey(brand), keys).apply();
    }

    private void sendOne(IrCatalog.Entry e) {
        if (sending || ir == null || !ir.hasIrEmitter()) return;
        sending = true;
        new Thread(() -> {
            try { send(e.signal); }
            catch (Exception ignored) { }
            runOnUiThread(() -> { sending=false; });
        }, "tv-ir-probe").start();
    }

    private void sendAll() {
        if (sending) return;
        final List<IrCatalog.Entry> queue = new ArrayList<>();
        int withoutOff = 0;
        for (Map.Entry<String,CheckBox> item: checked.entrySet()) {
            if (!item.getValue().isChecked()) continue;
            String brand = item.getKey();
            List<IrCatalog.Entry> entries = catalog.get(brand, false);
            if (entries.isEmpty()) { withoutOff++; continue; }
            Set<String> saved = prefs.getStringSet(prefKey(brand), Collections.emptySet());
            int old = queue.size();
            if (!saved.isEmpty()) {
                for (IrCatalog.Entry e : entries)
                    if (saved.contains(e.key)) queue.add(e);
            }
            if (queue.size() == old) queue.addAll(entries);
        }
        if (queue.isEmpty()) {
            status.setTextColor(RED);
            status.setText("No hay códigos OFF disponibles para las marcas elegidas.");
            return;
        }
        final int excluded = withoutOff;
        bigButton.setEnabled(false);
        sending = true;
        status.setTextColor(GREEN);
        status.setText("Enviando " + queue.size() + " códigos… Mantén apuntado el móvil.");
        new Thread(() -> {
            int completed = 0;
            for (IrCatalog.Entry e : queue) {
                try { send(e.signal); completed++; }
                catch (Exception ignored) { }
                if (completed % 8 == 0) {
                    final int sent = completed;
                    runOnUiThread(() -> status.setText("Enviados " + sent + "/" + queue.size() + ". Sigue apuntando…"));
                }
                try { Thread.sleep(90); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
            }
            final int sent = completed;
            runOnUiThread(() -> {
                sending = false;
                bigButton.setEnabled(ir != null && ir.hasIrEmitter());
                status.setTextColor(GREEN);
                status.setText("Enviados " + sent + " de " + queue.size() + " códigos OFF." +
                        (excluded > 0 ? " " + excluded + " marca(s) sin OFF independiente." : ""));
            });
        }, "tv-ir-all").start();
    }

    private void send(IrCodes.Signal signal) {
        if (ir == null || !ir.hasIrEmitter()) return;
        int freq = nearestFrequency(signal.hertz);
        int reps = signal.name.startsWith("SIRC") || signal.name.startsWith("Sony") ? 3 : 1;
        for (int i = 0; i < reps; i++) ir.transmit(freq, signal.pattern);
    }
    private int nearestFrequency(int hz) {
        try {
            ConsumerIrManager.CarrierFrequencyRange[] ranges = ir.getCarrierFrequencies();
            if (ranges == null || ranges.length == 0) return hz;
            int best = hz, delta = Integer.MAX_VALUE;
            for (ConsumerIrManager.CarrierFrequencyRange range : ranges) {
                int target = Math.max(range.getMinFrequency(), Math.min(range.getMaxFrequency(), hz));
                if (Math.abs(target-hz)<delta) { delta=Math.abs(target-hz); best=target; }
            }
            return best;
        } catch (RuntimeException ex) { return hz; }
    }

    private TextView label(String s,int size,int color,boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);tv.setTextSize(size);tv.setTextColor(color);
        if (bold) tv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return tv;
    }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private Button button(String title, int color, int textSize) {
        Button btn = new Button(this);btn.setText(title);btn.setTextSize(textSize);
        btn.setTextColor(Color.WHITE);btn.setAllCaps(false);
        btn.setBackground(shape(color,14));return btn;
    }
    private void add(LinearLayout root,View v,int gap) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(gap);root.addView(v,p);
    }
    private void addHeight(LinearLayout root,View v,int height,int gap) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,dp(height));
        p.bottomMargin=dp(gap);root.addView(v,p);
    }
    private int dp(int size) { return (int)(getResources().getDisplayMetrics().density * size+0.5f); }
}
