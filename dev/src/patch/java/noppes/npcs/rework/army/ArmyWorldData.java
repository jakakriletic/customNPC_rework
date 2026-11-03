package noppes.npcs.rework.army;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import noppes.npcs.LogWriter;

/**
 * M11.1 (Q297): the army register saved with the world in {@code data/rwarmies.dat} (shared map storage
 * of the overworld, so one register for all dimensions). Loaded only when RwArmies>=1 uses it.
 */
public class ArmyWorldData extends WorldSavedData {
    public static final String NAME = "rwarmies";

    private final ArmyRegistry registry = new ArmyRegistry();

    /** Called by MapStorage through reflection. */
    public ArmyWorldData(String name) {
        super(name);
    }

    public ArmyRegistry registry() {
        return registry;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        registry.read(nbt);
        LogWriter.info("RWARMY register loaded armies=" + registry.size() + " skipped=" + registry.skippedOnRead());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return registry.write(compound);
    }

    /** The register of this server; null when the overworld is not loaded. */
    public static ArmyWorldData get(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        World overworld = server.getWorld(0);
        if (overworld == null) {
            return null;
        }
        MapStorage storage = overworld.getMapStorage();
        ArmyWorldData data = (ArmyWorldData) storage.getOrLoadData(ArmyWorldData.class, NAME);
        if (data == null) {
            data = new ArmyWorldData(NAME);
            storage.setData(NAME, data);
        }
        return data;
    }
}
