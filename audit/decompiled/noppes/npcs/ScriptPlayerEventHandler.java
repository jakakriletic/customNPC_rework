/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.reflect.ClassPath
 *  com.google.common.reflect.ClassPath$ClassInfo
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.world.WorldServer
 *  net.minecraftforge.common.ForgeHooks
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.ServerChatEvent
 *  net.minecraftforge.event.entity.EntityEvent
 *  net.minecraftforge.event.entity.EntityEvent$EntityConstructing
 *  net.minecraftforge.event.entity.item.ItemTossEvent
 *  net.minecraftforge.event.entity.living.LivingAttackEvent
 *  net.minecraftforge.event.entity.living.LivingDeathEvent
 *  net.minecraftforge.event.entity.living.LivingHurtEvent
 *  net.minecraftforge.event.entity.player.ArrowLooseEvent
 *  net.minecraftforge.event.entity.player.EntityItemPickupEvent
 *  net.minecraftforge.event.entity.player.ItemTooltipEvent
 *  net.minecraftforge.event.entity.player.PlayerContainerEvent$Close
 *  net.minecraftforge.event.entity.player.PlayerContainerEvent$Open
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteract
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$LeftClickBlock
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickItem
 *  net.minecraftforge.event.world.BlockEvent$BreakEvent
 *  net.minecraftforge.event.world.GetCollisionBoxesEvent
 *  net.minecraftforge.event.world.WorldEvent
 *  net.minecraftforge.event.world.WorldEvent$PotentialSpawns
 *  net.minecraftforge.fml.common.Loader
 *  net.minecraftforge.fml.common.ModContainer
 *  net.minecraftforge.fml.common.eventhandler.Event
 *  net.minecraftforge.fml.common.eventhandler.EventPriority
 *  net.minecraftforge.fml.common.eventhandler.GenericEvent
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.gameevent.PlayerEvent
 *  net.minecraftforge.fml.common.gameevent.PlayerEvent$PlayerLoggedInEvent
 *  net.minecraftforge.fml.common.gameevent.PlayerEvent$PlayerLoggedOutEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$ClientTickEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$Phase
 *  net.minecraftforge.fml.common.gameevent.TickEvent$PlayerTickEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$RenderTickEvent
 *  net.minecraftforge.fml.common.network.FMLNetworkEvent$ClientCustomPacketEvent
 *  net.minecraftforge.fml.relauncher.Side
 */
package noppes.npcs;

import com.google.common.reflect.ClassPath;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.GenericEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.Server;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.event.ForgeEvent;
import noppes.npcs.api.event.ItemEvent;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerScriptData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemNbtBook;
import noppes.npcs.items.ItemScripted;

