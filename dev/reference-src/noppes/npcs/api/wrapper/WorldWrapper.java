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
                compound.setDouble(key, ((Number)value).doubleValue());
            } else if (value instanceof String) {
                compound.setString(key, (String)value);
            }
            ScriptController.Instance.shouldSave = true;
        }

        @Override
        public Object get(String key) {
            NBTTagCompound compound = ScriptController.Instance.compound;
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
            ScriptController.Instance.compound.removeTag(key);
            ScriptController.Instance.shouldSave = true;
        }

        @Override
        public boolean has(String key) {
            return ScriptController.Instance.compound.hasKey(key);
        }

        @Override
        public void clear() {
            ScriptController.Instance.compound = new NBTTagCompound();
            ScriptController.Instance.shouldSave = true;
        }

        @Override
        public String[] getKeys() {
            return ScriptController.Instance.compound.getKeySet().toArray(new String[ScriptController.Instance.compound.getKeySet().size()]);
        }
    };

    private WorldWrapper(World world) {
        this.world = (WorldServer)world;
        this.dimension = new DimensionWrapper(world.provider.getDimension(), world.provider.getDimensionType());
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
        AxisAlignedBB bb = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).offset(pos.getMCBlockPos()).grow((double)range, (double)range, (double)range);
        List entities = this.world.getEntitiesWithinAABB(this.getClassForType(type), bb);
        ArrayList<IEntity> list = new ArrayList<IEntity>();
        for (Entity living : entities) {
            list.add(NpcAPI.Instance().getIEntity(living));
        }
        return list.toArray(new IEntity[list.size()]);
    }

    @Override
    public IEntity[] getAllEntities(int type) {
        List entities = this.world.getEntities(this.getClassForType(type), EntitySelectors.NOT_SPECTATING);
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
        AxisAlignedBB bb = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).offset(pos.getMCBlockPos()).grow((double)range, (double)range, (double)range);
        List entities = this.world.getEntitiesWithinAABB(this.getClassForType(type), bb);
        double distance = range * range * range;
        Entity entity = null;
        for (Entity e : entities) {
            double r = pos.getMCBlockPos().distanceSq((Vec3i)e.getPosition());
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
            Entity e = this.world.getEntityFromUuid(id);
            if (e == null) {
                e = this.world.getPlayerEntityByUUID(id);
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
        Entity entity = EntityList.createEntityFromNBT((NBTTagCompound)nbt.getMCNBT(), (World)this.world);
        if (entity == null) {
            throw new CustomNPCsException("Failed to create an entity from given NBT", new Object[0]);
        }
        return NpcAPI.Instance().getIEntity(entity);
    }

    @Override
    public IEntity createEntity(String id) {
        ResourceLocation resource = new ResourceLocation(id);
        Entity entity = EntityList.createEntityByIDFromName((ResourceLocation)resource, (World)this.world);
        if (entity == null) {
            throw new CustomNPCsException("Failed to create an entity from given id: " + id, new Object[0]);
        }
        return NpcAPI.Instance().getIEntity(entity);
    }

    @Override
    public IPlayer getPlayer(String name) {
        EntityPlayer player = this.world.getPlayerEntityByName(name);
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
        return this.world.getWorldTime();
    }

    @Override
    public void setTime(long time) {
        this.world.setWorldTime(time);
    }

    @Override
    public long getTotalTime() {
        return this.world.getTotalWorldTime();
    }

    @Override
    public IBlock getBlock(int x, int y, int z) {
        return NpcAPI.Instance().getIBlock((World)this.world, new BlockPos(x, y, z));
    }

    public boolean isChunkLoaded(int x, int z) {
        return this.world.getChunkProvider().chunkExists(x >> 4, z >> 4);
    }

    @Override
    public void setBlock(int x, int y, int z, String name, int meta) {
        Block block = Block.getBlockFromName((String)name);
        if (block == null) {
            throw new CustomNPCsException("There is no such block: %s", new Object[0]);
        }
        this.world.setBlockState(new BlockPos(x, y, z), block.getStateFromMeta(meta));
    }

    @Override
    public void removeBlock(int x, int y, int z) {
        this.world.setBlockToAir(new BlockPos(x, y, z));
    }

    @Override
    public float getLightValue(int x, int y, int z) {
        return (float)this.world.getLight(new BlockPos(x, y, z)) / 16.0f;
    }

    @Override
    public IBlock getSpawnPoint() {
        BlockPos pos = this.world.getSpawnCoordinate();
        if (pos == null) {
            pos = this.world.getSpawnPoint();
        }
        return NpcAPI.Instance().getIBlock((World)this.world, pos);
    }

    @Override
    public void setSpawnPoint(IBlock block) {
        this.world.setSpawnPoint(new BlockPos(block.getX(), block.getY(), block.getZ()));
    }

    @Override
    public boolean isDay() {
        return this.world.getWorldTime() % 24000L < 12000L;
    }

    @Override
    public boolean isRaining() {
        return this.world.getWorldInfo().isRaining();
    }

    @Override
    public void setRaining(boolean bo) {
        this.world.getWorldInfo().setRaining(bo);
    }

    @Override
    public void thunderStrike(double x, double y, double z) {
        this.world.addWeatherEffect((Entity)new EntityLightningBolt((World)this.world, x, y, z, false));
    }

    @Override
    public void spawnParticle(String particle, double x, double y, double z, double dx, double dy, double dz, double speed, int count) {
        EnumParticleTypes particleType = null;
        for (EnumParticleTypes enumParticle : EnumParticleTypes.values()) {
            if (enumParticle.getArgumentCount() > 0) {
                if (!particle.startsWith(enumParticle.getParticleName())) continue;
                particleType = enumParticle;
                break;
            }
            if (!particle.equals(enumParticle.getParticleName())) continue;
            particleType = enumParticle;
            break;
        }
        if (particleType != null) {
            this.world.spawnParticle(particleType, x, y, z, count, dx, dy, dz, speed, new int[0]);
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
        Item item = (Item)Item.REGISTRY.getObject((Object)new ResourceLocation(name));
        if (item == null) {
            throw new CustomNPCsException("Unknown item id: " + name, new Object[0]);
        }
        return NpcAPI.Instance().getIItemStack(new ItemStack(item, size, damage));
    }

    @Override
    public IItemStack createItemFromNbt(INbt nbt) {
        ItemStack item = new ItemStack(nbt.getMCNBT());
        if (item.isEmpty()) {
            throw new CustomNPCsException("Failed to create an item from given NBT", new Object[0]);
        }
        return NpcAPI.Instance().getIItemStack(item);
    }

    @Override
    public void explode(double x, double y, double z, float range, boolean fire, boolean grief) {
        this.world.newExplosion(null, x, y, z, range, fire, grief);
    }

    @Override
    public IPlayer[] getAllPlayers() {
        List list = this.world.getMinecraftServer().getPlayerList().getPlayers();
        IPlayer[] arr = new IPlayer[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            arr[i] = (IPlayer)NpcAPI.Instance().getIEntity((Entity)list.get(i));
        }
        return arr;
    }

    @Override
    public String getBiomeName(int x, int z) {
        return this.world.getBiomeForCoordsBody((BlockPos)new BlockPos((int)x, (int)0, (int)z)).biomeName;
    }

    @Override
    public IEntity spawnClone(double x, double y, double z, int tab, String name) {
        return NpcAPI.Instance().getClones().spawn(x, y, z, tab, name, this);
    }

    @Override
    public void spawnEntity(IEntity entity) {
        Object e = entity.getMCEntity();
        if (this.world.getEntityFromUuid(e.getUniqueID()) != null) {
            throw new CustomNPCsException("Entity with this UUID already exists", new Object[0]);
        }
        e.setPosition(((Entity)e).posX, ((Entity)e).posY, ((Entity)e).posZ);
        this.world.spawnEntity(e);
    }

    @Override
    public IEntity getClone(int tab, String name) {
        return NpcAPI.Instance().getClones().get(tab, name, this);
    }

    @Override
    public IScoreboard getScoreboard() {
        return new ScoreboardWrapper(this.world.getMinecraftServer());
    }

    @Override
    public void broadcast(String message) {
        this.world.getMinecraftServer().getPlayerList().sendMessage((ITextComponent)new TextComponentString(message));
    }

    @Override
    public int getRedstonePower(int x, int y, int z) {
        return this.world.getStrongPower(new BlockPos(x, y, z));
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
        return this.world.getWorldInfo().getWorldName();
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

