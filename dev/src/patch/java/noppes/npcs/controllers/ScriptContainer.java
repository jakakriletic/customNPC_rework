/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.potion.PotionType
 *  net.minecraft.util.math.BlockPos
 *  net.minecraftforge.fml.common.eventhandler.Event
 */
package noppes.npcs.controllers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.constants.AnimationType;
import noppes.npcs.api.constants.EntityType;
import noppes.npcs.api.constants.JobType;
import noppes.npcs.api.constants.ParticleType;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.constants.SideType;
import noppes.npcs.api.constants.TacticalType;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.rework.script.SourceMap;

public class ScriptContainer {
    private static final String lock = "lock";
    public static ScriptContainer Current;
    private static String CurrentType;
    private static final HashMap<String, Object> Data;
    public String fullscript = "";
    public String script = "";
    public TreeMap<Long, String> console = new TreeMap();
    public boolean errored = false;
    public List<String> scripts = new ArrayList<String>();
    private HashSet<String> unknownFunctions = new HashSet();
    public long lastCreated = 0L;
    private String currentScriptLanguage = null;
    private ScriptEngine engine = null;
    private IScriptHandler handler = null;
    private boolean init = false;
    private static Method luaCoerce;
    private static Method luaCall;
    /** M6.5: segments of fullscript, so an error line maps back to the tab or the include file. */
    private SourceMap sourceMap = new SourceMap();

