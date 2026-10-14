package noppes.npcs.rework.core;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;

/**
 * M5.16: ali razred entitete prepisuje katero od metod, ki prispevata trkalno skatlo.
 *
 * <p>{@code Entity.getCollisionBoundingBox()} in {@code Entity.getCollisionBox(Entity)} v vanilli
 * vracata {@code null}; skatlo prispevajo samo razredi, ki eno od njiju prepisejo. Razred se steje
 * **konzervativno**: ce je metoda prepisana, se razred steje tudi, kadar vcasih vrne {@code null}.
 *
 * <p>Imena so dvojna: v dev okolju MCP, v produkciji SRG (razredi so takrat deobfuskirani samo do
 * SRG). Izid se predpomni po razredu, ker refleksija ni poceni, klicev pa je veliko.
 */
public final class CollisionClasses {
    /** {@code Entity.getCollisionBoundingBox()} brez argumentov. */
    private static final String[] BOUNDING_BOX = { "getCollisionBoundingBox", "func_70046_E" };
    /** {@code Entity.getCollisionBox(Entity)}. */
    private static final String[] COLLISION_BOX = { "getCollisionBox", "func_70114_g" };

    private static final Map<Class<?>, Boolean> CONTRIBUTES = new ConcurrentHashMap<Class<?>, Boolean>();
    private static final Map<Class<?>, Boolean> OVERRIDES_BOX = new ConcurrentHashMap<Class<?>, Boolean>();

    private CollisionClasses() {
    }

    /** Ali razred prepisuje katero od obeh metod (torej lahko prispeva skatlo). */
    public static boolean contributes(Class<?> type) {
        Boolean known = CONTRIBUTES.get(type);
        if (known != null) {
            return known.booleanValue();
        }
        boolean value = overrides(type, BOUNDING_BOX, false) || overrides(type, COLLISION_BOX, true);
        CONTRIBUTES.put(type, Boolean.valueOf(value));
        return value;
    }

    /** Ali razred prepisuje {@code getCollisionBox(Entity)} (skatla premikajoce se entitete). */
    public static boolean overridesGetCollisionBox(Class<?> type) {
        Boolean known = OVERRIDES_BOX.get(type);
        if (known != null) {
            return known.booleanValue();
        }
        boolean value = overrides(type, COLLISION_BOX, true);
        OVERRIDES_BOX.put(type, Boolean.valueOf(value));
        return value;
    }

    /**
     * Ali je metoda prepisana niz {@code Entity}. Ce je refleksija ne najde (drugacna preslikava
     * imen), se vrne {@code true} — konzervativno, torej brez preskoka.
     */
    private static boolean overrides(Class<?> type, String[] names, boolean withEntityArg) {
        for (int i = 0; i < names.length; ++i) {
            try {
                Method m = withEntityArg ? type.getMethod(names[i], Entity.class) : type.getMethod(names[i]);
                return m.getDeclaringClass() != Entity.class;
            } catch (NoSuchMethodException e) {
                continue;
            } catch (Throwable t) {
                return true;
            }
        }
        return true;
    }

    /** Samo za test in diagnostiko. */
    public static void clearCache() {
        CONTRIBUTES.clear();
        OVERRIDES_BOX.clear();
    }

    public static int cachedClasses() {
        return CONTRIBUTES.size();
    }
}
