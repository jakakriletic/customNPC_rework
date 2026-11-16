package noppes.npcs.rework.data;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.util.NBTJsonUtil;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class ReadableCloneTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private static NBTTagCompound round(NBTTagCompound n) { NBTTagCompound out = ReadableClone.decode(ReadableClone.encode(n)); assertTrue(ReadableClone.same(n,out)); return out; }
    private static JsonObject doc(NBTTagCompound n) { return StrictCloneJson.read(ReadableClone.encode(n)).getAsJsonObject(); }
    private static void rejects(String text) { try { ReadableClone.decode(text); fail("Accepted invalid document"); } catch (IllegalArgumentException expected) { assertFalse(expected.getMessage().isEmpty()); } }
    private static String text(JsonObject d) { return StrictCloneJson.write(d); }
    private static String minimal() { return "{\"$format\":\"customnpcs-readable-clone\",\"version\":1,\"defaults\":\"cnpc-01oct19-v1\"}"; }

    @Test public void entireCloneCorpusRoundtripsWithoutEntityNormalization() throws Exception {
        int count = 0;
        for (Path root : new Path[]{Paths.get("testworld/customnpcs/clones"), Paths.get("../examples/clones")}) {
            assertTrue("Corpus directory " + root, Files.isDirectory(root));
            try (java.util.stream.Stream<Path> files = Files.walk(root)) {
                for (Path p : (Iterable<Path>)files.filter(f -> f.toString().endsWith(".json"))::iterator) {
                    NBTTagCompound input = NBTJsonUtil.LoadFile(p.toFile());
                    try { round(input); } catch (RuntimeException | AssertionError e) { throw new AssertionError("Corpus " + p,e); }
                    count++;
                }
            }
        }
        assertTrue("Full corpus, including sparse Konjenik", count >= 89);
        System.out.println("READABLECLONE corpus=" + count);
    }
    @Test public void everyScalarArrayListAndEmptyElementTypeSurvives() {
        NBTTagCompound n = new NBTTagCompound();
        n.setByte("b",Byte.MIN_VALUE); n.setShort("s",Short.MAX_VALUE); n.setInteger("i",Integer.MIN_VALUE);
        n.setLong("min",Long.MIN_VALUE); n.setLong("max",Long.MAX_VALUE); n.setLong("large",9007199254740993L);
        n.setByteArray("ba",new byte[]{-128,0,127}); n.setIntArray("ia",new int[]{Integer.MIN_VALUE,0,Integer.MAX_VALUE});
        n.setTag("la",new NBTTagLongArray(new long[]{Long.MIN_VALUE,9007199254740993L,Long.MAX_VALUE}));
        n.setByteArray("be",new byte[0]); n.setIntArray("ie",new int[0]); n.setTag("le",new NBTTagLongArray(new long[0]));
        n.setString("", "\u0000 \"\\ Ž 🦊\n\t\ud800"); n.setString("a/~.b", " leading and trailing ");
        for (int id = 0; id <= 12; id++) { n.setTag("empty" + id, CloneNbt.emptyList(id)); }
        for (NBTBase v : new NBTBase[]{new NBTTagByte((byte)2),new NBTTagInt(7),new NBTTagLong(Long.MAX_VALUE)}) {
            NBTTagList l = new NBTTagList(); l.appendTag(v); n.setTag("list" + v.getId(),l);
        }
        round(n);
    }
    @Test public void floatingBitsIncludingNaNPayloadAndNegativeZeroSurvive() {
        NBTTagCompound n = new NBTTagCompound();
        int[] fs = {0,0x80000000,0x7f800000,0xff800000,0x7fc01234,0x7fa00001,1,0x7f7fffff};
        long[] ds = {0L,0x8000000000000000L,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000001234L,1L,0x7fefffffffffffffL};
        for (int i = 0; i < fs.length; i++) { n.setFloat("f" + i,Float.intBitsToFloat(fs[i])); }
        for (int i = 0; i < ds.length; i++) { n.setDouble("d" + i,Double.longBitsToDouble(ds[i])); }
        n.setFloat("Scale",-0.0f); NBTTagCompound rig = new NBTTagCompound(); rig.setFloat("Scale",-0.0f); n.setTag("RwRig",rig);
        round(n);
    }
    @Test public void absentExplicitDefaultWrongTypeAndEmptyParentAreDistinct() {
        NBTTagCompound empty = new NBTTagCompound(); round(empty);
        assertFalse(ReadableClone.decode(ReadableClone.encode(empty)).hasKey("Size"));
        NBTTagCompound defaults = new NBTTagCompound(); defaults.setInteger("Size",5); defaults.setString("Title",""); defaults.setInteger("MinExp",0); defaults.setInteger("MaxExp",0);
        JsonObject d = doc(defaults); assertFalse(d.getAsJsonObject("appearance").has("modelSize"));
        assertEquals(5,round(defaults).getInteger("Size"));
        NBTTagCompound wrong = new NBTTagCompound(); wrong.setByte("Size",(byte)5); wrong.setByte("CanSwim",(byte)2); round(wrong);
        assertFalse(doc(wrong).getAsJsonObject("ai").has("canSwim"));
        NBTTagCompound parent = new NBTTagCompound(); parent.setTag("RwRig",new NBTTagCompound()); round(parent);
        parent.setInteger("RwRig",7); round(parent);
        assertTrue(ReadableClone.decode(minimal()).hasKey("Size",3));
    }
    @Test public void authorEditsOnlyIntendedNbtPathsAcrossSections() throws Exception {
        round(ReadableClone.decode(new String(Files.readAllBytes(Paths.get("../examples/readable-clones/knight.json")),StandardCharsets.UTF_8)));
        JsonObject d = StrictCloneJson.read(minimal()).getAsJsonObject();
        d.add("entity", obj("type",new JsonPrimitive("customnpcs:customnpc")));
        d.add("appearance",obj("name",new JsonPrimitive("Knight"))); d.getAsJsonObject("appearance").addProperty("modelSize",7);
        d.add("stats",obj("meleeDamage",new JsonPrimitive(12)));
        d.add("ai",obj("walkingSpeed",new JsonPrimitive(8))); d.getAsJsonObject("ai").addProperty("canSwim",false);
        JsonArray items = new JsonArray(); JsonObject item = new JsonObject(); item.addProperty("slot",0); item.addProperty("item","minecraft:diamond_sword"); item.addProperty("count",1); item.addProperty("damage",4); items.add(item);
        d.add("inventory",obj("weapons",items)); d.add("advanced",obj("role",new JsonPrimitive(2)));
        JsonArray tabs = new JsonArray(); JsonArray source = new JsonArray(); source.add(new JsonPrimitive("// safe text")); tabs.add(obj("sourceLines",source));
        d.add("scripts",obj("tabs",tabs)); d.getAsJsonObject("scripts").addProperty("enabled",false);
        d.add("rig",obj("model",new JsonPrimitive("unknown_mod_model"))); d.getAsJsonObject("rig").addProperty("scale",1.25);
        NBTTagCompound n = ReadableClone.decode(text(d));
        NBTTagCompound expected = ReadableClone.decode(minimal()); expected.setString("id","customnpcs:customnpc"); expected.setString("Name","Knight"); expected.setInteger("Size",7); expected.setInteger("AttackStrenght",12); expected.setInteger("MoveSpeed",8); expected.setBoolean("CanSwim",false); expected.setInteger("Role",2); expected.setBoolean("ScriptEnabled",false);
        NBTTagCompound it = new NBTTagCompound(); it.setByte("Slot",(byte)0); it.setString("id","minecraft:diamond_sword"); it.setByte("Count",(byte)1); it.setShort("Damage",(short)4); NBTTagList il = new NBTTagList(); il.appendTag(it); expected.setTag("Weapons",il);
        NBTTagCompound tab = new NBTTagCompound(); tab.setString("Script","// safe text"); NBTTagList tl = new NBTTagList(); tl.appendTag(tab); expected.setTag("Scripts",tl);
        NBTTagCompound rig = new NBTTagCompound(); rig.setString("Model","unknown_mod_model"); rig.setFloat("Scale",1.25f); expected.setTag("RwRig",rig);
        assertTrue(ReadableClone.same(expected,n)); round(n);
    }
    private static JsonObject obj(String k,JsonElement v) { JsonObject o = new JsonObject(); o.add(k,v); return o; }
    @Test public void tabsIncludesConsoleMixedEndingsAndRigStringsSurviveExactly() {
        for (String language : new String[]{"Java","ECMAScript"}) {
            NBTTagCompound n = new NBTTagCompound(); n.setString("ScriptLanguage",language);
            NBTTagList tabs = new NBTTagList();
            for (String code : new String[]{"", "\n", "x\r\ny\rz\n\n", "a\r\n", "// \"$format\": \"customnpcs-readable-clone\"\n"}) {
                NBTTagCompound tab = new NBTTagCompound(); tab.setString("Script",code);
                NBTTagList includes = new NBTTagList(); NBTTagCompound include = new NBTTagCompound(); include.setString("Line","helper.java"); include.setLong("mod-extra",Long.MAX_VALUE); includes.appendTag(include); tab.setTag("ScriptList",includes);
                NBTTagCompound console = new NBTTagCompound(); console.setLong("timestamp",Long.MAX_VALUE); tab.setTag("Console",console); tabs.appendTag(tab);
            }
            tabs.appendTag(new NBTTagCompound()); n.setTag("Scripts",tabs);
            NBTTagCompound rig = new NBTTagCompound(); rig.setString("Clips","{\r\n \"duplicate\": 1,\r \"duplicate\": 2\n}\n"); rig.setString("Model","not-validated-at-conversion"); n.setTag("RwRig",rig);
            round(n);
        }
    }
    @Test public void deepUnknownItemRoleTransformAndModDataRemainTyped() {
        NBTTagCompound n = new NBTTagCompound(); NBTTagCompound deep = new NBTTagCompound(); deep.setLong("uuid",Long.MAX_VALUE); deep.setIntArray("points",new int[]{1,2});
        for (int i = 0; i < 40; i++) { NBTTagCompound next = new NBTTagCompound(); next.setTag("level/~",deep); deep = next; }
        n.setTag("TransformJob",deep); n.setTag("RoleData",deep.copy());
        NBTTagCompound item = new NBTTagCompound(); item.setByte("Slot",(byte)1); item.setTag("tag",deep.copy()); NBTTagList list = new NBTTagList(); list.appendTag(item); n.setTag("NpcInv",list);
        round(n);
    }
    @Test public void rejectsDuplicateCommentsTrailingMalformedAndNonJsonNumbers() {
        String valid = minimal();
        for (String invalid : new String[]{valid + " {}",valid.replace("\"version\":1","\"version\":1,\"version\":1"),valid.replace("\"version\":1","\"version\":01"),valid.replace("\"version\":1","\"version\":+1"),valid.replace("\"version\":1","\"version\":NaN"),valid.replace("\"version\":1","/* hi */\"version\":1"),valid.replace(",\"version\":1",",version:1"),valid.substring(0,valid.length()-1)+",}",valid.replace("clone\"","clone\n\""),valid.replace("version","ver\\xion")}) { rejects(invalid); }
    }
    @Test public void rejectsVersionProfileTypeRangeAndUnknownAlias() {
        rejects(minimal().replace("\"version\":1","\"version\":2")); rejects(minimal().replace(ReadableClone.DEFAULTS,"next"));
        for (String v : new String[]{"128","-129","1.5","\"1\"","true","null"}) { JsonObject d = StrictCloneJson.read(minimal()).getAsJsonObject(); d.add("appearance",obj("skinSource",StrictCloneJson.read(v))); rejects(text(d)); }
        JsonObject d = StrictCloneJson.read(minimal()).getAsJsonObject(); d.add("ai",obj("canSwim",new JsonPrimitive(1))); rejects(text(d));
        d = StrictCloneJson.read(minimal()).getAsJsonObject(); d.add("appearance",obj("typoName",new JsonPrimitive("x"))); rejects(text(d));
        d = StrictCloneJson.read(minimal()).getAsJsonObject(); d.add("rig",obj("scale",new JsonPrimitive(1e100))); rejects(text(d));
    }
    @Test public void rejectsAliasRawAbsentAndParentConflicts() {
        NBTTagCompound n = new NBTTagCompound(); JsonObject d = doc(n); d.getAsJsonObject("appearance").addProperty("modelSize",6); rejects(text(d));
        n.setString("Name","a"); d = doc(n); NBTTagCompound raw = new NBTTagCompound(); raw.setString("Name","b"); d.add("_nbt",obj("raw",CloneNbt.encode(raw))); rejects(text(d));
        n = new NBTTagCompound(); n.setInteger("RwRig",1); d = doc(n); d.getAsJsonObject("rig").addProperty("model","knight"); rejects(text(d));
        d = doc(new NBTTagCompound()); d.getAsJsonObject("_nbt").getAsJsonArray("absent").add(new JsonPrimitive("/does~1not~0exist")); rejects(text(d));
    }
    @Test public void rejectsInvalidTextAndRawListMetadataWithPaths() {
        JsonObject d = StrictCloneJson.read(minimal()).getAsJsonObject(); JsonArray tabs = new JsonArray(); JsonObject tab = new JsonObject(); JsonArray lines = new JsonArray(); lines.add(new JsonPrimitive("one")); lines.add(new JsonPrimitive("two")); tab.add("sourceLines",lines); tab.add("lineEndings",new JsonArray()); tabs.add(tab); d.add("scripts",obj("tabs",tabs)); rejects(text(d));
        tab.remove("lineEndings"); tab.addProperty("lineEnding","unix"); rejects(text(d));
        JsonObject list = new JsonObject(); list.addProperty("type","list"); list.addProperty("elementType","byte"); JsonArray values = new JsonArray(); values.add(CloneNbt.encode(new NBTTagInt(1))); list.add("value",values);
        JsonObject root = new JsonObject(); root.addProperty("type","compound"); root.add("value",obj("a/~",list)); d = StrictCloneJson.read(minimal()).getAsJsonObject(); d.add("_nbt",obj("raw",root)); rejects(text(d));
    }
    @Test public void markerInsideScriptDoesNotSelectFormatAndReadingNeverWrites() throws Exception {
        File f = temp.newFile("legacy.json"); NBTTagCompound n = new NBTTagCompound(); n.setString("Script","\"$format\":\"customnpcs-readable-clone\""); NBTJsonUtil.SaveFile(f,n);
        byte[] before = Files.readAllBytes(f.toPath()); assertFalse(ReadableCloneFiles.hasEnvelope(new String(before,StandardCharsets.UTF_8))); assertTrue(ReadableClone.same(n,ReadableCloneFiles.load(f))); assertArrayEquals(before,Files.readAllBytes(f.toPath()));
        assertTrue(ReadableCloneFiles.hasEnvelope("{\"\\u0024format\":\"future\"}"));
        Files.write(f.toPath(),"{\"$format\":\"future\"}".getBytes(StandardCharsets.UTF_8));
        try { ReadableCloneFiles.load(f); fail(); } catch (IOException expected) { assertTrue(expected.getMessage().contains("/$format")); }
    }
    @Test public void safeOverwriteBackupLegacyEqualityGuideAndCloneList() throws Exception {
        File dir = temp.newFolder(); File f = new File(dir,"one.json"); NBTTagCompound n = new NBTTagCompound(); n.setString("Name","One"); n.setInteger("Size",5);
        ReadableCloneFiles.save(f,n,false); assertArrayEquals(NBTJsonUtil.Convert(n).getBytes(StandardCharsets.UTF_8),Files.readAllBytes(f.toPath())); byte[] old = Files.readAllBytes(f.toPath());
        ReadableCloneFiles.save(f,n,true); File backup = new File(dir,"one.json.v0.bak"); assertArrayEquals(old,Files.readAllBytes(backup.toPath())); assertTrue(ReadableClone.same(n,ReadableCloneFiles.load(f)));
        n.setString("Name","Two"); ReadableCloneFiles.save(f,n,true); assertArrayEquals(old,Files.readAllBytes(backup.toPath()));
        File guide = new File(dir,"CLONE-FORMAT.md"); assertEquals(ReadableClone.guide(),new String(Files.readAllBytes(guide.toPath()),StandardCharsets.UTF_8)); Files.write(guide.toPath(),"owner notes".getBytes(StandardCharsets.UTF_8)); ReadableCloneFiles.save(f,n,true); assertEquals("owner notes",new String(Files.readAllBytes(guide.toPath()),StandardCharsets.UTF_8));
        ReadableCloneFiles.save(f,n,false); assertArrayEquals(NBTJsonUtil.Convert(n).getBytes(StandardCharsets.UTF_8),Files.readAllBytes(f.toPath())); ReadableCloneFiles.save(f,n,true); assertArrayEquals(old,Files.readAllBytes(backup.toPath()));
        final File base = dir.getParentFile(); final String tab = dir.getName();
        // Enumeration only recognizes .json (the real controller is covered in the client/server scenario).
        assertEquals(Collections.singletonList("one.json"),Arrays.asList(dir.list((parent,name) -> name.endsWith(".json"))));
    }
    @Test public void validationFailureAndBackupFailureLeaveTargetBytesUnchanged() throws Exception {
        File f = temp.newFile("safe.json"); byte[] old = "old target".getBytes(StandardCharsets.UTF_8); Files.write(f.toPath(),old);
        try { SafeFileWriter.writeUtf8(f,"bad",candidate -> ReadableCloneFiles.load(candidate)); fail(); } catch (IOException expected) { assertArrayEquals(old,Files.readAllBytes(f.toPath())); }
        File backup = new File(f.getPath() + ".v0.bak"); assertTrue(backup.mkdir());
        try { ReadableCloneFiles.save(f,new NBTTagCompound(),true); fail(); } catch (IOException expected) { assertArrayEquals(old,Files.readAllBytes(f.toPath())); }
    }
    @Test public void sizeDepthAndMalformedUtf8AreRejectedWithoutReplacingTarget() throws Exception {
        StringBuilder deep = new StringBuilder(); for (int i = 0; i < 514; i++) { deep.append('['); } for (int i = 0; i < 514; i++) { deep.append(']'); }
        try { StrictCloneJson.read(deep.toString()); fail(); } catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("512")); }
        File f = temp.newFile("size.json"); byte[] old = "old".getBytes(StandardCharsets.UTF_8); Files.write(f.toPath(),old);
        char[] chars = new char[ReadableClone.MAX_BYTES / 3 + 1]; Arrays.fill(chars,'漢'); NBTTagCompound n = new NBTTagCompound(); n.setString("huge",new String(chars));
        try { ReadableCloneFiles.save(f,n,true); fail(); } catch (IOException expected) { assertArrayEquals(old,Files.readAllBytes(f.toPath())); }
        byte[] prefix = (minimal().substring(0,minimal().length()-1)+",\"appearance\":{\"name\":\"").getBytes(StandardCharsets.UTF_8); ByteArrayOutputStream b = new ByteArrayOutputStream(); b.write(prefix); b.write(0xc3); b.write(0x28); b.write("\"}}".getBytes(StandardCharsets.UTF_8)); Files.write(f.toPath(),b.toByteArray());
        try { ReadableCloneFiles.load(f); fail(); } catch (IOException expected) { /* strict decoder */ }
    }
    @Test public void deterministicGeneratedStructuresKeepTypesAndPresence() {
        for (int seed = 0; seed < 200; seed++) { round(random(new Random(seed),0)); }
    }
    @Test public void parseReadsBothFormatsForCreaturePacks() throws Exception {
        NBTTagCompound n = new NBTTagCompound();
        n.setString("Name", "Pack troll"); n.setInteger("Size", 7); n.setLong("Big", Long.MAX_VALUE);
        assertTrue(ReadableClone.same(n, ReadableCloneFiles.parse(ReadableClone.encode(n))));
        assertTrue(ReadableClone.same(n, ReadableCloneFiles.parse(NBTJsonUtil.Convert(n))));
        try { ReadableCloneFiles.parse("{\"$format\":\"customnpcs-readable-clone\",\"version\":99}"); fail("unknown version must not fall back to legacy"); }
        catch (IllegalArgumentException expected) { }
    }
    private static NBTTagCompound random(Random r,int depth) {
        NBTTagCompound n = new NBTTagCompound();
        for (int i = 0; i < 6; i++) {
            String k = new String[]{"Size","CanSwim","RwRig","","mod/field~.","Scripts"}[i]; int type = r.nextInt(depth < 4 ? 8 : 7); NBTBase v;
            switch(type) {
                case 0: v = new NBTTagLong(r.nextLong()); break; case 1: v = new NBTTagInt(r.nextInt()); break;
                case 2: v = new NBTTagFloat(Float.intBitsToFloat(r.nextInt())); break; case 3: v = new NBTTagDouble(Double.longBitsToDouble(r.nextLong())); break;
                case 4: v = new NBTTagString("\r\n \"\\\t\ud800" + r.nextLong()); break; case 5: v = CloneNbt.emptyList(r.nextInt(13)); break;
                case 6: v = new NBTTagLongArray(new long[]{r.nextLong(),r.nextLong()}); break; default: v = random(r,depth+1);
            } n.setTag(k,v);
        } return n;
    }
}
