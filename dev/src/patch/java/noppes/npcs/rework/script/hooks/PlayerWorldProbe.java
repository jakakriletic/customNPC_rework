package noppes.npcs.rework.script.hooks;

import static noppes.npcs.rework.script.hooks.HookTraceProbe.*;

import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import noppes.npcs.EventHooks;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

/** Explicit M6.11 fixture entry point only (java-hooks-run.ps1 -Package m611). Same real EventHooks
 * drive as M6.2/M6.3: a typed Java tab and a JS tab use the existing IPlayer/IWorld API in interact
 * and timer, must leave the same trace and world change, and wrong player/world types are compile
 * errors with a location. Then one warmed JS/Java interact pair is timed as data (Q187, Q190).
 */
public final class PlayerWorldProbe {
    private PlayerWorldProbe() { }

    static final int X = 3, Y = 4, Z = 3;

    static final String JS = "function interact(e){var p=e.player,w=e.npc.getWorld();\n"
            + "var s='p:'+p.getName()+'/'+p.getDisplayName()+'/'+p.getGamemode()+'/'+p.getExpLevel()+'/'+(p.getWorld().getName()==w.getName())"
            + "+';w:'+w.getName()+'/'+w.getTime()+'/'+w.getTotalTime()+'/'+w.isDay()+'/'+w.isRaining()+'/'+w.getAllPlayers().length;\n"
            + "w.setBlock(" + X + "," + Y + "," + Z + ",'minecraft:stone',0);p.message('m611 '+w.getName());e.npc.getTempdata().put('i',s);}\n"
            + "function timer(e){var w=e.npc.getWorld();w.getTempdata().put('m611:'+e.id,w.getName());\n"
            + "e.npc.getTempdata().put('w','timer:'+e.id+':'+w.getTempdata().get('m611:'+e.id));w.getTempdata().remove('m611:'+e.id);}\n";
    static final String JAVA = "package m611;\nimport noppes.npcs.api.IWorld;\nimport noppes.npcs.api.entity.IPlayer;\n"
            + "import noppes.npcs.api.event.NpcEvent;\nimport noppes.npcs.rework.script.NpcScript;\n"
            + "public class PlayerWorld extends NpcScript {\n"
            + " @Override public void interact(NpcEvent.InteractEvent e) { IPlayer<?> p = e.player; IWorld w = e.npc.getWorld();\n"
            + "  String s = \"p:\" + p.getName() + \"/\" + p.getDisplayName() + \"/\" + p.getGamemode() + \"/\" + p.getExpLevel() + \"/\" + p.getWorld().getName().equals(w.getName())\n"
            + "   + \";w:\" + w.getName() + \"/\" + w.getTime() + \"/\" + w.getTotalTime() + \"/\" + w.isDay() + \"/\" + w.isRaining() + \"/\" + w.getAllPlayers().length;\n"
            + "  w.setBlock(" + X + ", " + Y + ", " + Z + ", \"minecraft:stone\", 0); p.message(\"m611 \" + w.getName()); e.npc.getTempdata().put(\"i\", s); }\n"
            + " @Override public void timer(NpcEvent.TimerEvent e) { IWorld w = e.npc.getWorld(); w.getTempdata().put(\"m611:\" + e.id, w.getName());\n"
            + "  e.npc.getTempdata().put(\"w\", \"timer:\" + e.id + \":\" + w.getTempdata().get(\"m611:\" + e.id)); w.getTempdata().remove(\"m611:\" + e.id); }\n}\n";
    /** IWorld.getTime() is long: javac must reject the narrowing at its line and column. */
    static final String LOSSY = "package m611;\nimport noppes.npcs.api.event.NpcEvent;\nimport noppes.npcs.rework.script.NpcScript;\n"
            + "public class LossyTime extends NpcScript {\n"
            + " @Override public void interact(NpcEvent.InteractEvent e) { int t = e.npc.getWorld().getTime(); e.npc.getTempdata().put(\"i\", \"ran\"); }\n}\n";
    /** IPlayer has no getWorldName(): unknown member on the typed player. */
    static final String UNKNOWN = "package m611;\nimport noppes.npcs.api.event.NpcEvent;\nimport noppes.npcs.rework.script.NpcScript;\n"
            + "public class UnknownPlayer extends NpcScript {\n"
            + " @Override public void interact(NpcEvent.InteractEvent e) { e.npc.getTempdata().put(\"i\", e.player.getWorldName()); }\n}\n";

