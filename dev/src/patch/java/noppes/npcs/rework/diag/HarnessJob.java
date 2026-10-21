package noppes.npcs.rework.diag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Navodilo za klientski harness (M5-K.0). Datoteko zapise {@code k-run.ps1}, klient jo dobi
 * prek {@code -Drwharness=<pot>}; brez lastnosti harness ne obstaja.
 *
 * <pre>
 * world=rwk-idle-200          mapa v saves/
 * createSeed=customnpc-rework ce svet se nima level.dat: nov FLAT svet s tem semenom (kot server.properties)
 * cmd.1=noppes slay npcs 2000 ukazi na integriranem strezniku (konzola), po vrsti od 1
 * expectNpc=200               toliko NPC-jev mora biti nalozenih v klientu
 * expectNpcMin=180            (boj) dovolj je toliko; brez kljuca natancno expectNpc
 * cam=60.5,12,-30,0,30        x,y,z,yaw,pitch (igralec v nacinu spectator)
 * renderDistance=8            chunki
 * fov=70
 * hideGui=0                   1 = F1 (brez HUD-a); v 1.12 to skrije tudi imena nad NPC-ji
 *                             ({@code RenderLivingBase.canRenderName} zahteva {@code isGuiEnabled}), zato privzeto 0
 * headwearSkip=-1             M5-K K14b: &gt;= 0 = ob vstopu nastavi {@code CustomNpcs.RwHeadwearSkip} (klient in
 *                             integrirani streznik si delita JVM); -1 = pusti nastavitev iz CustomNpcs.cfg
 * headwearCheck=0             M5-K K14b: 1 = po ogrevanju primerjaj piksle in globino z RwHeadwearSkip 0 in 1
 *                             ({@link HeadwearCheck}); izid v JSON
 * labelCache=-1               M5-K K4: &gt;= 0 = ob vstopu nastavi {@code CustomNpcs.RwLabelCache}; -1 = iz konfiguracije
 * labelCheck=0                M5-K K4: 1 = po ogrevanju primerjaj piksle in globino z RwLabelCache 0 in 1 ({@link LabelCheck})
 * eyesList=-1                 M5-K K3: &gt;= 0 = ob vstopu nastavi {@code CustomNpcs.RwEyesList}; -1 = iz konfiguracije
 * eyesCheck=0                 M5-K K3: 1 = po ogrevanju primerjaj piksle in globino z RwEyesList 0 in 1 ({@link EyesCheck})
 * lodStart=-1                 M5-K K5: &gt;= 0 = ob vstopu nastavi {@code CustomNpcs.RwLodStart} (bloki, 0 = original); -1 = iz konfiguracije
 * farView=-1                 M5-V.2: &gt;= 0 = ob vstopu nastavi {@code CustomNpcs.RwFarView} (klient: 1 = HELLO na kanalu rwfar,
 *                             shramba nadomestkov); -1 = iz konfiguracije
 * connect=127.0.0.1:25565     M5-V.1: namesto integriranega sveta se poveze na dedicirani streznik; ukazov cmd.* ne
 *                             izvaja (prizorisce postavi skripta prek konzole) in pred stetjem NPC-jev caka na
 *                             sporocilo {@code RWHARNESS-SCENA} v klepetu
 * renderWeight=0              M5-V.1 (varianta A): &gt; 0 = po nalaganju rendererjev {@code Entity.setRenderDistanceWeight};
 *                             0 = vanilla ({@code RD / 8}, omejeno na 1..2,5)
 * sampleMs=0                 M5-K.1: &gt; 0 = med merjenjem vzorci sklad render niti vsakih toliko ms
 *                             ({@link StackSampler}); izhod {@code out} s koncnico -skladi.txt
 * settle=5                  s po ukazih, preden se steje NPC-je
 * warmup=30                   s ogrevanja
 * measure=60                  s belezenja
 * out=C:/.../k-idle-200.json
 * </pre>
 *
 * Brez Minecraft tipov, testljivo brez klienta.
 */
public final class HarnessJob {
    public final String world;
    public final String createSeed;
    public final List<String> commands;
    public final int expectNpc;
    /** Spodnja meja za boj, kjer NPC-ji ze med umiritvijo umirajo; privzeto = expectNpc (natancno). */
    public final int expectNpcMin;
    public final double camX;
    public final double camY;
    public final double camZ;
    public final float camYaw;
    public final float camPitch;
    public final int renderDistance;
    public final float fov;
    /** M5-K.1: F1 skrije tudi imena NPC-jev; M5-K.0 je meril s skritim GUI-jem (brez imen). */
    public final boolean hideGui;
    /** M5-K.1: interval vzorcevalnika sklada render niti v ms, 0 = izklopljen. */
    public final int sampleMs;
    /** M5-K K14b: RwHeadwearSkip za zagon, -1 = iz konfiguracije. */
    public final int headwearSkip;
    /** M5-K K14b: preverba enakosti slike pokrivala po ogrevanju. */
    public final boolean headwearCheck;
    /** M5-K K4: RwLabelCache za zagon, -1 = iz konfiguracije. */
    public final int labelCache;
    /** M5-K K4: preverba enakosti slike napisov po ogrevanju. */
    public final boolean labelCheck;
    /** M5-K K3: RwEyesList za zagon, -1 = iz konfiguracije. */
    public final int eyesList;
    /** M5-K K3: preverba enakosti slike oci po ogrevanju. */
    public final boolean eyesCheck;
    /** M5-K K5: RwLodStart za zagon (bloki), -1 = iz konfiguracije. */
    public final int lodStart;
    /** M5-V.2: RwFarView za zagon (klient), -1 = iz konfiguracije. */
    public final int farView;
    /** M5-V.1: gostitelj dediciranega streznika ali {@code null} (integrirani svet). */
    public final String connectHost;
    public final int connectPort;
    public final double renderWeight;
    public final int settleSeconds;
    public final int warmupSeconds;
    public final int measureSeconds;
    public final String out;

