/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.client.gui.inventory.GuiInventory
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.client.CPacketCloseWindow
 *  net.minecraftforge.client.event.GuiScreenEvent$InitGuiEvent$Post
 *  net.minecraftforge.fml.client.FMLClientHandler
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package micdoodle8.mods.galacticraft.api.client.tabs;

import java.util.ArrayList;
import java.util.List;
import micdoodle8.mods.galacticraft.api.client.tabs.AbstractTab;
import micdoodle8.mods.galacticraft.api.client.tabs.InventoryTabVanilla;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TabRegistry {
    private static ArrayList<AbstractTab> tabList = new ArrayList();
    private static Class<?> clazzJEIConfig = null;
    public static Class<?> clazzNEIConfig = null;
    private static Minecraft mc;
    private static boolean initWithPotion;

    public static void registerTab(AbstractTab tab) {
        tabList.add(tab);
    }

    public static ArrayList<AbstractTab> getTabList() {
        return tabList;
    }

    public static void addTabsToInventory(GuiContainer gui) {
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public void guiPostInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiInventory) {
            int guiLeft = (event.getGui().field_146294_l - 176) / 2;
            int guiTop = (event.getGui().field_146295_m - 166) / 2;
            TabRegistry.updateTabValues(guiLeft += TabRegistry.getPotionOffset(), guiTop, InventoryTabVanilla.class);
            TabRegistry.addTabsToList(event.getButtonList());
        }
    }

    public static void openInventoryGui() {
        TabRegistry.mc.field_71439_g.field_71174_a.func_147297_a((Packet)new CPacketCloseWindow(TabRegistry.mc.field_71439_g.field_71070_bA.field_75152_c));
        GuiInventory inventory = new GuiInventory((EntityPlayer)TabRegistry.mc.field_71439_g);
        mc.func_147108_a((GuiScreen)inventory);
    }

    public static void updateTabValues(int cornerX, int cornerY, Class<?> selectedButton) {
        int count = 2;
        for (int i = 0; i < tabList.size(); ++i) {
            AbstractTab t = tabList.get(i);
            if (!t.shouldAddToList()) continue;
            t.field_146127_k = count;
            t.field_146128_h = cornerX + (count - 2) * 28;
            t.field_146129_i = cornerY - 28;
            t.field_146124_l = !((Object)((Object)t)).getClass().equals(selectedButton);
            t.potionOffsetLast = TabRegistry.getPotionOffsetNEI();
            ++count;
        }
    }

    public static void addTabsToList(List buttonList) {
        for (AbstractTab tab : tabList) {
            if (!tab.shouldAddToList()) continue;
            buttonList.add(tab);
        }
    }

    public static int getPotionOffset() {
        if (!TabRegistry.mc.field_71439_g.func_70651_bq().isEmpty()) {
            initWithPotion = true;
            return 60 + TabRegistry.getPotionOffsetJEI() + TabRegistry.getPotionOffsetNEI();
        }
        initWithPotion = false;
        return 0;
    }

    public static int getPotionOffsetJEI() {
        if (clazzJEIConfig != null) {
            try {
                Object enabled = clazzJEIConfig.getMethod("isOverlayEnabled", new Class[0]).invoke(null, new Object[0]);
                if (enabled instanceof Boolean) {
                    if (!((Boolean)enabled).booleanValue()) {
                        return 0;
                    }
                    return -60;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return 0;
    }

    public static int getPotionOffsetNEI() {
        if (initWithPotion && clazzNEIConfig != null) {
            try {
                Object hidden = clazzNEIConfig.getMethod("isHidden", new Class[0]).invoke(null, new Object[0]);
                Object enabled = clazzNEIConfig.getMethod("isEnabled", new Class[0]).invoke(null, new Object[0]);
                if (hidden instanceof Boolean && enabled instanceof Boolean) {
                    if (((Boolean)hidden).booleanValue() || !((Boolean)enabled).booleanValue()) {
                        return 0;
                    }
                    return -60;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return 0;
    }

    static {
        try {
            clazzJEIConfig = Class.forName("mezz.jei.config.Config");
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (clazzJEIConfig == null) {
            try {
                clazzNEIConfig = Class.forName("codechicken.nei.NEIClientConfig");
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        mc = FMLClientHandler.instance().getClient();
    }
}

