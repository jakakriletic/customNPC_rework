package noppes.npcs.rework.diag;

import java.io.File;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.commandstaff.CommandStaff;
import noppes.npcs.rework.commandstaff.GuiCommandStaff;
import noppes.npcs.rework.commandstaff.StaffClient;
import noppes.npcs.rework.commandstaff.StaffOrder;

public final class CommandStaffHarness {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final String world;
    private final boolean bundled;
    private final long deadline = System.nanoTime() + 190_000_000_000L;
    private final StaffClient input = new StaffClient();
    private int stage, delay;
    private Vec3d start, afterFollow, afterGuard;
    private volatile boolean complete;
    private volatile RuntimeException failure;
    private EntityVindicator enemy;

    private CommandStaffHarness(String world, boolean bundled) {
        this.world = world;
        this.bundled = bundled;
    }

    static boolean installIfRequested(String path) throws Exception {
        Properties job = new Properties();
        try (Reader in = Files.newBufferedReader(new File(path).toPath(), StandardCharsets.UTF_8)) { job.load(in); }
        if (!"1".equals(job.getProperty("commandStaff"))) { return false; }
        MinecraftForge.EVENT_BUS.register(new CommandStaffHarness(job.getProperty("world"), "1".equals(job.getProperty("bundledVaried"))));
        return true;
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (stage < 0 || event.phase != TickEvent.Phase.END || delay-- > 0) { return; }
        try {
            if (failure != null) { throw failure; }
            if (System.nanoTime() >= deadline) { throw new IllegalStateException("timed out at stage " + stage); }
            switch (stage) {
                case 0:
                    if (!(mc.currentScreen instanceof GuiMainMenu)) { return; }
                    if (bundled) {
                        Item coin = Item.getByNameOrId("variedcommodities:ancient_coin");
                        require(coin != null && net.minecraftforge.fml.common.Loader.isModLoaded("variedcommodities"), "bundled commodity registered");
                        require(mc.getRenderItem().getItemModelWithOverrides(new ItemStack(coin), null, null)
                                != mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel(), "bundled commodity model loaded");
                        require(mc.getRenderItem().getItemModelMesher().getModelManager().getModel(new ModelResourceLocation("rwcommandstaff:command_staff", "inventory"))
                                != mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel(), "command staff model loaded");
                    }
                    mc.gameSettings.pauseOnLostFocus = false;
                    mc.launchIntegratedServer(world, world,
                            new WorldSettings(HarnessJob.seedOf("staff"), GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
                    next(1, 10);
                    break;
                case 1:
                    if (mc.player == null || mc.world == null || mc.currentScreen != null) { return; }
                    server(() -> {
                        MinecraftServer s = mc.getIntegratedServer();
                        String player = mc.player.getName();
                        for (int x = -2; x <= 2; x++) {
                            for (int z = -2; z <= 2; z++) { s.getWorld(0).getChunkFromChunkCoords(x, z); }
                        }
                        for (String command : new String[]{"gamerule doMobSpawning false", "gamerule doDaylightCycle false",
                                "time set 6000", "tp " + player + " 0 4 -8 0 10"}) {
                            s.getCommandManager().executeCommand(s, command);
                        }
                        for (int i = 0; i < 4; i++) {
                            int x = i % 2 * 3 - 1, z = i / 2 * 3 + 1;
                            s.getCommandManager().executeCommand(s, "summon customnpcs:customnpc " + x + " 4 " + z
                                    + " {Name:\"Staff_" + i + "\",MovingState:0,Orientation:0,StartPosNew:[I;" + x + ",4," + z + "]}");
                        }
                        s.getCommandManager().executeCommand(s, "summon customnpcs:customnpc 20 4 3 {Name:\"Staff_outside\",MovingState:0}");
                        s.getPlayerList().getPlayerByUsername(player).inventory.setInventorySlotContents(0, new ItemStack(CommandStaff.item()));
                    });
                    next(2, 25);
                    break;
                case 2:
                    if (!complete || !CommandStaff.holding(mc.player) || members(mc.world.loadedEntityList).size() != 4) { return; }
                    sneak(true);
                    next(3, 8);
                    break;
                case 3:
                    clickWorld(0, true);
                    next(4, 12);
                    break;
                case 4:
                    clickWorld(0, false);
                    next(5, 7);
                    break;
                case 5:
                    clickWorld(1, true);
                    clickWorld(1, false);
                    next(6, 10);
                    break;
                case 6:
                    if (!(mc.currentScreen instanceof GuiCommandStaff)) { return; }
                    Field count = GuiCommandStaff.class.getDeclaredField("count");
                    count.setAccessible(true);
                    require(count.getInt(mc.currentScreen) == 4, "ring selected exactly four");
                    clickGui(1);
                    next(7, 10);
                    break;
                case 7:
                    clickGui(2);
                    next(8, 10);
                    break;
                case 8:
                    clickGui(5);
                    next(9, 10);
                    break;
                case 9:
                    clickGui(7);
                    sneak(false);
                    server(() -> {
                        require(modes(first()) == 5, "Follow, Stationary and Guard reached the server");
                        start = first().getPositionVector();
                        MinecraftServer s = mc.getIntegratedServer();
                        s.getCommandManager().executeCommand(s, "tp " + mc.player.getName() + " 8 4 -4 -30 6");
                    });
                    next(10, 60);
                    break;
                case 10:
                    if (!complete || npcClient().getDistanceSq(start.x, start.y, start.z) < 4) { return; }
                    check("follow walked");
                    sneak(true);
                    next(11, 8);
                    break;
                case 11:
                    clickWorld(1, true);
                    clickWorld(1, false);
                    next(12, 10);
                    break;
                case 12:
                    if (!(mc.currentScreen instanceof GuiCommandStaff)) { return; }
                    clickGui(0);
                    next(13, 10);
                    break;
                case 13:
                    clickGui(7);
                    sneak(false);
                    server(() -> {
                        EntityNPCInterface npc = first();
                        require(modes(npc) == 4, "Stay is active");
                        afterFollow = npc.getPositionVector();
                        enemy = new EntityVindicator(npc.world);
                        enemy.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, new ItemStack(Items.STICK));
                        enemy.setNoAI(true);
                        enemy.setPosition(npc.posX - 8, npc.posY, npc.posZ);
                        npc.world.spawnEntity(enemy);
                        npc.setAttackTarget(enemy);
                        require(npc.getAttackTarget() == null, "Guard rejects a target outside attack range");
                    });
                    next(14, 35);
                    break;
                case 14:
                    server(() -> {
                        EntityNPCInterface npc = first();
                        require(npc.getDistanceSq(afterFollow.x, afterFollow.y, afterFollow.z) < 1, "Guard held ground");
                        afterGuard = npc.getPositionVector();
                    });
                    sneak(true);
                    next(15, 8);
                    break;
                case 15:
                    clickWorld(1, true);
                    clickWorld(1, false);
                    next(16, 10);
                    break;
                case 16:
                    if (!(mc.currentScreen instanceof GuiCommandStaff)) { return; }
                    clickGui(4);
                    next(17, 10);
                    break;
                case 17:
                    clickGui(7);
                    sneak(false);
                    server(() -> {
                        require(modes(first()) == 0, "Attack mode reached server");
                        first().setAttackTarget(enemy);
                    });
                    next(18, 60);
                    break;
                case 18:
                    if (npcClient().getDistanceSq(afterGuard.x, afterGuard.y, afterGuard.z) < 4) { return; }
                    check("Attack pursued enemy");
                    server(() -> {
                        enemy.setDead();
                        first().setAttackTarget(null);
                        first().setRevengeTarget(null);
                        MinecraftServer s = mc.getIntegratedServer();
                        s.getCommandManager().executeCommand(s, "tp " + mc.player.getName() + " 0 4 -5 0 4");
                    });
                    next(19, 15);
                    break;
                case 19:
                    clickWorld(1, true);
                    clickWorld(1, false);
                    next(20, 120);
                    break;
                case 20:
                    List<EntityNPCInterface> npcs = members(mc.world.loadedEntityList);
                    if (npcs.size() != 4 || npcs.stream().anyMatch(npc -> npc.posZ < 13)) { return; }
                    for (int i = 0; i < npcs.size(); i++) {
                        for (int j = i + 1; j < npcs.size(); j++) {
                            if (npcs.get(i).getDistance(npcs.get(j)) < 1.15) { return; }
                        }
                    }
                    check("ray move arrived in separated slots");
                    sneak(true);
                    next(21, 8);
                    break;
                case 21:
                    clickWorld(1, true);
                    clickWorld(1, false);
                    next(22, 10);
                    break;
                case 22:
                    if (!(mc.currentScreen instanceof GuiCommandStaff)) { return; }
                    clickGui(3);
                    next(23, 12);
                    break;
                case 23:
                    server(() -> {
                        require(modes(first()) == StaffOrder.WANDER, "Wander reached server");
                        NBTTagCompound nbt = new NBTTagCompound();
                        first().rwStaffOrder.write(nbt);
                        require(StaffOrder.read(first(), nbt) != null, "saved staff state reads back");
                    });
                    next(24, 10);
                    break;
                case 24:
                    if (!complete) { return; }
                    clickGui(6);
                    next(25, 12);
                    break;
                case 25:
                    server(() -> require(first().rwStaffOrder == null, "restore cleared staff override"));
                    next(26, 10);
                    break;
                case 26:
                    if (complete) { finish(true); }
                    break;
                default: break;
            }
        } catch (Exception | LinkageError problem) {
            problem.printStackTrace();
            finish(false);
        }
    }

    private void server(Runnable work) {
        complete = false;
        mc.getIntegratedServer().addScheduledTask(() -> {
            try { work.run(); } catch (RuntimeException error) { failure = error; }
            complete = true;
        });
    }

    private void next(int stage, int delay) { this.stage = stage; this.delay = delay; }

    private void sneak(boolean value) {
        net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), value);
        mc.player.setSneaking(value);
        mc.player.connection.sendPacket(new net.minecraft.network.play.client.CPacketEntityAction(mc.player,
                value ? net.minecraft.network.play.client.CPacketEntityAction.Action.START_SNEAKING
                      : net.minecraft.network.play.client.CPacketEntityAction.Action.STOP_SNEAKING));
    }

    private void clickWorld(int button, boolean pressed) throws Exception {
        Method mouse = StaffClient.class.getDeclaredMethod("mouseButton", int.class, boolean.class);
        mouse.setAccessible(true);
        require(Boolean.TRUE.equals(mouse.invoke(input, button, pressed)), "captured mouse button " + button + " " + pressed);
    }

    private void clickGui(int id) throws Exception {
        GuiScreen screen = mc.currentScreen;
        Field buttons = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
        for (Object raw : (List<?>)buttons.get(screen)) {
            GuiButton button = (GuiButton)raw;
            if (button.id != id) { continue; }
            require(button.enabled, "GUI button " + id + " enabled");
            Method click = net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(GuiScreen.class,
                    "mouseClicked", "func_73864_a", int.class, int.class, int.class);
            screen.drawScreen(button.x + 2, button.y + 2, 0);
            click.invoke(screen, button.x + 2, button.y + 2, 0);
            return;
        }
        throw new IllegalStateException("button " + id + " missing");
    }

    private static List<EntityNPCInterface> members(List<Entity> entities) {
        List<EntityNPCInterface> list = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof EntityNPCInterface && e.getName().matches("Staff_[0-3]")) { list.add((EntityNPCInterface)e); }
        }
        return list;
    }

    private EntityNPCInterface first() {
        WorldServer world = mc.getIntegratedServer().getWorld(0);
        return members(world.loadedEntityList).stream().filter(npc -> "Staff_0".equals(npc.getName())).findFirst().get();
    }

    private EntityNPCInterface npcClient() {
        return members(mc.world.loadedEntityList).stream().filter(npc -> "Staff_0".equals(npc.getName())).findFirst().get();
    }

    private static int modes(EntityNPCInterface npc) {
        NBTTagCompound tag = new NBTTagCompound();
        npc.rwStaffOrder.write(tag);
        return tag.getCompoundTag("RwCommandStaff").getInteger("Modes");
    }

    private static void check(String label) { System.out.println("STAFF CHECK " + label); }
    private static void require(boolean condition, String label) {
        if (!condition) { throw new IllegalStateException(label); }
        check(label);
    }

    private void finish(boolean ok) {
        stage = -1;
        sneak(false);
        System.out.println("STAFF DONE " + ok);
        mc.shutdown();
    }
}
