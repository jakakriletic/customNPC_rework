# Povzetek skladov render niti: k-idle-500-2026-10-18-234241-klient-skladi.txt

Vzorcev: 39887.

### Faze slicice in deli upodobitve NPC-ja (prvo ujemanje)

| skupina | vzorcev | % |
|---|---:|---:|
| NPC: oci (LayerEyes) | 5654 | 14.2 |
| NPC: pokrivalo (LayerHeadwear, Model2DRenderer) | 10352 | 26.0 |
| NPC: sloji CNPC (ostali deli modela) | 2258 | 5.7 |
| NPC: vanilla sloji (oklep, predmet) | 10 | 0.0 |
| NPC: model (ModelBase.render in deli) | 15262 | 38.3 |
| NPC: ostalo v doRender (matrike, stanje, animacija) | 2166 | 5.4 |
| entitete: ostalo (frustum, seznam, ne-NPC) | 1130 | 2.8 |
| teren (sloji blokov, chunki) | 614 | 1.5 |
| nebo, oblaki, vreme, delci | 132 | 0.3 |
| HUD (GuiIngame) | 43 | 0.1 |
| zamenjava medpomnilnika (Display.update) | 637 | 1.6 |
| klientski tick (runTick) | 1139 | 2.9 |
| izris: ostalo (EntityRenderer) | 242 | 0.6 |
| ostalo | 248 | 0.6 |

### Lastni cas po metodi (list sklada)

| metoda | vzorcev | % |
|---|---:|---:|
| `org.lwjgl.opengl.GL11.nglCallList` | 22261 | 55.8 |
| `org.lwjgl.opengl.GL11.nglDrawArrays` | 3557 | 8.9 |
| `org.lwjgl.opengl.GL11.nglPushMatrix` | 3322 | 8.3 |
| `org.lwjgl.opengl.GL11.nglVertexPointer` | 1289 | 3.2 |
| `org.lwjgl.opengl.GL11.nglTranslatef` | 1173 | 2.9 |
| `org.lwjgl.opengl.GL13.nglMultiTexCoord2f` | 1142 | 2.9 |
| `org.lwjgl.opengl.GL11.nglRotatef` | 982 | 2.5 |
| `org.lwjgl.opengl.GL11.nglDisable` | 652 | 1.6 |
| `org.lwjgl.opengl.WindowsContextImplementation.nSwapBuffers` | 626 | 1.6 |
| `org.lwjgl.opengl.GL13.nglActiveTexture` | 617 | 1.5 |
| `org.lwjgl.opengl.GL11.nglEnable` | 595 | 1.5 |
| `org.lwjgl.opengl.GL11.nglScalef` | 573 | 1.4 |
| `org.lwjgl.opengl.GL11.nglPopMatrix` | 429 | 1.1 |
| `net.minecraft.network.datasync.EntityDataManager.get` | 326 | 0.8 |
| `net.minecraft.world.chunk.Chunk.getEntitiesWithinAABBForEntity` | 213 | 0.5 |
| `org.lwjgl.opengl.GL11.nglFogfv` | 165 | 0.4 |
| `org.lwjgl.opengl.GL11.nglShadeModel` | 130 | 0.3 |
| `org.lwjgl.opengl.GL11.nglClear` | 120 | 0.3 |
| `org.lwjgl.opengl.GL11.nglTexSubImage2D` | 108 | 0.3 |
| `org.lwjgl.opengl.GL15.nglBindBuffer` | 91 | 0.2 |
| `org.lwjgl.opengl.GL11.nglDepthMask` | 89 | 0.2 |
| `org.lwjgl.opengl.GL11.nglColor4f` | 83 | 0.2 |
| `org.lwjgl.opengl.GL11.nglDisableClientState` | 77 | 0.2 |
| `org.lwjgl.opengl.GL11.nglColorPointer` | 62 | 0.2 |
| `net.minecraft.client.renderer.RenderGlobal.renderEntities` | 62 | 0.2 |
| `org.lwjgl.opengl.GL30.nglBindFramebuffer` | 61 | 0.2 |
| `net.minecraft.world.World.isOutsideBuildHeight` | 53 | 0.1 |
| `org.lwjgl.opengl.GL11.nglEnableClientState` | 52 | 0.1 |
| `org.lwjgl.opengl.GL11.nglTexParameteri` | 52 | 0.1 |
| `org.lwjgl.opengl.GL11.nglMultMatrixf` | 51 | 0.1 |

### Vkljucni cas (noppes.*, net.minecraft*, org.lwjgl*)

