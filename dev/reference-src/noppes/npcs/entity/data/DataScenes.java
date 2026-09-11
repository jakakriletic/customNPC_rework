/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandBase
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.pathfinding.Path
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 */
package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.pathfinding.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityProjectile;
import noppes.npcs.util.ValueUtil;

public class DataScenes {
    private EntityNPCInterface npc;
    public List<SceneContainer> scenes = new ArrayList<SceneContainer>();
    public static Map<String, SceneState> StartedScenes = new HashMap<String, SceneState>();
    public static List<SceneContainer> ScenesToRun = new ArrayList<SceneContainer>();
    private EntityLivingBase owner = null;
    private String ownerScene = null;

    public DataScenes(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (SceneContainer scene : this.scenes) {
            list.appendTag((NBTBase)scene.writeToNBT(new NBTTagCompound()));
        }
        compound.setTag("Scenes", (NBTBase)list);
        return compound;
    }

    public void readFromNBT(NBTTagCompound compound) {
        NBTTagList list = compound.getTagList("Scenes", 10);
        ArrayList<SceneContainer> scenes = new ArrayList<SceneContainer>();
        for (int i = 0; i < list.tagCount(); ++i) {
            SceneContainer scene = new SceneContainer();
            scene.readFromNBT(list.getCompoundTagAt(i));
            scenes.add(scene);
        }
        this.scenes = scenes;
    }

    public EntityLivingBase getOwner() {
        return this.owner;
    }

    public static void Toggle(ICommandSender sender, String id) {
        SceneState state = StartedScenes.get(id.toLowerCase());
        if (state == null || state.paused) {
            DataScenes.Start(sender, id);
        } else {
            state.paused = true;
            NoppesUtilServer.NotifyOPs("Paused scene %s at %s", id, state.ticks);
        }
    }

    public static void Start(ICommandSender sender, String id) {
        SceneState state = StartedScenes.get(id.toLowerCase());
        if (state == null) {
            NoppesUtilServer.NotifyOPs("Started scene %s", id);
            StartedScenes.put(id.toLowerCase(), new SceneState());
        } else if (state.paused) {
            state.paused = false;
            NoppesUtilServer.NotifyOPs("Started scene %s from %s", id, state.ticks);
        }
    }

    public static void Pause(ICommandSender sender, String id) {
        if (id == null) {
            for (SceneState state : StartedScenes.values()) {
                state.paused = true;
            }
            NoppesUtilServer.NotifyOPs("Paused all scenes", new Object[0]);
        } else {
            SceneState state = StartedScenes.get(id.toLowerCase());
            state.paused = true;
            NoppesUtilServer.NotifyOPs("Paused scene %s at %s", id, state.ticks);
        }
    }

    public static void Reset(ICommandSender sender, String id) {
        if (id == null) {
            if (StartedScenes.isEmpty()) {
                return;
            }
            StartedScenes = new HashMap<String, SceneState>();
            NoppesUtilServer.NotifyOPs("Reset all scene", new Object[0]);
        } else if (StartedScenes.remove(id.toLowerCase()) == null) {
            sender.sendMessage((ITextComponent)new TextComponentTranslation("Unknown scene %s ", new Object[]{id}));
        } else {
            NoppesUtilServer.NotifyOPs("Reset scene %s", id);
        }
    }

    public void update() {
        for (SceneContainer scene : this.scenes) {
            if (!scene.validState()) continue;
            ScenesToRun.add(scene);
        }
        if (this.owner != null && !StartedScenes.containsKey(this.ownerScene.toLowerCase())) {
            this.owner = null;
            this.ownerScene = null;
        }
    }

    public void addScene(String name) {
        if (name.isEmpty()) {
            return;
        }
        SceneContainer scene = new SceneContainer();
        scene.name = name;
        this.scenes.add(scene);
    }

    public static enum SceneType {
        ANIMATE,
        MOVE,
        FACTION,
        COMMAND,
        EQUIP,
        THROW,
        ATTACK,
        FOLLOW,
        SAY,
        ROTATE,
        STATS;

    }

