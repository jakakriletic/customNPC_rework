/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockChest
 *  net.minecraft.block.BlockCrops
 *  net.minecraft.block.BlockStem
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.init.Blocks
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.tileentity.TileEntityChest
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.IBlockAccess
 */
package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockStem;
import net.minecraft.block.NpcBlockHelper;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.role.IJobFarmer;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.controllers.MassBlockController;
import noppes.npcs.controllers.data.BlockData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobInterface;

public class JobFarmer
extends JobInterface
implements MassBlockController.IMassBlock,
IJobFarmer {
    public int chestMode = 1;
    private List<BlockPos> trackedBlocks = new ArrayList<BlockPos>();
    private int ticks = 0;
    private int walkTicks = 0;
    private int blockTicks = 800;
    private boolean waitingForBlocks = false;
    private BlockPos ripe = null;
    private BlockPos chest = null;
    private ItemStack holding = ItemStack.field_190927_a;

    public JobFarmer(EntityNPCInterface npc) {
        super(npc);
        this.overrideMainHand = true;
    }

    @Override
    public IItemStack getMainhand() {
        String name = this.npc.getJobData();
        ItemStack item = this.stringToItem(name);
        if (item.func_190926_b()) {
            return this.npc.inventory.weapons.get(0);
        }
        return NpcAPI.Instance().getIItemStack(item);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.func_74768_a("JobChestMode", this.chestMode);
        if (!this.holding.func_190926_b()) {
            compound.func_74782_a("JobHolding", (NBTBase)this.holding.func_77955_b(new NBTTagCompound()));
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.chestMode = compound.func_74762_e("JobChestMode");
        this.holding = new ItemStack(compound.func_74775_l("JobHolding"));
        this.blockTicks = 1100;
    }

    public void setHolding(ItemStack item) {
        this.holding = item;
        this.npc.setJobData(this.itemToString(this.holding));
    }

    @Override
    public boolean aiShouldExecute() {
        if (!this.holding.func_190926_b()) {
            if (this.chestMode == 0) {
                this.setHolding(ItemStack.field_190927_a);
            } else if (this.chestMode == 1) {
                if (this.chest == null) {
                    this.dropItem(this.holding);
                    this.setHolding(ItemStack.field_190927_a);
                } else {
                    this.chest();
                }
            } else if (this.chestMode == 2) {
                this.dropItem(this.holding);
                this.setHolding(ItemStack.field_190927_a);
            }
            return false;
        }
        if (this.ripe != null) {
            this.pluck();
            return false;
        }
        if (!this.waitingForBlocks && this.blockTicks++ > 1200) {
            this.blockTicks = 0;
            this.waitingForBlocks = true;
            MassBlockController.Queue(this);
        }
        if (this.ticks++ < 100) {
            return false;
        }
        this.ticks = 0;
        return true;
    }

    private void dropItem(ItemStack item) {
        EntityItem entityitem = new EntityItem(this.npc.field_70170_p, this.npc.field_70165_t, this.npc.field_70163_u, this.npc.field_70161_v, item);
        entityitem.func_174869_p();
        this.npc.field_70170_p.func_72838_d((Entity)entityitem);
    }

    private void chest() {
        BlockPos pos = this.chest;
        this.npc.func_70661_as().func_75492_a((double)pos.func_177958_n(), (double)pos.func_177956_o(), (double)pos.func_177952_p(), 1.0);
        this.npc.func_70671_ap().func_75650_a((double)pos.func_177958_n(), (double)pos.func_177956_o(), (double)pos.func_177952_p(), 10.0f, (float)this.npc.func_70646_bf());
        if (this.npc.nearPosition(pos) || this.walkTicks++ > 400) {
            if (this.walkTicks < 400) {
                this.npc.func_184609_a(EnumHand.MAIN_HAND);
            }
            this.npc.func_70661_as().func_75499_g();
            this.ticks = 100;
            this.walkTicks = 0;
            IBlockState state = this.npc.field_70170_p.func_180495_p(pos);
            if (state.func_177230_c() instanceof BlockChest) {
                int i;
                TileEntityChest tile = (TileEntityChest)this.npc.field_70170_p.func_175625_s(pos);
                for (i = 0; !this.holding.func_190926_b() && i < tile.func_70302_i_(); ++i) {
                    this.holding = this.mergeStack((IInventory)tile, i, this.holding);
                }
                for (i = 0; !this.holding.func_190926_b() && i < tile.func_70302_i_(); ++i) {
                    ItemStack item = tile.func_70301_a(i);
                    if (!item.func_190926_b()) continue;
                    tile.func_70299_a(i, this.holding);
                    this.holding = ItemStack.field_190927_a;
                }
                if (!this.holding.func_190926_b()) {
                    this.dropItem(this.holding);
                    this.holding = ItemStack.field_190927_a;
                }
            } else {
                this.chest = null;
            }
            this.setHolding(this.holding);
        }
    }

    private ItemStack mergeStack(IInventory inventory, int slot, ItemStack item) {
        ItemStack item2 = inventory.func_70301_a(slot);
        if (!NoppesUtilPlayer.compareItems(item, item2, false, false)) {
            return item;
        }
        int size = item2.func_77976_d() - item2.func_190916_E();
        if (size >= item.func_190916_E()) {
            item2.func_190920_e(item2.func_190916_E() + item.func_190916_E());
            return ItemStack.field_190927_a;
        }
        item2.func_190920_e(item2.func_77976_d());
        item.func_190920_e(item.func_190916_E() - size);
        if (item.func_190926_b()) {
            return ItemStack.field_190927_a;
        }
        return item;
    }

    private void pluck() {
        BlockPos pos = this.ripe;
        this.npc.func_70661_as().func_75492_a((double)pos.func_177958_n(), (double)pos.func_177956_o(), (double)pos.func_177952_p(), 1.0);
        this.npc.func_70671_ap().func_75650_a((double)pos.func_177958_n(), (double)pos.func_177956_o(), (double)pos.func_177952_p(), 10.0f, (float)this.npc.func_70646_bf());
        if (this.npc.nearPosition(pos) || this.walkTicks++ > 400) {
            if (this.walkTicks > 400) {
                pos = NoppesUtilServer.GetClosePos(pos, this.npc.field_70170_p);
                this.npc.func_70634_a((double)pos.func_177958_n() + 0.5, pos.func_177956_o(), (double)pos.func_177952_p() + 0.5);
            }
            this.ripe = null;
            this.npc.func_70661_as().func_75499_g();
            this.ticks = 90;
            this.walkTicks = 0;
            this.npc.func_184609_a(EnumHand.MAIN_HAND);
            IBlockState state = this.npc.field_70170_p.func_180495_p(pos);
            Block b = state.func_177230_c();
            if (b instanceof BlockCrops && ((BlockCrops)b).func_185525_y(state)) {
                BlockCrops crop = (BlockCrops)b;
                this.npc.field_70170_p.func_175656_a(pos, crop.func_185528_e(0));
                this.holding = new ItemStack(NpcBlockHelper.getCrop((BlockCrops)b));
            }
            if (b instanceof BlockStem) {
                EnumFacing facing = (EnumFacing)(state = b.func_176221_a(state, (IBlockAccess)this.npc.field_70170_p, pos)).func_177229_b((IProperty)BlockStem.field_176483_b);
                if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
                    return;
                }
                pos = pos.func_177971_a(facing.func_176730_m());
                b = this.npc.field_70170_p.func_180495_p(pos).func_177230_c();
                this.npc.field_70170_p.func_175698_g(pos);
                if (b != Blocks.field_150350_a) {
                    this.holding = new ItemStack(b);
                }
            }
            this.setHolding(this.holding);
        }
    }

    @Override
    public boolean aiContinueExecute() {
        return false;
    }

    @Override
    public void aiUpdateTask() {
        Iterator<BlockPos> ite = this.trackedBlocks.iterator();
        while (ite.hasNext() && this.ripe == null) {
            BlockPos pos = ite.next();
            IBlockState state = this.npc.field_70170_p.func_180495_p(pos);
            Block b = state.func_177230_c();
            if (b instanceof BlockCrops) {
                if (!((BlockCrops)b).func_185525_y(state)) continue;
                this.ripe = pos;
                continue;
            }
            if (b instanceof BlockStem) {
                EnumFacing facing = (EnumFacing)(state = b.func_176221_a(state, (IBlockAccess)this.npc.field_70170_p, pos)).func_177229_b((IProperty)BlockStem.field_176483_b);
                if (facing == EnumFacing.UP) continue;
                this.ripe = pos;
                continue;
            }
            ite.remove();
        }
        boolean bl = this.npc.ais.returnToStart = this.ripe == null;
        if (this.ripe != null) {
            this.npc.func_70661_as().func_75499_g();
            this.npc.func_70671_ap().func_75650_a((double)this.ripe.func_177958_n(), (double)this.ripe.func_177956_o(), (double)this.ripe.func_177952_p(), 10.0f, (float)this.npc.func_70646_bf());
        }
    }

    @Override
    public boolean isPlucking() {
        return this.ripe != null || !this.holding.func_190926_b();
    }

    @Override
    public EntityNPCInterface getNpc() {
        return this.npc;
    }

    @Override
    public int getRange() {
        return 16;
    }

    @Override
    public void processed(List<BlockData> list) {
        ArrayList<BlockPos> trackedBlocks = new ArrayList<BlockPos>();
        BlockPos chest = null;
        for (BlockData data : list) {
            Block b = data.state.func_177230_c();
            if (b instanceof BlockChest) {
                if (chest != null && !(this.npc.func_174818_b(chest) > this.npc.func_174818_b(data.pos))) continue;
                chest = data.pos;
                continue;
            }
            if (!(b instanceof BlockCrops) && !(b instanceof BlockStem) || trackedBlocks.contains(data.pos)) continue;
            trackedBlocks.add(data.pos);
        }
        this.chest = chest;
        this.trackedBlocks = trackedBlocks;
        this.waitingForBlocks = false;
    }

    @Override
    public int getMutexBits() {
        return this.npc.func_70661_as().func_75500_f() ? (byte)0 : AiMutex.LOOK;
    }
}

