/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Charsets
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.Unpooled
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTSizeTracker
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.PacketBuffer
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.village.MerchantRecipeList
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.common.network.internal.FMLProxyPacket
 */
package noppes.npcs;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.util.CustomNPCsScheduler;

public class Server {
    public static void sendData(EntityPlayerMP player, EnumPacketClient enu, Object ... obs) {
        Server.sendDataDelayed(player, enu, 0, obs);
    }

    public static void sendDataDelayed(EntityPlayerMP player, EnumPacketClient type, int delay, Object ... obs) {
        CustomNPCsScheduler.runTack(() -> {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            try {
                if (!Server.fillBuffer((ByteBuf)buffer, type, obs)) {
                    return;
                }
                LogWriter.debug("Send: " + (Object)((Object)type));
                CustomNpcs.Channel.sendTo(new FMLProxyPacket(buffer, "CustomNPCs"), player);
            }
            catch (IOException e) {
                LogWriter.error((Object)((Object)type) + " Errored", e);
            }
        }, delay);
    }

    public static boolean sendDataChecked(EntityPlayerMP player, EnumPacketClient type, Object ... obs) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            if (!Server.fillBuffer((ByteBuf)buffer, type, obs)) {
                return false;
            }
            LogWriter.debug("SendDataChecked: " + (Object)((Object)type));
            CustomNpcs.Channel.sendTo(new FMLProxyPacket(buffer, "CustomNPCs"), player);
        }
        catch (IOException e) {
            LogWriter.error((Object)((Object)type) + " Errored", e);
        }
        return true;
    }

    public static void sendAssociatedData(Entity entity, EnumPacketClient type, Object ... obs) {
        List list = entity.world.getEntitiesWithinAABB(EntityPlayerMP.class, entity.getEntityBoundingBox().grow(160.0, 160.0, 160.0));
        if (list.isEmpty()) {
            return;
        }
        CustomNPCsScheduler.runTack(() -> {
            ByteBuf buffer = Unpooled.buffer();
            try {
                if (!Server.fillBuffer(buffer, type, obs)) {
                    return;
                }
                LogWriter.debug("SendAssociatedData: " + (Object)((Object)type));
                for (EntityPlayerMP player : list) {
                    CustomNpcs.Channel.sendTo(new FMLProxyPacket(new PacketBuffer(buffer.copy()), "CustomNPCs"), player);
                }
            }
            catch (IOException e) {
                LogWriter.error((Object)((Object)type) + " Errored", e);
            }
            finally {
                buffer.release();
            }
        });
    }

    public static void sendRangedData(Entity entity, int range, EnumPacketClient type, Object ... obs) {
        List list = entity.world.getEntitiesWithinAABB(EntityPlayerMP.class, entity.getEntityBoundingBox().grow((double)range, (double)range, (double)range));
        if (list.isEmpty()) {
            return;
        }
        CustomNPCsScheduler.runTack(() -> {
            ByteBuf buffer = Unpooled.buffer();
            try {
                if (!Server.fillBuffer(buffer, type, obs)) {
                    return;
                }
                LogWriter.debug("sendRangedData: " + (Object)((Object)type));
                for (EntityPlayerMP player : list) {
                    CustomNpcs.Channel.sendTo(new FMLProxyPacket(new PacketBuffer(buffer.copy()), "CustomNPCs"), player);
                }
            }
            catch (IOException e) {
                LogWriter.error((Object)((Object)type) + " Errored", e);
            }
            finally {
                buffer.release();
            }
        });
    }

    public static void sendRangedData(World world, BlockPos pos, int range, EnumPacketClient type, Object ... obs) {
        List list = world.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(pos).grow((double)range, (double)range, (double)range));
        if (list.isEmpty()) {
            return;
        }
        CustomNPCsScheduler.runTack(() -> {
            ByteBuf buffer = Unpooled.buffer();
            try {
                if (!Server.fillBuffer(buffer, type, obs)) {
                    return;
                }
                LogWriter.debug("sendRangedData: " + (Object)((Object)type));
                for (EntityPlayerMP player : list) {
                    CustomNpcs.Channel.sendTo(new FMLProxyPacket(new PacketBuffer(buffer.copy()), "CustomNPCs"), player);
                }
            }
            catch (IOException e) {
                LogWriter.error((Object)((Object)type) + " Errored", e);
            }
            finally {
                buffer.release();
            }
        });
    }

    public static void sendToAll(MinecraftServer server, EnumPacketClient type, Object ... obs) {
        ArrayList list = new ArrayList(server.getPlayerList().getPlayers());
        CustomNPCsScheduler.runTack(() -> {
            ByteBuf buffer = Unpooled.buffer();
            try {
                if (!Server.fillBuffer(buffer, type, obs)) {
                    return;
                }
                LogWriter.debug("SendToAll: " + (Object)((Object)type));
                for (EntityPlayerMP player : list) {
                    CustomNpcs.Channel.sendTo(new FMLProxyPacket(new PacketBuffer(buffer.copy()), "CustomNPCs"), player);
                }
            }
            catch (IOException e) {
                LogWriter.error((Object)((Object)type) + " Errored", e);
            }
            finally {
                buffer.release();
            }
        });
    }

    public static boolean fillBuffer(ByteBuf buffer, Enum enu, Object ... obs) throws IOException {
        buffer.writeInt(enu.ordinal());
        for (Object ob : obs) {
            if (ob == null) continue;
            if (ob instanceof Map) {
                Map map = (Map)ob;
                buffer.writeInt(map.size());
                for (String key : map.keySet()) {
                    int value = (Integer)map.get(key);
                    buffer.writeInt(value);
                    Server.writeString(buffer, key);
                }
                continue;
            }
            if (ob instanceof MerchantRecipeList) {
                ((MerchantRecipeList)ob).writeToBuf(new PacketBuffer(buffer));
                continue;
            }
            if (ob instanceof List) {
                List list = (List)ob;
                buffer.writeInt(list.size());
                for (String s : list) {
                    Server.writeString(buffer, s);
                }
                continue;
            }
            if (ob instanceof UUID) {
                Server.writeString(buffer, ob.toString());
                continue;
            }
            if (ob instanceof Enum) {
                buffer.writeInt(((Enum)ob).ordinal());
                continue;
            }
            if (ob instanceof Integer) {
                buffer.writeInt(((Integer)ob).intValue());
                continue;
            }
            if (ob instanceof Boolean) {
                buffer.writeBoolean(((Boolean)ob).booleanValue());
                continue;
            }
            if (ob instanceof String) {
                Server.writeString(buffer, (String)ob);
                continue;
            }
            if (ob instanceof Float) {
                buffer.writeFloat(((Float)ob).floatValue());
                continue;
            }
            if (ob instanceof Long) {
                buffer.writeLong(((Long)ob).longValue());
                continue;
            }
            if (ob instanceof Double) {
                buffer.writeDouble(((Double)ob).doubleValue());
                continue;
            }
            if (!(ob instanceof NBTTagCompound)) continue;
            Server.writeNBT(buffer, (NBTTagCompound)ob);
        }
        if (buffer.array().length >= 65534) {
            LogWriter.error("Packet " + enu + " was too big to be send");
            return false;
        }
        return true;
    }

    public static UUID readUUID(ByteBuf buffer) {
        return UUID.fromString(Server.readString(buffer));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void writeNBT(ByteBuf buffer, NBTTagCompound compound) throws IOException {
        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
        try (DataOutputStream dataoutputstream = new DataOutputStream(new GZIPOutputStream(bytearrayoutputstream));){
            CompressedStreamTools.write((NBTTagCompound)compound, (DataOutput)dataoutputstream);
        }
        byte[] bytes = bytearrayoutputstream.toByteArray();
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static NBTTagCompound readNBT(ByteBuf buffer) throws IOException {
        byte[] bytes = new byte[buffer.readInt()];
        buffer.readBytes(bytes);
        try (DataInputStream datainputstream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes))));){
            NBTTagCompound nBTTagCompound = CompressedStreamTools.read((DataInput)datainputstream, (NBTSizeTracker)NBTSizeTracker.INFINITE);
            return nBTTagCompound;
        }
    }

    public static void writeString(ByteBuf buffer, String s) {
        byte[] bytes = s.getBytes(Charsets.UTF_8);
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
    }

    public static String readString(ByteBuf buffer) {
        try {
            byte[] bytes = new byte[buffer.readInt()];
            buffer.readBytes(bytes);
            return new String(bytes, Charsets.UTF_8);
        }
        catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }
}

