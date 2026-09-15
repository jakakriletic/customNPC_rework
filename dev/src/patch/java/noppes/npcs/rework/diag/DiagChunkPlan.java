package noppes.npcs.rework.diag;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Izracun nabora chunkov, ki jih mora merilni scenarij prisilno nalozit.
 *
 * <p>Razred je namenoma brez Minecraft tipov: chunk je zapakiran v {@code long}, koordinate
 * so navadni {@code int}. Zaradi tega je edini del {@link DiagChunkLoader}, ki se da
 * enotsko testirati brez zagona igre, in prav v tem delu je logika, ki se lahko zmoti —
 * pretvorba blok/chunk pri negativnih koordinatah, odstranjevanje podvojenih chunkov in
 * obnasanje ob preseganju proracuna.
 *
 * <p>Zakaj to sploh obstaja, je zapisano v {@link DiagChunkLoader}.
 */
public final class DiagChunkPlan {
    /** Vec kot to nima smisla: 8 chunkov je 128 blokov na vsako stran. */
    public static final int MAX_RADIUS = 8;

    private DiagChunkPlan() {
    }

    /** Zapakira chunk koordinati v en {@code long}. */
    public static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | ((long) chunkZ & 0xFFFFFFFFL);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    public static int unpackZ(long packed) {
        return (int) packed;
    }

    /**
     * Blokovna koordinata v chunk koordinato.
     *
     * <p>Deljenje s 16 tu ne deluje: {@code -1 / 16} je 0, chunk bloka -1 pa je -1.
     * Vanilla uporablja {@code MathHelper.floor(x) >> 4} in isto dela ta metoda.
     */
    public static int chunkOf(double blockCoord) {
        return (int) Math.floor(blockCoord) >> 4;
    }

    /**
     * Iz sredisc naredi nabor chunkov, ki jih je treba drzati naloge.
     *
     * <p>Vrstni red ni nakljucen in je bistven: najprej gredo <b>vsa sredisca</b>, sele nato
     * obroc 1 okoli vseh sredisc, nato obroc 2 in tako naprej. Ce proracun zmanjka, so zato
     * odrezani robni chunki, ne pa chunk kaksnega NPC-ja. Obratni vrstni red (cel obroc
     * okoli prvega NPC-ja, potem drugi NPC) bi pri velikem scenariju pustil zadnje NPC-je
     * povsem brez pokritja in meritev bi tiho merila mesanico dveh stanj.
     *
     * @param centers  zapakirani chunki, v katerih so entitete
     * @param radius   koliko obrocev okoli vsakega sredisca; 0 pomeni samo sredisca
     * @param maxChunks zgornja meja; ob dosegu se vracanje ustavi
     * @return urejen nabor brez podvojitev, velikosti najvec {@code maxChunks}
     */
    public static LinkedHashSet<Long> expand(Collection<Long> centers, int radius, int maxChunks) {
        LinkedHashSet<Long> out = new LinkedHashSet<Long>();
        if (centers == null || maxChunks <= 0) {
            return out;
        }
        for (Long center : centers) {
            if (center == null) {
                continue;
            }
            if (out.size() >= maxChunks) {
                return out;
            }
            out.add(center);
        }
        int rings = radius < 0 ? 0 : Math.min(radius, MAX_RADIUS);
        for (int ring = 1; ring <= rings; ring++) {
            for (Long center : centers) {
                if (center == null) {
                    continue;
                }
                int cx = unpackX(center);
                int cz = unpackZ(center);
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                            continue;
                        }
                        if (out.size() >= maxChunks) {
                            return out;
                        }
                        out.add(pack(cx + dx, cz + dz));
                    }
                }
            }
        }
        return out;
    }
}
