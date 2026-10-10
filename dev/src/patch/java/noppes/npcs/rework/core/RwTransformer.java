package noppes.npcs.rework.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * M5.15 (D-031): transformer CNPC reworka. **Brez posega**: vrne iste bajte, kot jih je dobil.
 * Namen tega paketa je samo pot do vanilla razredov (manifest, nalagalni vticnik, imena SRG/MCP) in
 * dokaz, da pot deluje; prvi pravi poseg je M5.16.
 *
 * <p>Steje razrede, ki so sla skozi, in jih vsakih {@value #REPORT_EVERY} objavi v sistemski
 * lastnosti {@code rwcore.classes}, da jih vidi tudi koda moda (drug nalagalnik razredov).
 */
public class RwTransformer implements IClassTransformer {
    /** Na koliko razredov se osvezi sistemska lastnost in zapise vrstica v dnevnik. */
    public static final int REPORT_EVERY = 512;

    private static final Logger LOG = LogManager.getLogger("customnpcs-rework-core");

    private static long seen;
    private static boolean first = true;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        ++seen;
        if (first) {
            first = false;
            LOG.info(RwCoreMod.MARKER + " transformer aktiven, prvi razred=" + transformedName
                    + " deobf=" + (RwCoreMod.runtimeDeobf() ? 1 : 0));
        }
        if (seen % REPORT_EVERY == 0) {
            System.setProperty(RwCoreMod.PROP_CLASSES, Long.toString(seen));
        }
        return basicClass;
    }

    /** Stevilo razredov, ki so sla skozi (natancno; samo v nalagalniku transformerja). */
    public static long seen() {
        return seen;
    }
}
