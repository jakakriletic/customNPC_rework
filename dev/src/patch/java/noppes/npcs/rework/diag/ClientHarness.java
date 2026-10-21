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
import java.util.concurrent.atomic.AtomicLong;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.far.FarClient;
import noppes.npcs.rework.far.ImpostorBaker;
import noppes.npcs.rework.render.HeadwearSkip;
import noppes.npcs.rework.render.EyesList;
import noppes.npcs.rework.render.Lod;
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
 *
 * <p>M5-V.1 ({@code connect=}): namesto integriranega sveta se klient poveze na dedicirani
 * streznik, ki ga vodi {@code v-run.ps1}. Ukazov ne izvaja; po vstopu izpise
 * {@code RWHARNESS-IGRALEC <ime>}, skripta prek konzole nastavi nacin, teleport in prizorisce ter
 * na koncu poslje {@code say RWHARNESS-SCENA}, sele nato se NPC-je steje. Dodatno belezi zicne
 * bajte s streznika (stevec na zacetku Netty cevovoda, pred razpakiranjem) in cas klientskega
 * ticka (START do END {@code ClientTickEvent}).
 *
 * <p>M5-V.2: po koncu merjenja in zapisu JSON klient na dediciranem strezniku ostane povezan, dokler
 * skripta ne poslje {@code say RWHARNESS-IZHOD} (najvec 120 s), da streznik stevce na igralca
 * ({@code /rwfar}, {@code /rwdiag vd}) izpise, ko je igralec se prijavljen.
 */
@SideOnly(Side.CLIENT)
public final class ClientHarness {
    private enum Phase { MENU, JOINING, SCENE, PRELOAD, COMMANDS, SETTLE, WARMUP, MEASURE, DONE }

