/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.item.Item
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteract
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 */
package noppes.npcs.items;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import noppes.npcs.CustomItems;
import noppes.npcs.Server;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.util.IPermission;

public class ItemNbtBook
extends Item
implements IPermission {
    public ItemNbtBook() {
        this.maxStackSize = 1;
        this.setCreativeTab(CustomItems.tab);
    }

    public Item setUnlocalizedName(String name) {
        super.setUnlocalizedName(name);
        this.setRegistryName(new ResourceLocation("customnpcs", name));
        return this;
    }

    public void blockEvent(PlayerInteractEvent.RightClickBlock event) {
        Server.sendData((EntityPlayerMP)event.getEntityPlayer(), EnumPacketClient.GUI, new Object[]{EnumGuiType.NbtBook, event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()});
        IBlockState state = event.getWorld().getBlockState(event.getPos());
        NBTTagCompound data = new NBTTagCompound();
        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        if (tile != null) {
            tile.writeToNBT(data);
        }
        NBTTagCompound compound = new NBTTagCompound();
        compound.setTag("Data", (NBTBase)data);
        Server.sendData((EntityPlayerMP)event.getEntityPlayer(), EnumPacketClient.GUI_DATA, compound);
    }

    public void entityEvent(PlayerInteractEvent.EntityInteract event) {
        Server.sendData((EntityPlayerMP)event.getEntityPlayer(), EnumPacketClient.GUI, new Object[]{EnumGuiType.NbtBook, 0, 0, 0});
        NBTTagCompound data = new NBTTagCompound();
        event.getTarget().writeToNBTAtomically(data);
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("EntityId", event.getTarget().getEntityId());
        compound.setTag("Data", (NBTBase)data);
        Server.sendData((EntityPlayerMP)event.getEntityPlayer(), EnumPacketClient.GUI_DATA, compound);
    }

    @Override
    public boolean isAllowed(EnumPacketServer e) {
        return e == EnumPacketServer.NbtBookSaveEntity || e == EnumPacketServer.NbtBookSaveBlock;
    }
}

