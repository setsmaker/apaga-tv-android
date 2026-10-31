import es.apagatv.IrCodes;

public class IrCodesCheck {
    public static void main(String[] args) {
        for (String brand : new String[]{"Samsung", "LG", "Sony", "Philips", "Xiaomi"}) {
            for (IrCodes.Signal s : IrCodes.forBrand(brand)) {
                if (s.hertz < 30000 || s.hertz > 60000 || s.pattern.length % 2 != 0)
                    throw new AssertionError("Frecuencia o patrón inválido: " + s.name);
                int duration = 0;
                for (int p : s.pattern) {
                    if (p <= 0) throw new AssertionError("Duración inválida");
                    duration += p;
                }
                if (duration > 2000000) throw new AssertionError("Excede límite de Android");
                System.out.println(s.name + " (" + duration / 1000 + " ms)");
            }
        }
    }
}