    private static final int MAX_FRAMES = 1 << 20;
    private static final long JOIN_TIMEOUT_NS = 180L * 1000000000L;
    private static final long NPC_TIMEOUT_NS = 180L * 1000000000L;
    /** M5-V.1: skripta pred sporocilom generira do 49 x 49 chunkov in spawna do 5.000 NPC-jev. */
    private static final long SCENE_TIMEOUT_NS = 900L * 1000000000L;
    static final String SCENE_MARKER = "RWHARNESS-SCENA";
    /** M5-V.2: skripta je prebrala strezniske stevce; klient se lahko ugasne. */
    static final String EXIT_MARKER = "RWHARNESS-IZHOD";
    private static final long EXIT_TIMEOUT_NS = 120L * 1000000000L;

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
    private int renderWeightResets;
    private int renderWeightResetsMeasure;
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
    private EyesCheck eyesCheck;
    /** M5-V.4: izid primerjave impostor - model. */
    private ImpostorCheck impostorCheck;
    private final List<String> errors = new ArrayList<String>();
    /** M5-V.1: klientski tick (START..END) med merjenjem in zicni bajti s streznika. */
    private final long[] ticks = new long[MAX_FRAMES];
    private int tickCount;
    private long tickStart;
    private volatile boolean sceneReady;
    private boolean exitPending;
    private volatile boolean exitReady;
    private final AtomicLong bytesIn = new AtomicLong();
    private boolean bytesInstalled;
    private long bytesAtWarmup = -1L;
    private long bytesAtMeasure = -1L;
    private long bytesAtEnd = -1L;
    /** M5-V.2: nadomestki v shrambi in koristni bajti kanala rwfar ob zacetku merjenja. */
    private int farAtMeasure = -1;
    private long farBytesAtMeasure = -1L;
    private long farBytesAtEnd = -1L;

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
        if (ev.phase == TickEvent.Phase.START) {
            this.tickStart = System.nanoTime();
            return;
        }
        if (ev.phase != TickEvent.Phase.END) {
            return;
        }
        if (this.phase == Phase.MEASURE && this.tickStart != 0L && this.tickCount < MAX_FRAMES) {
            this.ticks[this.tickCount++] = System.nanoTime() - this.tickStart;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (this.job.renderWeight > 0.0 && this.phase.ordinal() > Phase.JOINING.ordinal()
                && Entity.getRenderDistanceWeight() != this.job.renderWeight) {
            // M5-V.1: vsak RenderGlobal.loadRenderers (tudi po vstopu) jo vrne na clamp(RD / 8, 1, 2,5).
            Entity.setRenderDistanceWeight(this.job.renderWeight);
            this.renderWeightResets++;
            if (this.phase == Phase.MEASURE) {
                this.renderWeightResetsMeasure++;
            }
        }
        try {
            switch (this.phase) {
                case MENU:
                    if (mc.currentScreen instanceof GuiMainMenu && this.job.connectHost != null) {
                        enter(Phase.JOINING);
                        System.out.println("RWHARNESS-POVEZAVA " + this.job.connectHost + ":" + this.job.connectPort);
                        mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, this.job.connectHost, this.job.connectPort));
                    } else if (mc.currentScreen instanceof GuiMainMenu) {
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
                    if (this.job.connectHost != null && mc.player != null && mc.world != null
                            && mc.getConnection() != null && mc.currentScreen == null) {
                        applySettings(mc);
                        installByteCounter(mc);
                        System.out.println("RWHARNESS-IGRALEC " + mc.player.getName());
                        enter(Phase.SCENE);
                    } else if (mc.player != null && mc.world != null && mc.getIntegratedServer() != null
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
                case SCENE:
                    // Kamero postavi streznik (tp); pred tem bi jo pribijanje vleklo v nepripravljen svet.
                    if (this.sceneReady) {
                        pinCamera(mc, false);
                        enter(Phase.SETTLE);
                    } else if (System.nanoTime() - this.phaseStart > SCENE_TIMEOUT_NS) {
                        fail("streznik ni poslal " + SCENE_MARKER + " v 900 s");
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
                            this.bytesAtWarmup = this.bytesIn.get();
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
                            || (this.job.labelCheck && this.labelCheck == null)
                            || (this.job.eyesCheck && this.eyesCheck == null)
                            || (this.job.impostorCheck && this.impostorCheck == null))) {
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
                        this.tickCount = 0;
                        this.bytesAtMeasure = this.bytesIn.get();
                        this.farAtMeasure = FarClient.size();
                        this.farBytesAtMeasure = FarClient.payloadBytes;
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
                        this.bytesAtEnd = this.bytesIn.get();
                        this.farBytesAtEnd = FarClient.payloadBytes;
                        finish(mc);
                    }
                    break;
                case DONE:
                    if (this.exitPending && (this.exitReady || System.nanoTime() - this.phaseStart > EXIT_TIMEOUT_NS)) {
                        this.exitPending = false;
                        System.out.println("RWHARNESS-IZHOD " + (this.exitReady ? "signal" : "cas"));
                        mc.shutdown();
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
    public void onChat(ClientChatReceivedEvent ev) {
        if (this.phase == Phase.SCENE && ev.getMessage() != null
                && ev.getMessage().getUnformattedText().contains(SCENE_MARKER)) {
            this.sceneReady = true;
        }
        if (this.exitPending && ev.getMessage() != null
                && ev.getMessage().getUnformattedText().contains(EXIT_MARKER)) {
            this.exitReady = true;
        }
    }

    /** M5-V.1: zicni bajti s streznika; prvi v cevovodu, torej pred razpakiranjem in razrezom paketov. */
    private void installByteCounter(Minecraft mc) {
        if (this.bytesInstalled) {
            return;
        }
        this.bytesInstalled = true;
        final AtomicLong counter = this.bytesIn;
        mc.getConnection().getNetworkManager().channel().pipeline().addFirst("rwharness_bytes", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof ByteBuf) {
                    counter.addAndGet(((ByteBuf) msg).readableBytes());
                }
                ctx.fireChannelRead(msg);
            }
        });
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
        if (this.job.eyesCheck) {
            EyesCheck r = EyesCheck.run(Minecraft.getMinecraft(), ev.getPartialTicks());
            StringBuilder s = new StringBuilder();
            for (EyesCheck.Series x : r.series) {
                s.append(" | ").append(x);
            }
            System.out.println("RWHARNESS-OCI ok=" + r.ok() + " npc=" + r.npcs + " obcutljivost=" + r.sensitivity + "/" + r.sensitivityZoom + s + " " + r.error);
            this.eyesCheck = r;
        }
        if (this.job.impostorCheck) {
            ImpostorCheck r = ImpostorCheck.run(Minecraft.getMinecraft());
            StringBuilder s = new StringBuilder();
            for (ImpostorCheck.Series x : r.series) {
                s.append(" | ").append(x);
            }
            System.out.println("RWHARNESS-IMPOSTOR ok=" + r.ok() + " videzov=" + r.series.size() + "/" + r.expected + " obcutljivost=" + r.sensitivity + s + " " + r.error);
            this.impostorCheck = r;
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
        if (this.job.eyesList >= 0) {
            CustomNpcs.RwEyesList = this.job.eyesList;
        }
        if (this.job.lodStart >= 0) {
            CustomNpcs.RwLodStart = this.job.lodStart;
        }
        if (this.job.farView >= 0) {
            // M5-V.2: FarClient poslje HELLO v naslednjem ticku (nov svet z vklopljenim stikalom).
            CustomNpcs.RwFarView = this.job.farView;
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
        if (this.job.renderWeight > 0.0) {
            // M5-V.1 varianta A: loadRenderers jo pravkar nastavi na RD / 8 (1..2,5).
            Entity.setRenderDistanceWeight(this.job.renderWeight);
        }
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
        if (this.job.connectHost != null && reached == Phase.MEASURE && this.errors.isEmpty()) {
            this.exitPending = true;
            System.out.println("RWHARNESS-CAKAM " + EXIT_MARKER);
            return;
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
        kv(b, "povezava", this.job.connectHost == null ? "integrirani" : this.job.connectHost + ":" + this.job.connectPort);
        FrameStats tk = FrameStats.of(this.ticks, this.tickCount);
        num(b, "klientTickov", tk.count);
        num(b, "klientTickMsP50", tk.p50 / 1.0e6);
        num(b, "klientTickMsP95", tk.p95 / 1.0e6);
        num(b, "klientTickMsP99", tk.p99 / 1.0e6);
        num(b, "klientTickMsMax", tk.max / 1.0e6);
        num(b, "bajtovPredMeritvijo", this.bytesAtWarmup);
        boolean bytesOk = this.bytesAtEnd >= 0L && this.bytesAtMeasure >= 0L;
        num(b, "bajtovMeritev", bytesOk ? this.bytesAtEnd - this.bytesAtMeasure : -1L);
        double measured = (this.measureEndNs - this.measureStartNs) / 1.0e9;
        num(b, "bajtovNaS", bytesOk && measured > 0.0 ? (this.bytesAtEnd - this.bytesAtMeasure) / measured : -1.0);
        num(b, "renderWeight", Entity.getRenderDistanceWeight());
        num(b, "renderWeightPonastavitev", this.renderWeightResets);
        num(b, "renderWeightPonastavitevMeritev", this.renderWeightResetsMeasure);
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
        num(b, "rwEyesList", CustomNpcs.RwEyesList);
        num(b, "ociZListom", EyesList.drawsList);
        num(b, "ociOriginal", EyesList.drawsOriginal);
        num(b, "ociPrevodov", EyesList.compiles);
        num(b, "rwLodStart", CustomNpcs.RwLodStart);
        num(b, "lodDalec", Lod.farDraws);
        num(b, "lodBlizu", Lod.nearDraws);
        num(b, "rwFarView", CustomNpcs.RwFarView);
        num(b, "farHello", FarClient.hellos);
        num(b, "farVKlientuZacetek", this.farAtMeasure);
        num(b, "farVKlientu", FarClient.size());
        num(b, "farSporocil", FarClient.messages);
        num(b, "farVnosov", FarClient.upserts);
        num(b, "farOdstranitev", FarClient.removes);
        num(b, "farBajtov", FarClient.payloadBytes);
        num(b, "farBajtovMeritev", this.farBytesAtEnd >= 0 ? this.farBytesAtEnd - this.farBytesAtMeasure : -1L);
        num(b, "farVidezov", FarClient.looks());
        num(b, "farVidezZahtev", FarClient.lookReqIds);
        num(b, "farVidezOdgovorov", FarClient.lookReplies);
        num(b, "farVidezNapak", FarClient.lookErrors);
        num(b, "farBrezVideza", FarClient.missingLooks());
        num(b, "impPeceno", ImpostorBaker.baked);
        num(b, "impVAtlasu", ImpostorBaker.inAtlas());
        num(b, "impBrezPeke", ImpostorBaker.missing());
        num(b, "impNapak", ImpostorBaker.failures);
        num(b, "impOdlozenih", ImpostorBaker.deferred);
        num(b, "impObrezanih", ImpostorBaker.clipped);
        num(b, "impPonovitev", ImpostorBaker.retries);
        num(b, "impIzrinjenih", ImpostorBaker.evictions());
        num(b, "impBrezFbo", ImpostorBaker.noFbo ? 1 : 0);
        num(b, "impPekaMsPovp", ImpostorBaker.baked > 0 ? ImpostorBaker.bakeNs / 1.0e6 / ImpostorBaker.baked : -1.0);
        num(b, "impPekaMsMax", ImpostorBaker.bakeNsMax / 1.0e6);
        ImpostorCheck ic = this.impostorCheck;
        num(b, "impPreverba", ic == null ? -1 : (ic.ok() ? 1 : 0));
        if (ic != null) {
            num(b, "impVidezov", ic.series.size());
            num(b, "impObcutljivost", ic.sensitivity);
            StringBuilder is = new StringBuilder();
            for (ImpostorCheck.Series x : ic.series) {
                is.append(is.length() == 0 ? "" : " | ").append(x);
            }
            kv(b, "impSerije", is.toString());
            kv(b, "impNapaka", ic.error);
        }
        EyesCheck ec = this.eyesCheck;
        num(b, "ociPreverba", ec == null ? -1 : (ec.ok() ? 1 : 0));
        if (ec != null) {
            num(b, "ociNpc", ec.npcs);
            num(b, "ociObcutljivost", ec.sensitivity);
            num(b, "ociObcutljivostPovecava", ec.sensitivityZoom);
            StringBuilder es = new StringBuilder();
            for (EyesCheck.Series x : ec.series) {
                es.append(es.length() == 0 ? "" : " | ").append(x);
            }
            kv(b, "ociSerije", es.toString());
            kv(b, "ociNapaka", ec.error);
        }
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
