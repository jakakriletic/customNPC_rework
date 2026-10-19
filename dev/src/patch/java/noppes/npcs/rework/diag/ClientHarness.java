package noppes.npcs.rework.diag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.render.HeadwearSkip;
import noppes.npcs.rework.render.LabelCache;

/**
 * Klientski merilni harness M2.4r (M5-K.0).
 *
 * <p>Obstaja samo, kadar razvojni klient dobi {@code -Drwharness=<navodilo>} (glej
 * {@link HarnessJob}); {@code CustomNpcs} ga takrat namesti prek refleksije, zato ga
 * dedicated server nikoli ne nalozi. Obnasanja igre ne spreminja, nastavitev ne shrani
 * ({@code options.txt} ostane, kot je bil).
 *
 * <p>Potek: glavni meni → integrirani svet {@code world} (nov FLAT, ce ga se ni) → spectator +
 * teleport → 5 s nalaganja chunkov → ukazi
 * navodila na integriranem strezniku (kot konzola, kot pri {@code perf-run.ps1}) → umiritev
 * in stetje NPC-jev v klientu → ogrevanje → belezenje casa med zaporednima
 * {@code RenderTickEvent} START (= cas slicice) → JSON → izhod iz igre.
 *
 * <p>Kamera je med ogrevanjem in merjenjem pribita vsak tick; odmik se zapise (merilo
 * "kamera res fiksna"). Vidni NPC-ji se berejo iz {@code RenderGlobal.getDebugInfoEntities()}
 * enkrat na sekundo (stevilo res upodobljenih entitet v zadnji slicici).
 */
@SideOnly(Side.CLIENT)
public final class ClientHarness {
    private enum Phase { MENU, JOINING, PRELOAD, COMMANDS, SETTLE, WARMUP, MEASURE, DONE }

    private static final int MAX_FRAMES = 1 << 20;
    private static final long JOIN_TIMEOUT_NS = 180L * 1000000000L;
    private static final long NPC_TIMEOUT_NS = 180L * 1000000000L;

    private final HarnessJob job;
    private Phase phase = Phase.MENU;
    private long phaseStart;
    private final long[] frames = new long[MAX_FRAMES];
    private int frameCount;
    private long lastFrame;
    private long lastSample;
    private int renderedMin = Integer.MAX_VALUE;
    private int renderedMax = -1;
    private int renderedSamples;
    private int aliveMin = Integer.MAX_VALUE;
    private double renderedShareMin = Double.MAX_VALUE;
    private int npcLoaded = -1;
    private double maxPosDrift;
    private double maxAngleDrift;
    private long gcCount0;
    private long gcMillis0;
    private long measureStartNs;
    private long measureEndNs;
    private volatile int commandsDone = -1;
    /** M5-K.1: vzorcevalnik sklada render niti med merjenjem (samo s {@code sampleMs > 0}). */
    private StackSampler sampler;
    /** M5-K K14b: zahteva za preverbo pokrivala v naslednjem RenderWorldLastEvent in njen izid. */
    private boolean headwearCheckPending;
    private HeadwearCheck headwearCheck;
    private LabelCheck labelCheck;
    private final List<String> errors = new ArrayList<String>();

    private ClientHarness(HarnessJob job) {
        this.job = job;
    }

    /** Klice {@code CustomNpcs} prek refleksije, samo na klientu in samo z lastnostjo. */
    public static void install(String path) {
        try {
            HarnessJob job = HarnessJob.load(new File(path));
            MinecraftForge.EVENT_BUS.register(new ClientHarness(job));
            System.out.println("RWHARNESS-INSTALL world=" + job.world + " out=" + job.out);
        } catch (Exception e) {
            System.out.println("RWHARNESS-NAPAKA navodilo " + path + ": " + e);
        }
    }

    private void enter(Phase p) {
        this.phase = p;
        this.phaseStart = System.nanoTime();
        System.out.println("RWHARNESS-FAZA " + p);
    }

