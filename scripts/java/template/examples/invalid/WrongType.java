package example;

public class WrongType extends noppes.npcs.rework.script.NpcScript {
    @Override public void tick(noppes.npcs.api.event.NpcEvent.UpdateEvent event) {
        int name = event.npc.getName();
    }
}
