package es.apagatv;

/**
 * Infrared timing encoders. The Flipper-IRDB 'parsed' data stores the logical
 * address and command LSB-first; encoders build the actual transmitted frames.
 */
public final class IrCodes {
    private IrCodes() {}

    public static final class Signal {
        public final String name;
        public final int hertz;
        public final int[] pattern;
        public Signal(String name, int hertz, int[] pattern) {
            this.name = name;
            this.hertz = hertz;
            this.pattern = pattern;
        }
    }

    private static int rev8(int n) {
        return (Integer.reverse(n & 255) >>> 24) & 255;
    }

    /** Samsung32 uses ~4.5ms leader; NEC uses ~9ms leader. */
    public static int[] necFrame(long bits, boolean samsung) {
        int[] out = new int[68];
        out[0] = samsung ? 4500 : 9000;
        out[1] = 4500;
        for (int i = 0; i < 32; i++) {
            out[2+i*2] = 560;
            out[3+i*2] = ((bits >>> (31-i)) & 1L) != 0 ? 1690 : 560;
        }
        out[66] = 560;
        out[67] = 28000;
        return out;
    }

    /** Expects a remote's physical 32-bit hexadecimal code (MSB first). */
    public static int[] nec(long code) { return necFrame(code, false); }

    /** Sony SIRC, logical command (7-bit) and device address (5/8/13-bit). */
    public static int[] sony(int address, int command, int bits) {
        int word = (address << 7) | (command & 127);
        int[] out = new int[2 + bits * 2];
        out[0] = 2400; out[1] = 600;
        for (int i = 0; i < bits; i++) {
            out[2 + i * 2] = ((word >>> i) & 1) != 0 ? 1200 : 600;
            out[3 + i * 2] = 600;
        }
        out[out.length-1] = 25000;
        return out;
    }

    /** Build the waveform from CC0 Flipper-IRDB parsed parameters. */
    public static Signal parsed(String protocol, int address, int command) {
        switch (protocol) {
            case "NEC": {
                int a = rev8(address);
                int c = rev8(command);
                long code = ((long)a << 24) | ((long)(a ^ 255) << 16) |
                            ((long)c << 8) | (c ^ 255);
                return new Signal(protocol, 38000, necFrame(code, false));
            }
            case "NECext": {
                int a = rev8(address & 255);
                int b = rev8((address >>> 8) & 255);
                int c = rev8(command);
                long code = ((long)a << 24) | ((long)b << 16) |
                            ((long)c << 8) | (c ^ 255);
                return new Signal(protocol, 38000, necFrame(code, false));
            }
            case "Samsung32": {
                int a = rev8(address);
                int c = rev8(command);
                long code = ((long)a << 24) | ((long)a << 16) |
                            ((long)c << 8) | (c ^ 255);
                return new Signal(protocol, 38000, necFrame(code, true));
            }
            case "SIRC":
                return new Signal(protocol, 40000, sony(address, command, 12));
            case "SIRC15":
                return new Signal(protocol, 40000, sony(address, command, 15));
            case "SIRC20":
                return new Signal(protocol, 40000, sony(address, command, 20));
            default:
                throw new IllegalArgumentException("IR unsupported: " + protocol);
        }
    }

    /** Philips Pronto '0000' raw hex only (not RC5 compact '5000'). */
    public static Signal pronto(String label, String hex) {
        String[] tokens = hex.trim().split("\\s+");
        int[] words = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++)
            words[i] = Integer.parseInt(tokens[i], 16);
        if (words.length < 6 || words[0] != 0 || words[1] == 0 ||
                words.length != 4 + 2 * (words[2] + words[3]))
            throw new IllegalArgumentException("Pronto invalido");
        double micro = words[1] * 0.241246;
        int carrier = (int)Math.round(1000000.0 / micro);
        int[] pat = new int[words.length - 4];
        for (int i = 0; i < pat.length; i++)
            pat[i] = Math.max(1, (int)Math.round(words[i+4] * micro));
        return new Signal(label, carrier, pat);
    }
}
