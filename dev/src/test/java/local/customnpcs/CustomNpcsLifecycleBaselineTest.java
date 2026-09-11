package local.customnpcs;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import noppes.npcs.CustomNpcs;

import org.junit.Test;

/** Characterizes the server lifecycle entry points before save-session integration. */
public class CustomNpcsLifecycleBaselineTest {
    @Test
    public void serverLifecycleEntryPointsKeepTheirOriginalSignatures() throws Exception {
        Method start = CustomNpcs.class.getDeclaredMethod("setAboutToStart",
                FMLServerAboutToStartEvent.class);
        Method stop = CustomNpcs.class.getDeclaredMethod("stopped", FMLServerStoppedEvent.class);

        assertEquals(Void.TYPE, start.getReturnType());
        assertEquals(Void.TYPE, stop.getReturnType());
    }
}
