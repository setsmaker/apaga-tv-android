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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Supervised, single-shot IR remote candidate tester for a Soler & Palau HE-1800.
 * The catalogue has no known S&P code. Never broadcasts unattended, and no
 * batch-scanning: a heater must remain under the user's physical supervision.
 */
public final class HeaterScannerActivity extends Activity {
    private static final int BG=Color.rgb(12,19,31), WHITE=Color.rgb(238,243,247),
        MUTED=Color.rgb(173,191,207), GREEN=Color.rgb(117,214,165),
        RED=Color.rgb(232,80,91), PANEL=Color.rgb(31,46,65);
    private final List<Candidate> entries = new ArrayList<>();
    private ConsumerIrManager ir;
    private SharedPreferences prefs;
    private int position=0;
    private Candidate saved=null;
    private TextView counter, title, info, status;
    private Button send,save,next,previous,stored;
    private boolean busy=false;

    static final class Candidate {
        final String brand,model,button,id;
        final IrCodes.Signal signal;
        Candidate(String brand,String model,String button,IrCodes.Signal s) {
            this.brand=brand;this.model=model;this.button=button;this.signal=s;
            // Deterministic identifier between application updates.
            this.id=brand+"/"+model+"/"+button+"/"+s.hertz+"/"+Arrays.hashCode(s.pattern);
        }
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        ir=(ConsumerIrManager)getSystemService(CONSUMER_IR_SERVICE);
        prefs=getSharedPreferences("heater_scan_v1",Context.MODE_PRIVATE);
        load();
        String win=prefs.getString("working_code","");
        for(Candidate c:entries) if(c.id.equals(win)) saved=c;
        position=Math.max(0,Math.min(entries.size()-1,prefs.getInt("index",0)));
        boolean available=ir!=null&&ir.hasIrEmitter();

        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);
        root.setPadding(dp(21),dp(23),dp(21),dp(35));
        root.setOrientation(LinearLayout.VERTICAL);sc.addView(root);
        add(root,text("CALEFACTOR S&P HE-1800",14,GREEN,true),14);
        add(root,text("Buscador de mando IR",29,WHITE,true),11);
        add(root,text("No se conoce el código original del HE-1800. Esta herramienta prueba señales publicadas de mandos de OTROS calefactores. Puede que ninguna funcione.",15,MUTED,false),17);
        add(root,text("PRUEBAS SOLO CON EL CALEFACTOR VIGILADO",14,RED,true),8);
        add(root,text("Deja el calefactor conectado, orienta el Xiaomi al receptor y pulsa una sola vez «Enviar señal». Observa el equipo después de cada envío. El móvil NO detecta por sí solo si ha reaccionado. No pruebes el escáner sin supervisión.",14,MUTED,false),22);

