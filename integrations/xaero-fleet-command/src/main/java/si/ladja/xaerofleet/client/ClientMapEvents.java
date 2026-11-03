package si.ladja.xaerofleet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import si.ladja.xaerofleet.XaeroFleetCommandMod;
import si.ladja.xaerofleet.client.xaero.MapViewTransform;
import si.ladja.xaerofleet.network.CancelOrderPacket;
import si.ladja.xaerofleet.network.CaptainView;
import si.ladja.xaerofleet.network.EngagementOrderPacket;
import si.ladja.xaerofleet.network.FleetNetwork;
import si.ladja.xaerofleet.network.MoveOrderPacket;
import si.ladja.xaerofleet.network.RequestCaptains;
import xaero.map.gui.GuiMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The fleet layer on Xaero's world map: captain markers, selection and orders.
 *
 * <p>Gestures, chosen to sit beside Xaero's own rather than on top of them -
 * a plain drag still pans the map, a plain right-click on an empty selection
 * still opens Xaero's menu:</p>
 *
 * <ul>
 *   <li>click a captain - select only him; Shift-click adds or removes;</li>
 *   <li>double-click - every friendly captain on screen;</li>
 *   <li>Ctrl + drag - a selection box; with Shift it adds to the selection;</li>
 *   <li>click on empty water - clears the selection (a drag there pans);</li>
 *   <li>right-click - the selection sails there, in formation;</li>
 *   <li>X - the selection stops; Esc - clears it; Ctrl+A - every friend;</li>
 *   <li>while any of the selection has a destination, three buttons under the
 *       panel: Stop course (same as X), Attack enemy (leave the route to fight,
 *       then carry on) and Passive cruise (hold the route whatever is met);</li>
 *   <li>Ctrl+1..9 saves a group, 1..9 recalls it, twice shows it; Space
 *       centres the map on the selection.</li>
 * </ul>
 *
 * <p>All of it is presentation. The server checks every id it is sent.</p>
 *
 * <p>It is all only on the ship's map. A switch at the top of the map - or
 * Tab - flips between that and Xaero's own world map, which is left exactly as
 * Xaero draws it: no captains, no panel, every click and key Xaero's. The
 * choice is remembered ({@link MapMode}).</p>
 */
public final class ClientMapEvents {

    /** Ticks between snapshot requests. The server refuses a faster client. */
    private static final int REFRESH_TICKS = 40;

    /**
     * Shortest gap between two orders, in real milliseconds.
     *
     * <p>The server's own cooldown is half a second. A click inside the gap is
     * not thrown away: it replaces the waiting order and goes out as soon as
     * the gap has passed, because the player meant it.</p>
     */
    private static final long ORDER_GAP_MS = 650L;

    private static final long DOUBLE_CLICK_MS = 320L;
    private static final long DOUBLE_TAP_MS = 400L;

    /** Real pixels a press may move and still be a click, not a drag. */
    private static final double CLICK_SLOP = 5.0D;

    private static final int SELECT = 0xFFFFD54A;
    private static final int SELECT_SOFT = 0x66FFD54A;
    private static final int BOX_FILL = 0x2AFFD54A;
    private static final int PANEL = 0xC0101418;
    /** Right edge of the fleet panel, GUI pixels; the map switch keeps clear of it. */
    private static final int PANEL_RIGHT = 174;
    private static final int PANEL_EDGE = 0x60FFFFFF;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9AA0A6;
    private static final int STOP_COLOUR = 0xFFFF6B5A;
    private static final int ATTACK_COLOUR = 0xFFFF9A3D;
    private static final int PASSIVE_COLOUR = 0xFF5AB0FF;

    /** How long a button press is shown before the server's snapshot confirms it. */
    private static final long ENGAGEMENT_UNCONFIRMED_MS = 4000L;

    private static final int BUTTON_STOP = 0;
    private static final int BUTTON_ATTACK = 1;
    private static final int BUTTON_PASSIVE = 2;
    private static final String[] BUTTON_LABELS = {"Stop course", "Attack enemy", "Passive cruise"};
    private static final String[] BUTTON_HINTS = {
            "stops and clears the destination", "attacks while under way", "sails on despite enemies"};
    private static final int[] BUTTON_COLOURS = {STOP_COLOUR, ATTACK_COLOUR, PASSIVE_COLOUR};

    private MapViewTransform lastView;
    private int refreshTicks;
    private int sinceRequest = REFRESH_TICKS;
    private boolean warnedUnsupported;

    private long lastOrderAt;
    private PendingOrder pendingOrder;

    /** Selection box, in real display pixels. */
    private boolean boxing;
    private boolean boxAdd;
    private double boxX0;
    private double boxY0;

    /** A left press on empty water, so that its release can clear the selection. */
    private boolean emptyPress;
    private double pressX;
    private double pressY;
    /** A right press we took, so that Xaero does not see its release either. */
    private boolean tookRightPress;
    /** A left press on the map switch, so that Xaero does not see its release. */
    private boolean tookSwitchPress;
    /** Where the switch was last drawn; null before the first frame. */
    private MapModeSwitch modeSwitch;

    private UUID lastClickId;
    private long lastClickAt;
    private int lastGroupKey = -1;
    private long lastGroupAt;

    private boolean showHelp;
    /** Screen area of the panel header, GUI pixels, for clicks on "pomoč". */
    private int headerX0, headerY0, headerX1, headerY1;
    private int panelBottom;
    /** Course buttons, GUI pixels: {x0, y0, x1, y1} each; null while hidden. */
    private int[][] buttons;

    /** captain -> {engage ? 1 : 0, sentAtMillis}: pressed, not yet in a snapshot. */
    private final Map<UUID, long[]> engagementSent = new HashMap<>();

