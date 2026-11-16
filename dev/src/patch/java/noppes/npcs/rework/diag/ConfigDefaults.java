package noppes.npcs.rework.diag;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import noppes.npcs.config.ConfigProp;

/**
 * M5.Z (D-055): test override for scripted regression runs. The dev config
 * {@code dev/run/config/CustomNpcs.cfg} holds 0 for every rework switch so that measurement
 * conditions stay the original; a regression pass must also run with the values the code
 * declares as defaults. With the JVM property {@code rwprivzeto=1} (gradle {@code -PrwPrivzeto=1})
 * every static {@link ConfigProp} field whose name starts with {@code Rw} gets back the value it
 * had before the config was loaded. The config file is not written: {@code ConfigLoader} writes
 * only inside {@code loadConfig}, which has already returned. With {@code rwprivzeto=0} nothing
 * changes and only the values from the config are reported. Without the property nothing happens.
 */
public final class ConfigDefaults {
    public static final String PROPERTY = "rwprivzeto";
    public static final String SET_PROPERTY = "rwset";

    private final List<Field> fields;
    private final List<Object> values;

    private ConfigDefaults(List<Field> fields, List<Object> values) {
        this.fields = fields;
        this.values = values;
    }

    /** Captures the current values; call it before the config is loaded. */
    public static ConfigDefaults capture(Class<?> configClass) {
        List<Field> fields = new ArrayList<Field>();
        for (Field f : configClass.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.isAnnotationPresent(ConfigProp.class)
                    && f.getName().startsWith("Rw")) {
                f.setAccessible(true);
                fields.add(f);
            }
        }
        Collections.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field a, Field b) {
                return a.getName().compareTo(b.getName());
            }
        });
        List<Object> values = new ArrayList<Object>();
        for (Field f : fields) {
            values.add(read(f));
        }
        return new ConfigDefaults(fields, values);
    }

    /**
     * Applies the override for the given property value. Returns the log line, or null when the
     * property is absent or not 0/1 (then nothing is reported and nothing changes).
     */
    public String apply(String property) {
        boolean restore = "1".equals(property);
        if (!restore && !"0".equals(property)) {
            return null;
        }
        int changed = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            Object want = values.get(i);
            if (restore && !want.equals(read(f))) {
                write(f, want);
                changed++;
            }
            sb.append(' ').append(f.getName()).append('=').append(read(f));
        }
        return "RWPRIVZETO " + (restore ? 1 : 0) + " spremenjenih=" + changed + " stikal=" + fields.size() + sb;
    }

    /**
     * M16.5: test override of single integer switches, JVM property {@code rwset} (gradle {@code -PrwSet}),
     * e.g. {@code RwAnimCore=1,RwFarView=0}; used by measurement harnesses that share the dev config and
     * need a switch read at startup. Applied after the config and {@link #apply}; the file is not written.
     * Returns the log line, or null without the property. An unknown or non-integer switch is an error.
     */
    public String applySet(String property) {
        if (property == null || property.trim().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("RWSET");
        for (String part : property.split(",")) {
            String[] kv = part.trim().split("=", 2);
            Field f = null;
            for (Field c : fields) {
                if (c.getName().equals(kv[0].trim())) {
                    f = c;
                }
            }
            if (kv.length != 2 || f == null || f.getType() != int.class) {
                throw new IllegalArgumentException("rwset: unknown integer switch in '" + part + "'");
            }
            write(f, Integer.valueOf(kv[1].trim()));
            sb.append(' ').append(f.getName()).append('=').append(read(f));
        }
        return sb.toString();
    }

    private static Object read(Field f) {
        try {
            return f.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(Field f, Object v) {
        try {
            f.set(null, v);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
