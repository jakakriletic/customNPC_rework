package noppes.npcs.rework.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import noppes.npcs.ModelData;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M5-K K12 (D-052): okvir upodobitve NPC-ja ({@code RwRenderBox}).
 *
 * <p>CNPC ne prepise {@code getRenderBoundingBox} ne {@code isInRangeToRenderDist}, zato vanilla frustum
 * ({@code Render.shouldRender}) in okluzija Entity Culling (RenderLib {@code IBoundingBoxCache}) uporabljata
 * hitbox. Ta je ozji od modela (0,6 proti ~1,0 z rokami pri Size 5, lezec NPC 0,8 proti dolzini telesa), NPC brez
 * hitboxa pa ima sirino 1e-5: okvir je navpicna crta, domet upodobitve (povprecni rob x 64) pa 0,63 namesto 1,03
 * (~40 namesto ~66 blokov pri RD 8).
 *
 * <p>Okvir upodobitve je unija hitboxa in okvira modela, zato vanilla ne izloci nicesar, kar je izlocil original.
 * Domet: stojec NPC s hitboxom ima isti domet kot v originalu (isti izracun); NPC brez hitboxa dobi domet sirine, ki
 * bi jo imel s hitboxom ({@code updateHitbox} brez {@code getHasHitbox}); lezec (ali ubit) NPC, ki ni morf, dobi domet
 * stojecega: hitbox 0,8 x 0,4 da domet 0,67 (~53 blokov pri RD 10), model pa je dolg kot stojec NPC (1,03, ~82 blokov).
 */
public final class RenderBox {
    /** Sirina pod to mejo pomeni "brez hitboxa" ({@code updateHitbox} postavi 1e-5). */
    public static final float NO_HITBOX = 1.0E-3f;
    /** Polovica sirine humanoidnega modela z rokami pri Size 5: telo 8 + roki 2 x 4 teksle = 1,0 bloka (skala 0,9375). */
    static final double HUMANOID_HALF = 0.5;
    /**
     * Polovica sirine humanoida s predmetom v roki pri Size 5: roka na 0,375 od osi, mec sega ~0,8 naprej
     * (sqrt(0,375^2 + 0,8^2) = 0,88), zaokrozeno navzgor.
     */
    static final double HELD_HALF = 1.0;
    /** Visina lezecega modela pri Size 5 (globina telesa in rok, zaokrozeno navzgor). */
    static final double LYING_HEIGHT = 0.6;
    /** Visina okvira morfa v enotah max(sirina, visina) entitete morfa. */
    static final double MORPH_HEIGHT = 1.5;

    private static final EnumParts[] PARTS = {EnumParts.HEAD, EnumParts.BODY, EnumParts.ARM_LEFT, EnumParts.ARM_RIGHT, EnumParts.LEG_LEFT, EnumParts.LEG_RIGHT};

    private RenderBox() {
    }

    /**
     * Sirina, ki doloca domet upodobitve: hitbox, ali brez hitboxa sirina, ki bi jo NPC imel s hitboxom
     * ({@code EntityNPCInterface/EntityCustomNpc.updateHitbox}); lezec NPC (ne morf) sirino stojecega.
     *
     * @param morphWidth sirina entitete morfa ali {@code < 0}, ce NPC ni morf
     */
    public static double rangeWidth(float width, int size, boolean lying, float morphWidth) {
        if (lying && morphWidth < 0.0f) {
            // updateHitbox da lezecemu 0,8 x Size / 5; stoje bi imel 0,6 (domet stojecega 1,03 > lezecega 0,67).
            return 0.6f / 5.0f * size;
        }
        if (width >= NO_HITBOX) {
            return width;
        }
        if (morphWidth >= 0.0f) {
            return Math.max(0.1f, morphWidth / 5.0f * size);
        }
        return 0.6f / 5.0f * size;
    }

    /** Visina, ki doloca domet upodobitve: hitbox, lezec NPC (ne morf) visino stojecega ({@code baseHeight x Size / 5}). */
    public static double rangeHeight(float height, int size, boolean lying, float morphWidth, float baseHeight) {
        if (lying && morphWidth < 0.0f) {
            return Math.max(height, baseHeight / 5.0f * size);
        }
        return height;
    }

    /**
     * Polovica sirine okvira upodobitve (vodoravno, okoli polozaja). Model morfa je lahko daljsi od svojega hitboxa
     * (prasic: hitbox 0,9, gobec do ~0,9 od sredine), zato vecja od sirine in visine entitete morfa.
     */
    public static double halfWidth(float width, int size, boolean lying, float morphWidth, float morphHeight, float baseHeight, float partScale,
            boolean held) {
        double s = size / 5.0;
        double model;
        if (morphWidth >= 0.0f) {
            model = Math.max(rangeWidth(width, size, lying, morphWidth) * 0.5, Math.max(morphWidth, morphHeight) / 5.0 * size);
        } else if (lying) {
            // Lezec model lezi v smeri telesa; tocka vrtenja in smer nista znani, zato cela dolzina na vsako stran.
            model = baseHeight * s;
        } else {
            model = (held ? HELD_HALF : HUMANOID_HALF) * s * Math.max(1.0f, partScale);
        }
        return Math.max(Math.max(width * 0.5, rangeWidth(width, size, lying, morphWidth) * 0.5), model);
    }

