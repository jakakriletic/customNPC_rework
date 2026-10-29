package corpus;
public class Plain {
    static { Tripwire.touch(); }
    public void interact(noppes.npcs.api.event.NpcEvent.InteractEvent event) { event.setCanceled(true); }
}
