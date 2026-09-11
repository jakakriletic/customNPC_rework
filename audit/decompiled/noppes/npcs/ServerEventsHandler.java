/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.util.concurrent.ListenableFutureTask
 *  net.minecraft.command.CommandBase
 *  net.minecraft.command.CommandGive
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.passive.EntityVillager
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.ClassInheritanceMultiMap
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.village.MerchantRecipeList
 *  net.minecraft.world.WorldServer
 *  net.minecraftforge.event.AttachCapabilitiesEvent
 *  net.minecraftforge.event.CommandEvent
 *  net.minecraftforge.event.entity.EntityJoinWorldEvent
 *  net.minecraftforge.event.entity.living.LivingDeathEvent
 *  net.minecraftforge.event.entity.player.EntityItemPickupEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$SaveToFile
 *  net.minecraftforge.event.entity.player.PlayerEvent$StartTracking
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteract
 *  net.minecraftforge.event.terraingen.PopulateChunkEvent$Post
 *  net.minecraftforge.event.world.ChunkDataEvent$Save
 *  net.minecraftforge.fml.common.eventhandler.EventPriority
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 */
package noppes.npcs;

import com.google.common.util.concurrent.ListenableFutureTask;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandGive;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.NPCSpawning;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.Server;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.api.wrapper.WrapperEntityData;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemSoulstoneEmpty;
import noppes.npcs.quests.QuestKill;

public class ServerEventsHandler {
    public static EntityVillager Merchant;
    public static Entity mounted;

    @SubscribeEvent
    public void invoke(PlayerInteractEvent.EntityInteract event) {
        ItemStack item = event.getEntityPlayer().func_184614_ca();
        if (item == null) {
            return;
        }
        boolean isRemote = event.getEntityPlayer().field_70170_p.field_72995_K;
        boolean npcInteracted = event.getTarget() instanceof EntityNPCInterface;
        if (!isRemote && CustomNpcs.OpsOnly && !event.getEntityPlayer().func_184102_h().func_184103_al().func_152596_g(event.getEntityPlayer().func_146103_bH())) {
            return;
        }
        if (!isRemote && item.func_77973_b() == CustomItems.soulstoneEmpty && event.getTarget() instanceof EntityLivingBase) {
            ((ItemSoulstoneEmpty)item.func_77973_b()).store((EntityLivingBase)event.getTarget(), item, event.getEntityPlayer());
        }
        if (item.func_77973_b() == CustomItems.wand && npcInteracted && !isRemote) {
            if (!CustomNpcsPermissions.hasPermission(event.getEntityPlayer(), CustomNpcsPermissions.NPC_GUI)) {
                return;
            }
            event.setCanceled(true);
            NoppesUtilServer.sendOpenGui(event.getEntityPlayer(), EnumGuiType.MainMenuDisplay, (EntityNPCInterface)event.getTarget());
        } else if (item.func_77973_b() == CustomItems.cloner && !isRemote && !(event.getTarget() instanceof EntityPlayer)) {
            NBTTagCompound compound = new NBTTagCompound();
            if (!event.getTarget().func_184198_c(compound)) {
                return;
            }
            PlayerData data = PlayerData.get(event.getEntityPlayer());
            ServerCloneController.Instance.cleanTags(compound);
            if (!Server.sendDataChecked((EntityPlayerMP)event.getEntityPlayer(), EnumPacketClient.CLONE, compound)) {
                event.getEntityPlayer().func_145747_a((ITextComponent)new TextComponentString("Entity too big to clone"));
            }
            data.cloned = compound;
            event.setCanceled(true);
        } else if (item.func_77973_b() == CustomItems.scripter && !isRemote && npcInteracted) {
            if (!CustomNpcsPermissions.hasPermission(event.getEntityPlayer(), CustomNpcsPermissions.NPC_GUI)) {
                return;
            }
            NoppesUtilServer.setEditingNpc(event.getEntityPlayer(), (EntityNPCInterface)event.getTarget());
            event.setCanceled(true);
            Server.sendData((EntityPlayerMP)event.getEntityPlayer(), EnumPacketClient.GUI, EnumGuiType.Script.ordinal(), 0, 0, 0);
        } else if (item.func_77973_b() == CustomItems.mount) {
            if (!CustomNpcsPermissions.hasPermission(event.getEntityPlayer(), CustomNpcsPermissions.TOOL_MOUNTER)) {
                return;
            }
            event.setCanceled(true);
            mounted = event.getTarget();
            if (isRemote) {
                CustomNpcs.proxy.openGui(MathHelper.func_76128_c((double)ServerEventsHandler.mounted.field_70165_t), MathHelper.func_76128_c((double)ServerEventsHandler.mounted.field_70163_u), MathHelper.func_76128_c((double)ServerEventsHandler.mounted.field_70161_v), EnumGuiType.MobSpawnerMounter, event.getEntityPlayer());
            }
        } else if (item.func_77973_b() == CustomItems.wand && event.getTarget() instanceof EntityVillager) {
            if (!CustomNpcsPermissions.hasPermission(event.getEntityPlayer(), CustomNpcsPermissions.EDIT_VILLAGER)) {
                return;
            }
            event.setCanceled(true);
            Merchant = (EntityVillager)event.getTarget();
            if (!isRemote) {
                EntityPlayerMP player = (EntityPlayerMP)event.getEntityPlayer();
                player.openGui((Object)CustomNpcs.instance, EnumGuiType.MerchantAdd.ordinal(), player.field_70170_p, 0, 0, 0);
                MerchantRecipeList merchantrecipelist = Merchant.func_70934_b((EntityPlayer)player);
                if (merchantrecipelist != null) {
                    Server.sendData(player, EnumPacketClient.VILLAGER_LIST, merchantrecipelist);
                }
            }
        }
    }

