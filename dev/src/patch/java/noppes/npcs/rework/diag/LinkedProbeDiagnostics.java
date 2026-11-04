package noppes.npcs.rework.diag;

import net.minecraft.entity.Entity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityNPCInterface;

/** Controller-only, read-only liveness diagnostics. Never loads chunks or invokes scripts. */
public final class LinkedProbeDiagnostics {
    private static LinkedProbeDiagnostics active;
    private int ticks;
    private int updates;

    public static void start() {
        if (Boolean.getBoolean("rw.m134Probe") && active == null) {
            active = new LinkedProbeDiagnostics();
            MinecraftForge.EVENT_BUS.register(active);
            WrapperNpcAPI.EVENT_BUS.register(active);
        }
    }

    public static void stop() {
        if (active != null) {
            MinecraftForge.EVENT_BUS.unregister(active);
            WrapperNpcAPI.EVENT_BUS.unregister(active);
        }
        active = null;
    }

    /** Counts tick events posted after the probe script ran: separates "tick not fired" from "script silent". */
    @SubscribeEvent public void update(NpcEvent.UpdateEvent event) {
        if (event.npc != null && "LCProbe".equals(event.npc.getName())) updates++;
    }

    private static Object field(Object owner, String name) {
        try {
            java.lang.reflect.Field f = ScriptContainer.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        } catch (ReflectiveOperationException e) {
            return "?" + e;
        }
    }

    /** Reads a script global without evaluating code. */
    private static Object engineVar(Object engine, String name) {
        if (!(engine instanceof javax.script.ScriptEngine)) return "no-engine";
        Object value = ((javax.script.ScriptEngine) engine).get(name);
        return "tick".equals(name) ? (value == null ? "null" : value.getClass().getSimpleName()) : value;
    }

    @SubscribeEvent public void tick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || event.world.provider.getDimension() != 0
                || ++ticks % 100 != 0) return;
        WorldServer world = (WorldServer) event.world;
        boolean found = false;
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof EntityNPCInterface)) continue;
            EntityNPCInterface npc = (EntityNPCInterface) entity;
            if (!"LCProbe".equals(npc.display.getName())) continue;
            found = true;
            int x = (int) Math.floor(npc.posX), z = (int) Math.floor(npc.posZ);
            LogWriter.info("CLONERDIAG tick=" + ticks + " entityTicks=" + npc.ticksExisted
                    + " aliveTicks=" + npc.totalTicksAlive + " players=" + world.playerEntities.size()
                    + " areaLoaded=" + world.isAreaLoaded(new net.minecraft.util.math.BlockPos(x - 32, 0, z - 32),
                            new net.minecraft.util.math.BlockPos(x + 32, 0, z + 32), true)
                    + " started=" + ScriptController.HasStart + " scripting=" + CustomNpcs.EnableScripting
                    + " enabled=" + npc.script.getEnabled() + " runnable=" + npc.script.isEnabled()
                    + " language=" + npc.script.getLanguage() + " tabs=" + npc.script.getScripts().size()
                    + " updates=" + updates + " lastLoaded=" + ScriptController.Instance.lastLoaded
                    + " lastInited=" + npc.script.lastInited);
            for (ScriptContainer script : npc.script.getScripts()) {
                Object engine = field(script, "engine");
                LogWriter.info("CLONERDIAG script length=" + script.script.length() + " valid=" + script.isValid()
                        + " errored=" + script.errored + " console=" + script.console
                        + " init=" + field(script, "init") + " unknown=" + field(script, "unknownFunctions")
                        + " lastCreated=" + script.lastCreated + " age=" + engineVar(engine, "age")
                        + " stage=" + engineVar(engine, "stage") + " tick=" + engineVar(engine, "tick"));
            }
        }
        if (!found) LogWriter.info("CLONERDIAG tick=" + ticks + " probe-not-loaded players=" + world.playerEntities.size());
    }
}