    private HarnessJob(Properties p) {
        this.world = required(p, "world");
        String seed = p.getProperty("createSeed");
        this.createSeed = seed == null || seed.trim().isEmpty() ? null : seed.trim();
        List<String> cmds = new ArrayList<String>();
        for (int i = 1; p.containsKey("cmd." + i); i++) {
            String c = p.getProperty("cmd." + i).trim();
            if (c.startsWith("/")) {
                c = c.substring(1);
            }
            if (!c.isEmpty()) {
                cmds.add(c);
            }
        }
        this.commands = Collections.unmodifiableList(cmds);
        this.expectNpc = intValue(p, "expectNpc", -1);
        this.expectNpcMin = intValue(p, "expectNpcMin", this.expectNpc);
        String[] cam = required(p, "cam").split(",");
        if (cam.length != 5) {
            throw new IllegalArgumentException("cam mora biti x,y,z,yaw,pitch: " + p.getProperty("cam"));
        }
        this.camX = Double.parseDouble(cam[0].trim());
        this.camY = Double.parseDouble(cam[1].trim());
        this.camZ = Double.parseDouble(cam[2].trim());
        this.camYaw = Float.parseFloat(cam[3].trim());
        this.camPitch = Float.parseFloat(cam[4].trim());
        this.renderDistance = intValue(p, "renderDistance", 8);
        this.fov = Float.parseFloat(p.getProperty("fov", "70").trim());
        this.hideGui = intValue(p, "hideGui", 0) != 0;
        this.sampleMs = intValue(p, "sampleMs", 0);
        this.headwearSkip = intValue(p, "headwearSkip", -1);
        this.headwearCheck = intValue(p, "headwearCheck", 0) != 0;
        this.labelCache = intValue(p, "labelCache", -1);
        this.labelCheck = intValue(p, "labelCheck", 0) != 0;
        this.eyesList = intValue(p, "eyesList", -1);
        this.eyesCheck = intValue(p, "eyesCheck", 0) != 0;
        this.lodStart = intValue(p, "lodStart", -1);
        this.farView = intValue(p, "farView", -1);
        String connect = p.getProperty("connect");
        if (connect == null || connect.trim().isEmpty()) {
            this.connectHost = null;
            this.connectPort = -1;
        } else {
            String c = connect.trim();
            int colon = c.lastIndexOf(':');
            this.connectHost = colon < 0 ? c : c.substring(0, colon);
            this.connectPort = colon < 0 ? 25565 : Integer.parseInt(c.substring(colon + 1).trim());
            if (this.connectHost.isEmpty() || this.connectPort < 1 || this.connectPort > 65535) {
                throw new IllegalArgumentException("connect mora biti gostitelj[:vrata]: " + connect);
            }
        }
        this.renderWeight = Double.parseDouble(p.getProperty("renderWeight", "0").trim());
        if (this.renderWeight < 0.0 || this.renderWeight > 16.0) {
            throw new IllegalArgumentException("renderWeight izven 0..16: " + this.renderWeight);
        }
        this.settleSeconds = intValue(p, "settle", 5);
        this.warmupSeconds = intValue(p, "warmup", 30);
        this.measureSeconds = intValue(p, "measure", 60);
        this.out = required(p, "out");
        if (this.renderDistance < 2 || this.renderDistance > 32) {
            throw new IllegalArgumentException("renderDistance izven 2..32: " + this.renderDistance);
        }
        if (this.measureSeconds < 1 || this.warmupSeconds < 0 || this.settleSeconds < 0 || this.sampleMs < 0
                || this.headwearSkip < -1 || this.headwearSkip > 1 || this.labelCache < -1 || this.labelCache > 1
                || this.eyesList < -1 || this.eyesList > 1 || this.lodStart < -1 || this.lodStart > 512 || this.farView < -1 || this.farView > 2) {
            throw new IllegalArgumentException("casi morajo biti nenegativni, measure >= 1, headwearSkip, labelCache in eyesList -1..1, lodStart -1..512, farView -1..2");
        }
    }

    public static HarnessJob parse(Properties p) {
        return new HarnessJob(p);
    }

    public static HarnessJob load(File f) throws IOException {
        Properties p = new Properties();
        InputStream in = new FileInputStream(f);
        try {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } finally {
            in.close();
        }
        return parse(p);
    }

    /** Seme kot ga izracuna dedicated server iz {@code level-seed}: stevilo ali hashCode niza. */
    public static long seedOf(String s) {
        try {
            long v = Long.parseLong(s);
            if (v != 0L) {
                return v;
            }
        } catch (NumberFormatException e) {
            // niz
        }
        return s.hashCode();
    }

    private static String required(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException("manjka kljuc " + key);
        }
        return v.trim();
    }

    private static int intValue(Properties p, String key, int def) {
        String v = p.getProperty(key);
        return v == null || v.trim().isEmpty() ? def : Integer.parseInt(v.trim());
    }
}
