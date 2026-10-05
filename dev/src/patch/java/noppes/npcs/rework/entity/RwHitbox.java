package noppes.npcs.rework.entity;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M3.8 (R6): odrivanje med entitetami za NPC-je z nacinom hitboxa SOLID ali SMART.
 *
 * <p>Vanilla trk ({@code EntityLivingBase.collideWithNearbyEntities}) poklice
 * {@code drugi.applyEntityCollision(ta)}, ta pa obe entiteti odrine z enako silo. NPC z
 * nacinom, ki ni ORIGINAL, preusmeri oba klica ({@code applyEntityCollision} in
 * {@code collideWithEntity}) sem: smer in velikost potiska sta vanilla, delez vsake strani
 * pa doloci {@link HitboxWeights}. Ista koda tece na klientu (igralec odriva sam sebe), zato
 * strezniku posreduje nacin v spawn podatkih.
 */
public final class RwHitbox {
    /** Kljucne besede v imenu predmeta za scite modov, ki ne razsirjajo ItemShield. */
    private static final String[] SHIELD_WORDS = {"shield", "buckler", "pavise"};
    private static final Map<Item, Boolean> SHIELD_BY_ITEM = new ConcurrentHashMap<>();
    private static volatile String extraSource = null;
    private static volatile Set<String> extraItems = new HashSet<>();

    private RwHitbox() {
    }

    /** Nacin entitete za trk: NPC-jev ucinkovit nacin, vse ostalo ORIGINAL. */
    public static int modeOf(Entity entity) {
        return entity instanceof EntityNPCInterface ? ((EntityNPCInterface) entity).getRwHitboxEffective() : HitboxWeights.ORIGINAL;
    }

    /** Trk entitet a in b po vanilla geometriji ({@code Entity.applyEntityCollision}) z utezmi. */
    public static void collide(Entity a, Entity b) {
        if (a.isRidingSameEntity(b) || a.noClip || b.noClip) {
            return;
        }
        double d0 = b.posX - a.posX;
        double d1 = b.posZ - a.posZ;
        double d2 = MathHelper.absMax(d0, d1);
        if (d2 < 0.009999999776482582D) {
            return;
        }
        d2 = (double) MathHelper.sqrt(d2);
        d0 = d0 / d2;
        d1 = d1 / d2;
        double d3 = 1.0D / d2;
        if (d3 > 1.0D) {
            d3 = 1.0D;
        }
        d0 = d0 * d3 * 0.05000000074505806D * (double) (1.0F - a.entityCollisionReduction);
        d1 = d1 * d3 * 0.05000000074505806D * (double) (1.0F - a.entityCollisionReduction);
        double[] s = HitboxWeights.shares(modeOf(a), mass(a), modeOf(b), mass(b));
        if (s[0] > 0.0 && !a.isBeingRidden()) {
            a.addVelocity(-d0 * s[0], 0.0D, -d1 * s[0]);
        }
        if (s[1] > 0.0 && !b.isBeingRidden()) {
            b.addVelocity(d0 * s[1], 0.0D, d1 * s[1]);
        }
    }

    public static double mass(Entity entity) {
        double shield = holdsShield(entity) ? Math.max(CustomNpcs.RwHitboxShieldWeight, 0) / 100.0 : 1.0;
        return HitboxWeights.mass(entity.width, entity.height, shield);
    }

    public static boolean holdsShield(Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        return isShield(living.getHeldItem(EnumHand.OFF_HAND), living) || isShield(living.getHeldItem(EnumHand.MAIN_HAND), living);
    }

    /**
     * Scit: Forge {@code Item.isShield} (vanilla, mod, ki ga prepise), podrazred {@code ItemShield}
     * (vecina modov s sciti), ime predmeta s "shield"/"buckler"/"pavise" ali predmet iz
     * {@code RwHitboxShieldItems} v configu (registrsko ime, npr. {@code mod:item}).
     */
    public static boolean isShield(ItemStack stack, EntityLivingBase holder) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item.isShield(stack, holder)) {
            return true;
        }
        Boolean known = SHIELD_BY_ITEM.get(item);
        if (known == null) {
            known = classify(item);
            SHIELD_BY_ITEM.put(item, known);
        }
        return known;
    }

    private static boolean classify(Item item) {
        if (item instanceof ItemShield) {
            return true;
        }
        ResourceLocation id = item.getRegistryName();
        if (id == null) {
            return false;
        }
        if (extraItems().contains(id.toString().toLowerCase(Locale.ROOT))) {
            return true;
        }
        String path = id.getResourcePath().toLowerCase(Locale.ROOT);
        for (String word : SHIELD_WORDS) {
            if (path.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> extraItems() {
        String src = CustomNpcs.RwHitboxShieldItems == null ? "" : CustomNpcs.RwHitboxShieldItems;
        if (!src.equals(extraSource)) {
            Set<String> parsed = new HashSet<>();
            for (String part : src.split(",")) {
                String t = part.trim().toLowerCase(Locale.ROOT);
                if (!t.isEmpty()) {
                    parsed.add(t);
                }
            }
            extraItems = parsed;
            extraSource = src;
            SHIELD_BY_ITEM.clear();
        }
        return extraItems;
    }
}