    public static class SceneEvent
    implements Comparable<SceneEvent> {
        public int ticks = 0;
        public SceneType type;
        public String param = "";

        public String toString() {
            return this.ticks + " " + this.type.name() + " " + this.param;
        }

        public static SceneEvent parse(String str) {
            SceneEvent event = new SceneEvent();
            int i = str.indexOf(" ");
            if (i <= 0) {
                return null;
            }
            try {
                event.ticks = Integer.parseInt(str.substring(0, i));
                str = str.substring(i + 1);
            }
            catch (NumberFormatException ex) {
                return null;
            }
            i = str.indexOf(" ");
            if (i <= 0) {
                return null;
            }
            String name = str.substring(0, i);
            for (SceneType type : SceneType.values()) {
                if (!name.equalsIgnoreCase(type.name())) continue;
                event.type = type;
            }
            if (event.type == null) {
                return null;
            }
            event.param = str.substring(i + 1);
            return event;
        }

        @Override
        public int compareTo(SceneEvent o) {
            return this.ticks - o.ticks;
        }
    }

    public class SceneContainer {
        public int btn = 0;
        public String name = "";
        public String lines = "";
        public boolean enabled = false;
        public int ticks = -1;
        private SceneState state = null;
        private List<SceneEvent> events = new ArrayList<SceneEvent>();

        public NBTTagCompound writeToNBT(NBTTagCompound compound) {
            compound.setBoolean("Enabled", this.enabled);
            compound.setString("Name", this.name);
            compound.setString("Lines", this.lines);
            compound.setInteger("Button", this.btn);
            compound.setInteger("Ticks", this.ticks);
            return compound;
        }

        public boolean validState() {
            if (!this.enabled) {
                return false;
            }
            if (this.state != null) {
                if (StartedScenes.containsValue(this.state)) {
                    return !this.state.paused;
                }
                this.state = null;
            }
            this.state = StartedScenes.get(this.name.toLowerCase());
            if (this.state == null) {
                this.state = StartedScenes.get(this.btn + "btn");
            }
            if (this.state != null) {
                return !this.state.paused;
            }
            return false;
        }

        public void readFromNBT(NBTTagCompound compound) {
            this.enabled = compound.getBoolean("Enabled");
            this.name = compound.getString("Name");
            this.lines = compound.getString("Lines");
            this.btn = compound.getInteger("Button");
            this.ticks = compound.getInteger("Ticks");
            ArrayList<SceneEvent> events = new ArrayList<SceneEvent>();
            for (String line : this.lines.split("\r\n|\r|\n")) {
                SceneEvent event = SceneEvent.parse(line);
                if (event == null) continue;
                events.add(event);
            }
            Collections.sort(events);
            this.events = events;
        }

        public void update() {
            if (!this.enabled || this.events.isEmpty() || this.state == null) {
                return;
            }
            for (SceneEvent event : this.events) {
                if (event.ticks > this.state.ticks) break;
                if (event.ticks != this.state.ticks) continue;
                try {
                    this.handle(event);
                }
                catch (Exception exception) {}
            }
            this.ticks = this.state.ticks;
        }