    private static void FillMap(Class c) {
        Field[] declaredFields;
        try {
            Data.put(c.getSimpleName(), c.newInstance());
        }
        catch (Exception exception) {
            // empty catch block
        }
        for (Field field : declaredFields = c.getDeclaredFields()) {
            try {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != Integer.TYPE) continue;
                Data.put(c.getSimpleName() + "_" + field.getName(), field.getInt(null));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public ScriptContainer(IScriptHandler handler) {
        this.handler = handler;
    }

    public void readFromNBT(NBTTagCompound compound) {
        this.script = compound.getString("Script");
        this.console = NBTTags.GetLongStringMap(compound.getTagList("Console", 10));
        this.scripts = NBTTags.getStringList(compound.getTagList("ScriptList", 10));
        this.lastCreated = 0L;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setString("Script", this.script);
        compound.setTag("Console", (NBTBase)NBTTags.NBTLongStringMap(this.console));
        compound.setTag("ScriptList", (NBTBase)NBTTags.nbtStringList(this.scripts));
        return compound;
    }

    private String getFullCode() {
        if (!this.init) {
            this.fullscript = this.script;
            this.sourceMap = new SourceMap();
            if (!this.fullscript.isEmpty()) {
                this.sourceMap.add(1, "tab");
                this.fullscript = this.fullscript + "\n";
            }
            for (String loc : this.scripts) {
                String code = ScriptController.Instance.scripts.get(loc);
                if (code == null || code.isEmpty()) continue;
                this.sourceMap.add(1 + SourceMap.lines(this.fullscript), loc);
                this.fullscript = this.fullscript + code + "\n";
            }
            this.unknownFunctions = new HashSet();
        }
        return this.fullscript;
    }

    public void run(EnumScriptType type, Event event) {
        this.run(type.function, (Object)event);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void run(String type, Object event) {
        if (!CustomNpcs.EnableScripting) {
            return;
        }
        // M6.5 B5: a script reload must reach a stopped or partial container before the guards below;
        // otherwise a fixed error or a newly added function never runs until the next GUI save.
        if (ScriptController.Instance.lastLoaded > this.lastCreated) {
            this.lastCreated = ScriptController.Instance.lastLoaded;
            this.init = false;
            this.errored = false;
        }
        if (this.errored || !this.hasCode() || this.unknownFunctions.contains(type)) {
            return;
        }
        this.setEngine(this.handler.getLanguage());
        if (this.engine == null) {
            return;
        }
        String string = lock;
        synchronized (lock) {
            // M6.6: the monitor is reentrant. A nested script must return ownership to
            // its caller, including when setup, invocation or console cleanup throws.
            ScriptContainer previous = Current;
            String previousType = CurrentType;
            Current = this;
            CurrentType = type;
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                this.engine.getContext().setWriter(pw);
                this.engine.getContext().setErrorWriter(pw);
                try {
                    if (!this.init) {
                        this.engine.eval(this.getFullCode());
                        this.init = true;
                    }
                    if (this.engine.getFactory().getLanguageName().equals("lua")) {
                        Object ob = this.engine.get(type);
                        if (ob != null) {
                            if (luaCoerce == null) {
                                luaCoerce = Class.forName("org.luaj.vm2.lib.jse.CoerceJavaToLua").getMethod("coerce", Object.class);
                                luaCall = ob.getClass().getMethod("call", Class.forName("org.luaj.vm2.LuaValue"));
                            }
                            luaCall.invoke(ob, luaCoerce.invoke(null, event));
                        } else {
                            this.unknownFunctions.add(type);
                        }
                    } else {
                        ((Invocable)((Object)this.engine)).invokeFunction(type, event);
                    }
                }
                catch (NoSuchMethodException e) {
                    this.unknownFunctions.add(type);
                }
                catch (Throwable e) {
                    this.errored = true;
                    e.printStackTrace(pw);
                    String source = this.sourceMap.locateScript(e);
                    if (source != null) {
                        pw.println("Script source: " + source);
                    }
                    NoppesUtilServer.NotifyOPs(this.handler.noticeString() + " script errored", new Object[0]);
                }
                finally {
                    this.appandConsole(sw.getBuffer().toString().trim());
                    pw.close();
                }
            } finally {
                Current = previous;
                CurrentType = previousType;
            }
            // ** MonitorExit[var3_3] (shouldn't be in output)
            return;
        }
    }

    public void appandConsole(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        long time = System.currentTimeMillis();
        if (this.console.containsKey(time)) {
            message = this.console.get(time) + "\n" + message;
        }
        this.console.put(time, message);
        while (this.console.size() > 40) {
            this.console.remove(this.console.firstKey());
        }
    }

    public boolean hasCode() {
        return !this.getFullCode().isEmpty();
    }

    public void setEngine(String scriptLanguage) {
        if (this.currentScriptLanguage != null && this.currentScriptLanguage.equals(scriptLanguage)) {
            return;
        }
        this.engine = ScriptController.Instance.getEngineByName(scriptLanguage);
        if (this.engine == null) {
            this.errored = true;
            return;
        }
        for (Map.Entry<String, Object> entry : Data.entrySet()) {
            this.engine.put(entry.getKey(), entry.getValue());
        }
        this.engine.put("dump", new Dump());
        this.engine.put("log", new Log());
        this.currentScriptLanguage = scriptLanguage;
        this.init = false;
    }

    public boolean isValid() {
        return this.init && !this.errored;
    }

    static {
        Data = new HashMap();
        ScriptContainer.FillMap(AnimationType.class);
        ScriptContainer.FillMap(EntityType.class);
        ScriptContainer.FillMap(RoleType.class);
        ScriptContainer.FillMap(JobType.class);
        ScriptContainer.FillMap(SideType.class);
        ScriptContainer.FillMap(TacticalType.class);
        ScriptContainer.FillMap(PotionType.class);
        ScriptContainer.FillMap(ParticleType.class);
        Data.put("PosZero", new BlockPosWrapper(BlockPos.ORIGIN));
    }

    private class Log
    implements Function<Object, Void> {
        private Log() {
        }

        @Override
        public Void apply(Object o) {
            ScriptContainer.this.appandConsole(o + "");
            LogWriter.info(o + "");
            return null;
        }
    }

    private class Dump
    implements Function<Object, String> {
        private Dump() {
        }

        @Override
        public String apply(Object o) {
            if (o == null) {
                return "null";
            }
            StringBuilder builder = new StringBuilder();
            builder.append(o + ":" + NoppesStringUtils.newLine());
            for (Field field : o.getClass().getFields()) {
                try {
                    builder.append(field.getName() + " - " + field.getType().getSimpleName() + ", ");
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
            for (AccessibleObject accessibleObject : o.getClass().getMethods()) {
                try {
                    String s = ((Method)accessibleObject).getName() + "(";
                    for (Class<?> c : ((Method)accessibleObject).getParameterTypes()) {
                        s = s + c.getSimpleName() + ", ";
                    }
                    if (s.endsWith(", ")) {
                        s = s.substring(0, s.length() - 2);
                    }
                    builder.append(s + "), ");
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
            return builder.toString();
        }
    }
}

