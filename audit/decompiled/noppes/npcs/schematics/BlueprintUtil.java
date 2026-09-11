/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.nbt.CompressedStreamTools
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraft.nbt.NBTUtil
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.common.Loader
 */
package noppes.npcs.schematics;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import noppes.npcs.schematics.Blueprint;

public class BlueprintUtil {
    public static Blueprint createBlueprint(World world, BlockPos pos, short sizeX, short sizeY, short sizeZ) {
        return BlueprintUtil.createBlueprint(world, pos, sizeX, sizeY, sizeZ, null, new String[0]);
    }

    public static Blueprint createBlueprint(World world, BlockPos pos, short sizeX, short sizeY, short sizeZ, String name, String ... architects) {
        ArrayList<IBlockState> pallete = new ArrayList<IBlockState>();
        short[][][] structure = new short[sizeY][sizeZ][sizeX];
        ArrayList<NBTTagCompound> tileEntities = new ArrayList<NBTTagCompound>();
        ArrayList<String> requiredMods = new ArrayList<String>();
        for (short y = 0; y < sizeY; y = (short)(y + 1)) {
            for (short z = 0; z < sizeZ; z = (short)(z + 1)) {
                for (short x = 0; x < sizeX; x = (short)(x + 1)) {
                    TileEntity te;
                    IBlockState state = world.func_180495_p(pos.func_177982_a((int)x, (int)y, (int)z));
                    String modName = state.func_177230_c().getRegistryName().func_110624_b();
                    if (!requiredMods.contains(modName)) {
                        requiredMods.add(modName);
                    }
                    if ((te = world.func_175625_s(pos.func_177982_a((int)x, (int)y, (int)z))) != null) {
                        NBTTagCompound teTag = te.serializeNBT();
                        teTag.func_74777_a("x", x);
                        teTag.func_74777_a("y", y);
                        teTag.func_74777_a("z", z);
                        tileEntities.add(teTag);
                    }
                    if (!pallete.contains(state)) {
                        pallete.add(state);
                    }
                    structure[y][z][x] = (short)pallete.indexOf(state);
                }
            }
        }
        IBlockState[] states = new IBlockState[pallete.size()];
        states = pallete.toArray(states);
        NBTTagCompound[] tes = new NBTTagCompound[tileEntities.size()];
        tes = tileEntities.toArray(tes);
        Blueprint schem = new Blueprint(sizeX, sizeY, sizeZ, (byte)pallete.size(), states, structure, tes, requiredMods);
        if (name != null) {
            schem.setName(name);
        }
        if (architects != null) {
            schem.setArchitects(architects);
        }
        return schem;
    }