    @SubscribeEvent
    public void invoke(LivingDeathEvent event) {
        if (event.getEntityLiving().field_70170_p.field_72995_K) {
            return;
        }
        Entity source = NoppesUtilServer.GetDamageSourcee(event.getSource());
        if (source != null) {
            if (source instanceof EntityNPCInterface && event.getEntityLiving() != null) {
                EntityNPCInterface npc = (EntityNPCInterface)source;
                Line line = npc.advanced.getKillLine();
                if (line != null) {
                    npc.saySurrounding(line.formatTarget(event.getEntityLiving()));
                }
                EventHooks.onNPCKills(npc, event.getEntityLiving());
            }
            EntityPlayer player = null;
            if (source instanceof EntityPlayer) {
                player = (EntityPlayer)source;
            } else if (source instanceof EntityNPCInterface && ((EntityNPCInterface)source).getOwner() instanceof EntityPlayer) {
                player = (EntityPlayer)((EntityNPCInterface)source).getOwner();
            }
            if (player != null) {
                this.doQuest(player, event.getEntityLiving(), true);
                if (event.getEntityLiving() instanceof EntityNPCInterface) {
                    this.doFactionPoints(player, (EntityNPCInterface)event.getEntityLiving());
                }
            }
        }
        if (event.getEntityLiving() instanceof EntityPlayer) {
            PlayerData data = PlayerData.get((EntityPlayer)event.getEntityLiving());
            data.save(false);
        }
    }

    private void doFactionPoints(EntityPlayer player, EntityNPCInterface npc) {
        npc.advanced.factions.addPoints(player);
    }