        if(!available) add(root,text("Android no detecta el emisor de infrarrojos del móvil.",16,RED,true),20);
        counter=text("",15,GREEN,true);add(root,counter,8);
        title=text("",22,WHITE,true);add(root,title,11);
        info=text("",14,MUTED,false);add(root,info,15);
        send=button("⏻  ENVIAR SEÑAL DE PRUEBA",RED);
        send.setEnabled(available);addHeight(root,send,77,10);
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
        previous=button("← Anterior",PANEL);next=button("Siguiente →",PANEL);
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(57),1);a.rightMargin=dp(7);
        nav.addView(previous,a);nav.addView(next,new LinearLayout.LayoutParams(0,dp(57),1));
        add(root,nav,14);
        save=button("✓  ESTE CÓDIGO FUNCIONA · GUARDAR",Color.rgb(38,126,86));
        addHeight(root,save,65,13);
        stored=button("UTILIZAR CÓDIGO GUARDADO",PANEL);
        addHeight(root,stored,63,8);
        status=text("",14,GREEN,false);
        add(root,status,20);
        add(root,text("Este buscador NO hace un barrido automático: enviar varios códigos seguidos podría encender o apagar repetidamente el calefactor. Cuando encuentres uno que funcione, guárdalo; podrá servir como botón personal, aunque podría ser un conmutador (encendido/apagado), no un OFF exclusivo.",13,MUTED,false),12);
        add(root,text("Alternativa sin mando: el HE-1800 tiene un pequeño pulsador manual bajo su caja de control (según el manual). Si no responde a los códigos, no es necesario cambiar el calefactor: un electricista puede estudiar un mando de sustitución o un interruptor adecuado en el circuito.",13,MUTED,false),8);
        send.setOnClickListener(v -> {
            if(!entries.isEmpty()) transmit(entries.get(position),false);
        });
        previous.setOnClickListener(v -> move(-1));
        next.setOnClickListener(v -> move(1));
        save.setOnClickListener(v -> {
            if(entries.isEmpty())return;
            saved=entries.get(position);
            prefs.edit().putString("working_code",saved.id).apply();
            status.setText("Código guardado. Comprueba que responde también al volver a probarlo.");
            draw();
        });
        stored.setOnClickListener(v -> {
            if(saved!=null)transmit(saved,true);
        });
        draw();
        setContentView(sc);
    }

    private void load() {
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
                getAssets().open("heater_scan_catalog.psv"),StandardCharsets.UTF_8))){
            String line;
            while((line=reader.readLine())!=null){
                if(line.startsWith("#")||line.trim().isEmpty())continue;
                try {
                    String[] f=line.split("\\|",-1);
                    if(f.length!=8)continue;
                    IrCodes.Signal s;
                    if("RAW".equals(f[3])){
                        String[] numbers=f[7].split(",");
                        if(numbers.length%2!=0||numbers.length<4)continue;
                        int[] raw=new int[numbers.length];
                        long total=0;
                        for(int i=0;i<numbers.length;i++){
                            raw[i]=Integer.parseInt(numbers[i]);total+=raw[i];
                        }
                        if(total>1900000L)continue;
                        s=new IrCodes.Signal("RAW",Integer.parseInt(f[6]),raw);
                    }else{
                        s=IrCodes.parsed(f[3],Integer.parseInt(f[4]),Integer.parseInt(f[5]));
                    }
                    if(s.hertz<30000||s.hertz>60000)continue;
                    entries.add(new Candidate(f[0],f[1],f[2],s));
                }catch(Exception ignored){}
            }
        }catch(Exception ignored){}
    }

    private void draw(){
        int total=entries.size();
        counter.setText("Prueba "+(total==0?0:position+1)+" de "+total);
        if(total==0){
            title.setText("Catálogo no disponible");
            info.setText("No se ha podido cargar ninguna señal de calefactor.");
            send.setEnabled(false);previous.setEnabled(false);next.setEnabled(false);save.setEnabled(false);
        }else{
            Candidate c=entries.get(position);
            title.setText(c.brand+" · "+c.button);
            info.setText("Referencia: "+c.model+"\nTipo: "+c.signal.name+" · "+c.signal.hertz+" Hz"+
              "\nNO confirmado para Soler & Palau HE-1800.");
            boolean available=ir!=null&&ir.hasIrEmitter();
            send.setEnabled(available&&!busy);previous.setEnabled(total>1&&!busy);
            next.setEnabled(total>1&&!busy);save.setEnabled(!busy);
        }
        stored.setEnabled(saved!=null&&ir!=null&&ir.hasIrEmitter()&&!busy);
        stored.setText(saved==null?"NO HAY CÓDIGO GUARDADO":
            "⏻  USAR CÓDIGO GUARDADO ("+saved.brand+")");
    }
    private void move(int offset){
        if(entries.isEmpty()||busy)return;
        position=(position+offset+entries.size())%entries.size();
        prefs.edit().putInt("index",position).apply();
        status.setText("");
        draw();
    }
    private void transmit(Candidate c,boolean fromSaved){
        if(busy||ir==null||!ir.hasIrEmitter())return;
        busy=true;draw();
        status.setText("Enviando señal…");
        new Thread(()->{
            boolean ok=true;
            try {
                int carrier=c.signal.hertz;
                ConsumerIrManager.CarrierFrequencyRange[] ranges=ir.getCarrierFrequencies();
                if(ranges!=null&&ranges.length>0){
                    int nearest=carrier,diff=Integer.MAX_VALUE;
                    for(ConsumerIrManager.CarrierFrequencyRange f:ranges){
                        int n=Math.max(f.getMinFrequency(),Math.min(f.getMaxFrequency(),carrier));
                        if(Math.abs(n-carrier)<diff){nearest=n;diff=Math.abs(n-carrier);}
                    }
                    carrier=nearest;
                }
                ir.transmit(carrier,c.signal.pattern);
            }catch(Exception ex){ok=false;}
            final boolean done=ok;
            runOnUiThread(()->{
                busy=false;draw();
                status.setText(done ? "Señal enviada. Observa si el calefactor reacciona.":
                  "No ha sido posible transmitir esta señal.");
            });
        },"test-heater-IR").start();
    }
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private Button button(String txt,int color){
        Button b=new Button(this);b.setText(txt);b.setAllCaps(false);
        b.setTextColor(Color.WHITE);b.setTextSize(16);
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));
        b.setBackground(d);return b;
    }
    private void add(LinearLayout root,View v,int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(margin);root.addView(v,p);
    }
    private void addHeight(LinearLayout root,View v,int height,int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(height));p.bottomMargin=dp(margin);root.addView(v,p);
    }
}
