package noppes.npcs.rework.commandstaff;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public final class StaffClient {
    private final Set<Integer> selected = new LinkedHashSet<>();
    private World world;
    private int held = -1;
    private boolean leftCaptured, rightCaptured;

    public static void install() {
        StaffClient client = new StaffClient();
        MinecraftForge.EVENT_BUS.register(client);
        if (StaffNetwork.channel() != null) { StaffNetwork.channel().register(client); }
    }

    @SubscribeEvent public void model(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(CommandStaff.item(), 0, new ModelResourceLocation("rwcommandstaff:command_staff", "inventory"));
    }

    @SubscribeEvent public void mouse(MouseEvent event) {
        if (mouseButton(event.getButton(), event.isButtonstate())) { event.setCanceled(true); }
    }

    private boolean mouseButton(int button, boolean pressed) {
        if (button != 0 && button != 1) { return false; }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.player == null) { return false; }
        if (!pressed) {
            if (button == 0 && leftCaptured) {
                leftCaptured = false;
                if (held >= 0 && CommandStaff.holding(mc.player)) { send(StaffNetwork.SELECT, 0, 0); }
                held = -1;
                return true;
            }
            if (button == 1 && rightCaptured) {
                rightCaptured = false;
                return true;
            }
            return false;
        }
        if (!CommandStaff.holding(mc.player)) { return false; }
        boolean sneak = mc.gameSettings.keyBindSneak.isKeyDown() || mc.player.isSneaking();
        if (button == 0 && sneak) {
            held = 0;
            leftCaptured = true;
            send(StaffNetwork.BEGIN, 0, 0);
            return true;
        } else if (button == 1) {
            rightCaptured = true;
            if (held < 0) { send(sneak ? StaffNetwork.MENU : StaffNetwork.MOVE, 0, 0); }
            return true;
        }
        return false;
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        if (world != mc.world) {
            selected.clear();
            held = -1;
            leftCaptured = rightCaptured = false;
            world = mc.world;
        }
        if (mc.player == null || mc.world == null) { return; }
        selected.removeIf(id -> {
            Entity entity = mc.world.getEntityByID(id);
            return entity == null || !entity.isEntityAlive();
        });
        if (held < 0) { return; }
        if (mc.currentScreen != null || !CommandStaff.holding(mc.player) || !mc.player.isEntityAlive()) {
            held = -1;
            send(StaffNetwork.CANCEL, 0, 0);
        } else if (!Mouse.isButtonDown(0)) {
            send(StaffNetwork.SELECT, 0, 0);
            held = -1;
        } else {
            held = Math.min(StaffNetwork.MAX_GROW_TICKS, held + 1);
        }
    }

    static void send(int op, int mask, int values) {
        if (StaffNetwork.channel() == null || Minecraft.getMinecraft().getConnection() == null) { return; }
        PacketBuffer out = new PacketBuffer(Unpooled.buffer(3));
        out.writeByte(op).writeByte(mask).writeByte(values);
        StaffNetwork.channel().sendToServer(new FMLProxyPacket(out, StaffNetwork.CHANNEL));
    }

    @SubscribeEvent public void packet(FMLNetworkEvent.ClientCustomPacketEvent event) {
        ByteBuf in = event.getPacket().payload().duplicate();
        if (in.readableBytes() < 5) { return; }
        int op = in.readUnsignedByte(), all = in.readUnsignedByte(), any = in.readUnsignedByte(), size = in.readUnsignedShort();
        if ((op != StaffNetwork.SELECT && op != StaffNetwork.MENU && op != StaffNetwork.MODES) || size > StaffNetwork.MAX_SELECTION
                || in.readableBytes() != size * 4 || (any & ~StaffOrder.MODES) != 0 || (all & ~any) != 0) { return; }
        Set<Integer> ids = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) { ids.add(in.readInt()); }
        Minecraft mc = Minecraft.getMinecraft();
        World receivedIn = mc.world;
        mc.addScheduledTask(() -> {
            if (mc.player == null || mc.world == null || receivedIn != mc.world) { return; }
            world = mc.world;
            selected.clear();
            selected.addAll(ids);
            if (op == StaffNetwork.MENU && mc.currentScreen == null && CommandStaff.holding(mc.player)) {
                mc.displayGuiScreen(new GuiCommandStaff(ids.size(), all, any));
            } else if (mc.currentScreen instanceof GuiCommandStaff) {
                ((GuiCommandStaff)mc.currentScreen).received(ids.size(), all, any);
            }
        });
    }

    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.player == null || !CommandStaff.holding(mc.player)) { return; }
        mc.fontRenderer.drawStringWithShadow(selected.size() + " NPCs selected", 8, 8, 0xf2cf70);
    }

    @SubscribeEvent public void render(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null || mc.player == null || !CommandStaff.holding(mc.player) || (held < 0 && selected.isEmpty())) { return; }
        float partial = event.getPartialTicks();
        double x = view.lastTickPosX + (view.posX - view.lastTickPosX) * partial;
        double y = view.lastTickPosY + (view.posY - view.lastTickPosY) * partial;
        double z = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partial;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2F);
        try {
            for (int id : selected) {
                Entity entity = mc.world.getEntityByID(id);
                if (entity == null) { continue; }
                double dx = (entity.lastTickPosX - entity.posX) * (1 - partial);
                double dy = (entity.lastTickPosY - entity.posY) * (1 - partial);
                double dz = (entity.lastTickPosZ - entity.posZ) * (1 - partial);
                RenderGlobal.drawSelectionBoundingBox(entity.getEntityBoundingBox().offset(dx - x, dy - y, dz - z).grow(0.06), 0.97F, 0.77F, 0.29F, 0.9F);
            }
            if (held >= 0) {
                RayTraceResult hit = mc.player.rayTrace(StaffNetwork.MAX_RANGE, partial);
                Vec3d point = hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK ? hit.hitVec : mc.player.getPositionVector();
                double radius = StaffNetwork.radius(held);
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buffer = tess.getBuffer();
                buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i < 64; i++) {
                    double angle = i * Math.PI / 32;
                    buffer.pos(point.x + Math.cos(angle) * radius - x, point.y + 0.12 - y, point.z + Math.sin(angle) * radius - z)
                            .color(1F, 0.78F, 0.25F, 0.9F).endVertex();
                }
                tess.draw();
            }
        } finally {
            GlStateManager.glLineWidth(1F);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }
}
