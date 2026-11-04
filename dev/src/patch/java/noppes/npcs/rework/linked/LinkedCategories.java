package noppes.npcs.rework.linked;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

/**
 * M13.2 (Q389): what a Linked NPC group passes on to its members with {@code RwLinkedGroups=1}.
 *
 * <p>Group data stays complete, so a category can be switched on later; {@link #filter} is applied when the data
 * reaches a member. The key lists follow the writers of DataDisplay, DataStats (with DataMelee and DataRanged),
 * DataAI, DataInventory and DataTransform. Role and job keys depend on the role, so every other key is
 * {@link #ADVANCED}.
 */
public final class LinkedCategories {
    public static final int APPEARANCE = 1;
    public static final int NAME = 1 << 1;
    public static final int STATS = 1 << 2;
    public static final int AI = 1 << 3;
    public static final int INVENTORY = 1 << 4;
    public static final int ADVANCED = 1 << 5;
    public static final int SCRIPTS = 1 << 6;
    public static final int TRANSFORM = 1 << 7;
    public static final int ALL = (1 << 8) - 1;
    /** What the original transfers: everything but scripts (Q389). */
    public static final int DEFAULT = ALL & ~SCRIPTS;

    private static final String[] NAMES =
            {"appearance", "name", "stats", "ai", "inventory", "advanced", "scripts", "transform"};
    private static final Set<String> NAME_KEYS = keys("Name", "Title");
    private static final Set<String> APPEARANCE_KEYS = keys("BossBar", "BossColor", "CloakTexture", "GlowTexture",
            "IsStatue", "NoLivingAnimation", "NpcVisible", "ShowName", "Size", "SkinColor", "SkinUrl", "SkinUsername",
            "Texture", "UsingSkinUrl", "ModelData");
    private static final Set<String> STATS_KEYS = keys("AggroRange", "BurnInSun", "CanDrown", "CombatRegen",
            "CreatureType", "HealthRegen", "HideBodyWhenKilled", "IgnoreCobweb", "ImmuneToFire", "MaxHealth",
            "NoFallDamage", "PotionImmune", "Resistances", "RespawnTime", "SpawnCycle",
            "AttackRange", "AttackSpeed", "AttackStrenght", "KnockBack", "PotionAmp", "PotionDuration", "PotionEffect",
            "Accuracy", "AimWhileShooting", "BurstCount", "DistanceToMelee", "FireIndirect", "FireRate", "FiringSound",
            "GroundSound", "HitSound", "MaxFiringRange", "ShotCount", "maxDelay", "minDelay", "pArea", "pDamage",
            "pDur", "pEffAmp", "pEffect", "pGlows", "pImpact", "pPhysics", "pRender3D", "pSize", "pSpeed", "pSpin",
            "pStick", "pTrail", "pXlr8");
    private static final Set<String> AI_KEYS = keys("AttackInvisible", "AvoidsSun", "AvoidsWater", "CanLeap",
            "CanSprint", "CanSwim", "DirectLOS", "DoorInteract", "FindShelter", "MoveSpeed", "MoveState",
            "MovementType", "MovingPatern", "MovingPathNew", "MovingPause", "MovingPos", "MovingState", "OnAttack",
            "Orientation", "PositionOffsetX", "PositionOffsetY", "PositionOffsetZ", "ReactsToFire", "ReturnToStart",
            "StandingState", "StartPosNew", "TacticalRadius", "TacticalVariant", "WalkingRange", "npcInteracting",
            "stopAndInteract");
    private static final Set<String> INVENTORY_KEYS =
            keys("Armor", "DropChance", "LootMode", "MaxExp", "MinExp", "NpcInv", "Weapons");
    private static final Set<String> SCRIPT_KEYS = keys("Scripts", "ScriptLanguage", "ScriptEnabled");

    private LinkedCategories() {
    }

    private static Set<String> keys(String... names) {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(names)));
    }

    public static boolean has(int mask, int category) {
        return (mask & category) != 0;
    }

    /** The one category a key of group data belongs to. */
    public static int categoryOf(String key) {
        if (NAME_KEYS.contains(key)) return NAME;
        if (APPEARANCE_KEYS.contains(key)) return APPEARANCE;
        if (STATS_KEYS.contains(key)) return STATS;
        if (AI_KEYS.contains(key)) return AI;
        if (INVENTORY_KEYS.contains(key)) return INVENTORY;
        if (SCRIPT_KEYS.contains(key)) return SCRIPTS;
        if (key.startsWith("Transform")) return TRANSFORM;
        return ADVANCED;
    }

    /** The part of the group data the mask passes on; the tags are shared, NBTMerge does not change them. */
    public static NBTTagCompound filter(NBTTagCompound group, int mask) {
        NBTTagCompound out = new NBTTagCompound();
        for (String key : group.getKeySet()) {
            if (has(mask, categoryOf(key))) {
                out.setTag(key, group.getTag(key));
            }
        }
        return out;
    }

    /** Comma separated names in a fixed order; "none" for an empty mask. */
    public static String toText(int mask) {
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < NAMES.length; i++) {
            if (has(mask, 1 << i)) {
                names.add(NAMES[i]);
            }
        }
        return names.isEmpty() ? "none" : String.join(",", names);
    }

    /** Mask of a {@link #toText} value, or -1 when any part is unknown. */
    public static int parse(String text) {
        if ("none".equals(text)) return 0;
        int mask = 0;
        for (String name : text.split(",")) {
            int bit = bit(name);
            if (bit == 0) return -1;
            mask |= bit;
        }
        return mask;
    }

    /**
     * Command arguments: {@code default}, {@code none}, {@code all}, or names. Plain names replace the selection,
     * {@code +name} and {@code -name} change the current one. Returns -1 for anything else.
     */
    public static int apply(int current, String[] tokens) {
        if (tokens.length == 0) return -1;
        if (tokens.length == 1) {
            String word = tokens[0].toLowerCase(Locale.ROOT);
            if (word.equals("default")) return DEFAULT;
            if (word.equals("none")) return 0;
            if (word.equals("all")) return ALL;
        }
        int mask = tokens[0].startsWith("+") || tokens[0].startsWith("-") ? current : 0;
        for (String token : tokens) {
            String word = token.toLowerCase(Locale.ROOT);
            boolean remove = word.startsWith("-");
            int bit = bit(remove || word.startsWith("+") ? word.substring(1) : word);
            if (bit == 0) return -1;
            mask = remove ? mask & ~bit : mask | bit;
        }
        return mask;
    }

    private static int bit(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) return 1 << i;
        }
        return 0;
    }
}
