package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.util.NBTJsonUtil;

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

    @Test
    public void patchedPlayerSavesAreOwnedByTheServerWorldSession() throws Exception {
        boolean patched = isPatched();
        String playerData = classBytes(PlayerData.class);
        String lifecycle = classBytes(CustomNpcs.class);
        String sessionType = "noppes/npcs/rework/data/WorldSaveSession";

        if (patched) {
            assertTrue(playerData.contains(sessionType));
            assertTrue(lifecycle.contains(sessionType));
            assertFalse(playerData.contains("noppes/npcs/util/CustomNPCsScheduler"));
        } else {
            assertFalse(playerData.contains(sessionType));
            assertFalse(lifecycle.contains(sessionType));
            assertTrue(playerData.contains("noppes/npcs/util/CustomNPCsScheduler"));
        }
    }

    private static boolean isPatched() {
        try {
            return NBTJsonUtil.class.getField("REWORK_SAFE_WRITER").getBoolean(null);
        } catch (Exception originalBinary) {
            return false;
        }
    }

    private static String classBytes(Class<?> type) throws Exception {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        InputStream input = type.getResourceAsStream(resource);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } finally {
            input.close();
        }
        return new String(output.toByteArray(), Charset.forName("ISO-8859-1"));
    }
}