    /**
     * Visina okvira upodobitve nad nogami. Morf: oblika modela ni znana (glava prasica sega do 1,0 pri hitboxu 0,9),
     * zato {@code MORPH_HEIGHT x max(sirina, visina)} entitete morfa.
     */
    public static double height(float height, int size, boolean lying, float morphWidth, float morphHeight) {
        if (morphWidth >= 0.0f) {
            return Math.max(height, MORPH_HEIGHT * Math.max(morphWidth, morphHeight) / 5.0 * size);
        }
        return lying ? Math.max(height, LYING_HEIGHT * size / 5.0) : height;
    }

    /** Vanilla merilo dometa ({@code Entity.isInRangeToRenderDist}) za okvir s podano sirino in visino. */
    public static boolean inRange(double distSq, double width, double height, double weight) {
        double d0 = (width + height + width) / 3.0;
        if (Double.isNaN(d0)) {
            d0 = 1.0;
        }
        d0 = d0 * 64.0 * weight;
        return distSq < d0 * d0;
    }

    /** Najvecja skala modela (x, z) med deli telesa, vsaj 1. */
    static float partScale(ModelData data) {
        float m = 1.0f;
        for (EnumParts p : PARTS) {
            ModelPartConfig c = data.getPartConfig(p);
            m = Math.max(m, Math.max(c.scaleX, c.scaleZ));
        }
        return m;
    }

    private static boolean lying(EntityNPCInterface npc) {
        return npc.currentAnimation == 2 || npc.currentAnimation == 7 || npc.deathTime > 0;
    }

    /** Entiteta morfa ali {@code null} (enako kot {@code EntityCustomNpc.updateHitbox}). */
    private static EntityLivingBase morph(EntityNPCInterface npc) {
        if (npc instanceof EntityCustomNpc) {
            ModelData data = ((EntityCustomNpc) npc).modelData;
            return data == null ? null : data.getEntity(npc);
        }
        return null;
    }

    /** Okvir upodobitve: unija hitboxa in okvira modela. */
    public static AxisAlignedBB box(EntityNPCInterface npc) {
        AxisAlignedBB bb = npc.getEntityBoundingBox();
        int size = npc.display.getSize();
        boolean lying = lying(npc);
        EntityLivingBase morph = morph(npc);
        float scale = npc instanceof EntityCustomNpc && morph == null && ((EntityCustomNpc) npc).modelData != null
                ? partScale(((EntityCustomNpc) npc).modelData) : 1.0f;
        double hw = halfWidth(npc.width, size, lying, morph == null ? -1.0f : morph.width, morph == null ? -1.0f : morph.height,
                npc.baseHeight, scale, !npc.getHeldItemMainhand().isEmpty() || !npc.getHeldItemOffhand().isEmpty());
        double h = height(npc.height, size, lying, morph == null ? -1.0f : morph.width, morph == null ? -1.0f : morph.height);
        return new AxisAlignedBB(Math.min(bb.minX, npc.posX - hw), bb.minY, Math.min(bb.minZ, npc.posZ - hw),
                Math.max(bb.maxX, npc.posX + hw), Math.max(bb.maxY, bb.minY + h), Math.max(bb.maxZ, npc.posZ + hw));
    }

    /**
     * Ali domet upodobitve racuna {@link #inRange(EntityNPCInterface, double)}: NPC brez hitboxa ali lezec NPC, ki ni
     * morf; stojec NPC s hitboxom gre po originalu ({@code super.isInRangeToRenderDist}), da je izid bitno enak.
     */
    public static boolean ownRange(EntityNPCInterface npc) {
        return npc.width < NO_HITBOX || lying(npc) && morph(npc) == null;
    }

    /** Domet upodobitve s sirino in visino, ki bi ju NPC imel s hitboxom stoje ({@link #ownRange}). */
    public static boolean inRange(EntityNPCInterface npc, double distSq) {
        EntityLivingBase morph = morph(npc);
        int size = npc.display.getSize();
        boolean lying = lying(npc);
        float mw = morph == null ? -1.0f : morph.width;
        return inRange(distSq, rangeWidth(npc.width, size, lying, mw), rangeHeight(npc.height, size, lying, mw, npc.baseHeight),
                Entity.getRenderDistanceWeight());
    }
}
