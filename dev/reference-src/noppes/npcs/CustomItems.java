/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockDispenser
 *  net.minecraft.block.BlockDoor
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.client.renderer.block.model.ModelResourceLocation
 *  net.minecraft.client.renderer.block.statemap.IStateMapper
 *  net.minecraft.client.renderer.block.statemap.StateMap$Builder
 *  net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
 *  net.minecraft.creativetab.CreativeTabs
 *  net.minecraft.dispenser.BehaviorDefaultDispenseItem
 *  net.minecraft.dispenser.IBlockSource
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.IRecipe
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.math.BlockPos
 *  net.minecraftforge.client.ForgeHooksClient
 *  net.minecraftforge.client.event.ModelRegistryEvent
 *  net.minecraftforge.client.model.ModelLoader
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.RegistryEvent$Register
 *  net.minecraftforge.fml.client.registry.ClientRegistry
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.registry.GameRegistry
 *  net.minecraftforge.fml.common.registry.GameRegistry$ObjectHolder
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  net.minecraftforge.registries.IForgeRegistryEntry
 */
package noppes.npcs;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistryEntry;
import noppes.npcs.CreativeTabNpcs;
import noppes.npcs.blocks.BlockBorder;
import noppes.npcs.blocks.BlockBuilder;
import noppes.npcs.blocks.BlockCarpentryBench;
import noppes.npcs.blocks.BlockCopy;
import noppes.npcs.blocks.BlockMailbox;
import noppes.npcs.blocks.BlockNpcRedstone;
import noppes.npcs.blocks.BlockScripted;
import noppes.npcs.blocks.BlockScriptedDoor;
import noppes.npcs.blocks.BlockWaypoint;
import noppes.npcs.blocks.tiles.TileBlockAnvil;
import noppes.npcs.blocks.tiles.TileBorder;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.blocks.tiles.TileCopy;
import noppes.npcs.blocks.tiles.TileDoor;
import noppes.npcs.blocks.tiles.TileMailbox;
import noppes.npcs.blocks.tiles.TileMailbox2;
import noppes.npcs.blocks.tiles.TileMailbox3;
import noppes.npcs.blocks.tiles.TileRedstoneBlock;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.blocks.tiles.TileWaypoint;
import noppes.npcs.client.renderer.blocks.BlockCarpentryBenchRenderer;
import noppes.npcs.client.renderer.blocks.BlockCopyRenderer;
import noppes.npcs.client.renderer.blocks.BlockDoorRenderer;
import noppes.npcs.client.renderer.blocks.BlockMailboxRenderer;
import noppes.npcs.client.renderer.blocks.BlockScriptedRenderer;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.items.ItemMounter;
import noppes.npcs.items.ItemNbtBook;
import noppes.npcs.items.ItemNpcBlock;
import noppes.npcs.items.ItemNpcCloner;
import noppes.npcs.items.ItemNpcMovingPath;
import noppes.npcs.items.ItemNpcScripter;
import noppes.npcs.items.ItemNpcWand;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.items.ItemScriptedDoor;
import noppes.npcs.items.ItemSoulstoneEmpty;
import noppes.npcs.items.ItemSoulstoneFilled;
import noppes.npcs.items.ItemTeleporter;

@GameRegistry.ObjectHolder(value="customnpcs")
public class CustomItems {
    @GameRegistry.ObjectHolder(value="npcwand")
    public static final Item wand = null;
    @GameRegistry.ObjectHolder(value="npcmobcloner")
    public static final Item cloner = null;
    @GameRegistry.ObjectHolder(value="npcscripter")
    public static final Item scripter = null;
    @GameRegistry.ObjectHolder(value="npcmovingpath")
    public static final Item moving = null;
    @GameRegistry.ObjectHolder(value="npcmounter")
    public static final Item mount = null;
    @GameRegistry.ObjectHolder(value="npcteleporter")
    public static final Item teleporter = null;
    @GameRegistry.ObjectHolder(value="npcscripteddoortool")
    public static final Item scriptedDoorTool = null;
    @GameRegistry.ObjectHolder(value="scripted_item")
    public static final ItemScripted scripted_item = null;
    @GameRegistry.ObjectHolder(value="nbt_book")
    public static final ItemNbtBook nbt_book = null;
    @GameRegistry.ObjectHolder(value="npcsoulstoneempty")
    public static final Item soulstoneEmpty = null;
    @GameRegistry.ObjectHolder(value="npcsoulstonefilled")
    public static final Item soulstoneFull = null;
    @GameRegistry.ObjectHolder(value="npcredstoneblock")
    public static final Block redstoneBlock = null;
    @GameRegistry.ObjectHolder(value="npcmailbox")
    public static final Block mailbox = null;
    @GameRegistry.ObjectHolder(value="npcwaypoint")
    public static final Block waypoint = null;
    @GameRegistry.ObjectHolder(value="npcborder")
    public static final Block border = null;
    @GameRegistry.ObjectHolder(value="npcscripted")
    public static final Block scripted = null;
    @GameRegistry.ObjectHolder(value="npcscripteddoor")
    public static final Block scriptedDoor = null;
    @GameRegistry.ObjectHolder(value="npcbuilderblock")
    public static final Block builder = null;
    @GameRegistry.ObjectHolder(value="npccopyblock")
    public static final Block copy = null;
    @GameRegistry.ObjectHolder(value="npccarpentybench")
    public static final Block carpentyBench = null;
    public static CreativeTabNpcs tab = new CreativeTabNpcs("cnpcs");

