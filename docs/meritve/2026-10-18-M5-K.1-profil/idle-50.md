# Povzetek skladov render niti: k-idle-50-2026-10-18-232924-klient-skladi.txt

Vzorcev: 12382.

### Faze slicice in deli upodobitve NPC-ja (prvo ujemanje)

| skupina | vzorcev | % |
|---|---:|---:|
| NPC: oci (LayerEyes) | 879 | 7.1 |
| NPC: ime in naslov | 2070 | 16.7 |
| NPC: senca in ogenj | 915 | 7.4 |
| NPC: pokrivalo (LayerHeadwear, Model2DRenderer) | 1724 | 13.9 |
| NPC: sloji CNPC (ostali deli modela) | 345 | 2.8 |
| NPC: model (ModelBase.render in deli) | 2753 | 22.2 |
| NPC: ostalo v doRender (matrike, stanje, animacija) | 366 | 3.0 |
| entitete: ostalo (frustum, seznam, ne-NPC) | 240 | 1.9 |
| teren (sloji blokov, chunki) | 841 | 6.8 |
| nebo, oblaki, vreme, delci | 247 | 2.0 |
| HUD (GuiIngame) | 58 | 0.5 |
| zamenjava medpomnilnika (Display.update) | 1044 | 8.4 |
| klientski tick (runTick) | 137 | 1.1 |
| izris: ostalo (EntityRenderer) | 390 | 3.1 |
| ostalo | 373 | 3.0 |

### Lastni cas po metodi (list sklada)

| metoda | vzorcev | % |
|---|---:|---:|
| `org.lwjgl.opengl.GL11.nglCallList` | 3931 | 31.7 |
| `org.lwjgl.opengl.GL11.nglDrawArrays` | 1533 | 12.4 |
| `org.lwjgl.opengl.WindowsContextImplementation.nSwapBuffers` | 1028 | 8.3 |
| `org.lwjgl.opengl.GL11.nglBegin` | 997 | 8.1 |
| `org.lwjgl.opengl.GL11.nglVertexPointer` | 674 | 5.4 |
| `org.lwjgl.opengl.GL11.nglPushMatrix` | 469 | 3.8 |
| `org.lwjgl.opengl.GL11.nglVertex3f` | 249 | 2.0 |
| `org.lwjgl.opengl.GL11.nglBindTexture` | 244 | 2.0 |
| `org.lwjgl.opengl.GL11.nglColor4f` | 236 | 1.9 |
| `org.lwjgl.opengl.GL11.nglEnable` | 236 | 1.9 |
| `org.lwjgl.opengl.GL11.nglTranslatef` | 229 | 1.8 |
| `org.lwjgl.opengl.GL11.nglClear` | 204 | 1.6 |
| `org.lwjgl.opengl.GL11.nglRotatef` | 198 | 1.6 |
| `org.lwjgl.opengl.GL11.nglTexCoord2f` | 196 | 1.6 |
| `org.lwjgl.opengl.GL11.nglDisable` | 187 | 1.5 |
| `org.lwjgl.opengl.GL11.nglEnd` | 157 | 1.3 |
| `org.lwjgl.opengl.GL13.nglMultiTexCoord2f` | 147 | 1.2 |
| `org.lwjgl.opengl.GL13.nglActiveTexture` | 140 | 1.1 |
| `org.lwjgl.opengl.GL11.nglScalef` | 124 | 1.0 |
| `org.lwjgl.opengl.GL30.nglBindFramebuffer` | 116 | 0.9 |
| `org.lwjgl.opengl.GL11.nglLightfv` | 93 | 0.8 |
| `org.lwjgl.opengl.GL11.nglPopMatrix` | 86 | 0.7 |
| `org.lwjgl.opengl.GL11.nglTexSubImage2D` | 79 | 0.6 |
| `org.lwjgl.opengl.GL11.nglTexCoordPointerBO` | 64 | 0.5 |
| `org.lwjgl.opengl.GL13.nglClientActiveTexture` | 56 | 0.5 |
| `org.lwjgl.opengl.GL15.nglBindBuffer` | 55 | 0.4 |
| `org.lwjgl.opengl.GL11.nglDisableClientState` | 48 | 0.4 |
| `org.lwjgl.opengl.GL11.nglMultMatrixf` | 45 | 0.4 |
| `org.lwjgl.opengl.GL11.nglTexParameteri` | 43 | 0.3 |
| `org.lwjgl.opengl.GL11.nglDepthMask` | 41 | 0.3 |

### Vkljucni cas (noppes.*, net.minecraft*, org.lwjgl*)

