/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 *  net.minecraftforge.common.util.FakePlayer
 */
package noppes.npcs.api.wrapper;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.ITimers;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.block.IBlockScripted;
import noppes.npcs.api.block.ITextPlane;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.BlockWrapper;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.entity.EntityNPCInterface;

public class BlockScriptedWrapper
extends BlockWrapper
implements IBlockScripted {
    private TileScripted tile;

    public BlockScriptedWrapper(World world, Block block, BlockPos pos) {
        super(world, block, pos);
        this.tile = (TileScripted)((BlockWrapper)this).tile;
    }

    @Override
    public void setModel(IItemStack item) {
        if (item == null) {
            this.tile.setItemModel(null, null);
        } else {
            this.tile.setItemModel(item.getMCItemStack(), Block.getBlockFromItem((Item)item.getMCItemStack().getItem()));
        }
    }

    @Override
    public void setModel(String name) {
        if (name == null) {
            this.tile.setItemModel(null, null);
        } else {
            ResourceLocation loc = new ResourceLocation(name);
            Block block = (Block)Block.REGISTRY.getObject((Object)loc);
            this.tile.setItemModel(new ItemStack((Item)Item.REGISTRY.getObject((Object)loc)), block);
        }
    }

    @Override
    public IItemStack getModel() {
        return NpcAPI.Instance().getIItemStack(this.tile.itemModel);
    }

    @Override
    public void setRedstonePower(int strength) {
        this.tile.setRedstonePower(strength);
    }

    @Override
    public int getRedstonePower() {
        return this.tile.powering;
    }

    @Override
    public void setIsLadder(boolean bo) {
        this.tile.isLadder = bo;
        this.tile.needsClientUpdate = true;
    }

    @Override
    public boolean getIsLadder() {
        return this.tile.isLadder;
    }

    @Override
    public void setIsPassible(boolean bo) {
        this.tile.isPassible = bo;
        this.tile.needsClientUpdate = true;
    }

    @Override
    public boolean getIsPassible() {
        return this.tile.isPassible;
    }

    @Override
    public void setLight(int value) {
        this.tile.setLightValue(value);
    }

    @Override
    public int getLight() {
        return this.tile.lightValue;
    }

    @Override
    public void setScale(float x, float y, float z) {
        this.tile.setScale(x, y, z);
    }

    @Override
    public float getScaleX() {
        return this.tile.scaleX;
    }

    @Override
    public float getScaleY() {
        return this.tile.scaleY;
    }

    @Override
    public float getScaleZ() {
        return this.tile.scaleZ;
    }

    @Override
    public void setRotation(int x, int y, int z) {
        this.tile.setRotation(x % 360, y % 360, z % 360);
    }

    @Override
    public int getRotationX() {
        return this.tile.rotationX;
    }

    @Override
    public int getRotationY() {
        return this.tile.rotationY;
    }

    @Override
    public int getRotationZ() {
        return this.tile.rotationZ;
    }

    @Override
    public float getHardness() {
        return this.tile.blockHardness;
    }

    @Override
    public void setHardness(float hardness) {
        this.tile.blockHardness = hardness;
    }

    @Override
    public float getResistance() {
        return this.tile.blockResistance;
    }

    @Override
    public void setResistance(float resistance) {
        this.tile.blockResistance = resistance;
    }

    @Override
    public String executeCommand(String command) {
        if (!this.tile.getWorld().getMinecraftServer().isCommandBlockEnabled()) {
            throw new CustomNPCsException("Command blocks need to be enabled to executeCommands", new Object[0]);
        }
        FakePlayer player = EntityNPCInterface.CommandPlayer;
        player.setWorld(this.tile.getWorld());
        player.setPosition((double)this.getX(), (double)this.getY(), (double)this.getZ());
        return NoppesUtilServer.runCommand(this.tile.getWorld(), this.tile.getPos(), "ScriptBlock: " + this.tile.getPos(), command, null, (ICommandSender)player);
    }

    @Override
    public ITextPlane getTextPlane() {
        return this.tile.text1;
    }

    @Override
    public ITextPlane getTextPlane2() {
        return this.tile.text2;
    }

    @Override
    public ITextPlane getTextPlane3() {
        return this.tile.text3;
    }

    @Override
    public ITextPlane getTextPlane4() {
        return this.tile.text4;
    }

    @Override
    public ITextPlane getTextPlane5() {
        return this.tile.text5;
    }

    @Override
    public ITextPlane getTextPlane6() {
        return this.tile.text6;
    }

    @Override
    public ITimers getTimers() {
        return this.tile.timers;
    }

    @Override
    protected void setTile(TileEntity tile) {
        this.tile = (TileScripted)tile;
        super.setTile(tile);
    }
}