    public static void load() {
        MinecraftForge.EVENT_BUS.register((Object)new CustomItems());
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        GameRegistry.registerTileEntity(TileRedstoneBlock.class, (String)"TileRedstoneBlock");
        GameRegistry.registerTileEntity(TileBlockAnvil.class, (String)"TileBlockAnvil");
        GameRegistry.registerTileEntity(TileMailbox.class, (String)"TileMailbox");
        GameRegistry.registerTileEntity(TileWaypoint.class, (String)"TileWaypoint");
        GameRegistry.registerTileEntity(TileScripted.class, (String)"TileNPCScripted");
        GameRegistry.registerTileEntity(TileScriptedDoor.class, (String)"TileNPCScriptedDoor");
        GameRegistry.registerTileEntity(TileBuilder.class, (String)"TileNPCBuilder");
        GameRegistry.registerTileEntity(TileCopy.class, (String)"TileNPCCopy");
        GameRegistry.registerTileEntity(TileBorder.class, (String)"TileNPCBorder");
        Block redstoneBlock = new BlockNpcRedstone().setHardness(50.0f).setResistance(2000.0f).setUnlocalizedName("npcredstoneblock").setCreativeTab((CreativeTabs)tab);
        Block mailbox = new BlockMailbox().setUnlocalizedName("npcmailbox").setHardness(5.0f).setResistance(10.0f).setCreativeTab((CreativeTabs)tab);
        Block waypoint = new BlockWaypoint().setUnlocalizedName("npcwaypoint").setHardness(5.0f).setResistance(10.0f).setCreativeTab((CreativeTabs)tab);
        Block border = new BlockBorder().setUnlocalizedName("npcborder").setHardness(5.0f).setResistance(10.0f).setCreativeTab((CreativeTabs)tab);
        Block scripted = new BlockScripted().setUnlocalizedName("npcscripted").setHardness(5.0f).setResistance(10.0f).setCreativeTab((CreativeTabs)tab);
        Block scriptedDoor = new BlockScriptedDoor().setUnlocalizedName("npcscripteddoor").setHardness(5.0f).setResistance(10.0f);
        Block builder = new BlockBuilder().setUnlocalizedName("npcbuilderblock").setHardness(5.0f).setResistance(10.0f).setCreativeTab((CreativeTabs)tab);
        Block copy = new BlockCopy().setUnlocalizedName("npccopyblock").setHardness(5.0f).setResistance(10.0f).setCreativeTab((CreativeTabs)tab);
        Block carpentyBench = new BlockCarpentryBench().setUnlocalizedName("npccarpentybench").setHardness(5.0f).setResistance(10.0f).setCreativeTab((CreativeTabs)tab);
        event.getRegistry().registerAll((IForgeRegistryEntry[])new Block[]{redstoneBlock, carpentyBench, mailbox, waypoint, border, scripted, scriptedDoor, builder, copy});
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        Item wand = new ItemNpcWand().setUnlocalizedName("npcwand").setFull3D();
        Item cloner = new ItemNpcCloner().setUnlocalizedName("npcmobcloner").setFull3D();
        Item scripter = new ItemNpcScripter().setUnlocalizedName("npcscripter").setFull3D();
        Item moving = new ItemNpcMovingPath().setUnlocalizedName("npcmovingpath").setFull3D();
        Item mount = new ItemMounter().setUnlocalizedName("npcmounter").setFull3D();
        Item teleporter = new ItemTeleporter().setUnlocalizedName("npcteleporter").setFull3D();
        Item scriptedDoorTool = new ItemScriptedDoor(scriptedDoor).setUnlocalizedName("npcscripteddoortool").setFull3D();
        Item soulstoneEmpty = new ItemSoulstoneEmpty().setUnlocalizedName("npcsoulstoneempty").setCreativeTab((CreativeTabs)tab);
        Item soulstoneFull = new ItemSoulstoneFilled().setUnlocalizedName("npcsoulstonefilled");
        Item scripted_item = new ItemScripted().setUnlocalizedName("scripted_item");
        Item nbt_book = new ItemNbtBook().setUnlocalizedName("nbt_book");
        event.getRegistry().registerAll((IForgeRegistryEntry[])new Item[]{wand, cloner, scripter, moving, mount, teleporter, scriptedDoorTool, soulstoneEmpty, soulstoneFull, scripted_item, nbt_book});
        event.getRegistry().registerAll((IForgeRegistryEntry[])new Item[]{new ItemNpcBlock(redstoneBlock), new ItemNpcBlock(carpentyBench), new ItemNpcBlock(mailbox).setHasSubtypes(true), new ItemNpcBlock(waypoint), new ItemNpcBlock(border), new ItemNpcBlock(scripted), new ItemNpcBlock(scriptedDoor), new ItemNpcBlock(builder), new ItemNpcBlock(copy)});
        CustomItems.tab.item = wand;
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject((Object)soulstoneFull, (Object)new BehaviorDefaultDispenseItem(){

            public ItemStack dispenseStack(IBlockSource source, ItemStack item) {
                EnumFacing enumfacing = (EnumFacing)source.getBlockState().getValue((IProperty)BlockDispenser.FACING);
                double x = source.getX() + (double)enumfacing.getFrontOffsetX();
                double z = source.getZ() + (double)enumfacing.getFrontOffsetZ();
                ItemSoulstoneFilled.Spawn(null, item, source.getWorld(), new BlockPos(x, source.getY(), z));
                item.splitStack(1);
                return item;
            }
        });
    }

