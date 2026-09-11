/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.toasts.IToast
 *  net.minecraft.client.renderer.block.model.ModelResourceLocation
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.PacketBuffer
 *  net.minecraft.util.SoundCategory
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.util.text.translation.I18n
 *  net.minecraft.village.MerchantRecipeList
 *  net.minecraftforge.client.model.ModelLoader
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.network.FMLNetworkEvent$ClientCustomPacketEvent
 */
package noppes.npcs.client;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.ModelData;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.PacketHandlerServer;
import noppes.npcs.Server;
import noppes.npcs.ServerEventsHandler;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.RenderChatMessages;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.gui.GuiAchievement;
import noppes.npcs.client.gui.GuiNpcMobSpawnerAdd;
import noppes.npcs.client.gui.player.GuiCustomChest;
import noppes.npcs.client.gui.player.GuiQuestCompletion;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.IGuiClose;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.IGuiError;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.SyncController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemScripted;

public class PacketHandlerClient
extends PacketHandlerServer {
    @SubscribeEvent
    public void onPacketData(FMLNetworkEvent.ClientCustomPacketEvent event) {
        EntityPlayerSP player = Minecraft.func_71410_x().field_71439_g;
        if (player == null) {
            return;
        }
        ByteBuf buffer = event.getPacket().payload();
        Minecraft.func_71410_x().func_152344_a(() -> this.lambda$onPacketData$0(buffer, (EntityPlayer)player));
    }

    private void client(ByteBuf buffer, EntityPlayer player, EnumPacketClient type) throws Exception {
        int config;
        if (type == EnumPacketClient.CHATBUBBLE) {
            Entity entity = Minecraft.func_71410_x().field_71441_e.func_73045_a(buffer.readInt());
            if (entity == null || !(entity instanceof EntityNPCInterface)) {
                return;
            }
            EntityNPCInterface npc = (EntityNPCInterface)entity;
            if (npc.messages == null) {
                npc.messages = new RenderChatMessages();
            }
            String text = NoppesStringUtils.formatText(Server.readString(buffer), new Object[]{player, npc});
            npc.messages.addMessage(text, npc);
            if (buffer.readBoolean()) {
                player.func_145747_a((ITextComponent)new TextComponentTranslation(npc.func_70005_c_() + ": " + text, new Object[0]));
            }
        } else if (type == EnumPacketClient.CHAT) {
            String str;
            String message = "";
            while ((str = Server.readString(buffer)) != null && !str.isEmpty()) {
                message = message + I18n.func_74838_a((String)str);
            }
            player.func_145747_a((ITextComponent)new TextComponentTranslation(message, new Object[0]));
        } else if (type == EnumPacketClient.EYE_BLINK) {
            Entity entity = Minecraft.func_71410_x().field_71441_e.func_73045_a(buffer.readInt());
            if (entity == null || !(entity instanceof EntityNPCInterface)) {
                return;
            }
            ModelData data = ((EntityCustomNpc)entity).modelData;
            data.eyes.blinkStart = System.currentTimeMillis();
        } else if (type == EnumPacketClient.MESSAGE) {
            TextComponentTranslation title = new TextComponentTranslation(Server.readString(buffer), new Object[0]);
            TextComponentTranslation message = new TextComponentTranslation(Server.readString(buffer), new Object[0]);
            int btype = buffer.readInt();
            Minecraft.func_71410_x().func_193033_an().func_192988_a((IToast)new GuiAchievement((ITextComponent)title, (ITextComponent)message, btype));
        } else if (type == EnumPacketClient.UPDATE_ITEM) {
            int id = buffer.readInt();
            NBTTagCompound compound = Server.readNBT(buffer);
            ItemStack stack = player.field_71071_by.func_70301_a(id);
            if (!stack.func_190926_b()) {
                ((ItemStackWrapper)NpcAPI.Instance().getIItemStack(stack)).setMCNbt(compound);
            }
        } else if (type == EnumPacketClient.SYNC_ADD || type == EnumPacketClient.SYNC_END) {
            int synctype = buffer.readInt();
            NBTTagCompound compound = Server.readNBT(buffer);
            SyncController.clientSync(synctype, compound, type == EnumPacketClient.SYNC_END);
            if (synctype == 8) {
                ClientProxy.playerData.setNBT(compound);
            } else if (synctype == 9) {
                if (player.func_184102_h() == null) {
                    ItemScripted.Resources = NBTTags.getIntegerStringMap(compound.func_150295_c("List", 10));
                }
                for (Map.Entry<Integer, String> entry : ItemScripted.Resources.entrySet()) {
                    ModelResourceLocation mrl = new ModelResourceLocation(entry.getValue(), "inventory");
                    Minecraft.func_71410_x().func_175599_af().func_175037_a().func_178086_a((Item)CustomItems.scripted_item, entry.getKey().intValue(), mrl);
                    ModelLoader.setCustomModelResourceLocation((Item)CustomItems.scripted_item, (int)entry.getKey(), (ModelResourceLocation)mrl);
                }
            }
        } else if (type == EnumPacketClient.SYNC_UPDATE) {
            int synctype = buffer.readInt();
            NBTTagCompound compound = Server.readNBT(buffer);
            SyncController.clientSyncUpdate(synctype, compound, buffer);
        } else if (type == EnumPacketClient.CHEST_NAME) {
            GuiScreen screen = Minecraft.func_71410_x().field_71462_r;
            if (screen instanceof GuiCustomChest) {
                ((GuiCustomChest)screen).title = I18n.func_74838_a((String)Server.readString(buffer));
            }
        } else if (type == EnumPacketClient.SYNC_REMOVE) {
            int synctype = buffer.readInt();
            int id = buffer.readInt();
            SyncController.clientSyncRemove(synctype, id);
        } else if (type == EnumPacketClient.MARK_DATA) {
            Entity entity = Minecraft.func_71410_x().field_71441_e.func_73045_a(buffer.readInt());
            if (entity == null || !(entity instanceof EntityLivingBase)) {
                return;
            }
            MarkData data = MarkData.get((EntityLivingBase)entity);
            data.setNBT(Server.readNBT(buffer));
        } else if (type == EnumPacketClient.DIALOG) {
            Entity entity = Minecraft.func_71410_x().field_71441_e.func_73045_a(buffer.readInt());
            if (entity == null || !(entity instanceof EntityNPCInterface)) {
                return;
            }
            Dialog dialog = DialogController.instance.dialogs.get(buffer.readInt());
            NoppesUtil.openDialog(dialog, (EntityNPCInterface)entity, player);
        } else if (type == EnumPacketClient.DIALOG_DUMMY) {
            EntityDialogNpc npc = new EntityDialogNpc(player.field_70170_p);
            npc.display.setName(Server.readString(buffer));
            EntityUtil.Copy((EntityLivingBase)player, (EntityLivingBase)npc);
            Dialog dialog = new Dialog(null);
            dialog.readNBT(Server.readNBT(buffer));
            NoppesUtil.openDialog(dialog, npc, player);
        } else if (type == EnumPacketClient.QUEST_COMPLETION) {
            int id = buffer.readInt();
            IQuest quest = QuestController.instance.get(id);
            if (!quest.getCompleteText().isEmpty()) {
                NoppesUtil.openGUI(player, new GuiQuestCompletion(quest));
            } else {
                NoppesUtilPlayer.sendData(EnumPlayerPacket.QuestCompletion, id);
            }
        } else if (type == EnumPacketClient.EDIT_NPC) {
            Entity entity = Minecraft.func_71410_x().field_71441_e.func_73045_a(buffer.readInt());
            if (entity == null || !(entity instanceof EntityNPCInterface)) {
                NoppesUtil.setLastNpc(null);
            } else {
                NoppesUtil.setLastNpc((EntityNPCInterface)entity);
            }
        } else if (type == EnumPacketClient.PLAY_MUSIC) {
            MusicController.Instance.playMusic(Server.readString(buffer), (Entity)player);
        } else if (type == EnumPacketClient.PLAY_SOUND) {
            MusicController.Instance.playSound(SoundCategory.VOICE, Server.readString(buffer), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readFloat(), buffer.readFloat());
        } else if (type == EnumPacketClient.UPDATE_NPC) {
            NBTTagCompound compound = Server.readNBT(buffer);
            Entity entity = Minecraft.func_71410_x().field_71441_e.func_73045_a(compound.func_74762_e("EntityId"));
            if (entity == null || !(entity instanceof EntityNPCInterface)) {
                return;
            }
            ((EntityNPCInterface)entity).readSpawnData(compound);
        } else if (type == EnumPacketClient.ROLE) {
            NBTTagCompound compound = Server.readNBT(buffer);
            Entity entity = Minecraft.func_71410_x().field_71441_e.func_73045_a(compound.func_74762_e("EntityId"));
            if (entity == null || !(entity instanceof EntityNPCInterface)) {
                return;
            }
            ((EntityNPCInterface)entity).advanced.setRole(compound.func_74762_e("Role"));
            ((EntityNPCInterface)entity).roleInterface.readFromNBT(compound);
            NoppesUtil.setLastNpc((EntityNPCInterface)entity);
        } else if (type == EnumPacketClient.GUI) {
            EnumGuiType gui = EnumGuiType.values()[buffer.readInt()];
            CustomNpcs.proxy.openGui(NoppesUtil.getLastNpc(), gui, buffer.readInt(), buffer.readInt(), buffer.readInt());
        } else if (type == EnumPacketClient.PARTICLE) {
            NoppesUtil.spawnParticle(buffer);
        } else if (type == EnumPacketClient.DELETE_NPC) {
            Entity entity = Minecraft.func_71410_x().field_71441_e.func_73045_a(buffer.readInt());
            if (entity == null || !(entity instanceof EntityNPCInterface)) {
                return;
            }
            ((EntityNPCInterface)entity).delete();
        } else if (type == EnumPacketClient.SCROLL_LIST) {
            NoppesUtil.setScrollList(buffer);
        } else if (type == EnumPacketClient.SCROLL_DATA) {
            NoppesUtil.setScrollData(buffer);
        } else if (type == EnumPacketClient.SCROLL_DATA_PART) {
            NoppesUtil.addScrollData(buffer);
        } else if (type == EnumPacketClient.SCROLL_SELECTED) {
            GuiScreen gui = Minecraft.func_71410_x().field_71462_r;
            if (gui == null || !(gui instanceof IScrollData)) {
                return;
            }
            String selected = Server.readString(buffer);
            ((IScrollData)gui).setSelected(selected);
        } else if (type == EnumPacketClient.CLONE) {
            NBTTagCompound compound = Server.readNBT(buffer);
            NoppesUtil.openGUI(player, new GuiNpcMobSpawnerAdd(compound));
        } else if (type == EnumPacketClient.GUI_DATA) {
            GuiScreen gui = Minecraft.func_71410_x().field_71462_r;
            if (gui == null) {
                return;
            }
            if (gui instanceof GuiNPCInterface && ((GuiNPCInterface)gui).hasSubGui()) {
                gui = ((GuiNPCInterface)gui).getSubGui();
            } else if (gui instanceof GuiContainerNPCInterface && ((GuiContainerNPCInterface)gui).hasSubGui()) {
                gui = ((GuiContainerNPCInterface)gui).getSubGui();
            }
            if (gui instanceof IGuiData) {
                ((IGuiData)gui).setGuiData(Server.readNBT(buffer));
            }
        } else if (type == EnumPacketClient.GUI_UPDATE) {
            GuiScreen gui = Minecraft.func_71410_x().field_71462_r;
            if (gui == null) {
                return;
            }
            gui.func_73866_w_();
        } else if (type == EnumPacketClient.GUI_ERROR) {
            GuiScreen gui = Minecraft.func_71410_x().field_71462_r;
            if (gui == null || !(gui instanceof IGuiError)) {
                return;
            }
            int i = buffer.readInt();
            NBTTagCompound compound = Server.readNBT(buffer);
            ((IGuiError)gui).setError(i, compound);
        } else if (type == EnumPacketClient.GUI_CLOSE) {
            GuiScreen gui = Minecraft.func_71410_x().field_71462_r;
            if (gui == null) {
                return;
            }
            if (gui instanceof IGuiClose) {
                int i = buffer.readInt();
                NBTTagCompound compound = Server.readNBT(buffer);
                ((IGuiClose)gui).setClose(i, compound);
            }
            Minecraft mc = Minecraft.func_71410_x();
            mc.func_147108_a(null);
            mc.func_71381_h();
        } else if (type == EnumPacketClient.VILLAGER_LIST) {
            MerchantRecipeList merchantrecipelist = MerchantRecipeList.func_151390_b((PacketBuffer)new PacketBuffer(buffer));
            ServerEventsHandler.Merchant.func_70930_a(merchantrecipelist);
        } else if (type == EnumPacketClient.CONFIG && (config = buffer.readInt()) == 0) {
            String font = Server.readString(buffer);
            int size = buffer.readInt();
            Runnable run = () -> {
                if (!font.isEmpty()) {
                    CustomNpcs.FontType = font;
                    CustomNpcs.FontSize = size;
                    ClientProxy.Font.clear();
                    ClientProxy.Font = new ClientProxy.FontContainer(CustomNpcs.FontType, CustomNpcs.FontSize);
                    CustomNpcs.Config.updateConfig();
                    player.func_145747_a((ITextComponent)new TextComponentTranslation("Font set to %s", new Object[]{ClientProxy.Font.getName()}));
                } else {
                    player.func_145747_a((ITextComponent)new TextComponentTranslation("Current font is %s", new Object[]{ClientProxy.Font.getName()}));
                }
            };
            Minecraft.func_71410_x().func_152344_a(run);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private /* synthetic */ void lambda$onPacketData$0(ByteBuf buffer, EntityPlayer player) {
        EnumPacketClient type = null;
        try {
            type = EnumPacketClient.values()[buffer.readInt()];
            LogWriter.debug("Received: " + (Object)((Object)type));
            this.client(buffer, player, type);
        }
        catch (Exception e) {
            LogWriter.error("Error with EnumPacketClient." + (Object)((Object)type), e);
        }
        finally {
            buffer.release();
        }
    }
}

