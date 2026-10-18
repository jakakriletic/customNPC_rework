# Povzetek skladov render niti: k-idle-200-2026-10-18-233532-klient-skladi.txt

Vzorcev: 39884.

### Faze slicice in deli upodobitve NPC-ja (prvo ujemanje)

| skupina | vzorcev | % |
|---|---:|---:|
| NPC: oci (LayerEyes) | 4013 | 10.1 |
| NPC: ime in naslov | 8178 | 20.5 |
| NPC: senca in ogenj | 1 | 0.0 |
| NPC: pokrivalo (LayerHeadwear, Model2DRenderer) | 7774 | 19.5 |
| NPC: sloji CNPC (ostali deli modela) | 1545 | 3.9 |
| NPC: vanilla sloji (oklep, predmet) | 1 | 0.0 |
| NPC: model (ModelBase.render in deli) | 12560 | 31.5 |
| NPC: ostalo v doRender (matrike, stanje, animacija) | 1547 | 3.9 |
| entitete: ostalo (frustum, seznam, ne-NPC) | 827 | 2.1 |
| teren (sloji blokov, chunki) | 1055 | 2.6 |
| nebo, oblaki, vreme, delci | 215 | 0.5 |
| HUD (GuiIngame) | 38 | 0.1 |
| zamenjava medpomnilnika (Display.update) | 844 | 2.1 |
| klientski tick (runTick) | 558 | 1.4 |
| izris: ostalo (EntityRenderer) | 373 | 0.9 |
| ostalo | 355 | 0.9 |

### Lastni cas po metodi (list sklada)

| metoda | vzorcev | % |
|---|---:|---:|
| `org.lwjgl.opengl.GL11.nglCallList` | 17918 | 44.9 |
| `org.lwjgl.opengl.GL11.nglBegin` | 3671 | 9.2 |
| `org.lwjgl.opengl.GL11.nglDrawArrays` | 2874 | 7.2 |
| `org.lwjgl.opengl.GL11.nglPushMatrix` | 1992 | 5.0 |
| `org.lwjgl.opengl.GL11.nglVertex3f` | 1098 | 2.8 |
| `org.lwjgl.opengl.GL11.nglTranslatef` | 955 | 2.4 |
| `org.lwjgl.opengl.GL11.nglVertexPointer` | 952 | 2.4 |
| `org.lwjgl.opengl.GL11.nglColor4f` | 868 | 2.2 |
| `org.lwjgl.opengl.WindowsContextImplementation.nSwapBuffers` | 833 | 2.1 |
| `org.lwjgl.opengl.GL11.nglTexCoord2f` | 813 | 2.0 |
| `org.lwjgl.opengl.GL13.nglMultiTexCoord2f` | 798 | 2.0 |
| `org.lwjgl.opengl.GL11.nglRotatef` | 796 | 2.0 |
| `org.lwjgl.opengl.GL11.nglEnd` | 762 | 1.9 |
| `org.lwjgl.opengl.GL11.nglEnable` | 682 | 1.7 |
| `org.lwjgl.opengl.GL11.nglDisable` | 680 | 1.7 |
| `org.lwjgl.opengl.GL11.nglBindTexture` | 670 | 1.7 |
| `org.lwjgl.opengl.GL11.nglScalef` | 530 | 1.3 |
| `org.lwjgl.opengl.GL13.nglActiveTexture` | 446 | 1.1 |
| `org.lwjgl.opengl.GL11.nglPopMatrix` | 330 | 0.8 |
| `org.lwjgl.opengl.GL11.nglClear` | 206 | 0.5 |
| `org.lwjgl.opengl.GL11.nglFogfv` | 137 | 0.3 |
| `org.lwjgl.opengl.GL11.nglDepthMask` | 122 | 0.3 |
| `net.minecraft.network.datasync.EntityDataManager.get` | 120 | 0.3 |
| `org.lwjgl.opengl.GL11.nglTexSubImage2D` | 106 | 0.3 |
| `org.lwjgl.opengl.GL30.nglBindFramebuffer` | 99 | 0.2 |
| `org.lwjgl.opengl.GL15.nglBindBuffer` | 96 | 0.2 |
| `org.lwjgl.opengl.GL11.nglDisableClientState` | 93 | 0.2 |
| `org.lwjgl.opengl.GL11.nglEnableClientState` | 86 | 0.2 |
| `org.lwjgl.opengl.GL11.nglShadeModel` | 84 | 0.2 |
| `org.lwjgl.opengl.GL11.nglLightfv` | 82 | 0.2 |

### Vkljucni cas (noppes.*, net.minecraft*, org.lwjgl*)