    public static NBTTagCompound writeBlueprintToNBT(Blueprint schem) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.func_74774_a("version", (byte)1);
        tag.func_74777_a("size_x", schem.getSizeX());
        tag.func_74777_a("size_y", schem.getSizeY());
        tag.func_74777_a("size_z", schem.getSizeZ());
        IBlockState[] palette = schem.getPallete();
        NBTTagList paletteTag = new NBTTagList();
        for (short i = 0; i < schem.getPalleteSize(); i = (short)(i + 1)) {
            NBTTagCompound state = new NBTTagCompound();
            NBTUtil.func_190009_a((NBTTagCompound)state, (IBlockState)palette[i]);
            paletteTag.func_74742_a((NBTBase)state);
        }
        tag.func_74782_a("palette", (NBTBase)paletteTag);
        int[] blockInt = BlueprintUtil.convertBlocksToSaveData(schem.getStructure(), schem.getSizeX(), schem.getSizeY(), schem.getSizeZ());
        tag.func_74783_a("blocks", blockInt);
        NBTTagList finishedTes = new NBTTagList();
        NBTTagCompound[] tes = schem.getTileEntities();
        for (int i = 0; i < tes.length; ++i) {
            finishedTes.func_74742_a((NBTBase)tes[i]);
        }
        tag.func_74782_a("tile_entities", (NBTBase)finishedTes);
        List<String> requiredMods = schem.getRequiredMods();
        NBTTagList modsList = new NBTTagList();
        for (int i = 0; i < requiredMods.size(); ++i) {
            modsList.func_74742_a((NBTBase)new NBTTagString(requiredMods.get(i)));
        }
        tag.func_74782_a("required_mods", (NBTBase)modsList);
        String name = schem.getName();
        String[] architects = schem.getArchitects();
        if (name != null) {
            tag.func_74778_a("name", name);
        }
        if (architects != null) {
            NBTTagList architectsTag = new NBTTagList();
            for (String architect : architects) {
                architectsTag.func_74742_a((NBTBase)new NBTTagString(architect));
            }
            tag.func_74782_a("architects", (NBTBase)architectsTag);
        }
        return tag;
    }

    public static Blueprint readBlueprintFromNBT(NBTTagCompound tag) {
        byte version = tag.func_74771_c("version");
        if (version == 1) {
            short sizeX = tag.func_74765_d("size_x");
            short sizeY = tag.func_74765_d("size_y");
            short sizeZ = tag.func_74765_d("size_z");
            ArrayList<String> requiredMods = new ArrayList<String>();
            NBTTagList modsList = (NBTTagList)tag.func_74781_a("required_mods");
            int modListSize = modsList.func_74745_c();
            for (int i = 0; i < modListSize; ++i) {
                requiredMods.add(((NBTTagString)modsList.func_179238_g(i)).func_150285_a_());
                if (Loader.isModLoaded((String)((String)requiredMods.get(i)))) continue;
                Logger.getGlobal().log(Level.WARNING, "Couldn't load Blueprint, the following mod is missing: " + (String)requiredMods.get(i));
                return null;
            }
            NBTTagList paletteTag = (NBTTagList)tag.func_74781_a("palette");
            short paletteSize = (short)paletteTag.func_74745_c();
            IBlockState[] palette = new IBlockState[paletteSize];
            for (int i = 0; i < palette.length; i = (int)((short)(i + 1))) {
                palette[i] = NBTUtil.func_190008_d((NBTTagCompound)paletteTag.func_150305_b(i));
            }
            short[][][] blocks = BlueprintUtil.convertSaveDataToBlocks(tag.func_74759_k("blocks"), sizeX, sizeY, sizeZ);
            NBTTagList teTag = (NBTTagList)tag.func_74781_a("tile_entities");
            NBTTagCompound[] tileEntities = new NBTTagCompound[teTag.func_74745_c()];
            for (int i = 0; i < tileEntities.length; i = (int)((short)(i + 1))) {
                tileEntities[i] = teTag.func_150305_b(i);
            }
            Blueprint schem = new Blueprint(sizeX, sizeY, sizeZ, paletteSize, palette, blocks, tileEntities, requiredMods);
            if (tag.func_74764_b("name")) {
                schem.setName(tag.func_74779_i("name"));
            }
            if (tag.func_74764_b("architects")) {
                NBTTagList architectsTag = (NBTTagList)tag.func_74781_a("architects");
                String[] architects = new String[architectsTag.func_74745_c()];
                for (int i = 0; i < architectsTag.func_74745_c(); ++i) {
                    architects[i] = architectsTag.func_150307_f(i);
                }
                schem.setArchitects(architects);
            }
            return schem;
        }
        return null;
    }

    public static void writeToFile(OutputStream os, Blueprint schem) {
        try {
            CompressedStreamTools.func_74799_a((NBTTagCompound)BlueprintUtil.writeBlueprintToNBT(schem), (OutputStream)os);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Blueprint readFromFile(InputStream is) {
        try {
            NBTTagCompound tag = CompressedStreamTools.func_74796_a((InputStream)is);
            return BlueprintUtil.readBlueprintFromNBT(tag);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int[] convertBlocksToSaveData(short[][][] multDimArray, short sizeX, short sizeY, short sizeZ) {
        short[] oneDimArray = new short[sizeX * sizeY * sizeZ];
        int j = 0;
        for (short y = 0; y < sizeY; y = (short)(y + 1)) {
            for (short z = 0; z < sizeZ; z = (short)(z + 1)) {
                for (short x = 0; x < sizeX; x = (short)(x + 1)) {
                    oneDimArray[j++] = multDimArray[y][z][x];
                }
            }
        }
        int[] ints = new int[(int)Math.ceil((float)oneDimArray.length / 2.0f)];
        int currentInt = 0;
        for (int i = 1; i < oneDimArray.length; i += 2) {
            currentInt = oneDimArray[i - 1];
            ints[(int)Math.ceil((double)((double)((float)i / 2.0f))) - 1] = currentInt = currentInt << 16 | oneDimArray[i];
            currentInt = 0;
        }
        if (oneDimArray.length % 2 == 1) {
            ints[ints.length - 1] = currentInt = oneDimArray[oneDimArray.length - 1] << 16;
        }
        return ints;
    }

    public static short[][][] convertSaveDataToBlocks(int[] ints, short sizeX, short sizeY, short sizeZ) {
        short[] oneDimArray = new short[ints.length * 2];
        for (int i = 0; i < ints.length; ++i) {
            oneDimArray[i * 2] = (short)(ints[i] >> 16);
            oneDimArray[i * 2 + 1] = (short)ints[i];
        }
        short[][][] multDimArray = new short[sizeY][sizeZ][sizeX];
        int i = 0;
        for (short y = 0; y < sizeY; y = (short)(y + 1)) {
            for (short z = 0; z < sizeZ; z = (short)(z + 1)) {
                for (short x = 0; x < sizeX; x = (short)(x + 1)) {
                    multDimArray[y][z][x] = oneDimArray[i++];
                }
            }
        }
        return multDimArray;
    }
}

