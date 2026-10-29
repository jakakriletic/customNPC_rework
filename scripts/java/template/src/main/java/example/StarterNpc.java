package example;

import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.rework.script.NpcScript;

/** One public class per NPC tab. Fields reset on successful reload; storeddata is explicit. */
public class StarterNpc extends NpcScript {
    private int updates;

    @Override public void init(NpcEvent.InitEvent event) {
        updates = 0;
        log("Ready: " + event.npc.getName());
    }

    @Override public void tick(NpcEvent.UpdateEvent event) {
        updates++;
    }

    @Override public void interact(NpcEvent.InteractEvent event) {
        // A Java 8 lambda, generics and a separate included compilation unit.
        log(CounterText.format(event.npc.getName(), () -> updates));
    }

    @Override public void timer(NpcEvent.TimerEvent event) {
        log("Timer " + event.id);
    }
}