| metoda | vzorcev | % |
|---|---:|---:|
| `net.minecraftforge.gradle.GradleStartCommon.launch` | 39887 | 100.0 |
| `net.minecraft.launchwrapper.Launch.main` | 39887 | 100.0 |
| `net.minecraft.launchwrapper.Launch.launch` | 39887 | 100.0 |
| `net.minecraft.client.main.Main.main` | 39887 | 100.0 |
| `net.minecraft.client.Minecraft.run` | 39887 | 100.0 |
| `net.minecraft.client.Minecraft.runGameLoop` | 39887 | 100.0 |
| `net.minecraft.client.renderer.EntityRenderer.updateCameraAndRender` | 37843 | 94.9 |
| `net.minecraft.client.renderer.EntityRenderer.renderWorld` | 37756 | 94.7 |
| `net.minecraft.client.renderer.EntityRenderer.renderWorldPass` | 37730 | 94.6 |
| `net.minecraft.client.renderer.RenderGlobal.renderEntities` | 36832 | 92.3 |
| `net.minecraft.client.renderer.entity.RenderManager.renderEntityStatic` | 36707 | 92.0 |
| `net.minecraft.client.renderer.entity.RenderManager.renderEntity` | 35702 | 89.5 |
| `noppes.npcs.client.renderer.RenderCustomNpc.doRender` | 35700 | 89.5 |
| `noppes.npcs.client.renderer.RenderNPCInterface.doRender` | 35685 | 89.5 |
| `net.minecraft.client.renderer.entity.RenderLiving.doRender` | 35683 | 89.5 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.doRender` | 35683 | 89.5 |
| `noppes.npcs.client.model.ModelScaleRenderer.render` | 24695 | 61.9 |
| `net.minecraft.client.renderer.GlStateManager.callList` | 22261 | 55.8 |
| `org.lwjgl.opengl.GL11.glCallList` | 22261 | 55.8 |
| `org.lwjgl.opengl.GL11.nglCallList` | 22261 | 55.8 |
| `noppes.npcs.client.renderer.RenderCustomNpc.renderLayers` | 18291 | 45.9 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.renderLayers` | 18291 | 45.9 |
| `noppes.npcs.client.layer.LayerInterface.doRenderLayer` | 18261 | 45.8 |
| `noppes.npcs.client.renderer.RenderCustomNpc.renderModel` | 15256 | 38.2 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderModel` | 15256 | 38.2 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.renderModel` | 15256 | 38.2 |
| `noppes.npcs.client.model.ModelPlayerAlt.render` | 15249 | 38.2 |
| `noppes.npcs.client.layer.LayerHeadwear.render` | 10352 | 26.0 |
| `noppes.npcs.client.model.Model2DRenderer.render` | 9745 | 24.4 |
| `noppes.npcs.client.layer.LayerEyes.render` | 5654 | 14.2 |
| `net.minecraft.client.renderer.Tessellator.draw` | 4637 | 11.6 |
| `net.minecraft.client.renderer.WorldVertexBufferUploader.draw` | 4637 | 11.6 |
| `net.minecraft.client.renderer.GlStateManager.glDrawArrays` | 3557 | 8.9 |
| `org.lwjgl.opengl.GL11.glDrawArrays` | 3557 | 8.9 |
| `org.lwjgl.opengl.GL11.nglDrawArrays` | 3557 | 8.9 |
| `net.minecraft.client.renderer.GlStateManager.pushMatrix` | 3322 | 8.3 |
| `org.lwjgl.opengl.GL11.glPushMatrix` | 3322 | 8.3 |
| `org.lwjgl.opengl.GL11.nglPushMatrix` | 3322 | 8.3 |
| `noppes.npcs.client.model.ModelScaleRenderer.postRender` | 1755 | 4.4 |
| `net.minecraft.client.renderer.vertex.VertexFormatElement$EnumUsage.preDraw` | 1404 | 3.5 |
| `net.minecraftforge.client.ForgeHooksClient.preDraw` | 1404 | 3.5 |
| `net.minecraft.client.renderer.GlStateManager.glVertexPointer` | 1305 | 3.3 |
| `org.lwjgl.opengl.GL11.glVertexPointer` | 1305 | 3.3 |
| `org.lwjgl.opengl.GL11.nglVertexPointer` | 1289 | 3.2 |
| `net.minecraft.client.model.ModelRenderer.postRender` | 1280 | 3.2 |
| `net.minecraft.client.renderer.GlStateManager$BooleanState.setState` | 1247 | 3.1 |
| `net.minecraft.client.renderer.GlStateManager.translate` | 1175 | 2.9 |
| `org.lwjgl.opengl.GL11.glTranslatef` | 1173 | 2.9 |
| `org.lwjgl.opengl.GL11.nglTranslatef` | 1173 | 2.9 |
| `net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords` | 1142 | 2.9 |
| `org.lwjgl.opengl.GL13.glMultiTexCoord2f` | 1142 | 2.9 |
| `org.lwjgl.opengl.GL13.nglMultiTexCoord2f` | 1142 | 2.9 |
| `net.minecraft.client.Minecraft.runTick` | 1141 | 2.9 |
| `net.minecraft.client.renderer.GlStateManager.rotate` | 982 | 2.5 |
| `org.lwjgl.opengl.GL11.glRotatef` | 982 | 2.5 |
| `org.lwjgl.opengl.GL11.nglRotatef` | 982 | 2.5 |
| `net.minecraft.world.World.updateEntities` | 922 | 2.3 |
| `net.minecraft.world.World.updateEntity` | 919 | 2.3 |
| `net.minecraft.world.World.updateEntityWithOptionalForce` | 914 | 2.3 |
| `noppes.npcs.entity.EntityCustomNpc.onUpdate` | 871 | 2.2 |