    @SubscribeEvent
    public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        RecipeController.Registry = event.getRegistry();
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomStateMapper((Block)mailbox, (IStateMapper)new StateMap.Builder().ignore(new IProperty[]{BlockMailbox.ROTATION, BlockMailbox.TYPE}).build());
        ModelLoader.setCustomStateMapper((Block)scriptedDoor, (IStateMapper)new StateMap.Builder().ignore(new IProperty[]{BlockDoor.POWERED}).build());
        ModelLoader.setCustomStateMapper((Block)builder, (IStateMapper)new StateMap.Builder().ignore(new IProperty[]{BlockBuilder.ROTATION}).build());
        ModelLoader.setCustomStateMapper((Block)carpentyBench, (IStateMapper)new StateMap.Builder().ignore(new IProperty[]{BlockCarpentryBench.ROTATION}).build());
        ModelLoader.setCustomModelResourceLocation((Item)wand, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:npcwand", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)cloner, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:npcmobcloner", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)scripter, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:npcscripter", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)moving, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:npcmovingpath", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)mount, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:npcmounter", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)teleporter, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:npcteleporter", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)scriptedDoorTool, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:npcscripteddoortool", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)soulstoneEmpty, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:npcsoulstoneempty", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)soulstoneFull, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:npcsoulstonefilled", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)scripted_item, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:scripted_item", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)nbt_book, (int)0, (ModelResourceLocation)new ModelResourceLocation("customnpcs:nbt_book", "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)redstoneBlock), (int)0, (ModelResourceLocation)new ModelResourceLocation(redstoneBlock.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)mailbox), (int)0, (ModelResourceLocation)new ModelResourceLocation(mailbox.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)mailbox), (int)1, (ModelResourceLocation)new ModelResourceLocation(mailbox.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)mailbox), (int)2, (ModelResourceLocation)new ModelResourceLocation(mailbox.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)waypoint), (int)0, (ModelResourceLocation)new ModelResourceLocation(waypoint.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)border), (int)0, (ModelResourceLocation)new ModelResourceLocation(border.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)scripted), (int)0, (ModelResourceLocation)new ModelResourceLocation(scripted.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)scriptedDoor), (int)0, (ModelResourceLocation)new ModelResourceLocation(scriptedDoor.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)builder), (int)0, (ModelResourceLocation)new ModelResourceLocation(builder.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)copy), (int)0, (ModelResourceLocation)new ModelResourceLocation(copy.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation((Item)Item.getItemFromBlock((Block)carpentyBench), (int)0, (ModelResourceLocation)new ModelResourceLocation(carpentyBench.getRegistryName(), "inventory"));
        ClientRegistry.bindTileEntitySpecialRenderer(TileBlockAnvil.class, (TileEntitySpecialRenderer)new BlockCarpentryBenchRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileMailbox.class, (TileEntitySpecialRenderer)new BlockMailboxRenderer(0));
        ClientRegistry.bindTileEntitySpecialRenderer(TileMailbox2.class, (TileEntitySpecialRenderer)new BlockMailboxRenderer(1));
        ClientRegistry.bindTileEntitySpecialRenderer(TileMailbox3.class, (TileEntitySpecialRenderer)new BlockMailboxRenderer(2));
        ClientRegistry.bindTileEntitySpecialRenderer(TileScripted.class, (TileEntitySpecialRenderer)new BlockScriptedRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileDoor.class, (TileEntitySpecialRenderer)new BlockDoorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileCopy.class, (TileEntitySpecialRenderer)new BlockCopyRenderer());
        ForgeHooksClient.registerTESRItemStack((Item)Item.getItemFromBlock((Block)carpentyBench), (int)0, TileBlockAnvil.class);
        ForgeHooksClient.registerTESRItemStack((Item)Item.getItemFromBlock((Block)mailbox), (int)0, TileMailbox.class);
        ForgeHooksClient.registerTESRItemStack((Item)Item.getItemFromBlock((Block)mailbox), (int)1, TileMailbox2.class);
        ForgeHooksClient.registerTESRItemStack((Item)Item.getItemFromBlock((Block)mailbox), (int)2, TileMailbox3.class);
    }
}

