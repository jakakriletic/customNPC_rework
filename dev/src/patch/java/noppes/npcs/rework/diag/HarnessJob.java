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
 * settle=5                    s po ukazih, preden se steje NPC-je
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
        this.settleSeconds = intValue(p, "settle", 5);
        this.warmupSeconds = intValue(p, "warmup", 30);
        this.measureSeconds = intValue(p, "measure", 60);
        this.out = required(p, "out");
        if (this.renderDistance < 2 || this.renderDistance > 32) {
            throw new IllegalArgumentException("renderDistance izven 2..32: " + this.renderDistance);
        }
        if (this.measureSeconds < 1 || this.warmupSeconds < 0 || this.settleSeconds < 0) {
            throw new IllegalArgumentException("casi morajo biti nenegativni, measure >= 1");
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
