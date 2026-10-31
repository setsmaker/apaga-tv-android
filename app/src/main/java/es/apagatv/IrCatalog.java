package es.apagatv;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Offline collection: Flipper-IRDB CC0 plus separately documented discrete codes. */
public final class IrCatalog {
    public static final class Entry {
        public final String brand;
        public final String mode;
        public final String model;
        public final IrCodes.Signal signal;
        public final String key;
        Entry(String brand, String mode, String model, IrCodes.Signal signal) {
            this.brand = brand;
            this.mode = mode;
            this.model = model;
            this.signal = signal;
            this.key = signal.hertz + ":" + Integer.toHexString(Arrays.hashCode(signal.pattern));
        }
    }

    private final Map<String, List<Entry>> off = new LinkedHashMap<>();
    private final Map<String, List<Entry>> toggle = new LinkedHashMap<>();
    private int rejected = 0;

    public IrCatalog(Context context) {
        addCurated();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("flipper_tv_catalog.psv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                try {
                    String[] f = line.split("\\|", -1);
                    if (f.length != 8) { rejected++; continue; }
                    String brand = f[0], mode = f[1], protocol = f[2];
                    IrCodes.Signal signal;
                    if ("RAW".equals(protocol)) {
                        String[] parts = f[6].split(",");
                        int[] data = new int[parts.length];
                        for (int i = 0; i < parts.length; i++)
                            data[i] = Integer.parseInt(parts[i]);
                        signal = new IrCodes.Signal("Flipper RAW", Integer.parseInt(f[5]), data);
                    } else {
                        signal = IrCodes.parsed(protocol, Integer.parseInt(f[3]),
                                Integer.parseInt(f[4]));
                    }
                    add(brand, mode, f[7], signal);
                } catch (RuntimeException ex) {
                    rejected++;
                }
            }
        } catch (Exception ex) {
            rejected++;
        }
    }

    private void add(String brand, String mode, String model, IrCodes.Signal signal) {
        if (signal.pattern == null || signal.pattern.length < 4 || signal.pattern.length % 2 != 0 ||
                signal.hertz < 30000 || signal.hertz > 60000) {
            rejected++;
            return;
        }
        long duration = 0;
        for (int n : signal.pattern) {
            if (n <= 0) { rejected++; return; }
            duration += n;
        }
        if (duration > 1900000) { rejected++; return; }
        Map<String, List<Entry>> dest = "OFF".equals(mode) ? off : toggle;
        List<Entry> list = dest.get(brand);
        if (list == null) {
            list = new ArrayList<>();
            dest.put(brand, list);
        }
        Entry entry = new Entry(brand, mode, model, signal);
        for (Entry old : list)
            if (old.signal.hertz == signal.hertz &&
                    Arrays.equals(old.signal.pattern, signal.pattern))
                return;
        list.add(entry);
    }

    private void addCurated() {
        add("Samsung", "OFF", "Samsung AA59, apagado específico · Tasmota",
                new IrCodes.Signal("Samsung OFF", 38000,
                        IrCodes.necFrame(0xE0E019E6L, true)));
        add("LG", "OFF", "LG webOS · DSC_IR_KEY_PWROFF",
                new IrCodes.Signal("LG OFF", 38000,
                        IrCodes.necFrame(0x20DFA35CL, false)));
        add("TCL", "OFF", "TCL TV NEC · apagado independiente 57E318E7 · DDRBoxman",
                new IrCodes.Signal("TCL OFF", 38000,
                        IrCodes.necFrame(0x57E318E7L, false)));
        add("Hisense", "OFF", "Hisense NEC · apagado independiente 00FD8B74 · Hawkins",
                new IrCodes.Signal("Hisense OFF", 38000,
                        IrCodes.necFrame(0x00FD8B74L, false)));
        add("Sony", "OFF", "Sony TV · SIRC address 1, command 47",
                IrCodes.parsed("SIRC", 1, 47));
        add("Philips", "OFF", "Philips LCD · Pronto independiente",
                IrCodes.pronto("Philips OFF",
                        "0000 0073 0000 000C 0020 0020 0040 0020 " +
                        "0020 0020 0020 0020 0020 0020 0020 0020 " +
                        "0020 0040 0020 0020 0020 0020 0020 0020 " +
                        "0040 0040 0020 0CA4"));
        add("Panasonic", "OFF", "Panasonic HDTV · Pronto independiente",
                IrCodes.pronto("Panasonic OFF",
                        "0000 0071 0000 0032 0080 003f 0010 0010 0010 0030 " +
                        "0010 0010 0010 0010 0010 0010 0010 0010 0010 0010 " +
                        "0010 0010 0010 0010 0010 0010 0010 0010 0010 0010 " +
                        "0010 0010 0010 0030 0010 0010 0010 0010 0010 0010 " +
                        "0010 0010 0010 0010 0010 0010 0010 0010 0010 0010 " +
                        "0010 0010 0010 0030 0010 0010 0010 0010 0010 0010 " +
                        "0010 0010 0010 0010 0010 0010 0010 0010 0010 0010 " +
                        "0010 0030 0010 0030 0010 0030 0010 0030 0010 0030 " +
                        "0010 0030 0010 0010 0010 0010 0010 0030 0010 0030 " +
                        "0010 0030 0010 0030 0010 0030 0010 0030 0010 0010 " +
                        "0010 0030 0010 0a98"));
        // NOTE: Xiaomi code is not a documented dedicated OFF. Only manual test.
        add("Xiaomi", "TOGGLE", "Mi TV · POWER experimental (no es apagado seguro)",
                new IrCodes.Signal("Xiaomi POWER", 38000,
                        IrCodes.necFrame(0x807F46B9L, false)));
    }

    public List<Entry> get(String brand, boolean powerToggle) {
        List<Entry> list = (powerToggle ? toggle : off).get(brand);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }
    public int count(String brand, boolean powerToggle) {
        return get(brand, powerToggle).size();
    }
    public List<String> brands() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.addAll(off.keySet());
        result.addAll(toggle.keySet());
        List<String> sorted = new ArrayList<>(result);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }
    public int offTotal() {
        int sum = 0;
        for (List<Entry> e : off.values()) sum += e.size();
        return sum;
    }
    public int toggleTotal() {
        int sum = 0;
        for (List<Entry> e : toggle.values()) sum += e.size();
        return sum;
    }
    public int rejectedCount() { return rejected; }
}
