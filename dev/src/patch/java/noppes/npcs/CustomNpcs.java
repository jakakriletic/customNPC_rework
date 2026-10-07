/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockIce
 *  net.minecraft.block.BlockLeaves
 *  net.minecraft.block.BlockVine
 *  net.minecraft.client.Minecraft
 *  net.minecraft.command.ICommand
 *  net.minecraft.entity.SharedMonsterAttributes
 *  net.minecraft.entity.ai.attributes.RangedAttribute
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.common.ForgeChunkManager
 *  net.minecraftforge.common.ForgeChunkManager$LoadingCallback
 *  net.minecraftforge.common.ForgeModContainer
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.common.capabilities.Capability
 *  net.minecraftforge.common.capabilities.Capability$IStorage
 *  net.minecraftforge.common.capabilities.CapabilityManager
 *  net.minecraftforge.common.util.FakePlayer
 *  net.minecraftforge.fml.common.Mod
 *  net.minecraftforge.fml.common.Mod$EventHandler
 *  net.minecraftforge.fml.common.ObfuscationReflectionHelper
 *  net.minecraftforge.fml.common.SidedProxy
 *  net.minecraftforge.fml.common.event.FMLInitializationEvent
 *  net.minecraftforge.fml.common.event.FMLPreInitializationEvent
 *  net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent
 *  net.minecraftforge.fml.common.event.FMLServerStartedEvent
 *  net.minecraftforge.fml.common.event.FMLServerStartingEvent
 *  net.minecraftforge.fml.common.event.FMLServerStoppedEvent
 *  net.minecraftforge.fml.common.network.FMLEventChannel
 *  net.minecraftforge.fml.common.network.IGuiHandler
 *  net.minecraftforge.fml.common.network.NetworkRegistry
 */
package noppes.npcs;

import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.Block;
import net.minecraft.block.BlockIce;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockVine;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.nbt.NBTBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import noppes.npcs.AbilityEventHandler;
import noppes.npcs.CommonProxy;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.LogWriter;
import noppes.npcs.ScriptItemEventHandler;
import noppes.npcs.ScriptPlayerEventHandler;
import noppes.npcs.ServerEventsHandler;
import noppes.npcs.ServerTickHandler;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.api.wrapper.WrapperEntityData;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.command.CommandNoppes;
import noppes.npcs.config.ConfigLoader;
import noppes.npcs.config.ConfigProp;
import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.ChunkController;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.GlobalDataController;
import noppes.npcs.controllers.LinkedNpcController;
import noppes.npcs.controllers.MassBlockController;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.controllers.SpawnController;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.rework.data.WorldSaveSession;
import noppes.npcs.rework.ai.AttackPriority;
import noppes.npcs.rework.ai.CommandRwAttack;
import noppes.npcs.rework.ai.CommandRwTarget;
import noppes.npcs.rework.ai.TargetPrefilter;
import noppes.npcs.rework.diag.CommandRwDiag;
import noppes.npcs.rework.diag.DiagChunkLoader;
import noppes.npcs.rework.diag.DiagEventCollector;
import noppes.npcs.rework.entity.CommandRwHitbox;
import noppes.npcs.rework.entity.HitboxTrack;
import noppes.npcs.rework.entity.CommandRwMount;
import noppes.npcs.rework.nav.CommandRwNav;
import noppes.npcs.rework.entity.RiderState;
import noppes.npcs.rework.formation.CommandRwSquad;
import noppes.npcs.rework.formation.SquadManager;

