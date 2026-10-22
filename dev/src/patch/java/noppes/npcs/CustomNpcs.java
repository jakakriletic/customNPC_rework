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
import noppes.npcs.rework.nav.CommandRwPath;
import noppes.npcs.rework.entity.CollideSkip;
import noppes.npcs.rework.entity.CommandRwCollide;
import noppes.npcs.rework.core.CollisionGate;
import noppes.npcs.rework.core.CollisionWatcher;
import noppes.npcs.rework.core.CommandRwCore;
import noppes.npcs.rework.core.CommandRwMove;
import noppes.npcs.rework.entity.CommandRwData;
import noppes.npcs.rework.entity.DataShadow;
import noppes.npcs.rework.nav.PathFollowCache;
import noppes.npcs.rework.ai.TargetPrefilter;
import noppes.npcs.rework.diag.CommandRwDiag;
import noppes.npcs.rework.diag.DiagChunkLoader;
import noppes.npcs.rework.diag.DiagEventCollector;
import noppes.npcs.rework.entity.CommandRwHitbox;
import noppes.npcs.rework.entity.HitboxTrack;
import noppes.npcs.rework.entity.CommandRwMount;
import noppes.npcs.rework.nav.CommandRwNav;
import noppes.npcs.rework.nav.NegativePathCache;
import noppes.npcs.rework.nav.PathSearchMemo;
import noppes.npcs.rework.entity.RiderState;
import noppes.npcs.rework.formation.CommandRwSquad;
import noppes.npcs.rework.formation.SquadManager;
import noppes.npcs.rework.net.AssociatedPlayers;
import noppes.npcs.rework.net.CommandRwBlink;
import noppes.npcs.rework.far.CommandRwFar;
import noppes.npcs.rework.far.FarRegistry;
import noppes.npcs.rework.far.FarServer;

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
    @ConfigProp(info="Rework M5.11 (S14b): ground NPC path following (PathNavigateGround.isDirectPathBetweenPoints, called every tick while an NPC follows a path) evaluates the path node type of each block position once per call and reuses it (the original evaluates the same positions about three times). Same result. 0 = original, 1 = reuse node types, 2 = verify (runs both, returns the original, counts mismatches; double cost, for test scenarios only). Default 1 since 2026-10-09 (D-032)")
    public static int RwPathFollowCache = 1;
    @ConfigProp(info="Rework M5.16 (D-031): World.getCollisionBoxes skips the entity query while no entity in the world can contribute a collision box. Entity.getCollisionBoundingBox() and Entity.getCollisionBox(Entity) both return null in vanilla, so only classes that override one of them contribute (boat, minecart, shulker, modded classes); such classes are counted conservatively per world. The query is skipped only when that count is 0 and the moving entity class does not override getCollisionBox(Entity). Same result. This needs the coremod patch of net.minecraft.world.World; without it the switch does nothing. 0 = original, 1 = skip when provably empty, 2 = verify (runs the query and counts entities that would have contributed). Default 1 since 2026-10-14 (D-032): idle-500 MSPT average 2.787 to 2.540 ms, p95 4.194 to 3.867, five repetitions per series with separated ranges, 0 mismatches in 945162 comparisons")
    public static int RwMoveBoxes = 1;
    @ConfigProp(info="Rework M5-K K14b (client): the 3D headwear layer (HeadWearType=1) skips the quads that sample only fully transparent texels of the NPC skin (41 percent of the CustomNPCs skins have no visible hat at all). The alpha test discards every fragment of such a quad. Texels exactly on a quad edge are ignored: a fragment exactly on the edge line could sample them in the original, so the result is not proven identical (0 differing pixels in 7 checked skin series). idle-500 frame p50 11.49 to 8.60 ms. Falls back to the original drawing with OptiFine, URL or player skins, textures with metadata, or when the alpha test is not active. 0 = original, 1 = skip transparent quads. Default 1 since 2026-10-21 (D-049, owner decision Q30)")
    public static int RwHeadwearSkip = 1;
    @ConfigProp(info="Rework M5-K K4 (client): the NPC name and title label is drawn from a display list recorded the second time the same text, colour and font settings are drawn, instead of one immediate-mode draw per character. The list holds the same GL commands with the same values; GlStateManager is kept in sync. The original drawing is used for the first draw of a text, for font renderer subclasses (font mods), with OptiFine, and for texts with the obfuscated, underline or strikethrough codes. 0 = original, 1 = display list. Default 1 since 2026-10-19 (D-032): idle-200 frame p50 6.556 to 6.100 ms, idle-50 2.164 to 2.025, three repetitions per series with separated ranges, 0 differing pixels")
    public static int RwLabelCache = 1;
    @ConfigProp(info="Rework M5-K K5 (client): level of detail L1. An NPC farther from the camera than this many blocks is drawn without eyes (including brows and glint), without name and chat bubbles, and with the flat hat layer of the model (the look of HeadWearType=0) instead of the 3D headwear. Changes the look of distant NPCs. 0 = original (full detail at any distance), otherwise the distance in blocks")
    public static int RwLodStart = 0;
    @ConfigProp(info="Rework M5-V.2 (D-034): NPCs beyond the vanilla entity tracker range (64 blocks) are sent as light placeholders (position, rotation, look id, flags; about 16 bytes, about once per second, only when changed) over the rwfar channel to clients that ask for them, up to RwFarRange blocks and never beyond (view-distance - 1) * 16. Invisible NPCs and killed NPCs with a hidden body are never sent. Server: 0 = send nothing (original), 1 or 2 = send. Client: 0 = original (does not ask), 1 = ask for placeholders and draw them as impostors beyond the tracker range (L3, M5-V.5), 2 = also draw tracked distant NPCs as impostors (M5-V.6). Default 2 since acceptance M5-V.8 on 2026-10-22 (D-035, D-051): at view distance 24 with 5,000 NPCs frame p95 is 9.188 ms original, 8.893 ms with 2 and 10.873 ms with 1 (L3 without L2, not recommended on a client); rwfar code 91.5 us per tick per player, 0 B/s idle, about 0.5 KB/s while walking. Existing configs keep their value")
    public static int RwFarView = 2;
    @ConfigProp(info="Rework M5-V.2: largest distance in blocks (Chebyshev, as the vanilla tracker) at which the server sends far NPC placeholders; limited by (view-distance - 1) * 16. 16..1024")
    public static int RwFarRange = 384;
    @ConfigProp(info="Rework M5-V.4 (client, D-046): side in texels of the impostor atlas for far NPCs (one strip of 8 views of 32 x 64 texels per look, 4 mipmap levels). 1024 = 42 looks (about 5 MB VRAM), 2048 = 168 looks (about 21 MB), 4096 = 672 looks (about 85 MB); other values mean 2048, and the size is lowered to the GPU limit. Used only with RwFarView >= 1")
    public static int RwImpostorAtlas = 2048;
    @ConfigProp(info="Rework M5-V.6 (client): with RwFarView=2, a tracked NPC farther from the camera than this many blocks is drawn as an impostor from the atlas instead of the model (level L2); it switches back to the model 4 blocks closer (hysteresis). Only standing, visible, living NPCs that do not ride and are not ridden; until the look is baked the model is drawn. Tracked NPCs that vanilla does not draw because of their render distance (NPCs without a hitbox, beyond about 38 blocks at render distance 8) are also drawn as impostors, up to the fog end. 8..512, default 56")
    public static int RwImpostorStart = 56;
    @ConfigProp(info="Rework M5-V.7 (client, D-050): far NPC impostors (RwFarView levels L2 and L3) while an OptiFine shader pack is active. 0 = off: with a shader pack the client draws every NPC as the original model and draws no impostors (the impostors would be drawn by the block entity shader program, and shadow passes call the entity rendering again). 1 = draw impostors also with shaders, but never in the shadow pass. Without OptiFine or without a shader pack this setting has no effect. Default 0")
    public static int RwFarShaders = 0;
    @ConfigProp(info="Rework M5-K K12 (client, D-052): the box used for frustum culling and by occlusion mods such as Entity Culling encloses the NPC model (arms, lying body, part scales) and is never smaller than the hitbox, so the original never draws an NPC this box would cull. An NPC without a hitbox (width 0.00001) gets the render distance it would have with a hitbox (64 instead of 38.4 blocks for Size 5 at render distance 8); the render distance of an NPC with a hitbox is unchanged. 0 = original (hitbox), 1 = model box. Default 1 since 2026-10-22 (D-052)")
    public static int RwRenderBox = 1;
    @ConfigProp(info="Rework M5.14 (S16): the NPC keeps its own EntityDataManager keys (Attacking, IsDead, Walking, FactionData) in plain fields refreshed by notifyDataManagerChange, and reads them from there. Every write to a data manager value calls that hook after storing the value (EntityDataManager.set and setEntryValues), so the field always equals the stored value. Same result; it avoids a read lock and a HashMap lookup per read (EntityDataManager.get is 11.6 percent of the server thread in idle-500). 0 = original, 1 = read from the fields, 2 = verify (reads the data manager, compares with the field, counts mismatches). Default 1 since 2026-10-10 (D-032)")
    public static int RwDataShadow = 1;
    @ConfigProp(info="Rework M5.17 (S14c): the path search (PathFinder.findPath) evaluates the path node type of each block position once per search and reuses it; the original evaluates the same positions several times because every node checks its neighbours and the neighbourhoods overlap. Same result (same memoised method and same invariants as M5.11). 0 = original, 1 = reuse node types during a search, 2 = verify (every evaluation is computed and compared with the memoised one; no saving, for test scenarios only). Default 1 since 2026-10-10 (D-032)")
    public static int RwPathSearchMemo = 1;
    @ConfigProp(info="Rework M5.6 (S5): after a path search that found no complete path to the target, the same NPC does not search for (almost) the same target again for 20 ticks. M5.9 measured that a visible but unreachable target costs 51 percent of the tick in path searching, 180 microseconds per search, because the vanilla A* spends its whole 200 node budget when it cannot reach the goal. This CHANGES BEHAVIOUR: if a path opens in the meantime, the NPC reacts up to 20 ticks later, so it is a behaviour switch (D-007). 0 = original, 1 = skip repeated failed searches, 2 = verify (searches anyway, answers with the original, counts how often the cached answer would have differed). Default 1 since 2026-10-16 by user decision (Q21, D-037): nedosegljiva-500 MSPT average 17.854 to 14.462 ms, allocation 292 to 135 MB/s")
    public static int RwPathNegCache = 1;
    @ConfigProp(info="Rework M5.12 (S4): an NPC without enabled scripts skips the collide area query (every 4th tick) when nothing listens for NpcEvent.CollideEvent on the CustomNPCs event bus; with no script and no listener the original query has no effect. Listeners are checked on every call. 0 = original, 1 = skip without an observer, 2 = verify (original, counts calls and events that would be skipped). Default 1 since 2026-10-09 (D-032)")
    public static int RwCollideSkip = 1;
    @ConfigProp(info="Rework M5.13 (S7): recipients of the eye blink packet (every NPC blinks about once every 140 ticks) are found by walking world.playerEntities with the same selection the vanilla area query makes, instead of scanning every loaded chunk in a 160 block cube (about 441 chunks). Same recipients. 0 = original, 1 = walk playerEntities, 2 = verify (runs both, sends by the original list, counts mismatches; double cost, for test scenarios only). Default 1 since 2026-10-10 (D-032)")
    public static int RwBlinkRecipients = 1;
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
        // M5.16 (D-031): stevec entitet, ki lahko prispevajo trkalno skatlo, po svetovih.
        MinecraftForge.EVENT_BUS.register(CollisionWatcher.createEventHandler());
        // M5-V.2 (D-034): register NPC-jev po svetovih in kanal rwfar; brez HELLO klienta in z
        // RwFarView=0 se ne poslje nic. Klientska stran prek refleksije, kot harness spodaj.
        MinecraftForge.EVENT_BUS.register(FarRegistry.createEventHandler());
        FarServer.init();
        if (ev.getSide().isClient()) {
            try {
                Class.forName("noppes.npcs.rework.far.FarClient").getMethod("install").invoke(null);
            } catch (ReflectiveOperationException e) {
                LogWriter.error("RWFAR klient ni namescen", e);
            }
        }
        MinecraftForge.EVENT_BUS.register((Object)new ServerEventsHandler());
        MinecraftForge.EVENT_BUS.register((Object)new ServerTickHandler());
        MinecraftForge.EVENT_BUS.register((Object)new CustomEntities());
        MinecraftForge.EVENT_BUS.register((Object)proxy);
        NpcAPI.Instance().events().register((Object)new AbilityEventHandler());
        ForgeChunkManager.setForcedChunkLoadingCallback((Object)this, (ForgeChunkManager.LoadingCallback)new ChunkController());
        proxy.load();
        // M5-K.0: klientski merilni harness, samo z -Drwharness=<navodilo>. Refleksija, da
        // dedicated server razreda s klientskimi tipi nikoli ne nalozi.
        String rwHarness = System.getProperty("rwharness");
        if (rwHarness != null && ev.getSide().isClient()) {
            try {
                Class.forName("noppes.npcs.rework.diag.ClientHarness")
                        .getMethod("install", String.class).invoke(null, rwHarness);
            } catch (ReflectiveOperationException e) {
                System.out.println("RWHARNESS-NAPAKA namestitev: " + e);
            }
        }
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
        // M5.11 (S14b): pomnjenje tipa vozlisca v sledenju poti; /rwpath ga med tekom preklopi.
        event.registerServerCommand((ICommand)new CommandRwPath());
        // M5.12 (S4): onCollide brez opazovalca; /rwcollide ga med tekom preklopi.
        event.registerServerCommand((ICommand)new CommandRwCollide());
        CollideSkip.setMode(RwCollideSkip);
        // M5.14 (S16): sencna polja CNPC kljucev; /rwdata jih med tekom preklopi.
        event.registerServerCommand((ICommand)new CommandRwData());
        DataShadow.setMode(RwDataShadow);
        // M5.15 (D-031): stanje coremoda; transformer ne spremeni nicesar.
        event.registerServerCommand((ICommand)new CommandRwCore());
        // M5.16 (D-031): preskok poizvedbe entitet za trke; /rwmove ga med tekom preklopi.
        event.registerServerCommand((ICommand)new CommandRwMove());
        CollisionGate.setMode(RwMoveBoxes);
        PathFollowCache.setMode(RwPathFollowCache);
        NegativePathCache.setMode(RwPathNegCache);
        PathSearchMemo.setMode(RwPathSearchMemo);
        // M5.13 (S7): prejemniki utripa oci; /rwblink jih med tekom preklopi.
        event.registerServerCommand((ICommand)new CommandRwBlink());
        AssociatedPlayers.setMode(RwBlinkRecipients);
        // M5-V.2: posiljanje nadomestkov oddaljenih NPC-jev; /rwfar ga med tekom preklopi.
        event.registerServerCommand((ICommand)new CommandRwFar());
        FarServer.start(RwFarView, RwFarRange);
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