    private void doQuest(EntityPlayer player, EntityLivingBase entity, boolean all) {
        PlayerData pdata = PlayerData.get(player);
        PlayerQuestData playerdata = pdata.questData;
        String entityName = EntityList.func_75621_b((Entity)entity);
        if (entity instanceof EntityPlayer) {
            entityName = "Player";
        }
        for (QuestData data : playerdata.activeQuests.values()) {
            if (data.quest.type != 2 && data.quest.type != 4) continue;
            if (data.quest.type == 4 && all) {
                List list = player.field_70170_p.func_72872_a(EntityPlayer.class, entity.func_174813_aQ().func_72314_b(10.0, 10.0, 10.0));
                for (EntityPlayer pl : list) {
                    if (pl == player) continue;
                    this.doQuest(pl, entity, false);
                }
            }
            String name = entityName;
            QuestKill quest = (QuestKill)data.quest.questInterface;
            if (quest.targets.containsKey(entity.func_70005_c_())) {
                name = entity.func_70005_c_();
            } else if (!quest.targets.containsKey(name)) continue;
            HashMap<String, Integer> killed = quest.getKilled(data);
            if (killed.containsKey(name) && killed.get(name) >= quest.targets.get(name)) continue;
            int amount = 0;
            if (killed.containsKey(name)) {
                amount = killed.get(name);
            }
            killed.put(name, amount + 1);
            quest.setKilled(data, killed);
            pdata.updateClient = true;
        }
        playerdata.checkQuestCompletion(player, 2);
        playerdata.checkQuestCompletion(player, 4);
    }

    @SubscribeEvent
    public void pickUp(EntityItemPickupEvent event) {
        if (event.getEntityPlayer().field_70170_p.field_72995_K) {
            return;
        }
        PlayerQuestData playerdata = PlayerData.get((EntityPlayer)event.getEntityPlayer()).questData;
        playerdata.checkQuestCompletion(event.getEntityPlayer(), 0);
    }

    @SubscribeEvent
    public void commandGive(CommandEvent event) {
        if (!(event.getSender().func_130014_f_() instanceof WorldServer) || !(event.getCommand() instanceof CommandGive)) {
            return;
        }
        try {
            EntityPlayerMP player = CommandBase.func_184888_a((MinecraftServer)event.getSender().func_184102_h(), (ICommandSender)event.getSender(), (String)event.getParameters()[0]);
            player.func_184102_h().field_175589_i.add(ListenableFutureTask.create(Executors.callable(() -> ServerEventsHandler.lambda$commandGive$0((EntityPlayer)player))));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    @SubscribeEvent
    public void world(EntityJoinWorldEvent event) {
        if (event.getWorld().field_72995_K || !(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        PlayerData data = PlayerData.get((EntityPlayer)event.getEntity());
        data.updateCompanion(event.getWorld());
    }

    @SubscribeEvent
    public void populateChunk(PopulateChunkEvent.Post event) {
        NPCSpawning.performWorldGenSpawning(event.getWorld(), event.getChunkX(), event.getChunkZ(), event.getRand());
    }

    @SubscribeEvent(priority=EventPriority.LOW)
    public void attachEntity(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            PlayerData.register(event);
        }
        if (event.getObject() instanceof EntityLivingBase) {
            MarkData.register(event);
        }
        if (((Entity)event.getObject()).field_70170_p != null && !((Entity)event.getObject()).field_70170_p.field_72995_K && ((Entity)event.getObject()).field_70170_p instanceof WorldServer) {
            WrapperEntityData.register(event);
        }
    }

    @SubscribeEvent
    public void attachItem(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStackWrapper.register(event);
    }

    @SubscribeEvent
    public void savePlayer(PlayerEvent.SaveToFile event) {
        PlayerData.get(event.getEntityPlayer()).save(false);
    }

    @SubscribeEvent
    public void saveChunk(ChunkDataEvent.Save event) {
        for (ClassInheritanceMultiMap map : event.getChunk().func_177429_s()) {
            for (Entity e : map) {
                if (!(e instanceof EntityLivingBase)) continue;
                MarkData.get((EntityLivingBase)e).save();
            }
        }
    }

    @SubscribeEvent
    public void playerTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof EntityLivingBase) || event.getTarget().field_70170_p.field_72995_K) {
            return;
        }
        MarkData data = MarkData.get((EntityLivingBase)event.getTarget());
        if (data.marks.isEmpty()) {
            return;
        }
        Server.sendData((EntityPlayerMP)event.getEntityPlayer(), EnumPacketClient.MARK_DATA, event.getTarget().func_145782_y(), data.getNBT());
    }

    private static /* synthetic */ void lambda$commandGive$0(EntityPlayer player) {
        PlayerQuestData playerdata = PlayerData.get((EntityPlayer)player).questData;
        playerdata.checkQuestCompletion(player, 0);
    }
}