    private double secondsInPhase() {
        return (System.nanoTime() - this.phaseStart) / 1.0e9;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            switch (this.phase) {
                case MENU:
                    if (mc.currentScreen instanceof GuiMainMenu) {
                        File save = new File(new File(mc.mcDataDir, "saves"), this.job.world);
                        WorldSettings create = null;
                        if (!new File(save, "level.dat").isFile()) {
                            if (this.job.createSeed == null) {
                                fail("svet ne obstaja in createSeed ni podan: " + save);
                                return;
                            }
                            // Kot dedicated server iz dev/testworld/server.properties: FLAT, brez struktur.
                            create = new WorldSettings(HarnessJob.seedOf(this.job.createSeed), GameType.CREATIVE,
                                    false, false, WorldType.FLAT).enableCommands();
                            create.setGeneratorOptions("");
                        }
                        enter(Phase.JOINING);
                        mc.launchIntegratedServer(this.job.world, this.job.world, create);
                    }
                    break;
                case JOINING:
                    if (mc.player != null && mc.world != null && mc.getIntegratedServer() != null
                            && mc.currentScreen == null) {
                        applySettings(mc);
                        String name = mc.player.getName();
                        List<String> pre = new ArrayList<String>();
                        pre.add("gamemode 3 " + name);
                        pre.add(String.format(Locale.ROOT, "tp %s %.3f %.3f %.3f %.2f %.2f", name,
                                this.job.camX, this.job.camY, this.job.camZ, this.job.camYaw, this.job.camPitch));
                        runOnServer(mc.getIntegratedServer(), pre, false);
                        enter(Phase.PRELOAD);
                    } else if (System.nanoTime() - this.phaseStart > JOIN_TIMEOUT_NS) {
                        fail("vstop v svet ni uspel v 180 s");
                    }
                    break;
                case PRELOAD:
                    // Chunki okoli kamere se nalozijo sele po teleportu; 'clone grid' v nenalozenem
                    // chunku ne postavi nicesar (perf-setup-commands.txt, setworldspawn).
                    pinCamera(mc, false);
                    if (secondsInPhase() >= 5.0) {
                        runOnServer(mc.getIntegratedServer(), this.job.commands, true);
                        enter(Phase.COMMANDS);
                    }
                    break;
                case COMMANDS:
                    pinCamera(mc, false);
                    if (this.commandsDone >= 0) {
                        enter(Phase.SETTLE);
                    }
                    break;
                case SETTLE:
                    pinCamera(mc, false);
                    if (secondsInPhase() >= this.job.settleSeconds) {
                        int n = countNpcs(mc);
                        if (this.job.expectNpc < 0 || (n >= this.job.expectNpcMin && n <= this.job.expectNpc)) {
                            this.npcLoaded = n;
                            enter(Phase.WARMUP);
                        } else if (System.nanoTime() - this.phaseStart > NPC_TIMEOUT_NS) {
                            this.npcLoaded = n;
                            fail("v klientu je " + n + " NPC-jev, pricakovano " + this.job.expectNpc);
                        }
                    }
                    break;
                case WARMUP:
                    pinCamera(mc, false);
                    if (secondsInPhase() >= this.job.warmupSeconds && ((this.job.headwearCheck && this.headwearCheck == null)
                            || (this.job.labelCheck && this.labelCheck == null))) {
                        // M5-K K14b: preverba v naslednjem RenderWorldLastEvent, merjenje po njej.
                        this.headwearCheckPending = true;
                    } else if (secondsInPhase() >= this.job.warmupSeconds) {
                        this.npcLoaded = countNpcs(mc);
                        this.maxPosDrift = 0.0;
                        this.maxAngleDrift = 0.0;
                        long[] gc = gcTotals();
                        this.gcCount0 = gc[0];
                        this.gcMillis0 = gc[1];
                        this.frameCount = 0;
                        this.lastFrame = 0L;
                        this.lastSample = 0L;
                        this.measureStartNs = System.nanoTime();
                        if (this.job.sampleMs > 0) {
                            // ClientTickEvent tece na render niti, zato je currentThread() prava nit.
                            this.sampler = new StackSampler(Thread.currentThread(), this.job.sampleMs, 96);
                            this.sampler.start();
                        }
                        enter(Phase.MEASURE);
                    }
                    break;
                case MEASURE:
                    pinCamera(mc, true);
                    if (secondsInPhase() >= this.job.measureSeconds) {
                        this.measureEndNs = System.nanoTime();
                        finish(mc);
                    }
                    break;
                default:
                    break;
            }
        } catch (RuntimeException e) {
            fail("izjema v fazi " + this.phase + ": " + e);
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent ev) {
        if (!this.headwearCheckPending) {
            return;
        }
        this.headwearCheckPending = false;
        if (this.job.headwearCheck) {
            HeadwearCheck r = HeadwearCheck.run(Minecraft.getMinecraft(), ev.getPartialTicks());
            StringBuilder s = new StringBuilder();
            for (HeadwearCheck.Series x : r.series) {
                s.append(" | ").append(x);
            }
            System.out.println("RWHARNESS-POKRIVALO ok=" + r.ok() + " npc=" + r.npcs + " obcutljivost=" + r.sensitivity + s + " " + r.error);
            this.headwearCheck = r;
        }
        if (this.job.labelCheck) {
            LabelCheck r = LabelCheck.run(Minecraft.getMinecraft(), ev.getPartialTicks());
            StringBuilder s = new StringBuilder();
            for (LabelCheck.Series x : r.series) {
                s.append(" | ").append(x);
            }
            System.out.println("RWHARNESS-NAPISI ok=" + r.ok() + " npc=" + r.npcs + " vDometu=" + r.labelled + " obcutljivost=" + r.sensitivity + s + " " + r.error);
            this.labelCheck = r;
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (ev.phase != TickEvent.Phase.START || this.phase != Phase.MEASURE) {
            return;
        }
        long now = System.nanoTime();
        if (this.lastFrame != 0L && this.frameCount < MAX_FRAMES) {
            this.frames[this.frameCount++] = now - this.lastFrame;
        }
        this.lastFrame = now;
        if (now - this.lastSample >= 1000000000L) {
            this.lastSample = now;
            int r = renderedEntities(Minecraft.getMinecraft());
            if (r >= 0) {
                this.renderedMin = Math.min(this.renderedMin, r);
                this.renderedMax = Math.max(this.renderedMax, r);
                this.renderedSamples++;
                // Boj: NPC-ji umirajo, zato se upodobljeni primerjajo z zivimi v istem vzorcu.
                int alive = countNpcs(Minecraft.getMinecraft());
                this.aliveMin = Math.min(this.aliveMin, alive);
                if (alive > 0) {
                    this.renderedShareMin = Math.min(this.renderedShareMin, r / (double) alive);
                }
            }
        }
    }

    private void applySettings(Minecraft mc) {
        // Samo v pomnilniku: saveOptions() se ne klice, options.txt ostane.
        if (this.job.headwearSkip >= 0) {
            // M5-K K14b: klient in integrirani streznik si delita JVM in staticno polje.
            CustomNpcs.RwHeadwearSkip = this.job.headwearSkip;
        }
        if (this.job.labelCache >= 0) {
            CustomNpcs.RwLabelCache = this.job.labelCache;
        }
        mc.gameSettings.renderDistanceChunks = this.job.renderDistance;
        mc.gameSettings.enableVsync = false;
        mc.gameSettings.viewBobbing = false;
        mc.gameSettings.limitFramerate = 260; // 260 = neomejeno v 1.12
        mc.gameSettings.fovSetting = this.job.fov;
        // M5-K.1: s skritim GUI-jem vanilla ne narise nobenega imena (canRenderName -> isGuiEnabled).
        mc.gameSettings.hideGUI = this.job.hideGui;
        mc.gameSettings.showDebugInfo = false;
        mc.gameSettings.pauseOnLostFocus = false;
        org.lwjgl.opengl.Display.setVSyncEnabled(false);
        mc.renderGlobal.loadRenderers();
    }

    private void runOnServer(final MinecraftServer server, final List<String> cmds, final boolean signal) {
        server.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                int n = 0;
                for (String c : cmds) {
                    int r = server.getCommandManager().executeCommand(server, c);
                    System.out.println("RWHARNESS-UKAZ " + r + " " + c);
                    n++;
                }
                if (signal) {
                    ClientHarness.this.commandsDone = n;
                }
            }
        });
    }

    private void pinCamera(Minecraft mc, boolean record) {
        Entity p = mc.player;
        if (p == null) {
            return;
        }
        if (record) {
            double dx = p.posX - this.job.camX;
            double dy = p.posY - this.job.camY;
            double dz = p.posZ - this.job.camZ;
            this.maxPosDrift = Math.max(this.maxPosDrift, Math.sqrt(dx * dx + dy * dy + dz * dz));
            float da = Math.abs(wrap(p.rotationYaw - this.job.camYaw)) + Math.abs(p.rotationPitch - this.job.camPitch);
            this.maxAngleDrift = Math.max(this.maxAngleDrift, da);
        }
        p.motionX = 0.0;
        p.motionY = 0.0;
        p.motionZ = 0.0;
        p.setPositionAndRotation(this.job.camX, this.job.camY, this.job.camZ, this.job.camYaw, this.job.camPitch);
        p.prevRotationYaw = this.job.camYaw;
        p.prevRotationPitch = this.job.camPitch;
    }

    private static float wrap(float a) {
        a %= 360.0f;
        if (a >= 180.0f) {
            a -= 360.0f;
        }
        if (a < -180.0f) {
            a += 360.0f;
        }
        return a;
    }

    private static int countNpcs(Minecraft mc) {
        int n = 0;
        for (Entity e : mc.world.loadedEntityList) {
            if (e instanceof EntityNPCInterface && !e.isDead) {
                n++;
            }
        }
        return n;
    }

    /** "E: 12/50, B: 0" → 12; -1, ce oblika ni znana. */
    static int renderedEntities(Minecraft mc) {
        return FrameStats.parseRendered(mc.renderGlobal.getDebugInfoEntities());
    }

    private static long[] gcTotals() {
        long c = 0L;
        long t = 0L;
        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            c += Math.max(0L, b.getCollectionCount());
            t += Math.max(0L, b.getCollectionTime());
        }
        return new long[] {c, t};
    }

    private void fail(String why) {
        this.errors.add(why);
        System.out.println("RWHARNESS-NAPAKA " + why);
        finish(Minecraft.getMinecraft());
    }

    private void finish(Minecraft mc) {
        if (this.phase == Phase.DONE) {
            return;
        }
        Phase reached = this.phase;
        enter(Phase.DONE);
        if (this.sampler != null) {
            this.sampler.stop();
            try {
                this.sampler.writeFolded(new File(this.job.out.replaceAll("\\.json$", "") + "-skladi.txt"));
            } catch (Exception e) {
                this.errors.add("zapis skladov: " + e);
            }
        }
        try {
            writeJson(mc, reached);
            System.out.println("RWHARNESS-KONEC " + this.job.out);
        } catch (Exception e) {
            System.out.println("RWHARNESS-NAPAKA zapis " + this.job.out + ": " + e);
        }
        mc.shutdown();
    }

    private void writeJson(Minecraft mc, Phase reached) throws Exception {
        FrameStats st = FrameStats.of(this.frames, this.frameCount);
        long[] gc = gcTotals();
        double serverMs = -1.0;
        MinecraftServer srv = mc.getIntegratedServer();
        if (srv != null) {
            long s = 0L;
            for (long v : srv.tickTimeArray) {
                s += v;
            }
            serverMs = s / (double) srv.tickTimeArray.length / 1.0e6;
        }
        StringBuilder b = new StringBuilder();
        b.append("{\n");
        kv(b, "harness", "M5-K.0");
        kv(b, "svet", this.job.world);
        kv(b, "faza", reached.name());
        b.append("  \"napake\": [");
        for (int i = 0; i < this.errors.size(); i++) {
            b.append(i == 0 ? "" : ", ").append('"').append(esc(this.errors.get(i))).append('"');
        }
        b.append("],\n");
        num(b, "pricakovanoNpc", this.job.expectNpc);
        num(b, "npcVKlientu", this.npcLoaded);
        num(b, "upodobljenoMin", this.renderedSamples == 0 ? -1 : this.renderedMin);
        num(b, "upodobljenoMax", this.renderedMax);
        num(b, "upodobljenoVzorcev", this.renderedSamples);
        num(b, "zivihMin", this.renderedSamples == 0 ? -1 : this.aliveMin);
        num(b, "upodobljenoDelezMin", this.renderedShareMin == Double.MAX_VALUE ? -1.0 : this.renderedShareMin);
        num(b, "slicic", st.count);
        num(b, "sekundMerjenja", (this.measureEndNs - this.measureStartNs) / 1.0e9);
        num(b, "frameMsP50", st.p50 / 1.0e6);
        num(b, "frameMsP95", st.p95 / 1.0e6);
        num(b, "frameMsP99", st.p99 / 1.0e6);
        num(b, "frameMsMax", st.max / 1.0e6);
        num(b, "fps", st.fps);
        num(b, "fpsLow1", st.low1);
        num(b, "fpsLow01", st.low01);
        num(b, "gcStevilo", gc[0] - this.gcCount0);
        num(b, "gcMs", gc[1] - this.gcMillis0);
        num(b, "kameraOdmikPoz", this.maxPosDrift);
        num(b, "kameraOdmikKot", this.maxAngleDrift);
        num(b, "integriraniStreznikMsPovp", serverMs);
        num(b, "renderDistance", mc.gameSettings.renderDistanceChunks);
        num(b, "fov", mc.gameSettings.fovSetting);
        num(b, "vsync", mc.gameSettings.enableVsync ? 1 : 0);
        num(b, "limitFramerate", mc.gameSettings.limitFramerate);
        num(b, "hideGui", mc.gameSettings.hideGUI ? 1 : 0);
        num(b, "vzorcevSklada", this.sampler == null ? 0 : this.sampler.samples());
        num(b, "vzorcevSkladaRunnable", this.sampler == null ? 0 : this.sampler.samplesInState(Thread.State.RUNNABLE));
        num(b, "vzorcevalnikUsNaVzorec", this.sampler == null ? 0.0 : this.sampler.avgCostMicros());
        num(b, "rwHeadwearSkip", CustomNpcs.RwHeadwearSkip);
        num(b, "pokrivaloIzrisov", HeadwearSkip.draws);
        num(b, "pokrivaloOriginal", HeadwearSkip.fallbacks);
        num(b, "pokrivaloOriginalStanje", HeadwearSkip.stateFallbacks);
        num(b, "pokrivaloAnaliz", HeadwearSkip.analyses);
        num(b, "pokrivaloDelovNic", HeadwearSkip.partsNone);
        num(b, "pokrivaloDelovNekaj", HeadwearSkip.partsSome);
        num(b, "pokrivaloDelovVse", HeadwearSkip.partsAll);
        num(b, "rwLabelCache", CustomNpcs.RwLabelCache);
        num(b, "napisiZListom", LabelCache.drawsList);
        num(b, "napisiOriginal", LabelCache.drawsOriginal);
        num(b, "napisiPrevodov", LabelCache.compiles);
        LabelCheck lc = this.labelCheck;
        num(b, "napisiPreverba", lc == null ? -1 : (lc.ok() ? 1 : 0));
        if (lc != null) {
            num(b, "napisiNpc", lc.npcs);
            num(b, "napisiVDometu", lc.labelled);
            num(b, "napisiObcutljivost", lc.sensitivity);
            StringBuilder ls = new StringBuilder();
            for (LabelCheck.Series x : lc.series) {
                ls.append(ls.length() == 0 ? "" : " | ").append(x);
            }
            kv(b, "napisiSerije", ls.toString());
            kv(b, "napisiNapaka", lc.error);
        }
        HeadwearCheck hc = this.headwearCheck;
        num(b, "pokrivaloPreverba", hc == null ? -1 : (hc.ok() ? 1 : 0));
        if (hc != null) {
            num(b, "pokrivaloNpc", hc.npcs);
            num(b, "pokrivaloObcutljivost", hc.sensitivity);
            StringBuilder s = new StringBuilder();
            for (HeadwearCheck.Series x : hc.series) {
                s.append(s.length() == 0 ? "" : " | ").append(x);
            }
            kv(b, "pokrivaloSerije", s.toString());
            kv(b, "pokrivaloNapaka", hc.error);
        }
        num(b, "sirina", mc.displayWidth);
        num(b, "visina", mc.displayHeight);
        num(b, "heapMaxMb", Runtime.getRuntime().maxMemory() / (1024L * 1024L));
        b.append("  \"cam\": [").append(this.job.camX).append(", ").append(this.job.camY).append(", ")
                .append(this.job.camZ).append(", ").append(this.job.camYaw).append(", ").append(this.job.camPitch).append("]\n");
        b.append("}\n");
        File out = new File(this.job.out);
        File tmp = new File(out.getPath() + ".tmp");
        if (out.getParentFile() != null) {
            out.getParentFile().mkdirs();
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8);
        try {
            w.write(b.toString());
        } finally {
            w.close();
        }
        if (out.exists() && !out.delete()) {
            throw new IllegalStateException("ne morem prepisati " + out);
        }
        if (!tmp.renameTo(out)) {
            throw new IllegalStateException("ne morem preimenovati " + tmp);
        }
    }

    private static void kv(StringBuilder b, String k, String v) {
        b.append("  \"").append(k).append("\": \"").append(esc(v)).append("\",\n");
    }

    private static void num(StringBuilder b, String k, double v) {
        b.append("  \"").append(k).append("\": ").append(String.format(Locale.ROOT, "%.4f", v)).append(",\n");
    }

    private static void num(StringBuilder b, String k, long v) {
        b.append("  \"").append(k).append("\": ").append(v).append(",\n");
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