    private final List<Ping> pings = new ArrayList<>();

    private static final class PendingOrder {
        final List<UUID> ids;
        final int dimension;
        final double x;
        final double z;

        PendingOrder(List<UUID> ids, int dimension, double x, double z) {
            this.ids = ids;
            this.dimension = dimension;
            this.x = x;
            this.z = z;
        }
    }

    private static final class Ping {
        final double x;
        final double z;
        final long at;
        final int colour;

        Ping(double x, double z, int colour) {
            this.x = x;
            this.z = z;
            this.at = System.currentTimeMillis();
            this.colour = colour;
        }
    }

    // ------------------------------------------------------------------ life

    @SubscribeEvent
    public void onOpen(GuiOpenEvent event) {
        if (event.getGui() instanceof GuiMap) {
            if (MapMode.isShipMap()) requestCaptains();
            StormClient.request();
            refreshTicks = 0;
        } else if (event.getGui() == null
                || Minecraft.getMinecraft().currentScreen instanceof GuiMap) {
            ClientCaptainStore.clearCaptains();
            lastView = null;
            boxing = false;
            emptyPress = false;
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(ClientCaptainStore::clear);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (sinceRequest < REFRESH_TICKS) sinceRequest++;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiMap) {
            if (MapMode.isShipMap() && ++refreshTicks >= REFRESH_TICKS) {
                refreshTicks = 0;
                requestCaptains();
            }
            flushPendingOrder();
        } else {
            refreshTicks = 0;
            pendingOrder = null;
        }
    }

    private void requestCaptains() {
        if (!MapViewTransform.isSupported() || sinceRequest < REFRESH_TICKS
                || !XaeroFleetCommandMod.serverSupportsFleet()) return;
        sinceRequest = 0;
        FleetNetwork.CHANNEL.sendToServer(new RequestCaptains());
    }

    // --------------------------------------------------------------- drawing