| metoda | vzorcev | % |
|---|---:|---:|
| `net.minecraftforge.gradle.GradleStartCommon.launch` | 39884 | 100.0 |
| `net.minecraft.launchwrapper.Launch.main` | 39884 | 100.0 |
| `net.minecraft.launchwrapper.Launch.launch` | 39884 | 100.0 |
| `net.minecraft.client.main.Main.main` | 39884 | 100.0 |
| `net.minecraft.client.Minecraft.run` | 39884 | 100.0 |
| `net.minecraft.client.Minecraft.runGameLoop` | 39884 | 100.0 |
| `net.minecraft.client.renderer.EntityRenderer.updateCameraAndRender` | 38111 | 95.6 |
| `net.minecraft.client.renderer.EntityRenderer.renderWorld` | 38000 | 95.3 |
| `net.minecraft.client.renderer.EntityRenderer.renderWorldPass` | 37962 | 95.2 |
| `net.minecraft.client.renderer.RenderGlobal.renderEntities` | 36446 | 91.4 |
| `net.minecraft.client.renderer.entity.RenderManager.renderEntityStatic` | 36316 | 91.1 |
| `net.minecraft.client.renderer.entity.RenderManager.renderEntity` | 35619 | 89.3 |
| `noppes.npcs.client.renderer.RenderCustomNpc.doRender` | 35618 | 89.3 |
| `noppes.npcs.client.renderer.RenderNPCInterface.doRender` | 35613 | 89.3 |
| `net.minecraft.client.renderer.entity.RenderLiving.doRender` | 35611 | 89.3 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.doRender` | 35611 | 89.3 |
| `noppes.npcs.client.model.ModelScaleRenderer.render` | 19640 | 49.2 |
| `net.minecraft.client.renderer.GlStateManager.callList` | 17918 | 44.9 |
| `org.lwjgl.opengl.GL11.glCallList` | 17918 | 44.9 |
| `org.lwjgl.opengl.GL11.nglCallList` | 17918 | 44.9 |
| `noppes.npcs.client.renderer.RenderCustomNpc.renderLayers` | 13344 | 33.5 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.renderLayers` | 13344 | 33.5 |
| `noppes.npcs.client.layer.LayerInterface.doRenderLayer` | 13332 | 33.4 |
| `noppes.npcs.client.renderer.RenderCustomNpc.renderModel` | 12557 | 31.5 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderModel` | 12556 | 31.5 |
| `net.minecraft.client.renderer.entity.RenderLivingBase.renderModel` | 12556 | 31.5 |
| `noppes.npcs.client.model.ModelPlayerAlt.render` | 12167 | 30.5 |
| `net.minecraft.client.renderer.entity.Render.doRender` | 8178 | 20.5 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderName` | 8178 | 20.5 |
| `noppes.npcs.client.renderer.RenderNPCInterface.renderLivingLabel` | 8177 | 20.5 |
| `noppes.npcs.client.layer.LayerHeadwear.render` | 7774 | 19.5 |
| `net.minecraft.client.gui.FontRenderer.drawString` | 7342 | 18.4 |
| `net.minecraft.client.gui.FontRenderer.renderString` | 7341 | 18.4 |
| `noppes.npcs.client.model.Model2DRenderer.render` | 7339 | 18.4 |
| `net.minecraft.client.gui.FontRenderer.renderStringAtPos` | 6598 | 16.5 |
| `net.minecraft.client.gui.FontRenderer.renderChar` | 6596 | 16.5 |
| `net.minecraft.client.gui.FontRenderer.renderDefaultChar` | 6596 | 16.5 |
| `noppes.npcs.client.layer.LayerEyes.render` | 4013 | 10.1 |
| `net.minecraft.client.renderer.GlStateManager.glBegin` | 3671 | 9.2 |
| `org.lwjgl.opengl.GL11.glBegin` | 3671 | 9.2 |
| `org.lwjgl.opengl.GL11.nglBegin` | 3671 | 9.2 |
| `net.minecraft.client.renderer.Tessellator.draw` | 3306 | 8.3 |
| `net.minecraft.client.renderer.WorldVertexBufferUploader.draw` | 3306 | 8.3 |
| `net.minecraft.client.renderer.GlStateManager.glDrawArrays` | 2874 | 7.2 |
| `org.lwjgl.opengl.GL11.glDrawArrays` | 2874 | 7.2 |
| `org.lwjgl.opengl.GL11.nglDrawArrays` | 2874 | 7.2 |
| `net.minecraft.client.renderer.GlStateManager.pushMatrix` | 1992 | 5.0 |
| `org.lwjgl.opengl.GL11.glPushMatrix` | 1992 | 5.0 |
| `org.lwjgl.opengl.GL11.nglPushMatrix` | 1992 | 5.0 |
| `net.minecraft.client.renderer.GlStateManager$BooleanState.setState` | 1362 | 3.4 |
| `noppes.npcs.client.model.ModelScaleRenderer.postRender` | 1249 | 3.1 |
| `net.minecraft.client.renderer.GlStateManager.glVertex3f` | 1098 | 2.8 |
| `org.lwjgl.opengl.GL11.glVertex3f` | 1098 | 2.8 |
| `org.lwjgl.opengl.GL11.nglVertex3f` | 1098 | 2.8 |
| `net.minecraft.client.renderer.vertex.VertexFormatElement$EnumUsage.preDraw` | 1093 | 2.7 |
| `net.minecraftforge.client.ForgeHooksClient.preDraw` | 1093 | 2.7 |
| `net.minecraft.client.renderer.RenderGlobal.renderBlockLayer` | 1055 | 2.6 |
| `net.minecraft.client.renderer.VboRenderList.renderChunkLayer` | 993 | 2.5 |
| `net.minecraft.client.renderer.GlStateManager.glVertexPointer` | 983 | 2.5 |
| `org.lwjgl.opengl.GL11.glVertexPointer` | 983 | 2.5 |
