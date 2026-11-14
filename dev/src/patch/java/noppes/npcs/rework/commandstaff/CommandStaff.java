package noppes.npcs.rework.commandstaff;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.CustomNpcs;

public final class CommandStaff extends Item {
    public static final ResourceLocation ID = new ResourceLocation("customnpcs", "command_staff");
    private static CommandStaff item;

    private CommandStaff() {
        setRegistryName(ID);
        setUnlocalizedName("command_staff");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    public static boolean holding(EntityPlayer player) {
        return CustomNpcs.RwCommandStaff == 1 && item != null && player.getHeldItemMainhand().getItem() == item;
    }

    public static Item item() { return item; }

    @Override public String getItemStackDisplayName(ItemStack stack) { return "Command Staff"; }

    public static final class Registration {
        @SubscribeEvent public void register(RegistryEvent.Register<Item> event) {
            if (CustomNpcs.RwCommandStaff == 1 || FMLCommonHandler.instance().getSide().isClient()) {
                item = new CommandStaff();
                event.getRegistry().register(item);
            }
        }

        @SubscribeEvent public void missing(RegistryEvent.MissingMappings<Item> event) {
            for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
                if (ID.equals(mapping.key)) { mapping.ignore(); }
            }
        }
    }
}
