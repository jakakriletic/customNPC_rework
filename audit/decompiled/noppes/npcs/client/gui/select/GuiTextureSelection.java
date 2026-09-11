/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.resources.AbstractResourcePack
 *  net.minecraft.client.resources.DefaultResourcePack
 *  net.minecraft.client.resources.FallbackResourceManager
 *  net.minecraft.client.resources.IResourcePack
 *  net.minecraft.client.resources.LegacyV2Adapter
 *  net.minecraft.client.resources.ResourcePackRepository
 *  net.minecraft.client.resources.ResourcePackRepository$Entry
 *  net.minecraft.client.resources.SimpleReloadableResourceManager
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.item.crafting.CraftingManager
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.translation.I18n
 *  net.minecraftforge.fml.common.Loader
 *  net.minecraftforge.fml.common.ModContainer
 *  net.minecraftforge.fml.common.ObfuscationReflectionHelper
 */
package noppes.npcs.client.gui.select;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiTextureSelection
extends SubGuiInterface
implements ICustomScrollListener {
    private String up = "..<" + I18n.func_74838_a((String)"gui.up") + ">..";
    private GuiCustomScroll scrollCategories;
    private GuiCustomScroll scrollQuests;
    private String location = "";
    private String selectedDomain;
    public ResourceLocation selectedResource;
    private HashMap<String, List<TextureData>> domains = new HashMap();
    private HashMap<String, TextureData> textures = new HashMap();

    public GuiTextureSelection(EntityNPCInterface npc, String texture) {
        File f;
        this.npc = npc;
        this.drawDefaultBackground = false;
        this.title = "";
        this.setBackground("menubg.png");
        this.xSize = 366;
        this.ySize = 226;
        SimpleReloadableResourceManager simplemanager = (SimpleReloadableResourceManager)Minecraft.func_71410_x().func_110442_L();
        Map map = (Map)ObfuscationReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class, (Object)simplemanager, (int)2);
        HashSet<String> set = new HashSet<String>();
        for (String name : map.keySet()) {
            FallbackResourceManager manager = (FallbackResourceManager)map.get(name);
            List list = (List)ObfuscationReflectionHelper.getPrivateValue(FallbackResourceManager.class, (Object)manager, (int)1);
            for (IResourcePack pack : list) {
                if (pack instanceof LegacyV2Adapter) {
                    pack = (IResourcePack)ObfuscationReflectionHelper.getPrivateValue(LegacyV2Adapter.class, (Object)((LegacyV2Adapter)pack), (int)0);
                }
                if (!(pack instanceof AbstractResourcePack)) continue;
                AbstractResourcePack p = (AbstractResourcePack)pack;
                File file = p.field_110597_b;
                if (file == null) continue;
                set.add(file.getAbsolutePath());
            }
        }
        for (String file : set) {
            File f2 = new File(file);
            if (f2.isDirectory()) {
                this.checkFolder(new File(f2, "assets"), f2.getAbsolutePath().length());
                continue;
            }
            this.progressFile(f2);
        }
        for (ModContainer mod : Loader.instance().getModList()) {
            if (!mod.getSource().exists()) continue;
            this.progressFile(mod.getSource());
        }
        ResourcePackRepository repos = Minecraft.func_71410_x().func_110438_M();
        repos.func_110611_a();
        List list = repos.func_110613_c();
        if (repos.func_148530_e() != null) {
            AbstractResourcePack p = (AbstractResourcePack)repos.func_148530_e();
            File file = p.field_110597_b;
            if (file != null) {
                this.progressFile(file);
            }
        }
        for (ResourcePackRepository.Entry entry : list) {
            File file = new File(repos.func_110612_e(), entry.func_110515_d());
            if (!file.exists()) continue;
            this.progressFile(file);
        }
        this.checkFolder(new File(CustomNpcs.Dir, "assets"), CustomNpcs.Dir.getAbsolutePath().length());
        URL url = DefaultResourcePack.class.getResource("/");
        if (url != null) {
            f = this.decodeFile(url.getFile());
            if (f.isDirectory()) {
                this.checkFolder(new File(f, "assets"), url.getFile().length());
            } else {
                this.progressFile(f);
            }
        }
        if ((url = CraftingManager.class.getResource("/assets/.mcassetsroot")) != null) {
            f = this.decodeFile(url.getFile());
            if (f.isDirectory()) {
                this.checkFolder(new File(f, "assets"), url.getFile().length());
            } else {
                this.progressFile(f);
            }
        }
        if (texture != null && !texture.isEmpty()) {
            this.selectedResource = new ResourceLocation(texture);
            this.selectedDomain = this.selectedResource.func_110624_b();
            if (!this.domains.containsKey(this.selectedDomain)) {
                this.selectedDomain = null;
            }
            int i = this.selectedResource.func_110623_a().lastIndexOf(47);
            this.location = this.selectedResource.func_110623_a().substring(9, i + 1);
        }
    }

    private File decodeFile(String url) {
        int i;
        if (url.startsWith("file:")) {
            url = url.substring(5);
        }
        if ((i = (url = url.replace('/', File.separatorChar)).indexOf(33)) > 0) {
            url = url.substring(0, i);
        }
        try {
            url = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException unsupportedEncodingException) {
            // empty catch block
        }
        return new File(url);
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.title = this.selectedDomain != null ? this.selectedDomain + ":" + this.location : "";
        this.addButton(new GuiNpcButton(2, this.guiLeft + 264, this.guiTop + 170, 90, 20, "gui.done"));
        this.addButton(new GuiNpcButton(1, this.guiLeft + 264, this.guiTop + 190, 90, 20, "gui.cancel"));
        if (this.scrollCategories == null) {
            this.scrollCategories = new GuiCustomScroll(this, 0);
            this.scrollCategories.setSize(120, 200);
        }
        if (this.selectedDomain == null) {
            this.scrollCategories.setList(Lists.newArrayList(this.domains.keySet()));
            if (this.selectedDomain != null) {
                this.scrollCategories.setSelected(this.selectedDomain);
            }
        } else {
            ArrayList<String> list = new ArrayList<String>();
            list.add(this.up);
            List<TextureData> data = this.domains.get(this.selectedDomain);
            for (TextureData td : data) {
                String path;
                int i;
                if (!this.location.isEmpty() && (!td.path.startsWith(this.location) || td.path.equals(this.location)) || (i = (path = td.path.substring(this.location.length())).indexOf(47)) < 0 || (path = path.substring(0, i)).isEmpty() || list.contains(path)) continue;
                list.add(path);
            }
            this.scrollCategories.setList(list);
        }
        this.scrollCategories.guiLeft = this.guiLeft + 4;
        this.scrollCategories.guiTop = this.guiTop + 14;
        this.addScroll(this.scrollCategories);
        if (this.scrollQuests == null) {
            this.scrollQuests = new GuiCustomScroll(this, 1);
            this.scrollQuests.setSize(130, 200);
        }
        if (this.selectedDomain != null) {
            this.textures.clear();
            List<TextureData> data = this.domains.get(this.selectedDomain);
            ArrayList<String> list = new ArrayList<String>();
            String loc = this.location;
            if (this.scrollCategories.hasSelected() && !this.scrollCategories.getSelected().equals(this.up)) {
                loc = loc + this.scrollCategories.getSelected() + '/';
            }
            for (TextureData td : data) {
                if (!td.path.equals(loc) || list.contains(td.name)) continue;
                list.add(td.name);
                this.textures.put(td.name, td);
            }
            this.scrollQuests.setList(list);
        }
        if (this.selectedResource != null) {
            this.scrollQuests.setSelected(this.selectedResource.func_110623_a());
        }
        this.scrollQuests.guiLeft = this.guiLeft + 125;
        this.scrollQuests.guiTop = this.guiTop + 14;
        this.addScroll(this.scrollQuests);
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        super.func_146284_a(guibutton);
        if (guibutton.field_146127_k == 2) {
            this.npc.display.setSkinTexture(this.selectedResource.toString());
        }
        this.npc.textureLocation = null;
        this.close();
        this.parent.func_73866_w_();
    }

    @Override
    public void func_73863_a(int i, int j, float f) {
        super.func_73863_a(i, j, f);
        this.npc.textureLocation = this.selectedResource;
        this.drawNpc((EntityLivingBase)this.npc, this.guiLeft + 276, this.guiTop + 140, 2.0f, 0);
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll scroll) {
        if (scroll == this.scrollQuests) {
            if (scroll.id == 1) {
                TextureData data = this.textures.get(scroll.getSelected());
                this.selectedResource = new ResourceLocation(this.selectedDomain, data.absoluteName);
            }
        } else {
            this.func_73866_w_();
        }
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
        if (scroll == this.scrollCategories) {
            if (this.selectedDomain == null) {
                this.selectedDomain = selection;
            } else if (selection.equals(this.up)) {
                int i = this.location.lastIndexOf(47, this.location.length() - 2);
                if (i < 0) {
                    if (this.location.isEmpty()) {
                        this.selectedDomain = null;
                    }
                    this.location = "";
                } else {
                    this.location = this.location.substring(0, i + 1);
                }
            } else {
                this.location = this.location + selection + '/';
            }
            this.scrollCategories.selected = -1;
            this.scrollQuests.selected = -1;
            this.func_73866_w_();
        } else {
            this.npc.display.setSkinTexture(this.selectedResource.toString());
            this.close();
            this.parent.func_73866_w_();
        }
    }

    private void progressFile(File file) {
        try {
            if (!file.isDirectory() && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))) {
                ZipFile zip = new ZipFile(file);
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipentry = entries.nextElement();
                    String entryName = zipentry.getName();
                    this.addFile(entryName);
                }
                zip.close();
            } else if (file.isDirectory()) {
                int length = file.getAbsolutePath().length();
                this.checkFolder(file, length);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkFolder(File file, int length) {
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String name = f.getAbsolutePath().substring(length);
            if (!(name = name.replace("\\", "/")).startsWith("/")) {
                name = "/" + name;
            }
            if (f.isDirectory()) {
                this.addFile(name + "/");
                this.checkFolder(f, length);
                continue;
            }
            this.addFile(name);
        }
    }

    private void addFile(String name) {
        if ((name = name.toLowerCase()).startsWith("/")) {
            name = name.substring(1);
        }
        if (!name.startsWith("assets/") || !name.toLowerCase().endsWith(".png")) {
            return;
        }
        name = name.substring(7);
        int i = name.indexOf(47);
        String domain = name.substring(0, i);
        name = name.substring(i + 10);
        List<TextureData> list = this.domains.get(domain);
        if (list == null) {
            list = new ArrayList<TextureData>();
            this.domains.put(domain, list);
        }
        boolean contains = false;
        for (TextureData data : list) {
            if (!data.absoluteName.equals(name)) continue;
            contains = true;
            break;
        }
        if (!contains) {
            list.add(new TextureData(domain, name));
        }
    }

    class TextureData {
        String domain;
        String absoluteName;
        String name;
        String path;

        public TextureData(String domain, String absoluteName) {
            this.domain = domain;
            int i = absoluteName.lastIndexOf(47);
            this.name = absoluteName.substring(i + 1);
            this.path = absoluteName.substring(0, i + 1);
            this.absoluteName = "textures/" + absoluteName;
        }
    }
}

