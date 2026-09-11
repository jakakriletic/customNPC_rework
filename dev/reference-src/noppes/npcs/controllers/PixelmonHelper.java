/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraftforge.fml.common.Loader
 *  net.minecraftforge.fml.common.eventhandler.EventBus
 *  org.apache.logging.log4j.LogManager
 */
package noppes.npcs.controllers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import noppes.npcs.LogWriter;
import org.apache.logging.log4j.LogManager;

public class PixelmonHelper {
    public static boolean Enabled = Loader.isModLoaded((String)"pixelmon");
    public static EventBus EVENT_BUS;
    public static Object storageManager;
    private static Method getPartyStorage;
    private static Method getPcStorage;
    private static Method getPokemonData;
    private static Method getPixelmonModel;
    private static Class modelSetupClass;
    private static Method modelSetupMethod;

    public static void load() {
        if (!Enabled) {
            return;
        }
        try {
            Class<?> c = Class.forName("com.pixelmonmod.pixelmon.Pixelmon");
            storageManager = c.getDeclaredField("storageManager").get(null);
            EVENT_BUS = (EventBus)c.getDeclaredField("EVENT_BUS").get(null);
            c = Class.forName("com.pixelmonmod.pixelmon.api.storage.IStorageManager");
            getPartyStorage = c.getMethod("getParty", EntityPlayerMP.class);
            getPcStorage = c.getMethod("getPCForPlayer", EntityPlayerMP.class);
            c = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.Entity1Base");
            getPokemonData = c.getMethod("getPokemonData", new Class[0]);
        }
        catch (Exception e) {
            LogWriter.except(e);
            Enabled = false;
        }
    }

    public static void loadClient() {
        if (!Enabled) {
            return;
        }
        try {
            Class<?> c = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.Entity2Client");
            getPixelmonModel = c.getMethod("getModel", new Class[0]);
            modelSetupClass = Class.forName("com.pixelmonmod.pixelmon.client.models.PixelmonModelSmd");
            modelSetupMethod = modelSetupClass.getMethod("setupForRender", c);
        }
        catch (Exception e) {
            LogWriter.except(e);
            Enabled = false;
        }
    }

    public static List<String> getPixelmonList() {
        ArrayList<String> list = new ArrayList<String>();
        if (!Enabled) {
            return list;
        }
        try {
            ?[] array;
            Class<?> c = Class.forName("com.pixelmonmod.pixelmon.enums.EnumPokemonModel");
            for (Object ob : array = c.getEnumConstants()) {
                list.add(ob.toString());
            }
        }
        catch (Exception e) {
            LogManager.getLogger().error("getPixelmonList", (Throwable)e);
        }
        return list;
    }

    public static boolean isPixelmon(Entity entity) {
        if (!Enabled) {
            return false;
        }
        String s = EntityList.getEntityString((Entity)entity);
        if (s == null) {
            return false;
        }
        return s.contains("Pixelmon");
    }

    public static String getName(EntityLivingBase entity) {
        if (!Enabled || !PixelmonHelper.isPixelmon((Entity)entity)) {
            return "";
        }
        try {
            Method m = entity.getClass().getMethod("getName", new Class[0]);
            return m.invoke(entity, new Object[0]).toString();
        }
        catch (Exception e) {
            LogManager.getLogger().error("getName", (Throwable)e);
            return "";
        }
    }

    public static Object getModel(EntityLivingBase entity) {
        try {
            return getPixelmonModel.invoke(entity, new Object[0]);
        }
        catch (Exception e) {
            LogManager.getLogger().error("getModel", (Throwable)e);
            return null;
        }
    }

    public static void setupModel(EntityLivingBase entity, Object model) {
        try {
            if (modelSetupClass.isAssignableFrom(model.getClass())) {
                modelSetupMethod.invoke(model, entity);
            }
        }
        catch (Exception e) {
            LogManager.getLogger().error("setupModel", (Throwable)e);
        }
    }

    public static Object getPokemonData(Entity entity) {
        try {
            return getPokemonData.invoke(entity, new Object[0]);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getParty(EntityPlayerMP player) {
        try {
            return getPartyStorage.invoke(storageManager, player);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getPc(EntityPlayerMP player) {
        try {
            return getPcStorage.invoke(storageManager, player);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static {
        getPixelmonModel = null;
    }
}

