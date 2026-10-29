package corpus;

import noppes.npcs.rework.script.NpcScript;
import noppes.npcs.api.event.NpcEvent;

public class Main extends NpcScript {
    static { Tripwire.touch(); }
    public Main() { Tripwire.touch(); }
    @Override public void tick(NpcEvent.UpdateEvent event) {
        java.util.function.Supplier<String> text = () -> Helper.name(event.npc);
        log(text.get());
    }
}
