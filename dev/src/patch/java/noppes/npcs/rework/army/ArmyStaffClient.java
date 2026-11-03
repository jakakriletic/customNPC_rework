package noppes.npcs.rework.army;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ArmyStaffClient {
    private static String armed;
    private static int shape, stance;
    public static void installModel() { MinecraftForge.EVENT_BUS.register(new ArmyStaffClient()); }
    @SubscribeEvent public void model(ModelRegistryEvent e) {
        ModelLoader.setCustomModelResourceLocation(ArmyStaff.item, 0, new ModelResourceLocation("customnpcs:npcwand", "inventory"));
    }
    public static void clear() { armed = null; }
    public static void arm(String name, int form, int intent) {
        armed = name; shape = form; stance = intent;
        Minecraft.getMinecraft().displayGuiScreen(null);
        Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Army " + name + ": look at ground and right-click. Sneak + right-click cancels."));
    }
    public static void use(int npc) {
        Minecraft mc = Minecraft.getMinecraft();
        if (armed != null && !mc.player.isSneaking()) {
            ArmyClient.action(ArmyStaffCodec.Op.MOVE, -1, armed, shape, stance);
            armed = null;
        } else {
            armed = null; mc.displayGuiScreen(new GuiArmyStaff(npc));
        }
    }
    public static void received(ArmyStaffCodec.Response reply) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof GuiArmyStaff) { ((GuiArmyStaff)mc.currentScreen).received(reply); }
        else if (mc.player != null) { mc.player.sendMessage(new TextComponentString(reply.text)); }
    }
}