public class ScriptPlayerEventHandler {
    @SubscribeEvent
    public void onServerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.START) {
            return;
        }
        EntityPlayer player = event.player;
        PlayerData data = PlayerData.get(player);
        if (player.field_70173_aa % 10 == 0) {
            EventHooks.onPlayerTick(data.scriptData);
            for (int i = 0; i < player.field_71071_by.func_70302_i_(); ++i) {
                ItemStack item = player.field_71071_by.func_70301_a(i);
                if (item.func_190926_b() || item.func_77973_b() != CustomItems.scripted_item) continue;
                ItemScriptedWrapper isw = (ItemScriptedWrapper)NpcAPI.Instance().getIItemStack(item);
                EventHooks.onScriptItemUpdate(isw, player);
                if (!isw.updateClient) continue;
                isw.updateClient = false;
                Server.sendData((EntityPlayerMP)player, EnumPacketClient.UPDATE_ITEM, i, isw.getMCNbt());
            }
        }
        if (data.playerLevel != player.field_71068_ca) {
            EventHooks.onPlayerLevelUp(data.scriptData, data.playerLevel - player.field_71068_ca);
            data.playerLevel = player.field_71068_ca;
        }
        data.timers.update();
    }

    @SubscribeEvent
    public void invoke(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntityPlayer().field_70170_p.field_72995_K || event.getHand() != EnumHand.MAIN_HAND || !(event.getWorld() instanceof WorldServer)) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getEntityPlayer()).scriptData;
        PlayerEvent.AttackEvent ev = new PlayerEvent.AttackEvent(handler.getPlayer(), 2, NpcAPI.Instance().getIBlock(event.getWorld(), event.getPos()));
        event.setCanceled(EventHooks.onPlayerAttack(handler, ev));
        if (event.getItemStack().func_77973_b() == CustomItems.scripted_item && !event.isCanceled()) {
            ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
            ItemEvent.AttackEvent eve = new ItemEvent.AttackEvent(isw, handler.getPlayer(), 2, NpcAPI.Instance().getIBlock(event.getWorld(), event.getPos()));
            eve.setCanceled(event.isCanceled());
            event.setCanceled(EventHooks.onScriptItemAttack(isw, eve));
        }
    }

    @SubscribeEvent
    public void invoke(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntityPlayer().field_70170_p.field_72995_K || event.getHand() != EnumHand.MAIN_HAND || !(event.getWorld() instanceof WorldServer)) {
            return;
        }
        if (event.getItemStack().func_77973_b() == CustomItems.nbt_book) {
            ((ItemNbtBook)event.getItemStack().func_77973_b()).blockEvent(event);
            event.setCanceled(true);
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getEntityPlayer()).scriptData;
        handler.hadInteract = true;
        PlayerEvent.InteractEvent ev = new PlayerEvent.InteractEvent(handler.getPlayer(), 2, NpcAPI.Instance().getIBlock(event.getWorld(), event.getPos()));
        event.setCanceled(EventHooks.onPlayerInteract(handler, ev));
        if (event.getItemStack().func_77973_b() == CustomItems.scripted_item && !event.isCanceled()) {
            ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
            ItemEvent.InteractEvent eve = new ItemEvent.InteractEvent(isw, handler.getPlayer(), 2, NpcAPI.Instance().getIBlock(event.getWorld(), event.getPos()));
            event.setCanceled(EventHooks.onScriptItemInteract(isw, eve));
        }
    }

    @SubscribeEvent
    public void invoke(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntityPlayer().field_70170_p.field_72995_K || event.getHand() != EnumHand.MAIN_HAND || !(event.getWorld() instanceof WorldServer)) {
            return;
        }
        if (event.getItemStack().func_77973_b() == CustomItems.nbt_book) {
            ((ItemNbtBook)event.getItemStack().func_77973_b()).entityEvent(event);
            event.setCanceled(true);
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getEntityPlayer()).scriptData;
        PlayerEvent.InteractEvent ev = new PlayerEvent.InteractEvent(handler.getPlayer(), 1, NpcAPI.Instance().getIEntity(event.getTarget()));
        event.setCanceled(EventHooks.onPlayerInteract(handler, ev));
        if (event.getItemStack().func_77973_b() == CustomItems.scripted_item && !event.isCanceled()) {
            ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
            ItemEvent.InteractEvent eve = new ItemEvent.InteractEvent(isw, handler.getPlayer(), 1, NpcAPI.Instance().getIEntity(event.getTarget()));
            event.setCanceled(EventHooks.onScriptItemInteract(isw, eve));
        }
    }

    @SubscribeEvent
    public void invoke(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntityPlayer().field_70170_p.field_72995_K || event.getHand() != EnumHand.MAIN_HAND || !(event.getWorld() instanceof WorldServer)) {
            return;
        }
        if (event.getEntityPlayer().func_184812_l_() && event.getEntityPlayer().func_70093_af() && event.getItemStack().func_77973_b() == CustomItems.scripted_item) {
            NoppesUtilServer.sendOpenGui(event.getEntityPlayer(), EnumGuiType.ScriptItem, null);
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getEntityPlayer()).scriptData;
        if (handler.hadInteract) {
            handler.hadInteract = false;
            return;
        }
        PlayerEvent.InteractEvent ev = new PlayerEvent.InteractEvent(handler.getPlayer(), 0, null);
        event.setCanceled(EventHooks.onPlayerInteract(handler, ev));
        if (event.getItemStack().func_77973_b() == CustomItems.scripted_item && !event.isCanceled()) {
            ItemScriptedWrapper isw = ItemScripted.GetWrapper(event.getItemStack());
            ItemEvent.InteractEvent eve = new ItemEvent.InteractEvent(isw, handler.getPlayer(), 0, null);
            event.setCanceled(EventHooks.onScriptItemInteract(isw, eve));
        }
    }

    @SubscribeEvent
    public void invoke(ArrowLooseEvent event) {
        if (event.getEntityPlayer().field_70170_p.field_72995_K || !(event.getWorld() instanceof WorldServer)) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getEntityPlayer()).scriptData;
        PlayerEvent.RangedLaunchedEvent ev = new PlayerEvent.RangedLaunchedEvent(handler.getPlayer());
        event.setCanceled(EventHooks.onPlayerRanged(handler, ev));
    }

    @SubscribeEvent
    public void invoke(BlockEvent.BreakEvent event) {
        if (event.getPlayer().field_70170_p.field_72995_K || !(event.getWorld() instanceof WorldServer)) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getPlayer()).scriptData;
        PlayerEvent.BreakEvent ev = new PlayerEvent.BreakEvent(handler.getPlayer(), NpcAPI.Instance().getIBlock(event.getWorld(), event.getPos()), event.getExpToDrop());
        event.setCanceled(EventHooks.onPlayerBreak(handler, ev));
        event.setExpToDrop(ev.exp);
    }

    @SubscribeEvent
    public void invoke(ItemTossEvent event) {
        if (!(event.getPlayer().field_70170_p instanceof WorldServer)) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getPlayer()).scriptData;
        event.setCanceled(EventHooks.onPlayerToss(handler, event.getEntityItem()));
    }

    @SubscribeEvent
    public void invoke(EntityItemPickupEvent event) {
        if (!(event.getEntityPlayer().field_70170_p instanceof WorldServer)) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getEntityPlayer()).scriptData;
        event.setCanceled(EventHooks.onPlayerPickUp(handler, event.getItem()));
    }

    @SubscribeEvent
    public void invoke(PlayerContainerEvent.Open event) {
        if (!(event.getEntityPlayer().field_70170_p instanceof WorldServer)) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getEntityPlayer()).scriptData;
        EventHooks.onPlayerContainerOpen(handler, event.getContainer());
    }

    @SubscribeEvent
    public void invoke(PlayerContainerEvent.Close event) {
        if (!(event.getEntityPlayer().field_70170_p instanceof WorldServer)) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getEntityPlayer()).scriptData;
        EventHooks.onPlayerContainerClose(handler, event.getContainer());
    }

    @SubscribeEvent
    public void invoke(LivingDeathEvent event) {
        PlayerScriptData handler;
        if (!(event.getEntityLiving().field_70170_p instanceof WorldServer)) {
            return;
        }
        Entity source = NoppesUtilServer.GetDamageSourcee(event.getSource());
        if (event.getEntityLiving() instanceof EntityPlayer) {
            handler = PlayerData.get((EntityPlayer)((EntityPlayer)event.getEntityLiving())).scriptData;
            EventHooks.onPlayerDeath(handler, event.getSource(), source);
        }
        if (source instanceof EntityPlayer) {
            handler = PlayerData.get((EntityPlayer)((EntityPlayer)source)).scriptData;
            EventHooks.onPlayerKills(handler, event.getEntityLiving());
        }
    }

    @SubscribeEvent
    public void invoke(LivingHurtEvent event) {
        PlayerEvent pevent;
        PlayerScriptData handler;
        if (!(event.getEntityLiving().field_70170_p instanceof WorldServer)) {
            return;
        }
        Entity source = NoppesUtilServer.GetDamageSourcee(event.getSource());
        if (event.getEntityLiving() instanceof EntityPlayer) {
            handler = PlayerData.get((EntityPlayer)((EntityPlayer)event.getEntityLiving())).scriptData;
            pevent = new PlayerEvent.DamagedEvent(handler.getPlayer(), source, event.getAmount(), event.getSource());
            event.setCanceled(EventHooks.onPlayerDamaged(handler, pevent));
            event.setAmount(pevent.damage);
        }
        if (source instanceof EntityPlayer) {
            handler = PlayerData.get((EntityPlayer)((EntityPlayer)source)).scriptData;
            pevent = new PlayerEvent.DamagedEntityEvent(handler.getPlayer(), (Entity)event.getEntityLiving(), event.getAmount(), event.getSource());
            event.setCanceled(EventHooks.onPlayerDamagedEntity(handler, (PlayerEvent.DamagedEntityEvent)pevent));
            event.setAmount(((PlayerEvent.DamagedEntityEvent)pevent).damage);
        }
    }

    @SubscribeEvent
    public void invoke(LivingAttackEvent event) {
        if (!(event.getEntityLiving().field_70170_p instanceof WorldServer)) {
            return;
        }
        Entity source = NoppesUtilServer.GetDamageSourcee(event.getSource());
        if (source instanceof EntityPlayer) {
            PlayerScriptData handler = PlayerData.get((EntityPlayer)((EntityPlayer)source)).scriptData;
            ItemStack item = ((EntityPlayer)source).func_184614_ca();
            IEntity target = NpcAPI.Instance().getIEntity((Entity)event.getEntityLiving());
            PlayerEvent.AttackEvent ev = new PlayerEvent.AttackEvent(handler.getPlayer(), 1, target);
            event.setCanceled(EventHooks.onPlayerAttack(handler, ev));
            if (item.func_77973_b() == CustomItems.scripted_item && !event.isCanceled()) {
                ItemScriptedWrapper isw = ItemScripted.GetWrapper(item);
                ItemEvent.AttackEvent eve = new ItemEvent.AttackEvent(isw, handler.getPlayer(), 1, target);
                eve.setCanceled(event.isCanceled());
                event.setCanceled(EventHooks.onScriptItemAttack(isw, eve));
            }
        }
    }

    @SubscribeEvent
    public void invoke(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player.field_70170_p instanceof WorldServer)) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.player).scriptData;
        EventHooks.onPlayerLogin(handler);
    }

    @SubscribeEvent
    public void invoke(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player.field_70170_p instanceof WorldServer)) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.player).scriptData;
        EventHooks.onPlayerLogout(handler);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void invoke(ServerChatEvent event) {
        if (!(event.getPlayer().field_70170_p instanceof WorldServer) || event.getPlayer() == EntityNPCInterface.ChatEventPlayer) {
            return;
        }
        PlayerScriptData handler = PlayerData.get((EntityPlayer)event.getPlayer()).scriptData;
        String message = event.getMessage();
        PlayerEvent.ChatEvent ev = new PlayerEvent.ChatEvent(handler.getPlayer(), event.getMessage());
        EventHooks.onPlayerChat(handler, ev);
        event.setCanceled(ev.isCanceled());
        if (!message.equals(ev.message)) {
            TextComponentTranslation chat = new TextComponentTranslation("", new Object[0]);
            chat.func_150257_a(ForgeHooks.newChatWithLinks((String)ev.message));
            event.setComponent((ITextComponent)chat);
        }
    }

    public ScriptPlayerEventHandler registerForgeEvents() {
        block8: {
            ForgeEventHandler handler = new ForgeEventHandler();
            try {
                Method m = handler.getClass().getMethod("forgeEntity", Event.class);
                Method register = MinecraftForge.EVENT_BUS.getClass().getDeclaredMethod("register", Class.class, Object.class, Method.class, ModContainer.class);
                register.setAccessible(true);
                ArrayList list = new ArrayList(ClassPath.from((ClassLoader)this.getClass().getClassLoader()).getTopLevelClassesRecursive("net.minecraftforge.event"));
                list.addAll(ClassPath.from((ClassLoader)this.getClass().getClassLoader()).getTopLevelClassesRecursive("net.minecraftforge.fml.common"));
                for (ClassPath.ClassInfo info : list) {
                    String name = info.getName();
                    if (name.startsWith("net.minecraftforge.event.terraingen")) continue;
                    Class infoClass = info.load();
                    ArrayList classes = new ArrayList(Arrays.asList(infoClass.getDeclaredClasses()));
                    if (classes.isEmpty()) {
                        classes.add(infoClass);
                    }
                    for (Class clazz : classes) {
                        if (GenericEvent.class.isAssignableFrom(clazz) || EntityEvent.EntityConstructing.class.isAssignableFrom(clazz) || WorldEvent.PotentialSpawns.class.isAssignableFrom(clazz) || TickEvent.RenderTickEvent.class.isAssignableFrom(clazz) || TickEvent.ClientTickEvent.class.isAssignableFrom(clazz) || GetCollisionBoxesEvent.class.isAssignableFrom(clazz) || FMLNetworkEvent.ClientCustomPacketEvent.class.isAssignableFrom(clazz) || ItemTooltipEvent.class.isAssignableFrom(clazz) || !Event.class.isAssignableFrom(clazz) || Modifier.isAbstract(clazz.getModifiers()) || !Modifier.isPublic(clazz.getModifiers())) continue;
                        register.invoke(MinecraftForge.EVENT_BUS, clazz, handler, m, Loader.instance().activeModContainer());
                    }
                }
                if (!PixelmonHelper.Enabled) break block8;
                try {
                    Field f = ClassLoader.class.getDeclaredField("classes");
                    f.setAccessible(true);
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    ArrayList classes = new ArrayList((Vector)f.get(classLoader));
                    for (Class c : classes) {
                        if (!c.getName().startsWith("com.pixelmonmod.pixelmon.api.events") || !Event.class.isAssignableFrom(c) || Modifier.isAbstract(c.getModifiers()) || !Modifier.isPublic(c.getModifiers())) continue;
                        register.invoke(PixelmonHelper.EVENT_BUS, c, handler, m, Loader.instance().activeModContainer());
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public class ForgeEventHandler {
        @SubscribeEvent
        public void forgeEntity(Event event) {
            if (CustomNpcs.Server == null || !ScriptController.Instance.forgeScripts.isEnabled()) {
                return;
            }
            if (event instanceof EntityEvent) {
                EntityEvent ev = (EntityEvent)event;
                if (ev.getEntity() == null || !(ev.getEntity().field_70170_p instanceof WorldServer)) {
                    return;
                }
                EventHooks.onForgeEntityEvent(ev);
                return;
            }
            if (event instanceof WorldEvent) {
                WorldEvent ev = (WorldEvent)event;
                if (!(ev.getWorld() instanceof WorldServer)) {
                    return;
                }
                EventHooks.onForgeWorldEvent(ev);
                return;
            }
            if (event instanceof TickEvent && ((TickEvent)event).side == Side.CLIENT) {
                return;
            }
            if (event instanceof net.minecraftforge.fml.common.gameevent.PlayerEvent) {
                net.minecraftforge.fml.common.gameevent.PlayerEvent ev = (net.minecraftforge.fml.common.gameevent.PlayerEvent)event;
                if (!(ev.player.field_70170_p instanceof WorldServer)) {
                    return;
                }
            }
            EventHooks.onForgeEvent(new ForgeEvent(event), event);
        }
    }
}

