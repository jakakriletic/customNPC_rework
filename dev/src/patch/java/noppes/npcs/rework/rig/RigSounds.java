package noppes.npcs.rework.rig;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.LogWriter;

/**
 * M16: the sounds of the built-in models ({@code assets/rwrig/sounds.json}, e.g. {@code troll.roar}),
 * registered as {@code rwrig:<name>} only with RwAnimCore=1. A clip's {@code sound} event names them
 * without a domain, or any other sound with its full id ({@code minecraft:entity.ravager.roar}).
 */
public final class RigSounds {
    public static final String DOMAIN = "rwrig";

    @SubscribeEvent
    public void register(RegistryEvent.Register<SoundEvent> event) {
        try (InputStream in = RigSounds.class.getResourceAsStream("/assets/rwrig/sounds.json")) {
            if (in == null) { return; }
            JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, ?> e : root.entrySet()) {
                ResourceLocation id = new ResourceLocation(DOMAIN, e.getKey());
                event.getRegistry().register(new SoundEvent(id).setRegistryName(id));
            }
        } catch (Exception e) {
            LogWriter.error("RWRIG sounds not registered", e);
        }
    }

    /** A sound by event name: rwrig first for a name without a domain, then the name itself. */
    public static SoundEvent get(String name) {
        if (name == null || name.isEmpty()) { return null; }
        if (name.indexOf(':') < 0) {
            SoundEvent s = SoundEvent.REGISTRY.getObject(new ResourceLocation(DOMAIN, name));
            if (s != null) { return s; }
        }
        return SoundEvent.REGISTRY.getObject(new ResourceLocation(name));
    }
}