@Mod(modid="customnpcs", name="CustomNpcs", version="1.12", acceptedMinecraftVersions="1.12, 1.12.1, 1.12.2")
public class CustomNpcs {
    public static final String MODID = "customnpcs";
    @ConfigProp(info="Whether scripting is enabled or not")
    public static boolean EnableScripting = true;
    @ConfigProp(info="Arguments given to the Nashorn scripting library")
    public static String NashorArguments = "-strict";
    @ConfigProp(info="Disable Chat Bubbles")
    public static boolean EnableChatBubbles = true;
    @ConfigProp(info="Navigation search range for NPCs. Not recommended to increase if you have a slow pc or on a server")
    public static int NpcNavRange = 32;
    @ConfigProp(info="Set to true if you want the dialog command option to be able to use op commands like tp etc")
    public static boolean NpcUseOpCommands = false;
    @ConfigProp
    public static boolean InventoryGuiEnabled = true;
    @ConfigProp
    public static boolean FixUpdateFromPre_1_12 = false;
    @ConfigProp(info="If you are running sponge and you want to disable the permissions set this to true")
    public static boolean DisablePermissions = false;
    @ConfigProp
    public static boolean SceneButtonsEnabled = true;
    @ConfigProp
    public static boolean EnableDefaultEyes = true;
    @ConfigProp(info="Rework M3.3 (R1): who steers an NPC mount carrying an NPC rider. 0 = original (rider overwrites the mount's path every tick), 1 = the mount steers itself unless only the rider has a path, 2 = the mount always steers")
    public static int RwMountSteering = 0;
    @ConfigProp(info="Rework M3.6: priority of the melee attack task against the movement task added after it (wander, moving path). 0 = original (same priority: a wandering NPC finishes its wander path before it starts attacking), 1 = attack before movement")
    public static int RwAttackPriority = 0;
    @ConfigProp(info="Rework M5-S S1: target search rejects candidates the NPC is certainly not hostile to (no guard target, faction not aggressive) before the line-of-sight raytrace. 0 = original order (raytrace first), 1 = hostility first, 2 = hostility first and (M5-S S2) the area query asks only for players when the NPC can target nothing else (no guard job, no companion guard, no AttackOtherFactions). Same targets are chosen; players that may be attacked keep the original order. Default 2 since 2026-10-07 (D-027)")
    public static int RwTargetPrefilter = 2;
    @ConfigProp(info="Rework M7: allow NPC Baritone as an optional navigation backend for NPCs with RwNavBackend=1 in NBT. 0 = original vanilla navigation, 1 = permit opt-in NPCs")
    public static int RwNavBackend = 0;
    @ConfigProp(info="Rework M3.8 (R6): honour per-NPC hitbox modes (NBT RwHitboxMode: 1 = solid, cannot be moved by entity pushing; 2 = smart, push strength by hitbox size and shield). With 1, an NPC carrier ridden only by NPCs is pushed too, with the mass of carrier and riders (D-028). 0 = original vanilla pushing for every NPC, 1 = honour the modes")
    public static int RwHitbox = 0;
    @ConfigProp(info="Rework M3.8: in smart mode an entity holding a shield counts as this percent of its mass (200 = twice as hard to push)")
    public static int RwHitboxShieldWeight = 200;
    @ConfigProp(info="Rework M3.8: extra shield items for smart hitbox, comma separated registry names (mod:item). Items extending ItemShield or named *shield*, *buckler*, *pavise* are detected automatically")
    public static String RwHitboxShieldItems = "";
    public static long ticks;
    @SidedProxy(clientSide="noppes.npcs.client.ClientProxy", serverSide="noppes.npcs.CommonProxy")
    public static CommonProxy proxy;
    @ConfigProp(info="Enables CustomNpcs startup update message")
    public static boolean EnableUpdateChecker;
    public static CustomNpcs instance;
    public static boolean FreezeNPCs;
    @ConfigProp(info="Only ops can create and edit npcs")
    public static boolean OpsOnly;
    @ConfigProp(info="Default interact line. Leave empty to not have one")
    public static String DefaultInteractLine;
    @ConfigProp(info="Number of chunk loading npcs that can be active at the same time")
    public static int ChuckLoaders;
    public static File Dir;
    @ConfigProp(info="Enables leaves decay")
    public static boolean LeavesDecayEnabled;
    @ConfigProp(info="Enables Vine Growth")
    public static boolean VineGrowthEnabled;
    @ConfigProp(info="Enables Ice Melting")
    public static boolean IceMeltsEnabled;
    @ConfigProp(info="Normal players can use soulstone on animals")
    public static boolean SoulStoneAnimals;
    @ConfigProp(info="Normal players can use soulstone on all npcs")
    public static boolean SoulStoneNPCs;
    @ConfigProp(info="Type 0 = Normal, Type 1 = Solid")
    public static int HeadWearType;
    @ConfigProp(info="When set to Minecraft it will use minecrafts font, when Default it will use OpenSans. Can only use fonts installed on your PC")
    public static String FontType;
    @ConfigProp(info="Font size for custom fonts (doesn't work with minecrafts font)")
    public static int FontSize;
    public static FMLEventChannel Channel;
    public static FMLEventChannel ChannelPlayer;
    public static ConfigLoader Config;
    public static CommandNoppes NoppesCommand;
    public static boolean VerboseDebug;
    public static MinecraftServer Server;

    public CustomNpcs() {
        instance = this;
    }

