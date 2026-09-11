/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.launchwrapper.Launch
 *  net.minecraft.launchwrapper.LaunchClassLoader
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraftforge.event.world.WorldEvent$Save
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 */
package noppes.npcs.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.api.wrapper.WorldWrapper;
import noppes.npcs.controllers.data.ForgeScriptData;
import noppes.npcs.controllers.data.PlayerScriptData;
import noppes.npcs.util.NBTJsonUtil;

public class ScriptController {
    public static ScriptController Instance;
    public static boolean HasStart;
    private ScriptEngineManager manager;
    public Map<String, String> languages = new HashMap<String, String>();
    public Map<String, ScriptEngineFactory> factories = new HashMap<String, ScriptEngineFactory>();
    public Map<String, String> scripts = new HashMap<String, String>();
    public PlayerScriptData playerScripts = new PlayerScriptData(null);
    public ForgeScriptData forgeScripts = new ForgeScriptData();
    public long lastLoaded = 0L;
    public long lastPlayerUpdate = 0L;
    public File dir;
    public NBTTagCompound compound = new NBTTagCompound();
    private boolean loaded = false;
    public boolean shouldSave = false;

    public ScriptController() {
        ScriptEngineFactory factory;
        Class<?> c;
        Instance = this;
        if (!CustomNpcs.NashorArguments.isEmpty()) {
            System.setProperty("nashorn.args", CustomNpcs.NashorArguments);
        }
        this.manager = new ScriptEngineManager(LaunchClassLoader.class.getClassLoader());
        try {
            if (this.manager.getEngineByName("ecmascript") == null) {
                Launch.classLoader.addClassLoaderExclusion("jdk.nashorn.");
                Launch.classLoader.addClassLoaderExclusion("jdk.internal.dynalink");
                c = Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory");
                factory = (ScriptEngineFactory)c.newInstance();
                factory.getScriptEngine();
                this.manager.registerEngineName("ecmascript", factory);
                this.manager.registerEngineExtension("js", factory);
                this.manager.registerEngineMimeType("application/ecmascript", factory);
                this.languages.put(factory.getLanguageName(), ".js");
                this.factories.put(factory.getLanguageName().toLowerCase(), factory);
            }
        }
        catch (Throwable c2) {
            // empty catch block
        }
        try {
            c = Class.forName("org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory");
            factory = (ScriptEngineFactory)c.newInstance();
            factory.getScriptEngine();
            this.manager.registerEngineName("kotlin", factory);
            this.manager.registerEngineExtension("ktl", factory);
            this.manager.registerEngineMimeType("application/kotlin", factory);
            this.languages.put(factory.getLanguageName(), ".ktl");
            this.factories.put(factory.getLanguageName().toLowerCase(), factory);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        LogWriter.info("Script Engines Available:");
        for (ScriptEngineFactory fac : this.manager.getEngineFactories()) {
            try {
                if (fac.getExtensions().isEmpty() || !(fac.getScriptEngine() instanceof Invocable) && !fac.getLanguageName().equals("lua")) continue;
                String ext = "." + fac.getExtensions().get(0).toLowerCase();
                LogWriter.info(fac.getLanguageName() + ": " + ext);
                this.languages.put(fac.getLanguageName(), ext);
                this.factories.put(fac.getLanguageName().toLowerCase(), fac);
            }
            catch (Throwable throwable) {}
        }
    }

    public void loadCategories() {
        this.dir = new File(CustomNpcs.getWorldSaveDirectory(), "scripts");
        if (!this.dir.exists()) {
            this.dir.mkdirs();
        }
        if (!this.worldDataFile().exists()) {
            this.shouldSave = true;
        }
        WorldWrapper.tempData.clear();
        this.scripts.clear();
        for (String language : this.languages.keySet()) {
            String ext = this.languages.get(language);
            File scriptDir = new File(this.dir, language.toLowerCase());
            if (!scriptDir.exists()) {
                scriptDir.mkdir();
                continue;
            }
            this.loadDir(scriptDir, "", ext);
        }
        this.lastLoaded = System.currentTimeMillis();
    }

    private void loadDir(File dir, String name, String ext) {
        for (File file : dir.listFiles()) {
            String filename = name + file.getName().toLowerCase();
            if (file.isDirectory()) {
                this.loadDir(file, filename + "/", ext);
                continue;
            }
            if (!filename.endsWith(ext)) continue;
            try {
                this.scripts.put(filename, this.readFile(file));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean loadStoredData() {
        this.compound = new NBTTagCompound();
        File file = this.worldDataFile();
        try {
            if (!file.exists()) {
                return false;
            }
            this.compound = NBTJsonUtil.LoadFile(file);
            this.shouldSave = false;
        }
        catch (Exception e) {
            LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
            return false;
        }
        return true;
    }

    private File worldDataFile() {
        return new File(this.dir, "world_data.json");
    }

    private File playerScriptsFile() {
        return new File(this.dir, "player_scripts.json");
    }

    private File forgeScriptsFile() {
        return new File(this.dir, "forge_scripts.json");
    }

    public boolean loadPlayerScripts() {
        this.playerScripts.clear();
        File file = this.playerScriptsFile();
        try {
            if (!file.exists()) {
                return false;
            }
            this.playerScripts.readFromNBT(NBTJsonUtil.LoadFile(file));
        }
        catch (Exception e) {
            LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
            return false;
        }
        return true;
    }

    public void setPlayerScripts(NBTTagCompound compound) {
        this.playerScripts.readFromNBT(compound);
        File file = this.playerScriptsFile();
        try {
            NBTJsonUtil.SaveFile(file, compound);
            this.lastPlayerUpdate = System.currentTimeMillis();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (NBTJsonUtil.JsonException e) {
            e.printStackTrace();
        }
    }

    public boolean loadForgeScripts() {
        this.forgeScripts.clear();
        File file = this.forgeScriptsFile();
        try {
            if (!file.exists()) {
                return false;
            }
            this.forgeScripts.readFromNBT(NBTJsonUtil.LoadFile(file));
        }
        catch (Exception e) {
            LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
            return false;
        }
        return true;
    }

    public void setForgeScripts(NBTTagCompound compound) {
        this.forgeScripts.readFromNBT(compound);
        File file = this.forgeScriptsFile();
        try {
            NBTJsonUtil.SaveFile(file, compound);
            this.forgeScripts.lastInited = -1L;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (NBTJsonUtil.JsonException e) {
            e.printStackTrace();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private String readFile(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(file), "UTF8"));){
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            String string = sb.toString();
            return string;
        }
    }

    public ScriptEngine getEngineByName(String language) {
        ScriptEngineFactory fac = this.factories.get(language.toLowerCase());
        if (fac == null) {
            return null;
        }
        return fac.getScriptEngine();
    }

    public NBTTagList nbtLanguages() {
        NBTTagList list = new NBTTagList();
        for (String language : this.languages.keySet()) {
            NBTTagCompound compound = new NBTTagCompound();
            NBTTagList scripts = new NBTTagList();
            for (String script : this.getScripts(language)) {
                scripts.func_74742_a((NBTBase)new NBTTagString(script));
            }
            compound.func_74782_a("Scripts", (NBTBase)scripts);
            compound.func_74778_a("Language", language);
            list.func_74742_a((NBTBase)compound);
        }
        return list;
    }

    private List<String> getScripts(String language) {
        ArrayList<String> list = new ArrayList<String>();
        String ext = this.languages.get(language);
        if (ext == null) {
            return list;
        }
        for (String script : this.scripts.keySet()) {
            if (!script.endsWith(ext)) continue;
            list.add(script);
        }
        return list;
    }

    @SubscribeEvent
    public void saveWorld(WorldEvent.Save event) {
        if (!this.shouldSave || event.getWorld().field_72995_K || event.getWorld() != event.getWorld().func_73046_m().field_71305_c[0]) {
            return;
        }
        try {
            NBTJsonUtil.SaveFile(this.worldDataFile(), this.compound.func_74737_b());
        }
        catch (Exception e) {
            LogWriter.except(e);
        }
        this.shouldSave = false;
    }

    static {
        HasStart = false;
    }
}

