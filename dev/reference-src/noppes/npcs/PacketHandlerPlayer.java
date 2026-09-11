/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.NetHandlerPlayServer
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.network.FMLNetworkEvent$ServerCustomPacketEvent
 */
package noppes.npcs;

import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.LogWriter;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.Server;
import noppes.npcs.api.event.ItemEvent;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.constants.EnumCompanionTalent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.containers.ContainerMail;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.BankData;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerFactionData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.controllers.data.PlayerMailData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.PlayerScriptData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleTransporter;

public class PacketHandlerPlayer {
    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        EntityPlayerMP player = ((NetHandlerPlayServer)event.getHandler()).player;
        ByteBuf buffer = event.getPacket().payload();
        player.getServer().addScheduledTask(() -> {
            EnumPlayerPacket type = null;
            try {
                type = EnumPlayerPacket.values()[buffer.readInt()];
                LogWriter.debug("Received: " + (Object)((Object)type));
                this.player(buffer, player, type);
            }
            catch (Exception e) {
                LogWriter.error("Error with EnumPlayerPacket." + (Object)((Object)type), e);
            }
            finally {
                buffer.release();
            }
        });
    }

    private void player(ByteBuf buffer, EntityPlayerMP player, EnumPlayerPacket type) throws Exception {
        block62: {
            block65: {
                block64: {
                    block63: {
                        if (type != EnumPlayerPacket.MarkData) break block63;
                        Entity entity = player.getServer().getEntityFromUuid(Server.readUUID(buffer));
                        if (entity == null || !(entity instanceof EntityLivingBase)) {
                            return;
                        }
                        MarkData markData = MarkData.get((EntityLivingBase)entity);
                        break block62;
                    }
                    if (type != EnumPlayerPacket.KeyPressed) break block64;
                    if (!CustomNpcs.EnableScripting || ScriptController.Instance.languages.isEmpty()) {
                        return;
                    }
                    EventHooks.onPlayerKeyPressed(player, buffer.readInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
                    break block62;
                }
                if (type != EnumPlayerPacket.LeftClick) break block65;
                if (!CustomNpcs.EnableScripting || ScriptController.Instance.languages.isEmpty()) {
                    return;
                }
                ItemStack item = player.getHeldItemMainhand();
                PlayerScriptData handler = PlayerData.get((EntityPlayer)player).scriptData;
                PlayerEvent.AttackEvent ev = new PlayerEvent.AttackEvent(handler.getPlayer(), 0, null);
                EventHooks.onPlayerAttack(handler, ev);
                if (item.getItem() != CustomItems.scripted_item) break block62;
                ItemScriptedWrapper isw = ItemScripted.GetWrapper(item);
                ItemEvent.AttackEvent eve = new ItemEvent.AttackEvent(isw, handler.getPlayer(), 0, null);
                EventHooks.onScriptItemAttack(isw, eve);
                break block62;
            }
            if (type == EnumPlayerPacket.CloseGui) {
                player.closeContainer();
            } else if (type == EnumPlayerPacket.CompanionTalentExp) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc == null || npc.advanced.role != 6 || player != npc.getOwner()) {
                    return;
                }
                int id = buffer.readInt();
                int exp = buffer.readInt();
                RoleCompanion role = (RoleCompanion)npc.roleInterface;
                if (exp <= 0 || !role.canAddExp(-exp) || id < 0 || id >= EnumCompanionTalent.values().length) {
                    return;
                }
                EnumCompanionTalent talent = EnumCompanionTalent.values()[id];
                role.addExp(-exp);
                role.addTalentExp(talent, exp);
            } else if (type == EnumPlayerPacket.CompanionOpenInv) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc == null || npc.advanced.role != 6 || player != npc.getOwner()) {
                    return;
                }
                NoppesUtilServer.sendOpenGui((EntityPlayer)player, EnumGuiType.CompanionInv, npc);
            } else if (type == EnumPlayerPacket.FollowerHire) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc == null || npc.advanced.role != 2) {
                    return;
                }
                NoppesUtilPlayer.hireFollower(player, npc);
            } else if (type == EnumPlayerPacket.FollowerExtend) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc == null || npc.advanced.role != 2) {
                    return;
                }
                NoppesUtilPlayer.extendFollower(player, npc);
                Server.sendData(player, EnumPacketClient.GUI_DATA, npc.roleInterface.writeToNBT(new NBTTagCompound()));
            } else if (type == EnumPlayerPacket.FollowerState) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc == null || npc.advanced.role != 2) {
                    return;
                }
                NoppesUtilPlayer.changeFollowerState(player, npc);
                Server.sendData(player, EnumPacketClient.GUI_DATA, npc.roleInterface.writeToNBT(new NBTTagCompound()));
            } else if (type == EnumPlayerPacket.RoleGet) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc == null || npc.advanced.role == 0) {
                    return;
                }
                Server.sendData(player, EnumPacketClient.GUI_DATA, npc.roleInterface.writeToNBT(new NBTTagCompound()));
            } else if (type == EnumPlayerPacket.Transport) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc == null || npc.advanced.role != 4) {
                    return;
                }
                ((RoleTransporter)npc.roleInterface).transport(player, Server.readString(buffer));
            } else if (type == EnumPlayerPacket.BankUpgrade) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc == null || npc.advanced.role != 3) {
                    return;
                }
                NoppesUtilPlayer.bankUpgrade(player, npc);
            } else if (type == EnumPlayerPacket.BankUnlock) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc == null || npc.advanced.role != 3) {
                    return;
                }
                NoppesUtilPlayer.bankUnlock(player, npc);
            } else if (type == EnumPlayerPacket.BankSlotOpen) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc == null || npc.advanced.role != 3) {
                    return;
                }
                int slot = buffer.readInt();
                int bankId = buffer.readInt();
                BankData data = PlayerDataController.instance.getBankData((EntityPlayer)player, bankId).getBankOrDefault(bankId);
                data.openBankGui((EntityPlayer)player, npc, bankId, slot);
            } else if (type == EnumPlayerPacket.Dialog) {
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                LogWriter.debug("Dialog npc: " + (Object)((Object)npc));
                if (npc == null) {
                    return;
                }
                NoppesUtilPlayer.dialogSelected(buffer.readInt(), buffer.readInt(), player, npc);
            } else if (type == EnumPlayerPacket.CheckQuestCompletion) {
                PlayerQuestData playerdata = PlayerData.get((EntityPlayer)player).questData;
                playerdata.checkQuestCompletion((EntityPlayer)player, -1);
            } else if (type == EnumPlayerPacket.QuestCompletion) {
                NoppesUtilPlayer.questCompletion(player, buffer.readInt());
            } else if (type == EnumPlayerPacket.FactionsGet) {
                PlayerFactionData data = PlayerData.get((EntityPlayer)player).factionData;
                Server.sendData(player, EnumPacketClient.GUI_DATA, data.getPlayerGuiData());
            } else if (type == EnumPlayerPacket.MailGet) {
                PlayerMailData data = PlayerData.get((EntityPlayer)player).mailData;
                Server.sendData(player, EnumPacketClient.GUI_DATA, data.saveNBTData(new NBTTagCompound()));
            } else if (type == EnumPlayerPacket.MailDelete) {
                long time = buffer.readLong();
                String username = Server.readString(buffer);
                PlayerMailData data = PlayerData.get((EntityPlayer)player).mailData;
                Iterator<PlayerMail> it = data.playermail.iterator();
                while (it.hasNext()) {
                    PlayerMail mail = it.next();
                    if (mail.time != time || !mail.sender.equals(username)) continue;
                    it.remove();
                }
                Server.sendData(player, EnumPacketClient.GUI_DATA, data.saveNBTData(new NBTTagCompound()));
            } else if (type == EnumPlayerPacket.MailSend) {
                String username = PlayerDataController.instance.hasPlayer(Server.readString(buffer));
                if (username.isEmpty()) {
                    NoppesUtilServer.sendGuiError((EntityPlayer)player, 0);
                    return;
                }
                PlayerMail mail = new PlayerMail();
                String s = player.getDisplayNameString();
                if (!s.equals(player.getName())) {
                    s = s + "(" + player.getName() + ")";
                }
                mail.readNBT(Server.readNBT(buffer));
                mail.sender = s;
                mail.items = ((ContainerMail)player.openContainer).mail.items;
                if (mail.subject.isEmpty()) {
                    NoppesUtilServer.sendGuiError((EntityPlayer)player, 1);
                    return;
                }
                NBTTagCompound comp = new NBTTagCompound();
                comp.setString("username", username);
                NoppesUtilServer.sendGuiClose(player, 1, comp);
                EntityNPCInterface npc = NoppesUtilServer.getEditingNpc((EntityPlayer)player);
                if (npc != null && EventHooks.onNPCRole(npc, new RoleEvent.MailmanEvent((EntityPlayer)player, npc.wrappedNPC, mail))) {
                    return;
                }
                PlayerDataController.instance.addPlayerMessage(player.getServer(), username, mail);
            } else if (type == EnumPlayerPacket.MailboxOpenMail) {
                long time = buffer.readLong();
                String username = Server.readString(buffer);
                player.closeContainer();
                PlayerMailData data = PlayerData.get((EntityPlayer)player).mailData;
                for (PlayerMail mail : data.playermail) {
                    if (mail.time != time || !mail.sender.equals(username)) continue;
                    ContainerMail.staticmail = mail;
                    player.openGui((Object)CustomNpcs.instance, EnumGuiType.PlayerMailman.ordinal(), player.world, 0, 0, 0);
                    break;
                }
            } else if (type == EnumPlayerPacket.MailRead) {
                long time = buffer.readLong();
                String username = Server.readString(buffer);
                PlayerMailData data = PlayerData.get((EntityPlayer)player).mailData;
                for (PlayerMail mail : data.playermail) {
                    if (mail.beenRead || mail.time != time || !mail.sender.equals(username)) continue;
                    if (mail.hasQuest()) {
                        PlayerQuestController.addActiveQuest(mail.getQuest(), (EntityPlayer)player);
                    }
                    mail.beenRead = true;
                }
            }
        }
    }
}