    @SubscribeEvent
    public void afterDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiMap)) return;
        if (!MapViewTransform.isSupported()) {
            if (!warnedUnsupported) {
                warnedUnsupported = true;
                XaeroFleetCommandMod.LOG.warn(
                        "Xaero overlay disabled: fields {} are missing. Orders via /dvfleet still work.",
                        MapViewTransform.missingFields());
            }
            return;
        }
        GuiMap map = (GuiMap) event.getGui();
        MapViewTransform view = MapViewTransform.read(map);
        if (view == null) return;
        lastView = view;
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.currentTimeMillis();
        double gs = view.guiScale;
        double mouseRealX = Mouse.getX();
        double mouseRealY = mc.displayHeight - Mouse.getY() - 1;
        double mouseX = mouseRealX / gs;
        double mouseY = mouseRealY / gs;

        if (!XaeroFleetCommandMod.serverSupportsFleet()) {
            modeSwitch = null;
            return;
        }
        if (!MapMode.isShipMap()) {
            // Xaero's own map: nothing of ours but the way back.
            drawModeSwitch(mc, map, mouseX, mouseY);
            drawToast(mc, map);
            return;
        }
        // A thin gilt edge round the whole map: this is the ship's map.
        MapDraw.frame(1, 1, map.width - 1, map.height - 1, 1, MapDraw.withAlpha(SELECT, 0.35F));

        if (boxing && !Mouse.isButtonDown(0)) {
            // The release happened somewhere we did not hear it - outside the
            // window, or while another screen had the mouse. Close it here.
            finishBox(view, mouseRealX, mouseRealY);
        }

        // Icon texels are whole real pixels, so the face stays crisp at every
        // GUI scale: about three quarters of a GUI pixel each.
        int texel = Math.max(1, (int) Math.round(gs * 0.75D));
        double iconSize = CaptainIcon.SIZE * texel / gs;
        double half = iconSize / 2.0D;

        List<Marker> markers = new ArrayList<>();
        for (CaptainView captain : ClientCaptainStore.captains()) {
            if (captain.dimension != view.dimension) continue;
            double wx = ClientCaptainStore.displayX(captain, now);
            double wz = ClientCaptainStore.displayZ(captain, now);
            // Snap the icon's corner to the real pixel grid.
            double left = Math.round(view.realX(wx) - CaptainIcon.SIZE * texel / 2.0D);
            double top = Math.round(view.realY(wz) - CaptainIcon.SIZE * texel / 2.0D);
            double sx = (left + CaptainIcon.SIZE * texel / 2.0D) / gs;
            double sy = (top + CaptainIcon.SIZE * texel / 2.0D) / gs;
            if (sx < -half * 3 || sy < -half * 3 || sx > map.width + half * 3
                    || sy > map.height + half * 3) continue;
            markers.add(new Marker(captain, sx, sy));
        }

        Marker hovered = null;
        if (!boxing && !overPanel(mouseX, mouseY)) {
            double best = (half + 2) * (half + 2);
            for (Marker marker : markers) {
                double d = sq(marker.x - mouseX) + sq(marker.y - mouseY);
                if (d <= best) {
                    best = d;
                    hovered = marker;
                }
            }
        }

        // 1. order lines, under everything
        double phase = (now % 100000L) / 60.0D;
        for (Marker marker : markers) {
            double[] target = ClientCaptainStore.orderTarget(marker.captain.captainId);
            if (target == null) continue;
            double tx = view.realX(target[0]) / gs;
            double ty = view.realY(target[1]) / gs;
            int colour = engaging(marker.captain) ? ATTACK_COLOUR
                    : CaptainIcon.accent(marker.captain.faction);
            MapDraw.line(marker.x, marker.y, tx, ty, 1.5F,
                    MapDraw.withAlpha(colour, 0.55F), 4, -phase);
            MapDraw.ring(tx, ty, 3, 1.5F, MapDraw.withAlpha(colour, 0.8F));
        }

        // 2. where the selection would go
        SelectionModel selection = ClientCaptainStore.selection();
        boolean aiming = !selection.isEmpty() && !boxing && hovered == null
                && !overPanel(mouseX, mouseY) && Mouse.isInsideWindow();
        if (aiming) {
            for (Marker marker : markers) {
                if (!selection.contains(marker.captain.captainId)) continue;
                MapDraw.line(marker.x, marker.y, mouseX, mouseY, 1F,
                        MapDraw.withAlpha(SELECT, 0.28F), 3, 0);
            }
        }

        // 3. pings where orders went
        for (Iterator<Ping> it = pings.iterator(); it.hasNext(); ) {
            Ping ping = it.next();
            float t = (now - ping.at) / 700F;
            if (t >= 1F) {
                it.remove();
                continue;
            }
            double px = view.realX(ping.x) / gs;
            double py = view.realY(ping.z) / gs;
            MapDraw.ring(px, py, 3 + 14 * t, 2F, MapDraw.withAlpha(ping.colour, 1F - t));
            MapDraw.ring(px, py, 2 + 7 * t, 1.5F, MapDraw.withAlpha(ping.colour, (1F - t) * 0.6F));
        }

        // 4. markers
        List<UUID> inBox = boxing ? boxed(markers, view, mouseRealX, mouseRealY)
                : Collections.<UUID>emptyList();
        float pulse = (float) (0.5 + 0.5 * Math.sin(now / 180.0D));
        for (Marker marker : markers) {
            CaptainView captain = marker.captain;
            boolean selected = selection.contains(captain.captainId);
            boolean boxedNow = inBox.contains(captain.captainId);
            if (selected) {
                MapDraw.dot(marker.x, marker.y + half * 0.2, half + 1.5,
                        MapDraw.withAlpha(SELECT, 0.18F + 0.10F * pulse));
            }
            if (boxedNow) {
                MapDraw.ring(marker.x, marker.y, half + 2.5, 1.5F, MapDraw.withAlpha(SELECT, 0.9F));
            }
            if (marker == hovered) {
                MapDraw.ring(marker.x, marker.y, half + 2, 1.5F,
                        captain.commandable ? 0xFFFFFFFF : 0xAAFFFFFF);
            }
            double yawRad = Math.toRadians(captain.heading);
            double screenAngle = Math.atan2(Math.cos(yawRad), -Math.sin(yawRad));
            MapDraw.pointer(marker.x, marker.y, screenAngle, half + 0.5, 2.5,
                    MapDraw.withAlpha(CaptainIcon.accent(captain.faction), 0.95F));
            MapDraw.icon(CaptainIcon.hatColour(captain.faction), marker.x, marker.y,
                    iconSize, captain.commandable ? 1F : 0.85F);
            if (selected) {
                MapDraw.brackets(marker.x, marker.y, half + 2.5, Math.max(3, half * 0.6), 1,
                        SELECT);
            }
            int badge = stateColour(captain.orderState);
            if (badge != 0) {
                MapDraw.dot(marker.x + half * 0.75, marker.y + half * 0.75, 2.6, 0xE0101014);
                MapDraw.dot(marker.x + half * 0.75, marker.y + half * 0.75, 1.8, badge);
            }
            int group = selection.groupOf(captain.captainId);
            if (group >= 0) {
                FontRenderer font = mc.fontRenderer;
                GlStateManager.pushMatrix();
                GlStateManager.translate(marker.x - half - 1, marker.y - half - 2, 0);
                GlStateManager.scale(0.75, 0.75, 1);
                font.drawStringWithShadow(Integer.toString(group), 0, 0, 0xFFFFFFFF);
                GlStateManager.popMatrix();
            }
        }

        // 5. the box itself
        if (boxing) {
            double x0 = boxX0 / gs;
            double y0 = boxY0 / gs;
            MapDraw.rect(x0, y0, mouseX, mouseY, BOX_FILL);
            MapDraw.frame(x0, y0, mouseX, mouseY, 1, 0xDDFFD54A);
            String count = Integer.toString(inBox.size());
            mc.fontRenderer.drawStringWithShadow(count,
                    (float) Math.max(x0, mouseX) + 3, (float) Math.max(y0, mouseY) - 4,
                    SELECT);
        }

        // 6. aiming reticle
        if (aiming) {
            MapDraw.ring(mouseX, mouseY, 5, 1.5F, SELECT);
            MapDraw.rect(mouseX - 8, mouseY - 0.5, mouseX - 6, mouseY + 0.5, SELECT);
            MapDraw.rect(mouseX + 6, mouseY - 0.5, mouseX + 8, mouseY + 0.5, SELECT);
            MapDraw.rect(mouseX - 0.5, mouseY - 8, mouseX + 0.5, mouseY - 6, SELECT);
            MapDraw.rect(mouseX - 0.5, mouseY + 6, mouseX + 0.5, mouseY + 8, SELECT);
            mc.fontRenderer.drawStringWithShadow("→ " + selection.size(),
                    (float) mouseX + 9, (float) mouseY + 6, SELECT);
        }

        // 7. panel, switch and news
        drawPanel(mc, map, now, mouseX, mouseY);
        drawModeSwitch(mc, map, mouseX, mouseY);
        drawToast(mc, map);

        // 8. tooltip last, over everything
        if (hovered != null) {
            GuiUtils.drawHoveringText(tooltip(hovered.captain, selection),
                    (int) mouseX, (int) mouseY, map.width, map.height, 220, mc.fontRenderer);
            GlStateManager.disableLighting();
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    private static final class Marker {
        final CaptainView captain;
        final double x;
        final double y;

        Marker(CaptainView captain, double x, double y) {
            this.captain = captain;
            this.x = x;
            this.y = y;
        }
    }

    private List<String> tooltip(CaptainView captain, SelectionModel selection) {
        List<String> lines = new ArrayList<>();
        String faction = "ALLY".equals(captain.faction) ? "§aAlly"
                : "ENEMY".equals(captain.faction) ? "§cEnemy" : "§7" + captain.faction;
        lines.add("§f" + captain.label);
        lines.add(faction + "§7  ·  " + stateName(captain.orderState));
        if (hasActiveOrder(captain)) {
            lines.add(engaging(captain) ? "§6Attacks enemies on the way"
                    : "§9Passive cruise §8- does not turn after enemies");
        }
        int group = selection.groupOf(captain.captainId);
        if (group >= 0) lines.add("§7Group " + group);
        if (!captain.commandable) {
            lines.add("§8Does not obey your orders");
        } else if (selection.contains(captain.captainId)) {
            lines.add("§eSelected §8· Shift+click removes");
        } else {
            lines.add("§8Click selects · Shift+click adds");
        }
        return lines;
    }

    private void drawPanel(Minecraft mc, GuiMap map, long now, double mouseX, double mouseY) {
        FontRenderer font = mc.fontRenderer;
        SelectionModel selection = ClientCaptainStore.selection();
        int x = 6;
        int y = map.width < 450 ? 26 : 6;
        int width = PANEL_RIGHT - x;
        List<String[]> rows = new ArrayList<>();
        List<UUID> ids = selection.ids();
        int shown = Math.min(ids.size(), 6);
        for (int i = 0; i < shown; i++) {
            CaptainView view = ClientCaptainStore.byId(ids.get(i));
            if (view == null) continue;
            rows.add(new String[]{view.label, stateName(view.orderState), view.faction,
                    Integer.toString(stateColour(view.orderState))});
        }
        List<String> help = new ArrayList<>();
        if (showHelp) {
            help.add("§eClick§7  select captain");
            help.add("§eShift+click§7  add / remove");
            help.add("§eCtrl+drag§7  box select (+Shift adds)");
            help.add("§eDouble-click§7  all visible allies");
            help.add("§eRight-click§7  sail there in formation");
            help.add("§eX§7  stop course  §eEsc§7  deselect");
            help.add("§eCtrl+A§7  all allies");
            help.add("§eCtrl+1..9§7  save group");
            help.add("§e1..9§7  select, twice to show");
            help.add("§eSpace§7  show selected");
        }
        int lineH = 11;
        List<CaptainView> underOrders = selectedWithOrders();
        boolean showButtons = !underOrders.isEmpty();
        int buttonH = 13;
        int height = 16 + (selection.isEmpty() ? lineH : rows.size() * lineH)
                + (ids.size() > shown ? lineH : 0)
                + (showButtons ? 5 + 3 * (buttonH + 2) : 0)
                + (help.isEmpty() ? 0 : 4 + help.size() * 10);
        MapDraw.rect(x, y, x + width, y + height, PANEL);
        MapDraw.frame(x, y, x + width, y + height, 1, PANEL_EDGE);

        String title = "§lFLEET";
        font.drawStringWithShadow(title, x + 5, y + 4, SELECT);
        String count = selection.size() + "/" + selection.capacity();
        font.drawStringWithShadow(count, x + 5 + font.getStringWidth(title) + 6, y + 4,
                selection.isEmpty() ? TEXT_DIM : TEXT);
        String helpLabel = showHelp ? "[x]" : "[?]";
        int helpX = x + width - 5 - font.getStringWidth(helpLabel);
        font.drawStringWithShadow(helpLabel, helpX, y + 4, TEXT_DIM);
        headerX0 = x;
        headerY0 = y;
        headerX1 = x + width;
        headerY1 = y + 14;
        panelBottom = y + height;

        int row = y + 16;
        if (selection.isEmpty()) {
            font.drawStringWithShadow("§8Click or Ctrl+drag to select", x + 5, row, TEXT_DIM);
            row += lineH;
        } else {
            for (String[] r : rows) {
                MapDraw.icon(CaptainIcon.hatColour(r[2]), x + 9, row + 3.5, 8, 1F);
                String name = font.trimStringToWidth(r[0], 92);
                font.drawStringWithShadow(name, x + 16, row, TEXT);
                int colour = Integer.parseInt(r[3]);
                font.drawStringWithShadow(r[1], x + width - 5 - font.getStringWidth(r[1]), row,
                        colour == 0 ? TEXT_DIM : colour);
                row += lineH;
            }
            if (ids.size() > shown) {
                font.drawStringWithShadow("§7+" + (ids.size() - shown) + " more", x + 16, row, TEXT_DIM);
                row += lineH;
            }
        }
        if (showButtons) {
            MapDraw.rect(x + 4, row + 1, x + width - 4, row + 2, PANEL_EDGE);
            row += 5;
            int engaging = 0;
            for (CaptainView view : underOrders) if (engaging(view)) engaging++;
            // How much of the selection each mode covers: lit in full when all
            // of it, dimly when only some - a mixed fleet shows as mixed.
            float[] lit = {0F, engaging / (float) underOrders.size(),
                    (underOrders.size() - engaging) / (float) underOrders.size()};
            buttons = new int[3][];
            for (int i = 0; i < 3; i++) {
                int x0 = x + 4;
                int x1 = x + width - 4;
                int y0 = row;
                int y1 = row + buttonH;
                buttons[i] = new int[]{x0, y0, x1, y1};
                boolean hover = mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1;
                int colour = BUTTON_COLOURS[i];
                float fill = lit[i] >= 1F ? 0.34F : lit[i] > 0F ? 0.16F : 0.06F;
                if (hover) fill += 0.14F;
                MapDraw.rect(x0, y0, x1, y1, MapDraw.withAlpha(colour, fill));
                MapDraw.frame(x0, y0, x1, y1, 1, MapDraw.withAlpha(colour,
                        lit[i] >= 1F || hover ? 0.95F : 0.45F));
                MapDraw.rect(x0 + 1, y0 + 1, x0 + 3, y1 - 1, colour);
                font.drawStringWithShadow(BUTTON_LABELS[i], x0 + 7, y0 + 3,
                        lit[i] > 0F || hover || i == BUTTON_STOP ? TEXT : TEXT_DIM);
                if (i == BUTTON_STOP) {
                    font.drawStringWithShadow("X", x1 - 4 - font.getStringWidth("X"), y0 + 3,
                            TEXT_DIM);
                } else if (lit[i] >= 1F) {
                    MapDraw.dot(x1 - 7, y0 + buttonH / 2.0D, 2.2, colour);
                } else if (lit[i] > 0F) {
                    String part = Math.round(lit[i] * underOrders.size()) + "/"
                            + underOrders.size();
                    font.drawStringWithShadow(part, x1 - 4 - font.getStringWidth(part), y0 + 3,
                            colour);
                }
                if (hover) {
                    String tip = BUTTON_HINTS[i];
                    int tx = x1 + 6;
                    MapDraw.rect(tx - 3, y0, tx + font.getStringWidth(tip) + 3, y1, PANEL);
                    font.drawStringWithShadow(tip, tx, y0 + 3, TEXT);
                }
                row += buttonH + 2;
            }
        } else {
            buttons = null;
        }
        if (!help.isEmpty()) {
            MapDraw.rect(x + 4, row + 1, x + width - 4, row + 2, PANEL_EDGE);
            row += 4;
            for (String line : help) {
                font.drawStringWithShadow(line, x + 5, row, TEXT);
                row += 10;
            }
        }
    }

    /** The two-part switch at the top: World map | Ship's map, and its key. */
    private void drawModeSwitch(Minecraft mc, GuiMap map, double mouseX, double mouseY) {
        FontRenderer font = mc.fontRenderer;
        MapModeSwitch sw = MapModeSwitch.layout(map.width,
                font.getStringWidth(MapModeSwitch.WORLD_LABEL), font.getStringWidth(MapModeSwitch.SHIP_LABEL),
                font.getStringWidth(MapModeSwitch.STORM_LABEL), map.width >= 450 ? PANEL_RIGHT + 8 : 4);
        if (ArmyClient.available() && XaeroFleetCommandMod.serverSupportsArmies()) {
            sw = MapModeSwitch.layout(map.width,
                    font.getStringWidth(MapModeSwitch.WORLD_LABEL), font.getStringWidth(MapModeSwitch.SHIP_LABEL),
                    font.getStringWidth(MapModeSwitch.STORM_LABEL), font.getStringWidth(MapModeSwitch.ARMY_LABEL),
                    map.width >= 550 ? PANEL_RIGHT + 8 : 4);
        }
        modeSwitch = sw;
        boolean ship = MapMode.isShipMap();
        int hover = sw.at(mouseX, mouseY);
        MapDraw.rect(sw.left(), MapModeSwitch.TOP, sw.right(), sw.bottom(), PANEL);
        for (int i = 0; i < sw.count(); i++) {
            int[] h = sw.half(i);
            boolean active = i == MapMode.current();
            if (active) {
                MapDraw.rect(h[0] + 1, h[1] + 1, h[2] - 1, h[3] - 1, MapDraw.withAlpha(SELECT, 0.30F));
            } else if (hover == i) {
                MapDraw.rect(h[0] + 1, h[1] + 1, h[2] - 1, h[3] - 1, 0x22FFFFFF);
            }
            String label = i == MapModeSwitch.ARMY ? MapModeSwitch.ARMY_LABEL : i == MapModeSwitch.STORM ? MapModeSwitch.STORM_LABEL : i == MapModeSwitch.SHIP ? MapModeSwitch.SHIP_LABEL : MapModeSwitch.WORLD_LABEL;
            font.drawStringWithShadow(label, h[0] + MapModeSwitch.PAD, h[1] + 3,
                    active ? SELECT : hover == i ? TEXT : TEXT_DIM);
        }
        int mid = sw.half(MapModeSwitch.SHIP)[0];
        MapDraw.rect(mid, MapModeSwitch.TOP + 2, mid + 1, sw.bottom() - 2, PANEL_EDGE);
        MapDraw.frame(sw.left(), MapModeSwitch.TOP, sw.right(), sw.bottom(), 1,
                ship ? MapDraw.withAlpha(SELECT, 0.7F) : PANEL_EDGE);
        font.drawStringWithShadow("Tab", sw.right() + 5, MapModeSwitch.TOP + 3, TEXT_DIM);
    }

    private void setMapMode(int mode) {
        if (mode == MapMode.current()) return;
        MapMode.setMode(mode);
        boxing = false;
        emptyPress = false;
        tookRightPress = false;
        if (mode == MapModeSwitch.STORM) StormClient.request();
        if (mode == MapModeSwitch.SHIP) {
            // Captains straight away if the server allows, otherwise on the next refresh.
            refreshTicks = 0;
            requestCaptains();
        }
    }

    private void drawToast(Minecraft mc, GuiMap map) {
        String text = MapToast.text();
        if (text.isEmpty()) return;
        float alpha = MapToast.alpha();
        FontRenderer font = mc.fontRenderer;
        int width = font.getStringWidth(text) + 16;
        int x = (map.width - width) / 2;
        int y = map.height - 44;
        MapDraw.rect(x, y, x + width, y + 16, MapDraw.withAlpha(PANEL, alpha));
        MapDraw.rect(x, y, x + 2, y + 16, MapDraw.withAlpha(MapToast.colour(), alpha));
        if (alpha > 0.05F) {
            GlStateManager.enableBlend();
            font.drawStringWithShadow(text, x + 9, y + 4,
                    MapDraw.withAlpha(MapToast.colour(), Math.max(0.1F, alpha)));
            GlStateManager.disableBlend();
        }
    }

    private boolean overPanel(double x, double y) {
        return x >= headerX0 && x <= headerX1 && y >= headerY0 && y <= panelBottom;
    }

    // ----------------------------------------------------------------- mouse

    @SubscribeEvent
    public void beforeMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiMap) || lastView == null
                || !XaeroFleetCommandMod.serverSupportsFleet()) return;
        GuiMap map = (GuiMap) event.getGui();
        MapViewTransform view = lastView;
        double realX = Mouse.getEventX();
        double realY = Minecraft.getMinecraft().displayHeight - Mouse.getEventY() - 1;
        double guiX = realX / view.guiScale;
        double guiY = realY / view.guiScale;
        int button = Mouse.getEventButton();
        boolean down = Mouse.getEventButtonState();

        if (button == 0 && !down && tookSwitchPress) {
            tookSwitchPress = false;
            event.setCanceled(true);
            return;
        }
        if (button == 0 && down && !boxing && modeSwitch != null) {
            int half = modeSwitch.at(guiX, guiY);
            if (half != MapModeSwitch.NONE) {
                setMapMode(half);
                tookSwitchPress = true;
                event.setCanceled(true);
                return;
            }
        }
        if (!MapMode.isShipMap()) return;

        if (boxing) {
            if (button == 0 && !down) finishBox(view, realX, realY);
            // Xaero must not pan while the box is being drawn.
            event.setCanceled(true);
            return;
        }
        if (button == 0 && down) {
            if (overPanel(guiX, guiY)) {
                if (guiY <= headerY1) {
                    showHelp = !showHelp;
                } else {
                    pressButton(buttonAt(guiX, guiY));
                }
                event.setCanceled(true);
                return;
            }
            if (GuiScreen.isCtrlKeyDown()) {
                boxing = true;
                boxAdd = GuiScreen.isShiftKeyDown();
                boxX0 = realX;
                boxY0 = realY;
                emptyPress = false;
                event.setCanceled(true);
                return;
            }
            CaptainView hit = commandableAt(map, view, guiX, guiY);
            if (hit != null) {
                clickCaptain(map, view, hit);
                event.setCanceled(true);
                return;
            }
            // Empty water: Xaero pans on a drag; a click here clears.
            emptyPress = true;
            pressX = realX;
            pressY = realY;
            return;
        }
        if (button == 0 && !down) {
            if (emptyPress) {
                emptyPress = false;
                if (Math.hypot(realX - pressX, realY - pressY) < CLICK_SLOP
                        && !GuiScreen.isShiftKeyDown()) {
                    ClientCaptainStore.selection().clear();
                }
            }
            return;
        }
        if (button == 1 && down) {
            SelectionModel selection = ClientCaptainStore.selection();
            if (selection.isEmpty() || overPanel(guiX, guiY)) return;
            order(view, view.worldXAtPixel(realX), view.worldZAtPixel(realY));
            tookRightPress = true;
            event.setCanceled(true);
            return;
        }
        if (button == 1 && !down && tookRightPress) {
            tookRightPress = false;
            event.setCanceled(true);
        }
    }

    private void clickCaptain(GuiMap map, MapViewTransform view, CaptainView hit) {
        SelectionModel selection = ClientCaptainStore.selection();
        long now = System.currentTimeMillis();
        boolean shift = GuiScreen.isShiftKeyDown();
        if (hit.captainId.equals(lastClickId) && now - lastClickAt < DOUBLE_CLICK_MS) {
            // Double click: every captain of his colour on the screen.
            List<UUID> same = new ArrayList<>();
            for (CaptainView captain : visibleCommandable(map, view)) {
                if (captain.faction.equals(hit.faction)) same.add(captain.captainId);
            }
            int dropped = selection.apply(nearestFirst(same, hit.x, hit.z),
                    shift ? SelectionModel.Mode.ADD : SelectionModel.Mode.REPLACE);
            announceDropped(dropped);
            lastClickId = null;
            return;
        }
        lastClickId = hit.captainId;
        lastClickAt = now;
        selection.apply(hit.captainId,
                shift ? SelectionModel.Mode.TOGGLE : SelectionModel.Mode.REPLACE);
    }

    private void finishBox(MapViewTransform view, double realX, double realY) {
        boxing = false;
        SelectionModel selection = ClientCaptainStore.selection();
        if (Math.hypot(realX - boxX0, realY - boxY0) < CLICK_SLOP) {
            // Ctrl-click on a captain toggles him; on water it does nothing.
            GuiScreen screen = Minecraft.getMinecraft().currentScreen;
            if (screen instanceof GuiMap) {
                CaptainView hit = commandableAt((GuiMap) screen, view,
                        realX / view.guiScale, realY / view.guiScale);
                if (hit != null) selection.apply(hit.captainId, SelectionModel.Mode.TOGGLE);
            }
            return;
        }
        List<UUID> inside = boxed(currentMarkers(view), view, realX, realY);
        int dropped = selection.apply(inside,
                boxAdd ? SelectionModel.Mode.ADD : SelectionModel.Mode.REPLACE);
        announceDropped(dropped);
    }

    /** Captains inside the box, nearest its middle first. */
    private List<UUID> boxed(List<Marker> markers, MapViewTransform view,
                             double realX, double realY) {
        double gs = view.guiScale;
        double x0 = boxX0 / gs;
        double y0 = boxY0 / gs;
        double x1 = realX / gs;
        double y1 = realY / gs;
        final double cx = (x0 + x1) / 2;
        final double cy = (y0 + y1) / 2;
        List<Marker> inside = new ArrayList<>();
        for (Marker marker : markers) {
            if (marker.captain.commandable
                    && SelectionModel.inBox(marker.x, marker.y, x0, y0, x1, y1)) {
                inside.add(marker);
            }
        }
        Collections.sort(inside, Comparator.comparingDouble(
                (Marker m) -> sq(m.x - cx) + sq(m.y - cy)));
        List<UUID> ids = new ArrayList<>();
        for (Marker marker : inside) ids.add(marker.captain.captainId);
        return ids;
    }

    private List<Marker> currentMarkers(MapViewTransform view) {
        long now = System.currentTimeMillis();
        List<Marker> markers = new ArrayList<>();
        for (CaptainView captain : ClientCaptainStore.captains()) {
            if (captain.dimension != view.dimension) continue;
            markers.add(new Marker(captain,
                    view.realX(ClientCaptainStore.displayX(captain, now)) / view.guiScale,
                    view.realY(ClientCaptainStore.displayZ(captain, now)) / view.guiScale));
        }
        return markers;
    }

    private CaptainView commandableAt(GuiMap map, MapViewTransform view, double x, double y) {
        CaptainView nearest = null;
        double best = 81;
        for (Marker marker : currentMarkers(view)) {
            if (!marker.captain.commandable) continue;
            double d = sq(marker.x - x) + sq(marker.y - y);
            if (d < best) {
                best = d;
                nearest = marker.captain;
            }
        }
        return nearest;
    }

    private List<CaptainView> visibleCommandable(GuiMap map, MapViewTransform view) {
        List<CaptainView> result = new ArrayList<>();
        for (Marker marker : currentMarkers(view)) {
            if (marker.captain.commandable && marker.x >= 0 && marker.y >= 0
                    && marker.x <= map.width && marker.y <= map.height) {
                result.add(marker.captain);
            }
        }
        return result;
    }

    private static List<UUID> nearestFirst(List<UUID> ids, final double x, final double z) {
        List<UUID> sorted = new ArrayList<>(ids);
        Collections.sort(sorted, Comparator.comparingDouble((UUID id) -> {
            CaptainView view = ClientCaptainStore.byId(id);
            return view == null ? Double.MAX_VALUE : sq(view.x - x) + sq(view.z - z);
        }));
        return sorted;
    }

    private static void announceDropped(int dropped) {
        if (dropped > 0) {
            MapToast.show("At most " + ClientCaptainStore.selection().capacity() + " can be selected"
                    + " - " + dropped + " left out", MapToast.INFO);
        }
    }

    // ---------------------------------------------------------------- orders

    private void order(MapViewTransform view, double x, double z) {
        List<UUID> ids = ClientCaptainStore.selectedIds();
        if (ids.isEmpty()) return;
        pendingOrder = new PendingOrder(ids, view.dimension, x, z);
        pings.add(new Ping(x, z, SELECT));
        ClientCaptainStore.rememberOrder(ids, x, z);
        flushPendingOrder();
    }

    private void flushPendingOrder() {
        PendingOrder order = pendingOrder;
        if (order == null) return;
        long now = System.currentTimeMillis();
        if (now - lastOrderAt < ORDER_GAP_MS) return;
        pendingOrder = null;
        lastOrderAt = now;
        FleetNetwork.CHANNEL.sendToServer(new MoveOrderPacket(order.ids, order.dimension,
                order.x, order.z, SelectionModel.orderRadius(order.ids.size())));
    }

    private int buttonAt(double x, double y) {
        if (buttons == null) return -1;
        for (int i = 0; i < buttons.length; i++) {
            int[] b = buttons[i];
            if (x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3]) return i;
        }
        return -1;
    }

    private void pressButton(int button) {
        if (button == BUTTON_STOP) {
            stopSelected();
        } else if (button == BUTTON_ATTACK) {
            setEngagement(true);
        } else if (button == BUTTON_PASSIVE) {
            setEngagement(false);
        }
    }

    /** Attack enemy / Passive cruise for every selected ship that has somewhere to go. */
    private void setEngagement(boolean engage) {
        List<UUID> ids = new ArrayList<>();
        for (CaptainView view : selectedWithOrders()) ids.add(view.captainId);
        if (ids.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (UUID id : ids) {
            engagementSent.put(id, new long[]{engage ? 1L : 0L, now});
            CaptainView view = ClientCaptainStore.byId(id);
            if (view != null) {
                pings.add(new Ping(view.x, view.z, engage ? ATTACK_COLOUR : PASSIVE_COLOUR));
            }
        }
        FleetNetwork.CHANNEL.sendToServer(new EngagementOrderPacket(ids, engage));
    }

    /** Selected captains with a destination: on the way to it, or holding it. */
    private List<CaptainView> selectedWithOrders() {
        List<CaptainView> result = new ArrayList<>();
        for (UUID id : ClientCaptainStore.selectedIds()) {
            CaptainView view = ClientCaptainStore.byId(id);
            if (view != null && hasActiveOrder(view)) result.add(view);
        }
        return result;
    }

    private static boolean hasActiveOrder(CaptainView view) {
        String state = view.orderState;
        return ClientCaptainStore.isUnderway(state) || "BLOCKED".equals(state)
                || "ARRIVED".equals(state)
                || ClientCaptainStore.orderTarget(view.captainId) != null;
    }

    /** Whether this ship attacks on her way, as last pressed or as the server says. */
    private boolean engaging(CaptainView view) {
        long[] sent = engagementSent.get(view.captainId);
        if (sent != null) {
            boolean pressed = sent[0] == 1L;
            if (System.currentTimeMillis() - sent[1] < ENGAGEMENT_UNCONFIRMED_MS
                    && pressed != view.isEngaging()) {
                return pressed;
            }
            engagementSent.remove(view.captainId);
        }
        return view.isEngaging();
    }

    private void stopSelected() {
        List<UUID> ids = new ArrayList<>();
        for (CaptainView view : selectedWithOrders()) ids.add(view.captainId);
        if (ids.isEmpty()) ids = ClientCaptainStore.selectedIds();
        if (ids.isEmpty()) return;
        pendingOrder = null;
        ClientCaptainStore.forgetOrders(ids);
        for (UUID id : ids) engagementSent.remove(id);
        for (UUID id : ids) {
            CaptainView view = ClientCaptainStore.byId(id);
            if (view != null) pings.add(new Ping(view.x, view.z, MapToast.BAD));
        }
        FleetNetwork.CHANNEL.sendToServer(new CancelOrderPacket(ids));
    }

    // -------------------------------------------------------------- keyboard

    @SubscribeEvent
    public void beforeKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiMap) || lastView == null
                || !XaeroFleetCommandMod.serverSupportsFleet()
                || !Keyboard.getEventKeyState()) return;
        GuiMap map = (GuiMap) event.getGui();
        if (map.getFocused() != null) return;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_TAB) {
            setMapMode((MapMode.current() + 1) % (ArmyClient.available() && XaeroFleetCommandMod.serverSupportsArmies() ? 4 : 3));
            event.setCanceled(true);
            return;
        }
        if (!MapMode.isShipMap()) return;
        SelectionModel selection = ClientCaptainStore.selection();
        boolean ctrl = GuiScreen.isCtrlKeyDown();
        boolean shift = GuiScreen.isShiftKeyDown();

        if (key == Keyboard.KEY_ESCAPE) {
            if (boxing || !selection.isEmpty()) {
                boxing = false;
                selection.clear();
                event.setCanceled(true);
            }
            return;
        }
        if (key == Keyboard.KEY_A && ctrl) {
            List<UUID> all = new ArrayList<>();
            for (CaptainView captain : ClientCaptainStore.captains()) {
                if (captain.commandable && captain.dimension == lastView.dimension) {
                    all.add(captain.captainId);
                }
            }
            double cx = lastView.worldXAtPixel(lastView.centreX);
            double cz = lastView.worldZAtPixel(lastView.centreY);
            announceDropped(selection.apply(nearestFirst(all, cx, cz),
                    SelectionModel.Mode.REPLACE));
            event.setCanceled(true);
            return;
        }
        if (key == Keyboard.KEY_X && !ctrl) {
            if (!selection.isEmpty()) {
                stopSelected();
                event.setCanceled(true);
            }
            return;
        }
        if (key == Keyboard.KEY_SPACE) {
            if (!selection.isEmpty()) {
                centreOnSelection(map);
                event.setCanceled(true);
            }
            return;
        }
        int group = groupKey(key);
        if (group >= 0) {
            long now = System.currentTimeMillis();
            if (ctrl) {
                selection.saveGroup(group);
                MapToast.show(selection.isEmpty() ? "Group " + group + " cleared"
                        : "Group " + group + ": " + selection.size() + " ships", MapToast.INFO);
            } else if (selection.hasGroup(group)) {
                selection.recallGroup(group, shift, ClientCaptainStore.commandableIds());
                if (group == lastGroupKey && now - lastGroupAt < DOUBLE_TAP_MS) {
                    centreOnSelection(map);
                }
            } else {
                return;
            }
            lastGroupKey = group;
            lastGroupAt = now;
            event.setCanceled(true);
        }
    }

    private static int groupKey(int key) {
        if (key >= Keyboard.KEY_1 && key <= Keyboard.KEY_9) return key - Keyboard.KEY_1 + 1;
        if (key == Keyboard.KEY_0) return 0;
        return -1;
    }

    private void centreOnSelection(GuiMap map) {
        double x = 0;
        double z = 0;
        int n = 0;
        for (UUID id : ClientCaptainStore.selectedIds()) {
            CaptainView view = ClientCaptainStore.byId(id);
            if (view == null) continue;
            x += view.x;
            z += view.z;
            n++;
        }
        if (n > 0 && lastView != null) lastView.centreOn(map, x / n, z / n);
    }

    // ----------------------------------------------------------------- words

    static String stateName(String state) {
        if (state == null) return "";
        switch (state) {
            case "NONE": return "idle";
            case "QUEUED":
            case "PLANNING": return "planning";
            case "SAILING": return "sailing";
            case "ARRIVED": return "arrived";
            case "BLOCKED": return "blocked";
            case "FAILED": return "failed";
            case "CANCELLED": return "stopped";
            default: return state.toLowerCase(java.util.Locale.ROOT);
        }
    }

    static int stateColour(String state) {
        if (state == null) return 0;
        switch (state) {
            case "QUEUED":
            case "PLANNING": return 0xFFFFD54A;
            case "SAILING": return 0xFF5AB0FF;
            case "ARRIVED": return 0xFF6BE08A;
            case "BLOCKED":
            case "FAILED": return 0xFFFF6B5A;
            default: return 0;
        }
    }

    private static double sq(double v) {
        return v * v;
    }
}
