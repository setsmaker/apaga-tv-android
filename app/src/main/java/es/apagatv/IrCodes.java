package es.apagatv;

import java.util.ArrayList;
import java.util.List;

/** Patrones IR en microsegundos (alternando marca/espacio). */
public final class IrCodes {
    private IrCodes() { }

    public static final class Signal {
        public final String name;
        public final int hertz;
        public final int[] pattern;
        Signal(String name, int hertz, int[] pattern) {
            this.name = name; this.hertz = hertz; this.pattern = pattern;
        }
    }

    public static List<Signal> forBrand(String brand) {
        ArrayList<Signal> result = new ArrayList<>();
        switch (brand) {
            case "Samsung":
                result.add(new Signal("Samsung · OFF", 38000, nec(0xE0E019E6L)));
                result.add(new Signal("Samsung · repetir OFF", 38000, nec(0xE0E019E6L)));
                break;
            case "LG":
                result.add(new Signal("LG · OFF", 38000, nec(0x20DFA35CL)));
                result.add(new Signal("LG · repetir OFF", 38000, nec(0x20DFA35CL)));
                break;
            case "Sony":
                for (int i = 0; i < 3; i++)
                    result.add(new Signal("Sony · OFF", 40000, sonySirc(0xF50, 12)));
                break;
            case "Philips":
                result.add(pronto("Philips LCD · OFF",
                    "0000 0073 0000 000C 0020 0020 0040 0020 " +
                    "0020 0020 0020 0020 0020 0020 0020 0020 " +
                    "0020 0040 0020 0020 0020 0020 0020 0020 " +
                    "0040 0040 0020 0CA4"));
                break;
            case "Xiaomi":
                // Código POWER de algunos Mi TV: puede alternar encendido/apagado.
                result.add(new Signal("Xiaomi · POWER (conmutado)", 38000,
                    nec(0x807F46B9L)));
                break;
            default:
                throw new IllegalArgumentException("Marca no reconocida: " + brand);
        }
        return result;
    }

    /** NEC 32 bit, MSB primero. */
    public static int[] nec(long code) {
        int[] result = new int[68];
        result[0] = 9000; result[1] = 4500;
        for (int i = 0; i < 32; i++) {
            result[2 + i * 2] = 560;
            result[3 + i * 2] = ((code >>> (31 - i)) & 1L) != 0 ? 1690 : 560;
        }
        result[66] = 560; result[67] = 30000;
        return result;
    }

    /** Sony SIRC 12 bit, LSB primero. */
    public static int[] sonySirc(int code, int bits) {
        int[] result = new int[2 + bits * 2];
        result[0] = 2400; result[1] = 600;
        for (int i = 0; i < bits; i++) {
            result[2 + 2 * i] = ((code >>> i) & 1) != 0 ? 1200 : 600;
            result[3 + 2 * i] = 600;
        }
        result[result.length - 1] = 25000;
        return result;
    }

    public static Signal pronto(String label, String hex) {
        String[] tokens = hex.trim().split("\\s+");
        int[] words = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++)
            words[i] = Integer.parseInt(tokens[i], 16);
        if (words.length < 6 || words[0] != 0 || words[1] == 0 ||
                words.length != 4 + 2 * (words[2] + words[3]))
            throw new IllegalArgumentException("Pronto raw inválido");
        double micro = words[1] * 0.241246;
        int carrier = (int) Math.round(1000000.0 / micro);
        int[] pattern = new int[words.length - 4];
        for (int i = 0; i < pattern.length; i++)
            pattern[i] = Math.max(1, (int) Math.round(words[i + 4] * micro));
        return new Signal(label, carrier, pattern);
    }
}
