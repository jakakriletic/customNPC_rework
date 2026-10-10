package noppes.npcs.rework.core;

import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * M5.15 (D-031): FML nalagalni vticnik CNPC reworka. Prijavi {@link RwTransformer} in si zapomni,
 * ali tece v produkciji (imena SRG) ali v dev okolju (imena MCP).
 *
 * <p>Razred nalozi LaunchWrapper **pred** modi, zato ne sme pritegniti nobenega CNPC razreda: kar se
 * nalozi v tej fazi, ni vec mogoce transformirati. Zato je dnevnik log4j (LaunchWrapper ga ze ima) in
 * ne {@code LogWriter}.
 *
 * <p>Stanje gre tudi v sistemske lastnosti ({@code rwcore.*}), ker so te deljene med nalagalniki
 * razredov; staticna polja tega razreda so iz moda lahko druga instanca.
 *
 * <p>{@code SortingIndex} 1001 pomeni, da transformer tece **za** Forgeovim deobfuskacijskim
 * transformerjem (ta je 1000): v produkciji torej vidi imena SRG, v dev okolju pa imena MCP, ker
 * tam deobfuskacije ni.
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("CustomNPCs Rework Core")
@IFMLLoadingPlugin.SortingIndex(1001)
public class RwCoreMod implements IFMLLoadingPlugin {
    /** Marker za scenarije in dimni test. */
    public static final String MARKER = "RWCORE";
    public static final String PROP_LOADED = "rwcore.loaded";
    public static final String PROP_DEOBF = "rwcore.deobf";
    public static final String PROP_CLASSES = "rwcore.classes";

    private static final Logger LOG = LogManager.getLogger("customnpcs-rework-core");

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "noppes.npcs.rework.core.RwTransformer" };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        Object flag = data == null ? null : data.get("runtimeDeobfuscationEnabled");
        boolean deobf = flag instanceof Boolean && ((Boolean) flag).booleanValue();
        System.setProperty(PROP_LOADED, "1");
        System.setProperty(PROP_DEOBF, deobf ? "1" : "0");
        if (System.getProperty(PROP_CLASSES) == null) {
            System.setProperty(PROP_CLASSES, "0");
        }
        LOG.info(MARKER + " coremod nalozen, runtimeDeobf=" + (deobf ? 1 : 0)
                + " (imena " + (deobf ? "SRG" : "MCP") + "), transformer brez posega");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    /** Ali je coremod v tem procesu tekel (bere sistemsko lastnost, zato velja med nalagalniki). */
    public static boolean loaded() {
        return "1".equals(System.getProperty(PROP_LOADED));
    }

    /** Ali tece v produkciji (imena SRG). */
    public static boolean runtimeDeobf() {
        return "1".equals(System.getProperty(PROP_DEOBF));
    }

    /** Stevilo razredov, ki so sla skozi transformer (zaokrozeno navzdol na 512). */
    public static long classesSeen() {
        try {
            return Long.parseLong(System.getProperty(PROP_CLASSES, "0"));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
