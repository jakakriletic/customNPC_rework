package noppes.npcs.rework.diag;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Orodja odlocilnega poskusa M5-V.1 ({@code /rwdiag vd|track|spawnnbt}).
 *
 * <p>Niso funkcija moda: privzeto stanje igre ostane nespremenjeno, dokler skripta
 * ({@code v-run.ps1}) ukaza ne poslje.
 *
 * <ul>
 *   <li>{@code vd <n>}: {@code PlayerList.setViewDistance} med tekom, kot ga vanilla klice ob
 *       spremembi nastavitve; posodobi {@code PlayerChunkMap} in mejo sledilnika
 *       ({@code EntityTracker.setViewDistance}: {@code (n - 1) * 16}). Prizorisce se zato spawna
 *       pri 24 in nato zmanjsa na celico, chunki onkraj se izpraznijo kot na pravem strezniku;</li>
 *   <li>{@code track <bloki>}: <b>varianta A</b> - domet sledilnika NPC-jev (CNPC registrira
 *       {@code 64}) v {@code EntityRegistration} za vse razrede NPC-jev; velja za entitete, ki se
 *       zacnejo slediti po ukazu, {@code 0} vrne izvirne vrednosti;</li>
 *   <li>{@code spawnnbt}: porazdelitev velikosti dodatnih podatkov spawna
 *       ({@code EntityNPCInterface.writeSpawnData}, GZIP NBT, {@code Server.writeNBT}) nalozenih
 *       NPC-jev - to dobi klient ob vsakem vstopu NPC-ja v domet.</li>
 * </ul>
 */
public final class FarProbe {
    private static final Map<EntityRegistry.EntityRegistration, Integer> ORIGINAL_RANGE =
            new LinkedHashMap<EntityRegistry.EntityRegistration, Integer>();
    private static int trackRange;

    private FarProbe() {
    }

    public static String viewDistance(MinecraftServer server, int vd) {
        if (vd < 3 || vd > 32) {
            return "RWDIAG-VD-NAPAKA razdalja izven 3..32: " + vd;
        }
        server.getPlayerList().setViewDistance(vd);
        return viewStatus(server);
    }

    public static String viewStatus(MinecraftServer server) {
        int vd = server.getPlayerList().getViewDistance();
        int chunks = 0;
        int around = 0;
        int npcs = 0;
        for (WorldServer w : server.worlds) {
            if (w == null) {
                continue;
            }
            chunks += w.getChunkProvider().getLoadedChunkCount();
            // Nalozeni chunki v kvadratu (2 vd + 1)^2 okoli igralcev - te nalozi PlayerChunkMap.
            for (EntityPlayer p : w.playerEntities) {
                for (int dx = -vd; dx <= vd; dx++) {
                    for (int dz = -vd; dz <= vd; dz++) {
                        if (w.getChunkProvider().getLoadedChunk(p.chunkCoordX + dx, p.chunkCoordZ + dz) != null) {
                            around++;
                        }
                    }
                }
            }
            for (Entity e : w.loadedEntityList) {
                if (e instanceof EntityNPCInterface && !e.isDead) {
                    npcs++;
                }
            }
        }
        return "RWDIAG-VD vd=" + server.getPlayerList().getViewDistance()
                + " sledilnikMeja=" + server.getPlayerList().getEntityViewDistance()
                + " chunki=" + chunks + " okoliIgralcev=" + around + " npc=" + npcs;
    }

    public static synchronized String track(int range) {
        if (range < 0 || range > 512) {
            return "RWDIAG-TRACK-NAPAKA domet izven 0..512: " + range;
        }
        Field f;
        try {
            f = EntityRegistry.EntityRegistration.class.getDeclaredField("trackingRange");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            return "RWDIAG-TRACK-NAPAKA " + e;
        }
        int n = 0;
        try {
            for (EntityEntry entry : ForgeRegistries.ENTITIES.getValuesCollection()) {
                Class<? extends Entity> c = entry.getEntityClass();
                if (c == null || !EntityNPCInterface.class.isAssignableFrom(c)) {
                    continue;
                }
                EntityRegistry.EntityRegistration er = EntityRegistry.instance().lookupModSpawn(c, false);
                if (er == null) {
                    continue;
                }
                if (!ORIGINAL_RANGE.containsKey(er)) {
                    ORIGINAL_RANGE.put(er, er.getTrackingRange());
                }
                f.setInt(er, range == 0 ? ORIGINAL_RANGE.get(er) : range);
                n++;
            }
        } catch (IllegalAccessException e) {
            return "RWDIAG-TRACK-NAPAKA " + e;
        }
        trackRange = range;
        return trackStatus() + " registracij=" + n;
    }

    public static synchronized String trackStatus() {
        StringBuilder ranges = new StringBuilder();
        java.util.TreeSet<Integer> distinct = new java.util.TreeSet<Integer>();
        for (EntityRegistry.EntityRegistration er : ORIGINAL_RANGE.keySet()) {
            distinct.add(er.getTrackingRange());
        }
        for (Integer r : distinct) {
            ranges.append(ranges.length() == 0 ? "" : ",").append(r);
        }
        return "RWDIAG-TRACK domet=" + (trackRange == 0 ? "original" : String.valueOf(trackRange))
                + " vRegistraciji=" + (ranges.length() == 0 ? "nespremenjeno" : ranges.toString());
    }

    public static String spawnNbt(MinecraftServer server) {
        int[] sizes = new int[64];
        int n = 0;
        for (WorldServer w : server.worlds) {
            if (w == null) {
                continue;
            }
            for (Entity e : w.loadedEntityList) {
                if (!(e instanceof EntityNPCInterface) || e.isDead) {
                    continue;
                }
                ByteBuf buf = Unpooled.buffer();
                try {
                    ((EntityNPCInterface) e).writeSpawnData(buf);
                    if (n == sizes.length) {
                        sizes = Arrays.copyOf(sizes, n * 2);
                    }
                    sizes[n++] = buf.readableBytes();
                } finally {
                    buf.release();
                }
            }
        }
        return "RWDIAG-SPAWNNBT " + sizeSummary(Arrays.copyOf(sizes, n));
    }

    /** {@code n=.. min=.. p50=.. p95=.. max=.. vsota=..} v bajtih; percentil z rangom ceil(p * n). */
    public static String sizeSummary(int[] sizes) {
        int[] s = sizes.clone();
        Arrays.sort(s);
        long sum = 0L;
        for (int v : s) {
            sum += v;
        }
        return String.format(Locale.ROOT, "n=%d min=%d p50=%d p95=%d max=%d vsota=%d", s.length,
                s.length == 0 ? 0 : s[0], rank(s, 0.50), rank(s, 0.95), s.length == 0 ? 0 : s[s.length - 1], sum);
    }

    private static int rank(int[] sorted, double p) {
        if (sorted.length == 0) {
            return 0;
        }
        int r = (int) Math.ceil(p * sorted.length);
        return sorted[Math.max(1, r) - 1];
    }
}
