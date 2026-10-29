package noppes.npcs.rework.script;

import noppes.npcs.LogWriter;
import noppes.npcs.api.event.DialogEvent;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.controllers.ScriptContainer;

/** M6.3 (D-090, Q148): optional typed base class for a Java NPC script tab.
 * One method per NPC hook of ScriptHooks.npc(), with the event type EventHooks passes, so
 * {@code @Override} lets javac reject a wrong name or event type. The API is the existing
 * noppes.npcs.api interfaces (event.npc, event.API); nothing is copied. Only overridden
 * methods are bound and called; these no-op defaults never run. A plain public class with
 * hook-named methods (M6.2) stays valid.
 */
public abstract class NpcScript {
    public void init(NpcEvent.InitEvent event) { }
    public void tick(NpcEvent.UpdateEvent event) { }
    public void interact(NpcEvent.InteractEvent event) { }
    public void dialog(DialogEvent.OpenEvent event) { }
    public void damaged(NpcEvent.DamagedEvent event) { }
    public void died(NpcEvent.DiedEvent event) { }
    public void meleeAttack(NpcEvent.MeleeAttackEvent event) { }
    public void target(NpcEvent.TargetEvent event) { }
    public void collide(NpcEvent.CollideEvent event) { }
    public void kill(NpcEvent.KilledEntityEvent event) { }
    public void dialogOption(DialogEvent.OptionEvent event) { }
    public void targetLost(NpcEvent.TargetLostEvent event) { }
    public void role(RoleEvent event) { }
    public void rangedLaunched(NpcEvent.RangedLaunchedEvent event) { }
    public void timer(NpcEvent.TimerEvent event) { }
    public void dialogClose(DialogEvent.CloseEvent event) { }

    /** Same output as the JS {@code log} helper: the running tab's console and the mod log.
     * Outside a hook call (no running tab) only the mod log. */
    protected final void log(Object message) {
        String text = String.valueOf(message);
        ScriptContainer current = ScriptContainer.Current;
        if (current != null) { current.appandConsole(text); }
        LogWriter.info(text);
    }
}
