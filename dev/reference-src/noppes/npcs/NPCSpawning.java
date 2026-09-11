/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  net.minecraft.block.Block
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.entity.EntityLiving$SpawnPlacementType
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.server.management.PlayerChunkMapEntry
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.ChunkPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraft.world.WorldServer
 *  net.minecraft.world.biome.Biome
 *  net.minecraft.world.chunk.Chunk
 *  net.minecraftforge.event.ForgeEventFactory
 *  net.minecraftforge.fml.common.eventhandler.Event$Result
 */
package noppes.npcs;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.controllers.SpawnController;
import noppes.npcs.controllers.data.SpawnData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class NPCSpawning {
    private static Set<ChunkPos> eligibleChunksForSpawning = Sets.newHashSet();

    public static void findChunksForSpawning(WorldServer world) {
        if (SpawnController.instance.data.isEmpty() || world.getWorldInfo().getWorldTotalTime() % 400L != 0L) {
            return;
        }
        eligibleChunksForSpawning.clear();
        for (int i = 0; i < world.playerEntities.size(); ++i) {
            EntityPlayer entityplayer = (EntityPlayer)world.playerEntities.get(i);
            if (entityplayer.isSpectator()) continue;
            int j = MathHelper.floor((double)(entityplayer.posX / 16.0));
            int k = MathHelper.floor((double)(entityplayer.posZ / 16.0));
            int size = 7;
            for (int x = -size; x <= size; ++x) {
                for (int z = -size; z <= size; ++z) {
                    PlayerChunkMapEntry playerinstance;
                    ChunkPos chunkcoordintpair = new ChunkPos(x + j, z + k);
                    if (eligibleChunksForSpawning.contains(chunkcoordintpair) || !world.getWorldBorder().contains(chunkcoordintpair) || (playerinstance = world.getPlayerChunkMap().getEntry(chunkcoordintpair.x, chunkcoordintpair.z)) == null || !playerinstance.isSentToPlayers()) continue;
                    eligibleChunksForSpawning.add(chunkcoordintpair);
                }
            }
        }
        if (NPCSpawning.countNPCs((World)world) > eligibleChunksForSpawning.size() / 16) {
            return;
        }
        ArrayList<ChunkPos> tmp = new ArrayList<ChunkPos>(eligibleChunksForSpawning);
        Collections.shuffle(tmp);
        for (ChunkPos chunkcoordintpair1 : tmp) {
            BlockPos chunkposition = NPCSpawning.getChunk((World)world, chunkcoordintpair1.x, chunkcoordintpair1.z);
            int j1 = chunkposition.getX();
            int k1 = chunkposition.getY();
            int l1 = chunkposition.getZ();
            for (int i = 0; i < 3; ++i) {
                BlockPos pos;
                int x = j1;
                int y = k1;
                int z = l1;
                String name = world.getBiomeForCoordsBody((BlockPos)pos).biomeName;
                int b1 = 6;
                pos = new BlockPos(x += world.rand.nextInt(b1) - world.rand.nextInt(b1), y += world.rand.nextInt(1) - world.rand.nextInt(1), z += world.rand.nextInt(b1) - world.rand.nextInt(b1));
                IBlockState state = world.getBlockState(pos);
                SpawnData data = SpawnController.instance.getRandomSpawnData(name, state.getMaterial() == Material.AIR);
                if (data == null || !NPCSpawning.canCreatureTypeSpawnAtLocation(data, (World)world, pos) || world.getClosestPlayer((double)x, (double)y, (double)z, 24.0, false) != null) continue;
                NPCSpawning.spawnData(data, (World)world, pos);
            }
        }
    }

    public static int countNPCs(World world) {
        int count = 0;
        List list = world.loadedEntityList;
        for (Entity entity : list) {
            if (!(entity instanceof EntityNPCInterface)) continue;
            ++count;
        }
        return count;
    }

    protected static BlockPos getChunk(World world, int x, int z) {
        Chunk chunk = world.getChunkFromChunkCoords(x, z);
        int k = x * 16 + world.rand.nextInt(16);
        int l = z * 16 + world.rand.nextInt(16);
        int i1 = MathHelper.roundUp((int)(chunk.getHeight(new BlockPos(k, 0, l)) + 1), (int)16);
        int j1 = world.rand.nextInt(i1 > 0 ? i1 : chunk.getTopFilledSegment() + 16 - 1);
        return new BlockPos(k, j1, l);
    }

    public static void performWorldGenSpawning(World world, int x, int z, Random rand) {
        Biome biome = world.getBiomeForCoordsBody(new BlockPos(x + 8, 0, z + 8));
        block0: while (rand.nextFloat() < biome.getSpawningChance()) {
            SpawnData data = SpawnController.instance.getRandomSpawnData(biome.biomeName, true);
            if (data == null) continue;
            int size = 16;
            int j1 = x + rand.nextInt(size);
            int k1 = z + rand.nextInt(size);
            int l1 = j1;
            int i2 = k1;
            for (int k2 = 0; k2 < 4; ++k2) {
                BlockPos pos = world.getTopSolidOrLiquidBlock(new BlockPos(j1, 0, k1));
                if (!NPCSpawning.canCreatureTypeSpawnAtLocation(data, world, pos)) {
                    j1 += rand.nextInt(5) - rand.nextInt(5);
                    k1 += rand.nextInt(5) - rand.nextInt(5);
                    while (j1 < x || j1 >= x + size || k1 < z || k1 >= z + size) {
                        j1 = l1 + rand.nextInt(5) - rand.nextInt(5);
                        k1 = i2 + rand.nextInt(5) - rand.nextInt(5);
                    }
                    continue;
                }
                if (NPCSpawning.spawnData(data, world, pos)) continue block0;
            }
        }
    }

    private static boolean spawnData(SpawnData data, World world, BlockPos pos) {
        EntityLiving entityliving;
        try {
            Entity entity = EntityList.createEntityFromNBT((NBTTagCompound)data.compound1, (World)world);
            if (entity == null || !(entity instanceof EntityLiving)) {
                return false;
            }
            entityliving = (EntityLiving)entity;
            if (entity instanceof EntityCustomNpc) {
                EntityCustomNpc npc = (EntityCustomNpc)entity;
                npc.stats.spawnCycle = 4;
                npc.stats.respawnTime = 0;
                npc.ais.returnToStart = false;
                npc.ais.setStartPos(pos);
            }
            entity.setLocationAndAngles((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, world.rand.nextFloat() * 360.0f, 0.0f);
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
        Event.Result canSpawn = ForgeEventFactory.canEntitySpawn((EntityLiving)entityliving, (World)world, (float)((float)pos.getX() + 0.5f), (float)pos.getY(), (float)((float)pos.getZ() + 0.5f));
        if (canSpawn == Event.Result.DENY || canSpawn == Event.Result.DEFAULT && !entityliving.getCanSpawnHere()) {
            return false;
        }
        world.spawnEntity((Entity)entityliving);
        return true;
    }

    public static boolean canCreatureTypeSpawnAtLocation(SpawnData data, World world, BlockPos pos) {
        if (!world.getWorldBorder().contains(pos)) {
            return false;
        }
        if (data.type == 1 && world.getLight(pos) > 8 || data.type == 2 && world.getLight(pos) <= 8) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (data.liquid) {
            return state.getMaterial().isLiquid() && world.getBlockState(pos.down()).getMaterial().isLiquid() && !world.getBlockState(pos.up()).isNormalCube();
        }
        BlockPos blockpos1 = pos.down();
        IBlockState state1 = world.getBlockState(blockpos1);
        Block block1 = state1.getBlock();
        if (!state1.isSideSolid((IBlockAccess)world, blockpos1, EnumFacing.UP)) {
            return false;
        }
        boolean flag = block1 != Blocks.BEDROCK && block1 != Blocks.BARRIER;
        BlockPos down = blockpos1.down();
        return (flag |= world.getBlockState(down).getBlock().canCreatureSpawn(world.getBlockState(down), (IBlockAccess)world, down, EntityLiving.SpawnPlacementType.ON_GROUND)) && !state.isNormalCube() && !state.getMaterial().isLiquid() && !world.getBlockState(pos.up()).isNormalCube();
    }
}

