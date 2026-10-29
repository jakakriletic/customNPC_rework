# NPC hooks v1

Generated from `ScriptHooks.npc()` in registry order; do not edit.

Java 8; trusted authors only, with process permissions and no sandbox. JS remains the default. Java dispatch is NPC-only. Public no-argument constructor required; one instance/loader per NPC, tab and generation. Only overridden NpcScript hooks are bound. Tabs run in their existing order, then the API bus; cancellation and missing hooks keep the existing handler behavior.

All examples are separate Java files, named after their public class. `public void` with the exact event type is the recommended typed signature. The plain-class binder also accepts a parameter supertype; overloads are rejected. Payload fields, mutability and inheritance are in [API.md](API.md). `event.setCanceled(true)` is valid only for a cancelable actual event; role subtypes differ. Cancelable metadata below describes the declared event type, not every runtime subtype. No frequency guarantee follows from the registry order.

Forge string hooks remain outside NPC Java v1: the existing rule is uncapitalize(simple binary event name with `$` removed). Excluded Java handlers: `[player, block, door, item, projectile, forge]`.

## init

`public void init(noppes.npcs.api.event.NpcEvent.InitEvent event)`

Enum: `INIT`; cancelable on declared type: `false`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_init extends noppes.npcs.rework.script.NpcScript {
    @Override public void init(noppes.npcs.api.event.NpcEvent.InitEvent event) {
        log(event.npc.getName());
    }
}
```

## tick

`public void tick(noppes.npcs.api.event.NpcEvent.UpdateEvent event)`

Enum: `TICK`; cancelable on declared type: `false`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_tick extends noppes.npcs.rework.script.NpcScript {
    @Override public void tick(noppes.npcs.api.event.NpcEvent.UpdateEvent event) {
        log(event.npc.getName());
    }
}
```

## interact

`public void interact(noppes.npcs.api.event.NpcEvent.InteractEvent event)`

Enum: `INTERACT`; cancelable on declared type: `true`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_interact extends noppes.npcs.rework.script.NpcScript {
    @Override public void interact(noppes.npcs.api.event.NpcEvent.InteractEvent event) {
        log(event.npc.getName());
    }
}
```

## dialog

`public void dialog(noppes.npcs.api.event.DialogEvent.OpenEvent event)`

Enum: `DIALOG`; cancelable on declared type: `true`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_dialog extends noppes.npcs.rework.script.NpcScript {
    @Override public void dialog(noppes.npcs.api.event.DialogEvent.OpenEvent event) {
        log(event.npc.getName());
    }
}
```

## damaged

`public void damaged(noppes.npcs.api.event.NpcEvent.DamagedEvent event)`

Enum: `DAMAGED`; cancelable on declared type: `true`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_damaged extends noppes.npcs.rework.script.NpcScript {
    @Override public void damaged(noppes.npcs.api.event.NpcEvent.DamagedEvent event) {
        log(event.npc.getName());
    }
}
```

## died

`public void died(noppes.npcs.api.event.NpcEvent.DiedEvent event)`

Enum: `DIED`; cancelable on declared type: `false`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_died extends noppes.npcs.rework.script.NpcScript {
    @Override public void died(noppes.npcs.api.event.NpcEvent.DiedEvent event) {
        log(event.npc.getName());
    }
}
```

## meleeAttack

`public void meleeAttack(noppes.npcs.api.event.NpcEvent.MeleeAttackEvent event)`

Enum: `ATTACK_MELEE`; cancelable on declared type: `true`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_meleeAttack extends noppes.npcs.rework.script.NpcScript {
    @Override public void meleeAttack(noppes.npcs.api.event.NpcEvent.MeleeAttackEvent event) {
        log(event.npc.getName());
    }
}
```

## target

`public void target(noppes.npcs.api.event.NpcEvent.TargetEvent event)`

Enum: `TARGET`; cancelable on declared type: `true`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_target extends noppes.npcs.rework.script.NpcScript {
    @Override public void target(noppes.npcs.api.event.NpcEvent.TargetEvent event) {
        log(event.npc.getName());
    }
}
```