| metoda | vzorcev | % |
|---|---:|---:|
| `net.minecraftforge.gradle.GradleStartCommon.launch` | 12382 | 100.0 |
| `net.minecraft.launchwrapper.Launch.main` | 12382 | 100.0 |
| `net.minecraft.launchwrapper.Launch.launch` | 12382 | 100.0 |
| `net.minecraft.client.main.Main.main` | 12382 | 100.0 |
| `net.minecraft.client.Minecraft.run` | 12382 | 100.0 |
| `net.minecraft.client.Minecraft.runGameLoop` | 12382 | 100.0 |
| `net.minecraft.client.renderer.EntityRenderer.updateCameraAndRender` | 10809 | 87.3 |
| `net.minecraft.client.renderer.EntityRenderer.renderWorld` | 10668 | 86.2 |
| `net.minecraft.client.renderer.EntityRenderer.renderWorldPass` | 10644 | 86.0 |
| `net.minecraft.client.renderer.RenderGlobal.renderEntities` | 9292 | 75.0 |
| `net.minecraft.client.renderer.entity.RenderManager.renderEntityStatic` | 9170 | 74.1 |
| `net.minecraft.client.renderer.entity.RenderManager.renderEntity` | 9052 | 73.1 |
| `noppes.npcs.client.renderer.RenderCustomNpc.doRender` | 8137 | 65.7 |
| `noppes.npcs.client.renderer.RenderNPCInterface.doRender` | 8137 | 65.7 |
| `net.minecraft.client.renderer.entity.RenderLiving.doRender` | 8136 | 65.7 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.doRender` | 8136 | 65.7 |
| `noppes.npcs.client.model.ModelScaleRenderer.render` | 4324 | 34.9 |
| `net.minecraft.client.renderer.GlStateManager.callList` | 3931 | 31.7 |
| `org.lwjgl.opengl.GL11.glCallList` | 3931 | 31.7 |
| `org.lwjgl.opengl.GL11.nglCallList` | 3931 | 31.7 |
| `noppes.npcs.client.renderer.RenderCustomNpc.renderLayers` | 2956 | 23.9 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.renderLayers` | 2956 | 23.9 |
| `noppes.npcs.client.layer.LayerInterface.doRenderLayer` | 2948 | 23.8 |
| `noppes.npcs.client.renderer.RenderCustomNpc.renderModel` | 2752 | 22.2 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderModel` | 2752 | 22.2 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.renderModel` | 2752 | 22.2 |
| `noppes.npcs.client.model.ModelPlayerAlt.render` | 2647 | 21.4 |
| `net.minecraft.client.renderer.entity.Render.doRender` | 2070 | 16.7 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderName` | 2070 | 16.7 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderLivingLabel` | 2067 | 16.7 |
| `net.minecraft.client.gui.FontRenderer.drawString` | 1870 | 15.1 |
| `net.minecraft.client.gui.FontRenderer.renderString` | 1870 | 15.1 |
| `noppes.npcs.client.layer.LayerHeadwear.render` | 1724 | 13.9 |
| `net.minecraft.client.renderer.Tessellator.draw` | 1697 | 13.7 |
| `net.minecraft.client.renderer.WorldVertexBufferUploader.draw` | 1697 | 13.7 |
| `net.minecraft.client.gui.FontRenderer.renderStringAtPos` | 1678 | 13.6 |
| `net.minecraft.client.gui.FontRenderer.renderChar` | 1678 | 13.6 |
| `net.minecraft.client.gui.FontRenderer.renderDefaultChar` | 1678 | 13.6 |
| `noppes.npcs.client.model.Model2DRenderer.render` | 1636 | 13.2 |
| `net.minecraft.client.renderer.GlStateManager.glDrawArrays` | 1533 | 12.4 |
| `org.lwjgl.opengl.GL11.glDrawArrays` | 1533 | 12.4 |
| `org.lwjgl.opengl.GL11.nglDrawArrays` | 1533 | 12.4 |
| `net.minecraft.client.Minecraft.updateDisplay` | 1044 | 8.4 |
| `org.lwjgl.opengl.Display.update` | 1044 | 8.4 |
| `org.lwjgl.opengl.Display.swapBuffers` | 1035 | 8.4 |
| `org.lwjgl.opengl.DrawableGL.swapBuffers` | 1035 | 8.4 |
| `org.lwjgl.opengl.ContextGL.swapBuffers` | 1035 | 8.4 |
| `org.lwjgl.opengl.WindowsContextImplementation.swapBuffers` | 1035 | 8.4 |
| `org.lwjgl.opengl.WindowsContextImplementation.nSwapBuffers` | 1028 | 8.3 |
| `net.minecraft.client.renderer.GlStateManager.glBegin` | 997 | 8.1 |
| `org.lwjgl.opengl.GL11.glBegin` | 997 | 8.1 |
| `org.lwjgl.opengl.GL11.nglBegin` | 997 | 8.1 |
| `noppes.npcs.client.renderer.RenderNPCInterface.doRenderShadowAndFire` | 915 | 7.4 |
| `net.minecraft.client.renderer.entity.Render.doRenderShadowAndFire` | 915 | 7.4 |
| `net.minecraft.client.renderer.entity.Render.renderShadow` | 915 | 7.4 |
| `noppes.npcs.client.layer.LayerEyes.render` | 879 | 7.1 |
| `net.minecraft.client.renderer.RenderGlobal.renderBlockLayer` | 841 | 6.8 |
| `net.minecraft.client.renderer.VboRenderList.renderChunkLayer` | 796 | 6.4 |
| `net.minecraft.client.renderer.vertex.VertexFormatElement$EnumUsage.preDraw` | 752 | 6.1 |
| `net.minecraftforge.client.ForgeHooksClient.preDraw` | 752 | 6.1 |