    @Mod.EventHandler
    public void load(FMLPreInitializationEvent ev) {
        Channel = NetworkRegistry.INSTANCE.newEventDrivenChannel("CustomNPCs");
        ChannelPlayer = NetworkRegistry.INSTANCE.newEventDrivenChannel("CustomNPCsPlayer");
        Dir = new File(new File(ev.getModConfigurationDirectory(), ".."), MODID);
        Dir.mkdir();
        Config = new ConfigLoader(this.getClass(), ev.getModConfigurationDirectory(), "CustomNpcs");
        Config.loadConfig();
        if (NpcNavRange < 16) {
            NpcNavRange = 16;
        }
        CustomItems.load();
        CapabilityManager.INSTANCE.register(PlayerData.class, new Capability.IStorage(){

            public NBTBase writeNBT(Capability capability, Object instance, EnumFacing side) {
                return null;
            }

            public void readNBT(Capability capability, Object instance, EnumFacing side, NBTBase nbt) {
            }
        }, PlayerData.class);
        CapabilityManager.INSTANCE.register(WrapperEntityData.class, new Capability.IStorage(){

            public NBTBase writeNBT(Capability capability, Object instance, EnumFacing side) {
                return null;
            }

            public void readNBT(Capability capability, Object instance, EnumFacing side, NBTBase nbt) {
            }
        }, WrapperEntityData.class);
        CapabilityManager.INSTANCE.register(MarkData.class, new Capability.IStorage(){

            public NBTBase writeNBT(Capability capability, Object instance, EnumFacing side) {
                return null;
            }

            public void readNBT(Capability capability, Object instance, EnumFacing side, NBTBase nbt) {
            }
        }, MarkData.class);
        CapabilityManager.INSTANCE.register(ItemStackWrapper.class, (Capability.IStorage)new Capability.IStorage<ItemStackWrapper>(){

            public NBTBase writeNBT(Capability capability, ItemStackWrapper instance, EnumFacing side) {
                return null;
            }

            public void readNBT(Capability capability, ItemStackWrapper instance, EnumFacing side, NBTBase nbt) {
            }
        }, () -> null);
        NetworkRegistry.INSTANCE.registerGuiHandler((Object)this, (IGuiHandler)proxy);
        MinecraftForge.EVENT_BUS.register((Object)new ServerEventsHandler());
        MinecraftForge.EVENT_BUS.register((Object)new ServerTickHandler());
        MinecraftForge.EVENT_BUS.register((Object)new CustomEntities());
        MinecraftForge.EVENT_BUS.register((Object)proxy);
        NpcAPI.Instance().events().register((Object)new AbilityEventHandler());
        ForgeChunkManager.setForcedChunkLoadingCallback((Object)this, (ForgeChunkManager.LoadingCallback)new ChunkController());
        proxy.load();
        ObfuscationReflectionHelper.setPrivateValue(RangedAttribute.class,
                (RangedAttribute)SharedMonsterAttributes.MAX_HEALTH, Double.MAX_VALUE, 1);
    }

    @Mod.EventHandler
    public void load(FMLInitializationEvent ev) {
        PixelmonHelper.load();
        ScriptController controller = new ScriptController();
        if (EnableScripting && controller.languages.size() > 0) {
            MinecraftForge.EVENT_BUS.register((Object)controller);
            MinecraftForge.EVENT_BUS.register((Object)new ScriptPlayerEventHandler().registerForgeEvents());
            MinecraftForge.EVENT_BUS.register((Object)new ScriptItemEventHandler());
        }
        ForgeModContainer.fullBoundingBoxLadders = true;
        new RecipeController();
        proxy.postload();
        new CustomNpcsPermissions();
    }