        private void handle(SceneEvent event) throws Exception {
            block77: {
                if (event.type == SceneType.MOVE) {
                    String[] param = event.param.split(" ");
                    while (param.length > 1) {
                        boolean move = false;
                        if (param[0].startsWith("to")) {
                            move = true;
                        } else if (!param[0].startsWith("tp")) break;
                        BlockPos pos = null;
                        if (param[0].startsWith("@")) {
                            EntityLivingBase entitylivingbase = (EntityLivingBase)CommandBase.getEntity((MinecraftServer)DataScenes.this.npc.getServer(), (ICommandSender)DataScenes.this.npc, (String)param[0], EntityLivingBase.class);
                            if (entitylivingbase != null) {
                                pos = entitylivingbase.getPosition();
                            }
                            param = Arrays.copyOfRange(param, 2, param.length);
                        } else {
                            if (param.length < 4) {
                                return;
                            }
                            pos = CommandBase.parseBlockPos((ICommandSender)DataScenes.this.npc, (String[])param, (int)1, (boolean)false);
                            param = Arrays.copyOfRange(param, 4, param.length);
                        }
                        if (pos == null) continue;
                        ((DataScenes)DataScenes.this).npc.ais.setStartPos(pos);
                        DataScenes.this.npc.getNavigator().clearPath();
                        if (move) {
                            Path pathentity = DataScenes.this.npc.getNavigator().getPathToPos(pos);
                            DataScenes.this.npc.getNavigator().setPath(pathentity, 1.0);
                            continue;
                        }
                        if (DataScenes.this.npc.isInRange((double)pos.getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.5, 2.0)) continue;
                        DataScenes.this.npc.setPosition((double)pos.getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.5);
                    }
                } else if (event.type == SceneType.SAY) {
                    DataScenes.this.npc.saySurrounding(new Line(event.param));
                } else if (event.type == SceneType.ROTATE) {
                    ((DataScenes)DataScenes.this).npc.lookAi.resetTask();
                    if (event.param.startsWith("@")) {
                        EntityLivingBase entitylivingbase = (EntityLivingBase)CommandBase.getEntity((MinecraftServer)DataScenes.this.npc.getServer(), (ICommandSender)DataScenes.this.npc, (String)event.param, EntityLivingBase.class);
                        ((DataScenes)DataScenes.this).npc.lookAi.rotate((Entity)((DataScenes)DataScenes.this).npc.world.getClosestPlayerToEntity((Entity)entitylivingbase, 30.0));
                    } else {
                        ((DataScenes)DataScenes.this).npc.lookAi.rotate(Integer.parseInt(event.param));
                    }
                } else if (event.type == SceneType.EQUIP) {
                    String[] args = event.param.split(" ");
                    if (args.length < 2) {
                        return;
                    }
                    IItemStack itemstack = null;
                    if (!args[1].equalsIgnoreCase("none")) {
                        Item item = CommandBase.getItemByText((ICommandSender)DataScenes.this.npc, (String)args[1]);
                        int i = args.length >= 3 ? CommandBase.parseInt((String)args[2], (int)1, (int)64) : 1;
                        int j = args.length >= 4 ? CommandBase.parseInt((String)args[3]) : 0;
                        itemstack = NpcAPI.Instance().getIItemStack(new ItemStack(item, i, j));
                    }
                    if (args[0].equalsIgnoreCase("main")) {
                        ((DataScenes)DataScenes.this).npc.inventory.weapons.put(0, itemstack);
                    } else if (args[0].equalsIgnoreCase("off")) {
                        ((DataScenes)DataScenes.this).npc.inventory.weapons.put(2, itemstack);
                    } else if (args[0].equalsIgnoreCase("proj")) {
                        ((DataScenes)DataScenes.this).npc.inventory.weapons.put(1, itemstack);
                    } else if (args[0].equalsIgnoreCase("head")) {
                        ((DataScenes)DataScenes.this).npc.inventory.armor.put(0, itemstack);
                    } else if (args[0].equalsIgnoreCase("body")) {
                        ((DataScenes)DataScenes.this).npc.inventory.armor.put(1, itemstack);
                    } else if (args[0].equalsIgnoreCase("legs")) {
                        ((DataScenes)DataScenes.this).npc.inventory.armor.put(2, itemstack);
                    } else if (args[0].equalsIgnoreCase("boots")) {
                        ((DataScenes)DataScenes.this).npc.inventory.armor.put(3, itemstack);
                    }
                } else if (event.type == SceneType.ATTACK) {
                    if (event.param.equals("none")) {
                        DataScenes.this.npc.setAttackTarget(null);
                    } else {
                        EntityLivingBase entity = (EntityLivingBase)CommandBase.getEntity((MinecraftServer)DataScenes.this.npc.getServer(), (ICommandSender)DataScenes.this.npc, (String)event.param, EntityLivingBase.class);
                        if (entity != null) {
                            DataScenes.this.npc.setAttackTarget(entity);
                        }
                    }
                } else if (event.type == SceneType.THROW) {
                    String[] args = event.param.split(" ");
                    EntityLivingBase entity = (EntityLivingBase)CommandBase.getEntity((MinecraftServer)DataScenes.this.npc.getServer(), (ICommandSender)DataScenes.this.npc, (String)args[0], EntityLivingBase.class);
                    if (entity == null) {
                        return;
                    }
                    float damage = Float.parseFloat(args[1]);
                    if (damage <= 0.0f) {
                        damage = 0.01f;
                    }
                    ItemStack stack = ItemStackWrapper.MCItem(((DataScenes)DataScenes.this).npc.inventory.getProjectile());
                    if (args.length > 2) {
                        Item item = CommandBase.getItemByText((ICommandSender)DataScenes.this.npc, (String)args[2]);
                        stack = new ItemStack(item, 1, 0);
                    }
                    EntityProjectile projectile = DataScenes.this.npc.shoot(entity, 100, stack, false);
                    projectile.damage = damage;
                } else if (event.type == SceneType.ANIMATE) {
                    ((DataScenes)DataScenes.this).npc.animateAi.temp = 0;
                    if (event.param.equalsIgnoreCase("sleep")) {
                        ((DataScenes)DataScenes.this).npc.animateAi.temp = 2;
                    } else if (event.param.equalsIgnoreCase("sneak")) {
                        ((DataScenes)DataScenes.this).npc.ais.animationType = 4;
                    } else if (event.param.equalsIgnoreCase("normal")) {
                        ((DataScenes)DataScenes.this).npc.ais.animationType = 0;
                    } else if (event.param.equalsIgnoreCase("sit")) {
                        ((DataScenes)DataScenes.this).npc.animateAi.temp = 1;
                    } else if (event.param.equalsIgnoreCase("crawl")) {
                        ((DataScenes)DataScenes.this).npc.ais.animationType = 7;
                    } else if (event.param.equalsIgnoreCase("bow")) {
                        ((DataScenes)DataScenes.this).npc.animateAi.temp = 11;
                    } else if (event.param.equalsIgnoreCase("yes")) {
                        ((DataScenes)DataScenes.this).npc.animateAi.temp = 13;
                    } else if (event.param.equalsIgnoreCase("no")) {
                        ((DataScenes)DataScenes.this).npc.animateAi.temp = 12;
                    }
                } else if (event.type == SceneType.COMMAND) {
                    NoppesUtilServer.runCommand(DataScenes.this.npc, DataScenes.this.npc.getName(), event.param, null);
                } else if (event.type == SceneType.STATS) {
                    int i = event.param.indexOf(" ");
                    if (i <= 0) {
                        return;
                    }
                    String type = event.param.substring(0, i).toLowerCase();
                    String value = event.param.substring(i).trim();
                    try {
                        if (type.equals("walking_speed")) {
                            ((DataScenes)DataScenes.this).npc.ais.setWalkingSpeed(ValueUtil.CorrectInt(Integer.parseInt(value), 0, 10));
                            break block77;
                        }
                        if (type.equals("size")) {
                            ((DataScenes)DataScenes.this).npc.display.setSize(ValueUtil.CorrectInt(Integer.parseInt(value), 1, 30));
                            break block77;
                        }
                        NoppesUtilServer.NotifyOPs("Unknown scene stat: " + type, new Object[0]);
                    }
                    catch (NumberFormatException e) {
                        NoppesUtilServer.NotifyOPs("Unknown scene stat " + type + " value: " + value, new Object[0]);
                    }
                } else if (event.type == SceneType.FACTION) {
                    DataScenes.this.npc.setFaction(Integer.parseInt(event.param));
                } else if (event.type == SceneType.FOLLOW) {
                    if (event.param.equalsIgnoreCase("none")) {
                        DataScenes.this.owner = null;
                        DataScenes.this.ownerScene = null;
                    } else {
                        EntityLivingBase entity = (EntityLivingBase)CommandBase.getEntity((MinecraftServer)DataScenes.this.npc.getServer(), (ICommandSender)DataScenes.this.npc, (String)event.param, EntityLivingBase.class);
                        if (entity == null) {
                            return;
                        }
                        DataScenes.this.owner = entity;
                        DataScenes.this.ownerScene = this.name;
                    }
                }
            }
        }
    }

    public static class SceneState {
        public boolean paused = false;
        public int ticks = -1;
    }
}

