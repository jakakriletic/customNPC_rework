/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.entity.RenderLivingBase
 *  net.minecraft.client.renderer.texture.TextureMap
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.common.registry.EntityEntry
 *  net.minecraftforge.fml.common.registry.ForgeRegistries
 */
package noppes.npcs.client.gui.model;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.entity.NPCRendererHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.entity.EntityNPC64x32;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityNpcAlex;
import noppes.npcs.entity.EntityNpcClassicPlayer;

public class GuiCreationEntities
extends GuiCreationScreenInterface
implements ICustomScrollListener {
    public HashMap<String, Class<? extends EntityLivingBase>> data = new HashMap();
    private List<String> list;
    private GuiCustomScroll scroll;
    private boolean resetToSelected = true;

    public GuiCreationEntities(EntityNPCInterface npc) {
        super(npc);
        for (EntityEntry ent : ForgeRegistries.ENTITIES.getValues()) {
            String name = ent.getName();
            Class c = ent.getEntityClass();
            try {
                String s;
                if (!EntityLiving.class.isAssignableFrom(c) || c.getConstructor(World.class) == null || Modifier.isAbstract(c.getModifiers()) || !(Minecraft.getMinecraft().getRenderManager().getEntityClassRenderObject(c) instanceof RenderLivingBase) || (s = name).toLowerCase().contains("customnpc")) continue;
                this.data.put(name, c.asSubclass(EntityLivingBase.class));
            }
            catch (SecurityException e) {
                e.printStackTrace();
            }
            catch (NoSuchMethodException noSuchMethodException) {}
        }
        this.data.put("NPC 64x32", EntityNPC64x32.class);
        this.data.put("NPC Alex Arms", EntityNpcAlex.class);
        this.data.put("NPC Classic Player", EntityNpcClassicPlayer.class);
        this.list = new ArrayList<String>(this.data.keySet());
        this.list.add("NPC");
        Collections.sort(this.list, String.CASE_INSENSITIVE_ORDER);
        this.active = 1;
        this.xOffset = 60;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addButton(new GuiNpcButton(10, this.guiLeft, this.guiTop + 46, 120, 20, "Reset To NPC"));
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
            this.scroll.setUnsortedList(this.list);
        }
        this.scroll.guiLeft = this.guiLeft;
        this.scroll.guiTop = this.guiTop + 68;
        this.scroll.setSize(100, this.ySize - 96);
        String selected = "NPC";
        if (this.entity != null) {
            for (Map.Entry<String, Class<? extends EntityLivingBase>> en : this.data.entrySet()) {
                if (!en.getValue().toString().equals(this.entity.getClass().toString())) continue;
                selected = en.getKey();
            }
        }
        this.scroll.setSelected(selected);
        if (this.resetToSelected) {
            this.scroll.scrollTo(this.scroll.getSelected());
            this.resetToSelected = false;
        }
        this.addScroll(this.scroll);
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        if (btn.id == 10) {
            this.playerdata.setEntityClass(null);
            this.resetToSelected = true;
            this.initGui();
        }
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll scroll) {
        this.playerdata.setEntityClass(this.data.get(scroll.getSelected()));
        EntityLivingBase entity = this.playerdata.getEntity(this.npc);
        if (entity != null) {
            RenderLivingBase render = (RenderLivingBase)this.mc.getRenderManager().getEntityClassRenderObject(entity.getClass());
            if (!NPCRendererHelper.getTexture(render, (Entity)entity).equals(TextureMap.LOCATION_MISSING_TEXTURE.toString())) {
                this.npc.display.setSkinTexture(NPCRendererHelper.getTexture(render, (Entity)entity));
            }
        } else {
            this.npc.display.setSkinTexture("customnpcs:textures/entity/humanmale/steve.png");
        }
        this.initGui();
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }
}

