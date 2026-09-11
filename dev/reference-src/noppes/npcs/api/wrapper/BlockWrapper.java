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
                compound.setDouble(key, ((Number)value).doubleValue());
            } else if (value instanceof String) {
                compound.setString(key, (String)value);
            }
        }

        @Override
        public Object get(String key) {
            NBTTagCompound compound = this.getNBT();
            if (compound == null) {
                return null;
            }
            if (!compound.hasKey(key)) {
                return null;
            }
            NBTBase base = compound.getTag(key);
            if (base instanceof NBTPrimitive) {
                return ((NBTPrimitive)base).getDouble();
            }
            return ((NBTTagString)base).getString();
        }

        @Override
        public void remove(String key) {
            NBTTagCompound compound = this.getNBT();
            if (compound == null) {
                return;
            }
            compound.removeTag(key);
        }

        @Override
        public boolean has(String key) {
            NBTTagCompound compound = this.getNBT();
            if (compound == null) {
                return false;
            }
            return compound.hasKey(key);
        }

        @Override
        public void clear() {
            if (BlockWrapper.this.tile == null) {
                return;
            }
            BlockWrapper.this.tile.getTileData().setTag("CustomNPCsData", (NBTBase)new NBTTagCompound());
        }

        private NBTTagCompound getNBT() {
            if (BlockWrapper.this.tile == null) {
                return null;
            }
            NBTTagCompound compound = BlockWrapper.this.tile.getTileData().getCompoundTag("CustomNPCsData");
            if (compound.hasNoTags() && !BlockWrapper.this.tile.getTileData().hasKey("CustomNPCsData")) {
                BlockWrapper.this.tile.getTileData().setTag("CustomNPCsData", (NBTBase)compound);
            }
            return compound;
        }

        @Override
        public String[] getKeys() {
            NBTTagCompound compound = this.getNBT();
            if (compound == null) {
                return new String[0];
            }
            return compound.getKeySet().toArray(new String[compound.getKeySet().size()]);
        }
    };

    protected BlockWrapper(World world, Block block, BlockPos pos) {
        this.world = NpcAPI.Instance().getIWorld((WorldServer)world);
        this.block = block;
        this.pos = pos;
        this.bPos = new BlockPosWrapper(pos);
        this.setTile(world.getTileEntity(pos));
    }

    @Override
    public int getX() {
        return this.pos.getX();
    }

    @Override
    public int getY() {
        return this.pos.getY();
    }

    @Override
    public int getZ() {
        return this.pos.getZ();
    }

    @Override
    public IPos getPos() {
        return this.bPos;
    }

    @Override
    public int getMetadata() {
        return this.block.getMetaFromState(this.world.getMCWorld().getBlockState(this.pos));
    }

    @Override
    public void setMetadata(int i) {
        this.world.getMCWorld().setBlockState(this.pos, this.block.getStateFromMeta(i), 3);
    }

    @Override
    public void remove() {
        this.world.getMCWorld().setBlockToAir(this.pos);
    }

    @Override
    public boolean isRemoved() {
        IBlockState state = this.world.getMCWorld().getBlockState(this.pos);
        if (state == null) {
            return true;
        }
        return state.getBlock() != this.block;
    }

    @Override
    public boolean isAir() {
        return this.block.isAir(this.world.getMCWorld().getBlockState(this.pos), (IBlockAccess)this.world.getMCWorld(), this.pos);
    }

    @Override
    public BlockWrapper setBlock(String name) {
        Block block = (Block)Block.REGISTRY.getObject((Object)new ResourceLocation(name));
        if (block == null) {
            return this;
        }
        this.world.getMCWorld().setBlockState(this.pos, block.getDefaultState());
        return new BlockWrapper((World)this.world.getMCWorld(), block, this.pos);
    }

    @Override
    public BlockWrapper setBlock(IBlock block) {
        this.world.getMCWorld().setBlockState(this.pos, block.getMCBlock().getDefaultState());
        return new BlockWrapper((World)this.world.getMCWorld(), block.getMCBlock(), this.pos);
    }

    @Override
    public boolean isContainer() {
        if (this.tile == null || !(this.tile instanceof IInventory)) {
            return false;
        }
        return ((IInventory)this.tile).getSizeInventory() > 0;
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
        return Block.REGISTRY.getNameForObject((Object)this.block) + "";
    }

    @Override
    public String getDisplayName() {
        if (this.tile == null) {
            return this.getName();
        }
        return this.tile.getDisplayName().getUnformattedText();
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
        Block block = state.getBlock();
        String key = state.toString() + pos.toString();
        BlockWrapper b = blockCache.get(key);
        if (b != null) {
            b.setTile(world.getTileEntity(pos));
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
        this.tile.writeToNBT(compound);
        return NpcAPI.Instance().getINbt(compound);
    }

    @Override
    public void setTileEntityNBT(INbt nbt) {
        this.tile.readFromNBT(nbt.getMCNBT());
        this.tile.markDirty();
        IBlockState state = this.world.getMCWorld().getBlockState(this.pos);
        this.world.getMCWorld().notifyBlockUpdate(this.pos, state, state, 3);
    }

    @Override
    public TileEntity getMCTileEntity() {
        return this.tile;
    }

    @Override
    public IBlockState getMCBlockState() {
        return this.world.getMCWorld().getBlockState(this.pos);
    }

    @Override
    public void blockEvent(int type, int data) {
        this.world.getMCWorld().addBlockEvent(this.pos, this.getMCBlock(), type, data);
    }

    @Override
    public void interact(int side) {
        FakePlayer player = EntityNPCInterface.GenericPlayer;
        WorldServer w = this.world.getMCWorld();
        player.setWorld((World)w);
        player.setPosition((double)this.pos.getX(), (double)this.pos.getY(), (double)this.pos.getZ());
        this.block.onBlockActivated((World)w, this.pos, w.getBlockState(this.pos), (EntityPlayer)EntityNPCInterface.CommandPlayer, EnumHand.MAIN_HAND, EnumFacing.getFront((int)side), 0.0f, 0.0f, 0.0f);
    }
}

