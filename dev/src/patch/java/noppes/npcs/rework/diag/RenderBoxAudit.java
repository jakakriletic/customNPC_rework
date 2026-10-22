package noppes.npcs.rework.diag;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.far.FarRender;
import noppes.npcs.rework.far.ImpostorAtlas;
import noppes.npcs.rework.render.RenderBox;

/**
 * Harness M5-K K12: okvir upodobitve ({@code getRenderBoundingBox}) in domet ({@code isInRangeToRender3d}) vseh NPC-jev
 * v svetu klienta ob koncu merjenja. Obris modela je pecen videz L2 ({@link FarRender#bakedLook}): sirina simetricnega
 * obreza 8 pogledov okoli navpicne osi ter spodnji in zgornji rob glede na noge, tj. izmerjen obseg modela v pozi
 * mirovanja. Presezek je, koliko obris sega cez okvir (vodoravno od osi entitete ali navpicno), v blokih.
 */
@SideOnly(Side.CLIENT)
public final class RenderBoxAudit {
    /** Dovoljen presezek obrisa na Size 5 (bloki): ~4 piksle peke (FB 256 / ~3,4 bloka) in rob plasti pokrivala. */
    public static final double TOL = 0.05;

    private RenderBoxAudit() {
    }

    /**
     * {@code npc=N;niVsebuje=N;izvenDometa=N;brezHitboxa=N;videzov=N;videzIzven=N;najvecPresega=X;ime=domet/vsebuje/presega,...}
     * ({@code domet} 1 = v dometu upodobitve od kamere, {@code vsebuje} 1 = okvir objame hitbox, {@code presega} v
     * blokih ali {@code -} brez pecenega videza); posebni NPC-ji po predponi {@code prefix} (ali brez).
     */
    public static String summary(WorldClient world, Entity view, String prefix) {
        int n = 0;
        int notContains = 0;
        int outOfRange = 0;
        int noHitbox = 0;
        int looks = 0;
        int lookOut = 0;
        double worst = Double.NEGATIVE_INFINITY;
        Map<String, String> special = new TreeMap<String, String>();
        if (world != null) {
            for (Entity en : world.loadedEntityList) {
                if (!(en instanceof EntityNPCInterface)) {
                    continue;
                }
                EntityNPCInterface npc = (EntityNPCInterface) en;
                ++n;
                AxisAlignedBB box = npc.getRenderBoundingBox();
                AxisAlignedBB bb = npc.getEntityBoundingBox();
                boolean contains = box.minX <= bb.minX && box.minY <= bb.minY && box.minZ <= bb.minZ
                        && box.maxX >= bb.maxX && box.maxY >= bb.maxY && box.maxZ >= bb.maxZ;
                if (!contains) {
                    ++notContains;
                }
                boolean in = view == null || npc.isInRangeToRender3d(view.posX, view.posY, view.posZ);
                if (!in) {
                    ++outOfRange;
                }
                if (npc.width < RenderBox.NO_HITBOX) {
                    ++noHitbox;
                }
                String over = "-";
                ImpostorAtlas.Entry ae = FarRender.bakedLook(npc);
                if (ae != null) {
                    ++looks;
                    double o = excess(box, npc.posX, npc.posY, npc.posZ, ae.width, ae.bottom, ae.height);
                    if (o > TOL * Math.max(1.0, npc.display.getSize() / 5.0)) {
                        ++lookOut;
                    }
                    worst = Math.max(worst, o);
                    over = String.format(java.util.Locale.ROOT, "%.3f", o);
                }
                String name = npc.display.getName();
                if (prefix != null && name != null && name.startsWith(prefix)) {
                    special.put(name, (in ? 1 : 0) + "/" + (contains ? 1 : 0) + "/" + over);
                }
            }
        }
        StringBuilder b = new StringBuilder();
        b.append("npc=").append(n).append(";niVsebuje=").append(notContains).append(";izvenDometa=").append(outOfRange)
                .append(";brezHitboxa=").append(noHitbox).append(";videzov=").append(looks).append(";videzIzven=").append(lookOut)
                .append(";najvecPresega=").append(looks == 0 ? "-" : String.format(java.util.Locale.ROOT, "%.3f", worst));
        for (Map.Entry<String, String> me : special.entrySet()) {
            b.append(";").append(me.getKey()).append("=").append(me.getValue());
        }
        return b.toString();
    }

    /**
     * Najvecji presezek obrisa (polovica sirine {@code width} okoli osi, od {@code bottom} do {@code bottom + height}
     * nad nogami) cez okvir {@code box} entitete na polozaju {@code x, y, z}; negativno = obris je znotraj z rezervo.
     */
    static double excess(AxisAlignedBB box, double x, double y, double z, double width, double bottom, double height) {
        double half = Math.min(Math.min(box.maxX - x, x - box.minX), Math.min(box.maxZ - z, z - box.minZ));
        double o = width * 0.5 - half;
        o = Math.max(o, (y + bottom + height) - box.maxY);
        o = Math.max(o, box.minY - (y + bottom));
        return o;
    }
}
