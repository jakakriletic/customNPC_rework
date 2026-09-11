/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.client.resources.FolderResourcePack
 *  net.minecraft.client.resources.IResourceManager
 *  net.minecraft.client.resources.IResourceManagerReloadListener
 *  net.minecraft.client.resources.IResourcePack
 *  net.minecraft.client.resources.SimpleReloadableResourceManager
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.TextureCache;

public class CustomNpcResourceListener
implements IResourceManagerReloadListener {
    public static int DefaultTextColor = 0x404040;

    public void func_110549_a(IResourceManager var1) {
        if (var1 instanceof SimpleReloadableResourceManager) {
            this.createTextureCache();
            SimpleReloadableResourceManager simplemanager = (SimpleReloadableResourceManager)var1;
            FolderResourcePack pack = new FolderResourcePack(CustomNpcs.Dir);
            simplemanager.func_110545_a((IResourcePack)pack);
            try {
                DefaultTextColor = Integer.parseInt(I18n.func_74838_a((String)"customnpcs.defaultTextColor"), 16);
            }
            catch (NumberFormatException e) {
                DefaultTextColor = 0x404040;
            }
        }
    }

    private void createTextureCache() {
        this.enlargeTexture("planks_oak");
        this.enlargeTexture("planks_big_oak");
        this.enlargeTexture("planks_birch");
        this.enlargeTexture("planks_jungle");
        this.enlargeTexture("planks_spruce");
        this.enlargeTexture("planks_acacia");
        this.enlargeTexture("iron_block");
        this.enlargeTexture("diamond_block");
        this.enlargeTexture("stone");
        this.enlargeTexture("gold_block");
        this.enlargeTexture("wool_colored_white");
    }

    private void enlargeTexture(String texture) {
        TextureManager manager = Minecraft.func_71410_x().func_110434_K();
        if (manager == null) {
            return;
        }
        ResourceLocation location = new ResourceLocation("customnpcs:textures/cache/" + texture + ".png");
        Object ob = manager.func_110581_b(location);
        if (ob == null || !(ob instanceof TextureCache)) {
            ob = new TextureCache(location);
            manager.func_110579_a(location, ob);
        }
        ((TextureCache)((Object)ob)).setImage(new ResourceLocation("textures/blocks/" + texture + ".png"));
    }
}