## collide

`public void collide(noppes.npcs.api.event.NpcEvent.CollideEvent event)`

Enum: `COLLIDE`; cancelable on declared type: `false`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_collide extends noppes.npcs.rework.script.NpcScript {
    @Override public void collide(noppes.npcs.api.event.NpcEvent.CollideEvent event) {
        log(event.npc.getName());
    }
}
```

## kill

`public void kill(noppes.npcs.api.event.NpcEvent.KilledEntityEvent event)`

Enum: `KILL`; cancelable on declared type: `false`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_kill extends noppes.npcs.rework.script.NpcScript {
    @Override public void kill(noppes.npcs.api.event.NpcEvent.KilledEntityEvent event) {
        log(event.npc.getName());
    }
}
```

## dialogOption

`public void dialogOption(noppes.npcs.api.event.DialogEvent.OptionEvent event)`

Enum: `DIALOG_OPTION`; cancelable on declared type: `true`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_dialogOption extends noppes.npcs.rework.script.NpcScript {
    @Override public void dialogOption(noppes.npcs.api.event.DialogEvent.OptionEvent event) {
        log(event.npc.getName());
    }
}
```

## targetLost

`public void targetLost(noppes.npcs.api.event.NpcEvent.TargetLostEvent event)`

Enum: `TARGET_LOST`; cancelable on declared type: `true`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_targetLost extends noppes.npcs.rework.script.NpcScript {
    @Override public void targetLost(noppes.npcs.api.event.NpcEvent.TargetLostEvent event) {
        log(event.npc.getName());
    }
}
```

## role

`public void role(noppes.npcs.api.event.RoleEvent event)`

Enum: `ROLE`; cancelable on declared type: `false`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_role extends noppes.npcs.rework.script.NpcScript {
    @Override public void role(noppes.npcs.api.event.RoleEvent event) {
        log(event.npc.getName());
    }
}
```

## rangedLaunched

`public void rangedLaunched(noppes.npcs.api.event.NpcEvent.RangedLaunchedEvent event)`

Enum: `RANGED_LAUNCHED`; cancelable on declared type: `false`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_rangedLaunched extends noppes.npcs.rework.script.NpcScript {
    @Override public void rangedLaunched(noppes.npcs.api.event.NpcEvent.RangedLaunchedEvent event) {
        log(event.npc.getName());
    }
}
```

## timer

`public void timer(noppes.npcs.api.event.NpcEvent.TimerEvent event)`

Enum: `TIMER`; cancelable on declared type: `false`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_timer extends noppes.npcs.rework.script.NpcScript {
    @Override public void timer(noppes.npcs.api.event.NpcEvent.TimerEvent event) {
        log(event.npc.getName());
    }
}
```

## dialogClose

`public void dialogClose(noppes.npcs.api.event.DialogEvent.CloseEvent event)`

Enum: `DIALOG_CLOSE`; cancelable on declared type: `false`. NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.

```java
public class Example_dialogClose extends noppes.npcs.rework.script.NpcScript {
    @Override public void dialogClose(noppes.npcs.api.event.DialogEvent.CloseEvent event) {
        log(event.npc.getName());
    }
}
```

## Player and world

The existing `event.player` (IPlayer, interact and other player events) and `event.npc.getWorld()` (IWorld); their members are in [API.md](API.md).

```java
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.NpcEvent;

public class Example_playerWorld extends noppes.npcs.rework.script.NpcScript {
    @Override public void interact(NpcEvent.InteractEvent event) {
        IPlayer<?> player = event.player;
        IWorld world = event.npc.getWorld();
        long time = world.getTime();
        player.message("Hello " + player.getDisplayName() + " in " + world.getName() + " at " + time);
        world.getTempdata().put("lastVisitor", player.getName());
        if (!world.isDay()) {
            world.setBlock(event.npc.getBlockX(), event.npc.getBlockY() + 2, event.npc.getBlockZ(), "minecraft:torch", 0);
        }
    }
}
```
