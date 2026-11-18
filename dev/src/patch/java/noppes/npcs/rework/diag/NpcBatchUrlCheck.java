package noppes.npcs.rework.diag;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.ImageDownloadAlt;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.rework.far.SkinDownload;

/** Controller-only M14.3b6. Real loopback HTTP + original ImageDownloadAlt, no injected
 * downloader fields or sleep on the render thread. The response body stays latched until
 * two complete render callbacks have checked the pending original-renderer contract.
 * This is compatibility proof, not M12 downloader work or a cost measurement. */
final class NpcBatchUrlCheck implements AutoCloseable {
    private final HttpServer server;
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicInteger requests = new AtomicInteger(), failures = new AtomicInteger();
    private final byte[] png;
    private final ResourceLocation known;
    private final JsonArray rows, checks;
    private final String images, base;
    private volatile String httpError;
    private volatile boolean sent;
    private int stage;
    private NBTTagCompound display;
    private ResourceLocation texture, glow;
    private EntityCustomNpc entity;
    private boolean update, ok = true;

    NpcBatchUrlCheck(Minecraft mc, JsonArray rows, JsonArray checks, String images) throws Exception {
        this.rows = rows; this.checks = checks; this.images = images;
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        // Opaque contrasting pattern, deliberately unlike the default skin. Pure data fixture.
        for (int y = 0; y < 64; y++) { for (int x = 0; x < 64; x++) {
            image.setRGB(x, y, ((x / 4 + y / 4) & 1) == 0 ? 0xFF20D0E0 : 0xFFE03090);
        } }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", bytes)) { throw new IllegalStateException("PNG writer"); }
        png = bytes.toByteArray();
        known = mc.getTextureManager().getDynamicTextureLocation("m143b-url-known", new DynamicTexture(image));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/skin.png", exchange -> {
            requests.incrementAndGet();
            try {
                exchange.getResponseHeaders().set("Content-Type", "image/png");
                exchange.sendResponseHeaders(200, png.length);
                if (!release.await(90, TimeUnit.SECONDS)) { throw new IllegalStateException("HTTP body latch timeout"); }
                exchange.getResponseBody().write(png); sent = true;
            } catch (Exception e) { httpError = e.toString(); }
            finally { exchange.close(); }
        });
        server.createContext("/missing.png", exchange -> {
            failures.incrementAndGet();
            try { exchange.sendResponseHeaders(404, -1); }
            catch (Exception e) { httpError = e.toString(); }
            finally { exchange.close(); }
        });
        server.start();
    }

    boolean frame(Minecraft mc, Framebuffer fb, EntityCustomNpc npc) throws Exception {
        if (httpError != null) { throw new IllegalStateException(httpError); }
        RenderCustomNpc renderer = (RenderCustomNpc) mc.getRenderManager().getEntityRenderObject(npc);
        switch (stage) {
            case 0:
                entity = npc; display = npc.display.writeToNBT(new NBTTagCompound());
                texture = npc.textureLocation; glow = npc.textureGlowLocation; update = npc.updateClient;
                setUrl(npc, base + "/skin.png");
                renderer.getEntityTexture(npc); stage++; break;
            case 1:
                renderer.getEntityTexture(npc);
                if (requests.get() != 1 || !SkinDownload.pending(object(mc, npc))) { break; }
                witness(mc, npc, "pending-before", true, false);
                trial(mc, fb, npc, "url-pending", DefaultPlayerSkin.getDefaultSkinLegacy(), true);
                witness(mc, npc, "pending-after", true, false);
                stage++; break;
            case 2:
                witness(mc, npc, "pending-later-before", true, false);
                trial(mc, fb, npc, "url-pending-later", DefaultPlayerSkin.getDefaultSkinLegacy(), true);
                witness(mc, npc, "pending-later-after", true, false);
                check("body-held", !sent && release.getCount() == 1 && requests.get() == 1,
                    "requests=" + requests.get() + " sent=" + sent);
                release.countDown(); stage++; break;
            case 3:
                if (!sent || !ended(mc, npc, true)) { break; }
                trial(mc, fb, npc, "url-loaded", known, true);
                witness(mc, npc, "loaded", false, true);
                stage++; break;
            case 4:
                local(npc);
                trial(mc, fb, npc, "ordinary-after-url", DefaultPlayerSkin.getDefaultSkinLegacy(), false);
                setUrl(npc, base + "/missing.png"); renderer.getEntityTexture(npc); stage++; break;
            case 5:
                renderer.getEntityTexture(npc);
                if (failures.get() != 1 || !ended(mc, npc, false)) { break; }
                trial(mc, fb, npc, "url-failed", DefaultPlayerSkin.getDefaultSkinLegacy(), true);
                witness(mc, npc, "failed", false, false);
                stage++; break;
            case 6:
                local(npc);
                trial(mc, fb, npc, "ordinary-after-failure", DefaultPlayerSkin.getDefaultSkinLegacy(), false);
                check("http-counts", requests.get() == 1 && failures.get() == 1 && sent && httpError == null,
                    "successRequests=" + requests.get() + " missingRequests=" + failures.get() + " bytes=" + png.length);
                stage++; return true;
            default: throw new IllegalStateException("URL stage " + stage);
        }
        return false;
    }

    private void trial(Minecraft mc, Framebuffer fb, EntityCustomNpc npc, String key, ResourceLocation expected, boolean original) throws Exception {
        JsonObject row = NpcBatchSceneCheck.url(mc, fb, npc, key, expected, images + "-" + key, original);
        rows.add(row); check(key, row.get("success").getAsBoolean(), row.toString());
    }

    private void check(String key, boolean pass, String detail) {
        JsonObject c = new JsonObject(); c.addProperty("key", key); c.addProperty("ok", pass); c.addProperty("detail", detail);
        checks.add(c); ok &= pass; System.out.println("M143URL CHECK " + key + " ok=" + pass + " " + detail);
    }

    private static void setUrl(EntityCustomNpc npc, String url) {
        npc.display.setSkinUrl(url); npc.textureLocation = null; npc.textureGlowLocation = null;
    }
    private static void local(EntityCustomNpc npc) {
        npc.display.skinType = 0; npc.textureLocation = DefaultPlayerSkin.getDefaultSkinLegacy(); npc.textureGlowLocation = null;
    }
    private static ITextureObject object(Minecraft mc, EntityCustomNpc npc) {
        return npc.textureLocation == null ? null : mc.getTextureManager().getTexture(npc.textureLocation);
    }
    private static Object field(ITextureObject tex, String name) throws Exception {
        Field f = ImageDownloadAlt.class.getDeclaredField(name); f.setAccessible(true); return f.get(tex);
    }
    private static boolean ended(Minecraft mc, EntityCustomNpc npc, boolean image) throws Exception {
        ITextureObject tex = object(mc, npc);
        if (!(tex instanceof ImageDownloadAlt)) { return false; }
        Thread thread = (Thread) field(tex, "imageThread");
        return thread != null && !thread.isAlive() && (field(tex, "bufferedImage") != null) == image;
    }
    private void witness(Minecraft mc, EntityCustomNpc npc, String key, boolean pending, boolean image) throws Exception {
        ITextureObject tex = object(mc, npc);
        boolean type = tex instanceof ImageDownloadAlt;
        boolean hasImage = type && field(tex, "bufferedImage") != null;
        boolean alive = type && field(tex, "imageThread") instanceof Thread && ((Thread) field(tex, "imageThread")).isAlive();
        boolean uploaded = type && Boolean.TRUE.equals(field(tex, "textureUploaded"));
        check(key, type && SkinDownload.pending(tex) == pending && hasImage == image && alive == pending && uploaded == image,
            "ImageDownloadAlt=" + type + " pending=" + SkinDownload.pending(tex) + " image=" + hasImage + " threadAlive=" + alive + " uploaded=" + uploaded);
    }
    boolean success() { return ok; }
    @Override public void close() {
        release.countDown(); server.stop(0);
        if (entity != null) {
            entity.display.readToNBT(display); entity.textureLocation = texture; entity.textureGlowLocation = glow; entity.updateClient = update;
        }
        Minecraft.getMinecraft().getTextureManager().deleteTexture(known);
    }
}
