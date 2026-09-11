/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTPrimitive
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraft.world.WorldServer
 *  net.minecraftforge.common.util.FakePlayer
 *  net.minecraftforge.fluids.BlockFluidBase
 */
package noppes.npcs.api.wrapper;

import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.BlockFluidBase;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.INbt;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.wrapper.BlockFluidContainerWrapper;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.api.wrapper.BlockScriptedDoorWrapper;
import noppes.npcs.api.wrapper.BlockScriptedWrapper;
import noppes.npcs.blocks.BlockScripted;
import noppes.npcs.blocks.BlockScriptedDoor;
import noppes.npcs.blocks.tiles.TileNpcEntity;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.LRUHashMap;

public class BlockWrapper
implements IBlock {
    private static final Map<String, BlockWrapper> blockCache = new LRUHashMap<String, BlockWrapper>(400);
    protected final IWorld world;
    protected final Block block;
    protected final BlockPos pos;
    protected final BlockPosWrapper bPos;
    protected TileEntity tile;
    protected TileNpcEntity storage;
    private final IData tempdata = new IData(){

        @Override
        public void remove(String key) {
            if (BlockWrapper.this.storage == null) {
                return;
            }
            BlockWrapper.this.storage.tempData.remove(key);
        }

        @Override
        public void put(String key, Object value) {
            if (BlockWrapper.this.storage == null) {
                return;
            }
            BlockWrapper.this.storage.tempData.put(key, value);
        }

        @Override
        public boolean has(String key) {
            if (BlockWrapper.this.storage == null) {
                return false;
            }
            return BlockWrapper.this.storage.tempData.containsKey(key);
        }

        @Override
        public Object get(String key) {
            if (BlockWrapper.this.storage == null) {
                return null;
            }
            return BlockWrapper.this.storage.tempData.get(key);
        }

        @Override
        public void clear() {
            if (BlockWrapper.this.storage == null) {
                return;
            }
            BlockWrapper.this.storage.tempData.clear();
        }

        @Override
        public String[] getKeys() {
            return BlockWrapper.this.storage.tempData.keySet().toArray(new String[BlockWrapper.this.storage.tempData.size()]);
        }
    };
    private final IData storeddata = new IData(){

        @Override
        public void put(String key, Object value) {
            NBTTagCompound compound = this.getNBT();
            if (compound == null) {
                return;
            }
            if (value instanceof Number) {
                compound.func_74780_a(key, ((Number)value).doubleValue());
            } else if (value instanceof String) {
                compound.func_74778_a(key, (String)value);
            }
        }

        @Override
        public Object get(String key) {
            NBTTagCompound compound = this.getNBT();
            if (compound == null) {
                return null;
            }
            if (!compound.func_74764_b(key)) {
                return null;
            }
            NBTBase base = compound.func_74781_a(key);
            if (base instanceof NBTPrimitive) {
                return ((NBTPrimitive)base).func_150286_g();
            }
            return ((NBTTagString)base).func_150285_a_();
        }

        @Override
        public void remove(String key) {
            NBTTagCompound compound = this.getNBT();
            if (compound == null) {
                return;
            }
            compound.func_82580_o(key);
        }

        @Override
        public boolean has(String key) {
            NBTTagCompound compound = this.getNBT();
            if (compound == null) {
                return false;
            }
            return compound.func_74764_b(key);
        }

        @Override
        public void clear() {
            if (BlockWrapper.this.tile == null) {
                return;
            }
            BlockWrapper.this.tile.getTileData().func_74782_a("CustomNPCsData", (NBTBase)new NBTTagCompound());
        }

        private NBTTagCompound getNBT() {
            if (BlockWrapper.this.tile == null) {
                return null;
            }
            NBTTagCompound compound = BlockWrapper.this.tile.getTileData().func_74775_l("CustomNPCsData");
            if (compound.func_82582_d() && !BlockWrapper.this.tile.getTileData().func_74764_b("CustomNPCsData")) {
                BlockWrapper.this.tile.getTileData().func_74782_a("CustomNPCsData", (NBTBase)compound);
            }
            return compound;
        }

        @Override
        public String[] getKeys() {
            NBTTagCompound compound = this.getNBT();
            if (compound == null) {
                return new String[0];
            }
            return compound.func_150296_c().toArray(new String[compound.func_150296_c().size()]);
        }
    };

    protected BlockWrapper(World world, Block block, BlockPos pos) {
        this.world = NpcAPI.Instance().getIWorld((WorldServer)world);
        this.block = block;
        this.pos = pos;
        this.bPos = new BlockPosWrapper(pos);
        this.setTile(world.func_175625_s(pos));
    }

    @Override
    public int getX() {
        return this.pos.func_177958_n();
    }

    @Override
    public int getY() {
        return this.pos.func_177956_o();
    }

    @Override
    public int getZ() {
        return this.pos.func_177952_p();
    }

    @Override
    public IPos getPos() {
        return this.bPos;
    }

    @Override
    public int getMetadata() {
        return this.block.func_176201_c(this.world.getMCWorld().func_180495_p(this.pos));
    }

    @Override
    public void setMetadata(int i) {
        this.world.getMCWorld().func_180501_a(this.pos, this.block.func_176203_a(i), 3);
    }

    @Override
    public void remove() {
        this.world.getMCWorld().func_175698_g(this.pos);
    }

    @Override
    public boolean isRemoved() {
        IBlockState state = this.world.getMCWorld().func_180495_p(this.pos);
        if (state == null) {
            return true;
        }
        return state.func_177230_c() != this.block;
    }

    @Override
    public boolean isAir() {
        return this.block.isAir(this.world.getMCWorld().func_180495_p(this.pos), (IBlockAccess)this.world.getMCWorld(), this.pos);
    }

    @Override
    public BlockWrapper setBlock(String name) {
        Block block = (Block)Block.field_149771_c.func_82594_a((Object)new ResourceLocation(name));
        if (block == null) {
            return this;
        }
        this.world.getMCWorld().func_175656_a(this.pos, block.func_176223_P());
        return new BlockWrapper((World)this.world.getMCWorld(), block, this.pos);
    }

    @Override
    public BlockWrapper setBlock(IBlock block) {
        this.world.getMCWorld().func_175656_a(this.pos, block.getMCBlock().func_176223_P());
        return new BlockWrapper((World)this.world.getMCWorld(), block.getMCBlock(), this.pos);
    }

    @Override
    public boolean isContainer() {
        if (this.tile == null || !(this.tile instanceof IInventory)) {
            return false;
        }
        return ((IInventory)this.tile).func_70302_i_() > 0;
    }

    @Override
    public IContainer getContainer() {
        if (!this.isContainer()) {
            throw new CustomNPCsException("This block is not a container", new Object[0]);
        }
        return NpcAPI.Instance().getIContainer((IInventory)this.tile);
    }

    @Override
    public IData getTempdata() {
        return this.tempdata;
    }

    @Override
    public IData getStoreddata() {
        return this.storeddata;
    }

    @Override
    public String getName() {
        return Block.field_149771_c.func_177774_c((Object)this.block) + "";
    }

    @Override
    public String getDisplayName() {
        if (this.tile == null) {
            return this.getName();
        }
        return this.tile.func_145748_c_().func_150260_c();
    }

    @Override
    public IWorld getWorld() {
        return this.world;
    }

    @Override
    public Block getMCBlock() {
        return this.block;
    }

    @Deprecated
    public static IBlock createNew(World world, BlockPos pos, IBlockState state) {
        Block block = state.func_177230_c();
        String key = state.toString() + pos.toString();
        BlockWrapper b = blockCache.get(key);
        if (b != null) {
            b.setTile(world.func_175625_s(pos));
            return b;
        }
        b = block instanceof BlockScripted ? new BlockScriptedWrapper(world, block, pos) : (block instanceof BlockScriptedDoor ? new BlockScriptedDoorWrapper(world, block, pos) : (block instanceof BlockFluidBase ? new BlockFluidContainerWrapper(world, block, pos) : new BlockWrapper(world, block, pos)));
        blockCache.put(key, b);
        return b;
    }

    public static void clearCache() {
        blockCache.clear();
    }

    @Override
    public boolean hasTileEntity() {
        return this.tile != null;
    }

    protected void setTile(TileEntity tile) {
        this.tile = tile;
        if (tile instanceof TileNpcEntity) {
            this.storage = (TileNpcEntity)tile;
        }
    }

    @Override
    public INbt getTileEntityNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        this.tile.func_189515_b(compound);
        return NpcAPI.Instance().getINbt(compound);
    }

    @Override
    public void setTileEntityNBT(INbt nbt) {
        this.tile.func_145839_a(nbt.getMCNBT());
        this.tile.func_70296_d();
        IBlockState state = this.world.getMCWorld().func_180495_p(this.pos);
        this.world.getMCWorld().func_184138_a(this.pos, state, state, 3);
    }

    @Override
    public TileEntity getMCTileEntity() {
        return this.tile;
    }

    @Override
    public IBlockState getMCBlockState() {
        return this.world.getMCWorld().func_180495_p(this.pos);
    }

    @Override
    public void blockEvent(int type, int data) {
        this.world.getMCWorld().func_175641_c(this.pos, this.getMCBlock(), type, data);
    }

    @Override
    public void interact(int side) {
        FakePlayer player = EntityNPCInterface.GenericPlayer;
        WorldServer w = this.world.getMCWorld();
        player.func_70029_a((World)w);
        player.func_70107_b((double)this.pos.func_177958_n(), (double)this.pos.func_177956_o(), (double)this.pos.func_177952_p());
        this.block.func_180639_a((World)w, this.pos, w.func_180495_p(this.pos), (EntityPlayer)EntityNPCInterface.CommandPlayer, EnumHand.MAIN_HAND, EnumFacing.func_82600_a((int)side), 0.0f, 0.0f, 0.0f);
    }
}

