/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.ITileEntityProvider
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.math.BlockPos
 */
package noppes.npcs.roles;

import java.util.Stack;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.role.IJobBuilder;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.controllers.data.BlockData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobInterface;

public class JobBuilder
extends JobInterface
implements IJobBuilder {
    public TileBuilder build = null;
    private BlockPos possibleBuildPos = null;
    private Stack<BlockData> placingList = null;
    private BlockData placing = null;
    private int tryTicks = 0;
    private int ticks = 0;

    public JobBuilder(EntityNPCInterface npc) {
        super(npc);
        this.overrideMainHand = true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (this.build != null) {
            compound.func_74768_a("BuildX", this.build.func_174877_v().func_177958_n());
            compound.func_74768_a("BuildY", this.build.func_174877_v().func_177956_o());
            compound.func_74768_a("BuildZ", this.build.func_174877_v().func_177952_p());
            if (this.placingList != null && !this.placingList.isEmpty()) {
                NBTTagList list = new NBTTagList();
                for (BlockData data : this.placingList) {
                    list.func_74742_a((NBTBase)data.getNBT());
                }
                if (this.placing != null) {
                    list.func_74742_a((NBTBase)this.placing.getNBT());
                }
                compound.func_74782_a("Placing", (NBTBase)list);
            }
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        if (compound.func_74764_b("BuildX")) {
            this.possibleBuildPos = new BlockPos(compound.func_74762_e("BuildX"), compound.func_74762_e("BuildY"), compound.func_74762_e("BuildZ"));
        }
        if (this.possibleBuildPos != null && compound.func_74764_b("Placing")) {
            Stack<BlockData> placing = new Stack<BlockData>();
            NBTTagList list = compound.func_150295_c("Placing", 10);
            for (int i = 0; i < list.func_74745_c(); ++i) {
                BlockData data = BlockData.getData(list.func_150305_b(i));
                if (data == null) continue;
                placing.add(data);
            }
            this.placingList = placing;
        }
        this.npc.ais.doorInteract = 1;
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
    public boolean aiShouldExecute() {
        if (this.possibleBuildPos != null) {
            TileEntity tile = this.npc.field_70170_p.func_175625_s(this.possibleBuildPos);
            if (tile instanceof TileBuilder) {
                this.build = (TileBuilder)tile;
            } else {
                this.placingList.clear();
            }
            this.possibleBuildPos = null;
        }
        return this.build != null;
    }

    @Override
    public void aiUpdateTask() {
        if (this.build.finished && this.placingList == null || !this.build.enabled || this.build.func_145837_r()) {
            this.build = null;
            this.npc.func_70661_as().func_75492_a((double)this.npc.getStartXPos(), this.npc.getStartYPos(), (double)this.npc.getStartZPos(), 1.0);
            return;
        }
        if (this.ticks++ < 10) {
            return;
        }
        this.ticks = 0;
        if ((this.placingList == null || this.placingList.isEmpty()) && this.placing == null) {
            this.placingList = this.build.getBlock();
            this.npc.setJobData("");
            return;
        }
        if (this.placing == null) {
            this.placing = this.placingList.pop();
            if (this.placing.state.func_177230_c() == Blocks.field_189881_dj) {
                this.placing = null;
                return;
            }
            this.tryTicks = 0;
            this.npc.setJobData(this.blockToString(this.placing));
        }
        this.npc.func_70661_as().func_75492_a((double)this.placing.pos.func_177958_n(), (double)(this.placing.pos.func_177956_o() + 1), (double)this.placing.pos.func_177952_p(), 1.0);
        if (this.tryTicks++ > 40 || this.npc.nearPosition(this.placing.pos)) {
            BlockPos blockPos = this.placing.pos;
            this.placeBlock();
            if (this.tryTicks > 40) {
                blockPos = NoppesUtilServer.GetClosePos(blockPos, this.npc.field_70170_p);
                this.npc.func_70634_a((double)blockPos.func_177958_n() + 0.5, blockPos.func_177956_o(), (double)blockPos.func_177952_p() + 0.5);
            }
        }
    }

    private String blockToString(BlockData data) {
        if (data.state.func_177230_c() == Blocks.field_150350_a) {
            return Items.field_151035_b.getRegistryName().toString();
        }
        return this.itemToString(data.getStack());
    }

    @Override
    public void resetTask() {
        this.reset();
    }

    @Override
    public void reset() {
        this.build = null;
        this.npc.setJobData("");
    }

    public void placeBlock() {
        TileEntity tile;
        if (this.placing == null) {
            return;
        }
        this.npc.func_70661_as().func_75499_g();
        this.npc.func_184609_a(EnumHand.MAIN_HAND);
        this.npc.field_70170_p.func_180501_a(this.placing.pos, this.placing.state, 2);
        if (this.placing.state.func_177230_c() instanceof ITileEntityProvider && this.placing.tile != null && (tile = this.npc.field_70170_p.func_175625_s(this.placing.pos)) != null) {
            try {
                tile.func_145839_a(this.placing.tile);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.placing = null;
    }

    @Override
    public boolean isBuilding() {
        return this.build != null && this.build.enabled && !this.build.finished && this.build.started;
    }
}

