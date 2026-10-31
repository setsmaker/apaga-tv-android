import es.apagatv.IrCodes;

public final class IrCodesCheck {
    public static void main(String[] args) {
        for (IrCodes.Signal signal : new IrCodes.Signal[]{
            new IrCodes.Signal("Samsung", 38000, IrCodes.necFrame(0xE0E019E6L, true)),
            new IrCodes.Signal("LG", 38000, IrCodes.necFrame(0x20DFA35CL, false)),
            new IrCodes.Signal("TCL", 38000, IrCodes.necFrame(0x57E318E7L, false)),
            new IrCodes.Signal("Hisense", 38000, IrCodes.necFrame(0x00FD8B74L, false)),
            IrCodes.parsed("SIRC", 1, 47),
            IrCodes.parsed("RC5", 0, 12),
            IrCodes.parsed("RC5X", 1, 76),
            IrCodes.pronto("Philips", "0000 0073 0000 000C 0020 0020 0040 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0040 0020 0020 0020 0020 0020 0020 0040 0040 0020 0CA4")
        }) {
            if (signal.hertz < 30000 || signal.hertz > 60000 || signal.pattern.length % 2 != 0)
                throw new AssertionError(signal.name + " has bad length/carrier");
            int total=0;
            for (int t : signal.pattern) {
                if (t <= 0) throw new AssertionError(signal.name + " has nonpositive timing");
                total += t;
            }
            if (total > 1900000) throw new AssertionError(signal.name + " exceeds Android IR maximum");
            System.out.println(signal.name + " " + total/1000 + " ms OK");
        }
    }
}
