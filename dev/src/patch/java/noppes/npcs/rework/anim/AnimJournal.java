package noppes.npcs.rework.anim;

/** P7a (D-098): static entry points called by the instrumented EntityAnimatedNpc (see
 * rework.core.AnimatedNpcPatch, only with RwAnimHooks>=1). Each call only records into the
 * entity's ActionJournal; scripts run later at world tick END. Never throws into the entities mod. */
public final class AnimJournal {
    private static volatile AnimationHooks sink;

    private AnimJournal() { }

    static void attach(AnimationHooks hooks) { sink = hooks; }

    /** Head of startAction: the running action (if any) is being replaced. */
    public static void beforeStart(Object npc) { AnimationHooks s = sink; if (s != null) { s.record(npc, AnimationHooks.Record.REPLACE, 0, null); } }

    /** Every return of startAction: the new action is set. */
    public static void afterStart(Object npc) { AnimationHooks s = sink; if (s != null) { s.record(npc, AnimationHooks.Record.START, 0, null); } }

    /** Head of stopAction. */
    public static void beforeStop(Object npc) { AnimationHooks s = sink; if (s != null) { s.record(npc, AnimationHooks.Record.STOP, 0, null); } }

    /** Right after the stopAction of tickAnimation that ends a clip at its length. */
    public static void naturalEnd(Object npc) { AnimationHooks s = sink; if (s != null) { s.record(npc, AnimationHooks.Record.NATURAL, 0, null); } }

    /** Head of onServerEvent(action, event): the server fires this clip event now. */
    public static void serverEvent(Object npc, int action, Object event) { AnimationHooks s = sink; if (s != null) { s.record(npc, AnimationHooks.Record.EVENT, action, event); } }
}
