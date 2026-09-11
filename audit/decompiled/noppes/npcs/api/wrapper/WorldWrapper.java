/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.effect.EntityLightningBolt
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.monster.EntityMob
 *  net.minecraft.entity.passive.EntityAnimal
 *  net.minecraft.entity.passive.EntityVillager
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTPrimitive
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraft.util.EntitySelectors
 *  net.minecraft.util.EnumParticleTypes
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.world.World
 *  net.minecraft.world.WorldServer
 */
package noppes.npcs.api.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import noppes.npcs.Server;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IDimension;
import noppes.npcs.api.INbt;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IScoreboard;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.api.wrapper.DimensionWrapper;
import noppes.npcs.api.wrapper.ScoreboardWrapper;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityProjectile;

public class WorldWrapper
implements IWorld {
    public static Map<String, Object> tempData = new HashMap<String, Object>();
    public WorldServer world;
    public IDimension dimension;
    private IData tempdata = new IData(){

        @Override
        public void put(String key, Object value) {
            tempData.put(key, value);
        }

        @Override
        public Object get(String key) {
            return tempData.get(key);
        }

        @Override
        public void remove(String key) {
            tempData.remove(key);
        }

        @Override
        public boolean has(String key) {
            return tempData.containsKey(key);
        }

        @Override
        public void clear() {
            tempData.clear();
        }

        @Override
        public String[] getKeys() {
            return tempData.keySet().toArray(new String[tempData.size()]);
        }
    };
    private IData storeddata = new IData(){

        @Override
        public void put(String key, Object value) {
            NBTTagCompound compound = ScriptController.Instance.compound;
            if (value instanceof Number) {
                compound.func_74780_a(key, ((Number)value).doubleValue());
            } else if (value instanceof String) {
                compound.func_74778_a(key, (String)value);
            }
            ScriptController.Instance.shouldSave = true;
        }

        @Override
        public Object get(String key) {
            NBTTagCompound compound = ScriptController.Instance.compound;
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
            ScriptController.Instance.compound.func_82580_o(key);
            ScriptController.Instance.shouldSave = true;
        }

        @Override
        public boolean has(String key) {
            return ScriptController.Instance.compound.func_74764_b(key);
        }

        @Override
        public void clear() {
            ScriptController.Instance.compound = new NBTTagCompound();
            ScriptController.Instance.shouldSave = true;
        }

        @Override
        public String[] getKeys() {
            return ScriptController.Instance.compound.func_150296_c().toArray(new String[ScriptController.Instance.compound.func_150296_c().size()]);
        }
    };

    private WorldWrapper(World world) {
        this.world = (WorldServer)world;
        this.dimension = new DimensionWrapper(world.field_73011_w.getDimension(), world.field_73011_w.func_186058_p());
    }

    @Override
    public WorldServer getMCWorld() {
        return this.world;
    }

    @Override
    public IEntity[] getNearbyEntities(int x, int y, int z, int range, int type) {
        return this.getNearbyEntities(new BlockPosWrapper(new BlockPos(x, y, z)), range, type);
    }

    @Override
    public IEntity[] getNearbyEntities(IPos pos, int range, int type) {
        AxisAlignedBB bb = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).func_186670_a(pos.getMCBlockPos()).func_72314_b((double)range, (double)range, (double)range);
        List entities = this.world.func_72872_a(this.getClassForType(type), bb);
        ArrayList<IEntity> list = new ArrayList<IEntity>();
        for (Entity living : entities) {
            list.add(NpcAPI.Instance().getIEntity(living));
        }
        return list.toArray(new IEntity[list.size()]);
    }

    @Override
    public IEntity[] getAllEntities(int type) {
        List entities = this.world.func_175644_a(this.getClassForType(type), EntitySelectors.field_180132_d);
        ArrayList<IEntity> list = new ArrayList<IEntity>();
        for (Entity living : entities) {
            list.add(NpcAPI.Instance().getIEntity(living));
        }
        return list.toArray(new IEntity[list.size()]);
    }

    @Override
    public IEntity getClosestEntity(int x, int y, int z, int range, int type) {
        return this.getClosestEntity(new BlockPosWrapper(new BlockPos(x, y, z)), range, type);
    }

    @Override
    public IEntity getClosestEntity(IPos pos, int range, int type) {
        AxisAlignedBB bb = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).func_186670_a(pos.getMCBlockPos()).func_72314_b((double)range, (double)range, (double)range);
        List entities = this.world.func_72872_a(this.getClassForType(type), bb);
        double distance = range * range * range;
        Entity entity = null;
        for (Entity e : entities) {
            double r = pos.getMCBlockPos().func_177951_i((Vec3i)e.func_180425_c());
            if (entity == null) {
                distance = r;
                entity = e;
                continue;
            }
            if (!(r < distance)) continue;
            distance = r;
            entity = e;
        }
        return NpcAPI.Instance().getIEntity(entity);
    }

    @Override
    public IEntity getEntity(String uuid) {
        try {
            UUID id = UUID.fromString(uuid);
            Entity e = this.world.func_175733_a(id);
            if (e == null) {
                e = this.world.func_152378_a(id);
            }
            if (e == null) {
                return null;
            }
            return NpcAPI.Instance().getIEntity(e);
        }
        catch (Exception e) {
            throw new CustomNPCsException("Given uuid was invalid " + uuid, new Object[0]);
        }
    }

    @Override
    public IEntity createEntityFromNBT(INbt nbt) {
        Entity entity = EntityList.func_75615_a((NBTTagCompound)nbt.getMCNBT(), (World)this.world);
        if (entity == null) {
            throw new CustomNPCsException("Failed to create an entity from given NBT", new Object[0]);
        }
        return NpcAPI.Instance().getIEntity(entity);
    }

    @Override
    public IEntity createEntity(String id) {
        ResourceLocation resource = new ResourceLocation(id);
        Entity entity = EntityList.func_188429_b((ResourceLocation)resource, (World)this.world);
        if (entity == null) {
            throw new CustomNPCsException("Failed to create an entity from given id: " + id, new Object[0]);
        }
        return NpcAPI.Instance().getIEntity(entity);
    }

    @Override
    public IPlayer getPlayer(String name) {
        EntityPlayer player = this.world.func_72924_a(name);
        if (player == null) {
            return null;
        }
        return (IPlayer)NpcAPI.Instance().getIEntity((Entity)player);
    }

    private Class getClassForType(int type) {
        if (type == -1) {
            return Entity.class;
        }
        if (type == 5) {
            return EntityLivingBase.class;
        }
        if (type == 1) {
            return EntityPlayer.class;
        }
        if (type == 4) {
            return EntityAnimal.class;
        }
        if (type == 3) {
            return EntityMob.class;
        }
        if (type == 2) {
            return EntityNPCInterface.class;
        }
        if (type == 6) {
            return EntityItem.class;
        }
        if (type == 7) {
            return EntityProjectile.class;
        }
        if (type == 3) {
            return EntityMob.class;
        }
        if (type == 9) {
            return EntityVillager.class;
        }
        return Entity.class;
    }

    @Override
    public long getTime() {
        return this.world.func_72820_D();
    }

    @Override
    public void setTime(long time) {
        this.world.func_72877_b(time);
    }

    @Override
    public long getTotalTime() {
        return this.world.func_82737_E();
    }

    @Override
    public IBlock getBlock(int x, int y, int z) {
        return NpcAPI.Instance().getIBlock((World)this.world, new BlockPos(x, y, z));
    }

    public boolean isChunkLoaded(int x, int z) {
        return this.world.func_72863_F().func_73149_a(x >> 4, z >> 4);
    }

    @Override
    public void setBlock(int x, int y, int z, String name, int meta) {
        Block block = Block.func_149684_b((String)name);
        if (block == null) {
            throw new CustomNPCsException("There is no such block: %s", new Object[0]);
        }
        this.world.func_175656_a(new BlockPos(x, y, z), block.func_176203_a(meta));
    }

    @Override
    public void removeBlock(int x, int y, int z) {
        this.world.func_175698_g(new BlockPos(x, y, z));
    }

    @Override
    public float getLightValue(int x, int y, int z) {
        return (float)this.world.func_175699_k(new BlockPos(x, y, z)) / 16.0f;
    }

    @Override
    public IBlock getSpawnPoint() {
        BlockPos pos = this.world.func_180504_m();
        if (pos == null) {
            pos = this.world.func_175694_M();
        }
        return NpcAPI.Instance().getIBlock((World)this.world, pos);
    }

    @Override
    public void setSpawnPoint(IBlock block) {
        this.world.func_175652_B(new BlockPos(block.getX(), block.getY(), block.getZ()));
    }

    @Override
    public boolean isDay() {
        return this.world.func_72820_D() % 24000L < 12000L;
    }

    @Override
    public boolean isRaining() {
        return this.world.func_72912_H().func_76059_o();
    }

    @Override
    public void setRaining(boolean bo) {
        this.world.func_72912_H().func_76084_b(bo);
    }

    @Override
    public void thunderStrike(double x, double y, double z) {
        this.world.func_72942_c((Entity)new EntityLightningBolt((World)this.world, x, y, z, false));
    }

    @Override
    public void spawnParticle(String particle, double x, double y, double z, double dx, double dy, double dz, double speed, int count) {
        EnumParticleTypes particleType = null;
        for (EnumParticleTypes enumParticle : EnumParticleTypes.values()) {
            if (enumParticle.func_179345_d() > 0) {
                if (!particle.startsWith(enumParticle.func_179346_b())) continue;
                particleType = enumParticle;
                break;
            }
            if (!particle.equals(enumParticle.func_179346_b())) continue;
            particleType = enumParticle;
            break;
        }
        if (particleType != null) {
            this.world.func_175739_a(particleType, x, y, z, count, dx, dy, dz, speed, new int[0]);
        }
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
    public IItemStack createItem(String name, int damage, int size) {
        Item item = (Item)Item.field_150901_e.func_82594_a((Object)new ResourceLocation(name));
        if (item == null) {
            throw new CustomNPCsException("Unknown item id: " + name, new Object[0]);
        }
        return NpcAPI.Instance().getIItemStack(new ItemStack(item, size, damage));
    }

    @Override
    public IItemStack createItemFromNbt(INbt nbt) {
        ItemStack item = new ItemStack(nbt.getMCNBT());
        if (item.func_190926_b()) {
            throw new CustomNPCsException("Failed to create an item from given NBT", new Object[0]);
        }
        return NpcAPI.Instance().getIItemStack(item);
    }

    @Override
    public void explode(double x, double y, double z, float range, boolean fire, boolean grief) {
        this.world.func_72885_a(null, x, y, z, range, fire, grief);
    }

    @Override
    public IPlayer[] getAllPlayers() {
        List list = this.world.func_73046_m().func_184103_al().func_181057_v();
        IPlayer[] arr = new IPlayer[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            arr[i] = (IPlayer)NpcAPI.Instance().getIEntity((Entity)list.get(i));
        }
        return arr;
    }

    @Override
    public String getBiomeName(int x, int z) {
        return this.world.getBiomeForCoordsBody((BlockPos)new BlockPos((int)x, (int)0, (int)z)).field_76791_y;
    }

    @Override
    public IEntity spawnClone(double x, double y, double z, int tab, String name) {
        return NpcAPI.Instance().getClones().spawn(x, y, z, tab, name, this);
    }

    @Override
    public void spawnEntity(IEntity entity) {
        Object e = entity.getMCEntity();
        if (this.world.func_175733_a(e.func_110124_au()) != null) {
            throw new CustomNPCsException("Entity with this UUID already exists", new Object[0]);
        }
        e.func_70107_b(((Entity)e).field_70165_t, ((Entity)e).field_70163_u, ((Entity)e).field_70161_v);
        this.world.func_72838_d(e);
    }

    @Override
    public IEntity getClone(int tab, String name) {
        return NpcAPI.Instance().getClones().get(tab, name, this);
    }

    @Override
    public IScoreboard getScoreboard() {
        return new ScoreboardWrapper(this.world.func_73046_m());
    }

    @Override
    public void broadcast(String message) {
        this.world.func_73046_m().func_184103_al().func_148539_a((ITextComponent)new TextComponentString(message));
    }

    @Override
    public int getRedstonePower(int x, int y, int z) {
        return this.world.func_175676_y(new BlockPos(x, y, z));
    }

    @Deprecated
    public static WorldWrapper createNew(WorldServer world) {
        return new WorldWrapper((World)world);
    }

    @Override
    public IDimension getDimension() {
        return this.dimension;
    }

    @Override
    public String getName() {
        return this.world.func_72912_H().func_76065_j();
    }

    @Override
    public BlockPos getMCBlockPos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    @Override
    public void playSoundAt(IPos pos, String sound, float volume, float pitch) {
        Server.sendRangedData((World)this.world, pos.getMCBlockPos(), 16, EnumPacketClient.PLAY_SOUND, sound, pos.getX(), pos.getY(), pos.getZ(), Float.valueOf(volume), Float.valueOf(pitch));
    }
}