    static final int WARMUP = 2000, ROUNDS = 5, CALLS = 1000;

    public static boolean run(ICustomNpc control, String output, String mode) throws Exception {
        Map<String, Object> proof = new LinkedHashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();
        Map<String, Object> traces = new LinkedHashMap<>();
        Map<String, Object> timing = new LinkedHashMap<>();
        proof.put("mode", mode); proof.put("checks", checks); proof.put("traces", traces); proof.put("timing", timing); proof.put("success", false);
        WorldServer world = control.getWorld().getMCWorld();
        BlockPos pos = new BlockPos(X, Y, Z);
        try {
            FakePlayer player = FakePlayerFactory.getMinecraft(world);
            // Creative skips the faction lookup; the probe NPCs are neither attacking nor killed.
            player.capabilities.isCreativeMode = true;
            world.setBlockToAir(pos);
            EntityNPCInterface js = npc(world, "ECMAScript", JS);
            EntityNPCInterface java = npc(world, ScriptHooks.JAVA, JAVA);
            EntityNPCInterface lossy = npc(world, ScriptHooks.JAVA, LOSSY);
            EntityNPCInterface unknown = npc(world, ScriptHooks.JAVA, UNKNOWN);

            // Cold first event (JS eval / Java compile + load) is data only, never in the warm pair.
            timing.put("jsColdFirstEventMicros", firstEvent(js));
            timing.put("javaColdFirstEventMicros", firstEvent(java));
            String before = blockName(world, pos);
            boolean jsRet = EventHooks.onNPCInteract(js, player);
            String jsPlaced = blockName(world, pos);
            world.setBlockToAir(pos);
            boolean javaRet = EventHooks.onNPCInteract(java, player);
            String javaPlaced = blockName(world, pos);
            EventHooks.onNPCTimer(js, 11);
            EventHooks.onNPCTimer(java, 11);
            String jsTrace = js.wrappedNPC.getTempdata().get("i") + "|" + js.wrappedNPC.getTempdata().get("w");
            String javaTrace = java.wrappedNPC.getTempdata().get("i") + "|" + java.wrappedNPC.getTempdata().get("w");
            String expectedWorld = ";w:" + world.getWorldInfo().getWorldName() + "/" + world.getWorldTime() + "/" + world.getTotalWorldTime()
                    + "/" + (world.getWorldTime() % 24000L < 12000L) + "/" + world.getWorldInfo().isRaining()
                    + "/" + world.getMinecraftServer().getPlayerList().getPlayers().size();
            String expectedPlayer = "p:" + player.getName() + "/" + player.getDisplayNameString() + "/"
                    + player.interactionManager.getGameType().getID() + "/" + player.experienceLevel + "/true";
            String expected = expectedPlayer + expectedWorld + "|timer:11:" + world.getWorldInfo().getWorldName();
            traces.put("js", jsTrace); traces.put("java", javaTrace); traces.put("expected", expected);
            traces.put("blockBefore", before); traces.put("jsPlaced", jsPlaced); traces.put("javaPlaced", javaPlaced);
            check(checks, "js-player-world-trace", expected.equals(jsTrace));
            check(checks, "java-equals-js", javaTrace.equals(jsTrace));
            check(checks, "java-tab-not-errored", !java.script.getScripts().get(0).errored && !js.script.getScripts().get(0).errored);
            check(checks, "same-interact-result", jsRet == javaRet);
            String stone = Block.REGISTRY.getNameForObject(Blocks.STONE).toString();
            check(checks, "world-write-js", "minecraft:air".equals(before) && stone.equals(jsPlaced));
            check(checks, "world-write-java", stone.equals(javaPlaced));
            check(checks, "world-tempdata-cleaned", !js.wrappedNPC.getWorld().getTempdata().has("m611:11"));

            EventHooks.onNPCTick(lossy);
            EventHooks.onNPCInteract(lossy, player);
            traces.put("lossyConsole", console(lossy));
            check(checks, "lossy-time-typed-error", lossy.script.getScripts().get(0).errored
                    && console(lossy).contains("LossyTime.java:5:") && console(lossy).contains("compiler.err.prob.found.req"));
            check(checks, "lossy-time-not-run", !lossy.wrappedNPC.getTempdata().has("i"));
            EventHooks.onNPCTick(unknown);
            EventHooks.onNPCInteract(unknown, player);
            traces.put("unknownConsole", console(unknown));
            check(checks, "unknown-player-member-typed-error", unknown.script.getScripts().get(0).errored
                    && console(unknown).contains("UnknownPlayer.java:5:") && console(unknown).contains("compiler.err.cant.resolve"));
            check(checks, "unknown-player-member-not-run", !unknown.wrappedNPC.getTempdata().has("i"));

            // One warmed pair (Q187/Q190): same NPC setup, event, player and world state; the block is
            // already stone, so both languages make the same no-op world write. Alternating order.
            for (int i = 0; i < WARMUP; i++) { EventHooks.onNPCInteract(js, player); EventHooks.onNPCInteract(java, player); }
            List<Double> jsRounds = new ArrayList<>(), javaRounds = new ArrayList<>();
            for (int round = 0; round < ROUNDS; round++) {
                boolean jsFirst = round % 2 == 0;
                double first = perCall(jsFirst ? js : java, player), second = perCall(jsFirst ? java : js, player);
                jsRounds.add(jsFirst ? first : second); javaRounds.add(jsFirst ? second : first);
            }
            timing.put("warmupPerLanguage", WARMUP); timing.put("rounds", ROUNDS); timing.put("callsPerRound", CALLS);
            timing.put("jsMicrosPerInteract", jsRounds); timing.put("javaMicrosPerInteract", javaRounds);
            timing.put("jsMedian", median(jsRounds)); timing.put("javaMedian", median(javaRounds));
            String jsAfter = String.valueOf(js.wrappedNPC.getTempdata().get("i")), javaAfter = String.valueOf(java.wrappedNPC.getTempdata().get("i"));
            traces.put("jsAfterTiming", jsAfter); traces.put("javaAfterTiming", javaAfter);
            check(checks, "timed-traces-equal", jsAfter.equals(javaAfter) && !java.script.getScripts().get(0).errored && !js.script.getScripts().get(0).errored);
            boolean allOk = true;
            for (Map<String, Object> c : checks) { allOk &= Boolean.TRUE.equals(c.get("ok")); }
            proof.put("success", allOk);
            return allOk;
        } catch (Throwable failure) {
            proof.put("error", failure.toString());
            failure.printStackTrace();
            return false;
        } finally {
            world.setBlockToAir(pos);
            Files.write(Paths.get(output), new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static double firstEvent(EntityNPCInterface npc) {
        long start = System.nanoTime();
        EventHooks.onNPCTick(npc);
        return (System.nanoTime() - start) / 1000.0;
    }

    private static double perCall(EntityNPCInterface npc, FakePlayer player) {
        long start = System.nanoTime();
        for (int i = 0; i < CALLS; i++) { EventHooks.onNPCInteract(npc, player); }
        return (System.nanoTime() - start) / 1000.0 / CALLS;
    }

    private static double median(List<Double> values) {
        Double[] sorted = values.toArray(new Double[0]);
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static String blockName(WorldServer world, BlockPos pos) {
        return String.valueOf(Block.REGISTRY.getNameForObject(world.getBlockState(pos).getBlock()));
    }
}