    @Mod.EventHandler
    public void setAboutToStart(FMLServerAboutToStartEvent event) {
        Availability.scoreboardValues.clear();
        Server = event.getServer();
        WorldSaveSession.begin(CustomNpcs.getWorldSaveDirectory());
        ChunkController.instance.clear();
        FactionController.instance.load();
        new PlayerDataController();
        new TransportController();
        new GlobalDataController();
        new SpawnController();
        new LinkedNpcController();
        new MassBlockController();
        ScriptController.Instance.loadCategories();
        ScriptController.Instance.loadStoredData();
        ScriptController.Instance.loadPlayerScripts();
        ScriptController.Instance.loadForgeScripts();
        ScriptController.HasStart = false;
        WrapperNpcAPI.clearCache();
        Set<ResourceLocation> names = Block.REGISTRY.getKeys();
        for (ResourceLocation name : names) {
            Block block = Block.REGISTRY.getObject(name);
            if (block instanceof BlockLeaves) {
                block.setTickRandomly(LeavesDecayEnabled);
            }
            if (block instanceof BlockVine) {
                block.setTickRandomly(VineGrowthEnabled);
            }
            if (!(block instanceof BlockIce)) continue;
            block.setTickRandomly(IceMeltsEnabled);
        }
    }

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) {
        RecipeController.instance.load();
        new BankController();
        DialogController.instance.load();
        QuestController.instance.load();
        ScriptController.HasStart = true;
        ServerCloneController.Instance = new ServerCloneController();
    }

    @Mod.EventHandler
    public void stopped(FMLServerStoppedEvent event) {
        DiagEventCollector.disable();
        // M2.1d: merilni ticketi ne smejo prezivet zaustavitve serverja. Ce bi se zapisali
        // v forcedchunks.dat, bi naslednji zagon tiho tekel pod drugacnim pogojem meritve.
        DiagChunkLoader.disable();
        SquadManager.clear();
        HitboxTrack.stop();
        if (!WorldSaveSession.end(30L, TimeUnit.SECONDS)) {
            LogWriter.error("CustomNPCs world save queue did not drain cleanly before shutdown");
        }
        ServerCloneController.Instance = null;
        Server = null;
        ItemScripted.Resources.clear();
    }

    @Mod.EventHandler
    public void serverstart(FMLServerStartingEvent event) {
        event.registerServerCommand((ICommand)NoppesCommand);
        // M2.1: instrumentacija je privzeto izklopljena in takrat sploh ni prijavljena na
        // event bus. Ukaz je registriran vedno, da jo je mogoce vklopiti brez zagona z
        // drugacnimi parametri; -Drwdiag=on jo vklopi ze pred prvim tickom.
        event.registerServerCommand((ICommand)new CommandRwDiag());
        DiagEventCollector.enableIfRequestedByProperty();
        // M3.3: nacin krmiljenja nosilca iz configa; /rwmount ga med tekom preklopi.
        event.registerServerCommand((ICommand)new CommandRwMount());
        RiderState.setMode(RwMountSteering);
        // M3.6: prioriteta napada pred gibanjem; /rwattack jo med tekom preklopi.
        event.registerServerCommand((ICommand)new CommandRwAttack());
        AttackPriority.setMode(RwAttackPriority);
        // M5-S S1: predzavrnitev v iskalniku tarc; /rwtarget jo med tekom preklopi.
        event.registerServerCommand((ICommand)new CommandRwTarget());
        TargetPrefilter.setMode(RwTargetPrefilter);
        // M4.14: formacije. Brez ukaza (ali klica iz skripte) paket ni prijavljen na event
        // bus in ne doda nobenega AI taska, zato je obnasanje brez ukaza enako originalu.
        event.registerServerCommand((ICommand)new CommandRwSquad());
        event.registerServerCommand((ICommand)new CommandRwNav());
        event.registerServerCommand((ICommand)new CommandRwHitbox());
        EntityNPCInterface.ChatEventPlayer = new FakePlayer(event.getServer().getWorld(0), (GameProfile)EntityNPCInterface.ChatEventProfile);
        EntityNPCInterface.CommandPlayer = new FakePlayer(event.getServer().getWorld(0), (GameProfile)EntityNPCInterface.CommandProfile);
        EntityNPCInterface.GenericPlayer = new FakePlayer(event.getServer().getWorld(0), (GameProfile)EntityNPCInterface.GenericProfile);
    }

    public static File getWorldSaveDirectory() {
        return CustomNpcs.getWorldSaveDirectory(null);
    }

    public static File getWorldSaveDirectory(String s) {
        try {
            File dir = new File(".");
            if (Server != null) {
                if (!Server.isDedicatedServer()) {
                    dir = new File(Minecraft.getMinecraft().mcDataDir, "saves");
                }
                dir = new File(new File(dir, Server.getFolderName()), MODID);
            }
            if (s != null) {
                dir = new File(dir, s);
            }
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return dir;
        }
        catch (Exception e) {
            LogWriter.error("Error getting worldsave", e);
            return null;
        }
    }

    static {
        EnableUpdateChecker = true;
        FreezeNPCs = false;
        OpsOnly = false;
        DefaultInteractLine = "Hello @p";
        ChuckLoaders = 20;
        LeavesDecayEnabled = true;
        VineGrowthEnabled = true;
        IceMeltsEnabled = true;
        SoulStoneAnimals = true;
        SoulStoneNPCs = false;
        HeadWearType = 1;
        FontType = "Default";
        FontSize = 18;
        NoppesCommand = new CommandNoppes();
        VerboseDebug = false;
    }
}
