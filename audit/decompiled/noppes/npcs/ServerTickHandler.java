/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.world.WorldServer
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.gameevent.PlayerEvent$PlayerLoggedInEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$Phase
 *  net.minecraftforge.fml.common.gameevent.TickEvent$PlayerTickEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$ServerTickEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$WorldTickEvent
 *  net.minecraftforge.fml.relauncher.Side
 */
package noppes.npcs;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.NPCSpawning;
import noppes.npcs.Server;
import noppes.npcs.client.AnalyticsTracking;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.MassBlockController;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.controllers.SyncController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.data.DataScenes;

public class ServerTickHandler {
    public int ticks = 0;
    private String serverName = null;

    @SubscribeEvent
    public void onServerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.START) {
            return;
        }
        EntityPlayer player = event.player;
        PlayerData data = PlayerData.get(player);
        if (data.updateClient) {
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.SYNC_END, 8, data.getSyncNBT());
            data.updateClient = false;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.WorldTickEvent event) {
        if (event.side == Side.SERVER && event.phase == TickEvent.Phase.START) {
            NPCSpawning.findChunksForSpawning((WorldServer)event.world);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.side == Side.SERVER && event.phase == TickEvent.Phase.START && this.ticks++ >= 20) {
            SchematicController.Instance.updateBuilding();
            MassBlockController.Update();
            this.ticks = 0;
            for (DataScenes.SceneState state : DataScenes.StartedScenes.values()) {
                if (state.paused) continue;
                ++state.ticks;
            }
            for (DataScenes.SceneContainer entry : DataScenes.ScenesToRun) {
                entry.update();
            }
            DataScenes.ScenesToRun = new ArrayList<DataScenes.SceneContainer>();
        }
    }

    @SubscribeEvent
    public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (this.serverName == null) {
            String e = "local";
            MinecraftServer server = event.player.func_184102_h();
            if (server.func_71262_S()) {
                try {
                    e = InetAddress.getByName(server.func_71211_k()).getCanonicalHostName();
                }
                catch (UnknownHostException e1) {
                    e = server.func_71211_k();
                }
                if (server.func_71215_F() != 25565) {
                    e = e + ":" + server.func_71215_F();
                }
            }
            if (e == null || e.startsWith("192.168") || e.contains("127.0.0.1") || e.startsWith("localhost")) {
                e = "local";
            }
            this.serverName = e;
        }
        AnalyticsTracking.sendData(event.player, "join", this.serverName);
        SyncController.syncPlayer((EntityPlayerMP)event.player);
    }
}

