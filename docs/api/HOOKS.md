# NPC hooks v1

Generated from `ScriptHooks.java()` in registry order; do not edit.

Java 8; trusted authors only, with process permissions and no sandbox. JS remains the default. NPC tabs bind registry hooks; player, forge, block, door and item tabs bind public one-event methods by hook name (P5). Public no-argument constructor required; one instance/loader per NPC, tab and generation. Only overridden NpcScript hooks are bound. Tabs run in their existing order, then the API bus; cancellation and missing hooks keep the existing handler behavior.

All examples are separate Java files, named after their public class. `public void` with the exact event type is the recommended typed signature. The plain-class binder also accepts a parameter supertype; overloads are rejected. Payload fields, mutability and inheritance are in [API.md](API.md). `event.setCanceled(true)` is valid only for a cancelable actual event; role subtypes differ. Cancelable metadata below describes the declared event type, not every runtime subtype. No frequency guarantee follows from the registry order.

Forge string hooks (forge Java tabs, P5) follow the existing rule is uncapitalize(simple binary event name with `$` removed). Excluded Java handlers: `[projectile]`.

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

## animationStart

`public void animationStart(noppes.npcs.rework.anim.AnimationEvent.Started event)`

Enum: `none`; cancelable on declared type: `false`. Optional animation hook (M7.5), called only with RwAnimHooks=1 for an EntityAnimatedNpc of customnpcentities 0.7.0; never called by EventHooks and not posted to the API event bus. The server observes the action once per world tick END and calls, in this order, animationEnd of the previous action, animationStart of a new one (also the same clip again), then animationEvent for each timed clip event the entity fired since the last observation, with tick = the action age it fired at. finished=false means stopped, replaced or cut by death; removal or unload of the NPC calls no hook.

```java
public class Example_animationStart extends noppes.npcs.rework.script.NpcScript {
    @Override public void animationStart(noppes.npcs.rework.anim.AnimationEvent.Started event) {
        log(event.npc.getName());
    }
}
```

## animationEnd

`public void animationEnd(noppes.npcs.rework.anim.AnimationEvent.Ended event)`

Enum: `none`; cancelable on declared type: `false`. Optional animation hook (M7.5), called only with RwAnimHooks=1 for an EntityAnimatedNpc of customnpcentities 0.7.0; never called by EventHooks and not posted to the API event bus. The server observes the action once per world tick END and calls, in this order, animationEnd of the previous action, animationStart of a new one (also the same clip again), then animationEvent for each timed clip event the entity fired since the last observation, with tick = the action age it fired at. finished=false means stopped, replaced or cut by death; removal or unload of the NPC calls no hook.

```java
public class Example_animationEnd extends noppes.npcs.rework.script.NpcScript {
    @Override public void animationEnd(noppes.npcs.rework.anim.AnimationEvent.Ended event) {
        log(event.npc.getName());
    }
}
```

## animationEvent

`public void animationEvent(noppes.npcs.rework.anim.AnimationEvent.ClipEvent event)`

Enum: `none`; cancelable on declared type: `false`. Optional animation hook (M7.5), called only with RwAnimHooks=1 for an EntityAnimatedNpc of customnpcentities 0.7.0; never called by EventHooks and not posted to the API event bus. The server observes the action once per world tick END and calls, in this order, animationEnd of the previous action, animationStart of a new one (also the same clip again), then animationEvent for each timed clip event the entity fired since the last observation, with tick = the action age it fired at. finished=false means stopped, replaced or cut by death; removal or unload of the NPC calls no hook.

```java
public class Example_animationEvent extends noppes.npcs.rework.script.NpcScript {
    @Override public void animationEvent(noppes.npcs.rework.anim.AnimationEvent.ClipEvent event) {
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

## Optional action animation

`Animators.of(event.npc)` returns a non-null `IAnimator`. Requires a live `EntityAnimatedNpc` from customnpcentities 0.7.0; ordinary NPCs and a missing/incompatible mod return an inert handle (`isAvailable=false`, play/queue/stop=false, clip empty, tick 0, clips empty). No change to `ICustomNpc`. Call from the server thread; mutations elsewhere return false. Clip names are case-sensitive; unknown/null clips return false. `play` replaces the action and clears the queue only on success; `stop` clears it and stops the action. `queue` starts immediately when idle, otherwise appends FIFO, up to 32 pending clips shared by all tabs/handles of the entity. The next clip starts at world tick END after the current action ends. External AI/actions may interrupt; the queue waits for idle. Pending clips are discarded on death/entity unload and are not saved in NBT; script-tab reload does not clear the shared entity queue. No client locomotion or blending; the animation hooks above need RwAnimHooks=1. `getClip`/`getTick` describe the action only (idle: empty/0); `getClips` returns a fresh sorted array.

```java
import noppes.npcs.rework.anim.Animators;
import noppes.npcs.rework.anim.IAnimator;
import noppes.npcs.api.event.NpcEvent;

public class Example_animation extends noppes.npcs.rework.script.NpcScript {
    @Override public void interact(NpcEvent.InteractEvent event) {
        IAnimator animator = Animators.of(event.npc);
        if (animator.isAvailable() && animator.play("hurt")) {
            animator.queue("idle");
        }
    }
}
```

JS: `var a = Java.type('noppes.npcs.rework.anim.Animators').of(event.npc);`
