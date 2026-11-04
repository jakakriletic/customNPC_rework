package noppes.npcs.rework.army;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Q435: a client always registers it (saves, client 0 on server 1; an entry the server lacks is simply
 * inactive for that session). A dedicated server registers it only with RwArmies>=1, because Forge refuses
 * a client that lacks an entry of the server, so an original client could not join a server at 0.
 * Inert with RwArmies=0.
 */
public final class ArmyStaff extends Item {
    private static final net.minecraft.util.ResourceLocation REGISTRY_NAME = new net.minecraft.util.ResourceLocation("customnpcs", "army_staff");
    public static ArmyStaff item;
    private ArmyStaff() {
        setRegistryName(REGISTRY_NAME); setUnlocalizedName("army_staff"); setMaxStackSize(1);
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.TOOLS);
    }
    public static final class Registration {
        @SubscribeEvent public void items(RegistryEvent.Register<Item> event) {
            if (noppes.npcs.CustomNpcs.RwArmies >= 1
                    || net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide().isClient()) {
                item = new ArmyStaff(); event.getRegistry().register(item);
            }
        }
        /** A world saved with the entry must still start on a dedicated server at 0, without a console query. */
        @SubscribeEvent public void missing(RegistryEvent.MissingMappings<Item> event) {
            for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
                if (REGISTRY_NAME.equals(mapping.key)) { mapping.ignore(); }
            }
        }
        @SubscribeEvent public void interact(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract e) {
            if (active(e) && e.getTarget() instanceof EntityNPCInterface) {
                if (e.getWorld().isRemote) { open(e.getTarget().getEntityId()); }
                e.setCanceled(true); e.setCancellationResult(EnumActionResult.SUCCESS);
            }
        }
        @SubscribeEvent public void interactAt(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific e) {
            if (active(e) && e.getTarget() instanceof EntityNPCInterface) {
                if (e.getWorld().isRemote) { open(e.getTarget().getEntityId()); }
                e.setCanceled(true); e.setCancellationResult(EnumActionResult.SUCCESS);
            }
        }
        @SubscribeEvent public void block(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
            if (active(e)) {
                if (e.getWorld().isRemote) { open(-1); }
                e.setCanceled(true); e.setCancellationResult(EnumActionResult.SUCCESS);
            }
        }
        private boolean active(net.minecraftforge.event.entity.player.PlayerInteractEvent e) {
            return noppes.npcs.CustomNpcs.RwArmies >= 1 && e.getHand() == EnumHand.MAIN_HAND
                    && e.getEntityPlayer().getHeldItemMainhand().getItem() == item;
        }
    }
    @Override public String getItemStackDisplayName(ItemStack stack) { return "Army Command Staff"; }
    private static void open(int npc) {
        if (noppes.npcs.CustomNpcs.RwArmies < 1) { return; }
        try { Class.forName("noppes.npcs.rework.army.ArmyStaffClient").getMethod("use", int.class).invoke(null, npc); }
        catch (ReflectiveOperationException e) { noppes.npcs.LogWriter.error("RWARMY staff client", e); }
    }
    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (hand == EnumHand.MAIN_HAND && world.isRemote) { open(-1); }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
    @Override public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        if (!(target instanceof EntityNPCInterface) || hand != EnumHand.MAIN_HAND) { return false; }
        if (player.world.isRemote) { open(target.getEntityId()); }
        return true;
    }
}
