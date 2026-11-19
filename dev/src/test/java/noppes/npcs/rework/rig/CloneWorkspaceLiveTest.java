package noppes.npcs.rework.rig;

import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.rework.data.ReadableClone;
import org.junit.Test;
import static org.junit.Assert.*;

/** The live gate must reject silent normalization while disk workspaces remain lossless. */
public class CloneWorkspaceLiveTest {
    private static final String BASE = "{\"animations\":{\"idle\":{},\"walk\":{},\"hurt\":{},\"death\":{}}}";
    private static RigModels.Model model() {
        return new RigModels.Model("known", "head", 0.5F, null,
                AnimLibrary.merge(AnimLibrary.EMPTY, BASE, new ArrayList<>()), Collections.emptyList(), false, false);
    }
    private static NBTTagCompound clone(String clips) {
        NBTTagCompound n = new NBTTagCompound(), rig = new NBTTagCompound();
        rig.setString("Model", "known"); rig.setString("Clips", clips); n.setTag("RwRig", rig);
        return n;
    }
    private static void validate(NBTTagCompound n) { RigCreatureAssets.validateWorkspace(n, id -> id.equals("known") ? model() : null); }
    private static void refused(NBTTagCompound n) {
        try { validate(n); fail("invalid live rig accepted"); } catch (RuntimeException expected) { assertNotNull(expected.getMessage()); }
    }
    @Test public void snapshotOwnsIndependentTypedCloneCopies() {
        NBTTagCompound n = clone("{\"animations\":{}}");
        n.setString("ScriptLanguage", "Java"); n.setBoolean("ScriptEnabled", false);
        NBTTagList tabs = new NBTTagList();
        for (int i = 0; i < 2; i++) {
            NBTTagCompound tab = new NBTTagCompound(); tab.setString("Script", "// tab " + i + "\r\n");
            tab.setLong("unknown", Long.MAX_VALUE); tabs.appendTag(tab);
        }
        n.setTag("Scripts", tabs);
        RigCreatureAssets.Pack pack = new RigCreatureAssets.Pack(n);
        assertTrue(ReadableClone.same(n, pack.workspace()));
        NBTTagCompound pristine = n.copy(); n.removeTag("Scripts");
        pack.workspace().getCompoundTag("RwRig").setString("Model", "changed");
        assertTrue(ReadableClone.same(pristine, pack.workspace()));
        assertNull(new RigCreatureAssets.Pack(new com.google.gson.JsonObject(), "").workspace());
    }
    @Test public void noRigNeedsNoModelOrAnimation() {
        NBTTagCompound n = new NBTTagCompound(); n.setString("ScriptLanguage", "other");
        RigCreatureAssets.validateWorkspace(n, id -> { throw new AssertionError("no rig lookup"); });
    }
    @Test public void missingOrInvalidModelIsNotSilentlyCleared() {
        NBTTagCompound n = clone(""); n.getCompoundTag("RwRig").setString("Model", "../outside"); refused(n);
        n.getCompoundTag("RwRig").setString("Model", "missing"); refused(n);
    }
    @Test public void activeMalformedClipsAreRefused() {
        for (String text : new String[]{"[1]", "{broken", "{\"animations\":[]}", "{\"animations\":null}", "{\"animations\":{\"a\":7}}"}) { refused(clone(text)); }
        NBTTagCompound n = clone(""); n.getCompoundTag("RwRig").setInteger("Clips", 1); refused(n);
    }
    @Test public void clipBoundsAndBindingsAreValidated() {
        refused(clone("{\"animations\":{\"bad\":{\"animation_length\":0}}}"));
        refused(clone("{\"animations\":{\"bad\":{\"animation_length\":1,\"events\":[{\"t\":2,\"type\":\"sound\"}]}}}"));
        NBTTagCompound n = clone(""); n.getCompoundTag("RwRig").setString("Idle", "not_installed"); refused(n);
        n = clone(""); n.getCompoundTag("RwRig").setFloat("Scale", Float.NaN); refused(n);
        n.getCompoundTag("RwRig").setFloat("Scale", 25); refused(n);
    }
    @Test public void clipsCannotBeTruncatedAtSpawn() {
        char[] bytes = new char[RigData.MAX_CLIPS + 1]; java.util.Arrays.fill(bytes, ' ');
        refused(clone(new String(bytes)));
    }
    @Test public void ownBindingsAndBothClipShapesArePreservedWithoutNormalization() {
        for (String clips : new String[]{"{\"animations\":{\"own\":{\"animation_length\":2}}}", "{\"own\":{\"animation_length\":2}}"}) {
            NBTTagCompound n = clone(clips); n.getCompoundTag("RwRig").setString("Idle", "own");
            NBTTagCompound before = n.copy(); validate(n); assertTrue(ReadableClone.same(before, n));
        }
    }
}
