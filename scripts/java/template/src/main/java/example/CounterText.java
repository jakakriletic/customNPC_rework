package example;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

/** Include this file in the same tab's ScriptList, after StarterNpc. */
public final class CounterText {
    private CounterText() { }

    public static String format(String name, IntSupplier count) {
        List<String> parts = Arrays.asList(name, "updates=" + count.getAsInt());
        return String.join(" ", parts);
    }
}
