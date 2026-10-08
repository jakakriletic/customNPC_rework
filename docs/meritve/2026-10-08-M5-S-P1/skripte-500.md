# Povzetek JFR: 2026-10-08-0925-skripte-500.jfr

Nit: `Server thread`. Trajanje posnetka: 301 s.
Vzorcev CPU niti: 1501 (vseh niti 1503). Alokacij niti (ocena iz TLAB vzorcev): 7782.3 MB = 25.9 MB/s (vse niti 7788.8 MB, dogodkov niti 3643).

### CPU: lastni cas po metodi (vrh sklada)

| metoda | vzorcev | % |
|---|---:|---:|
| `net.minecraft.world.chunk.Chunk.getEntitiesWithinAABBForEntity` | 361 | 24.1 |
| `java.util.HashMap$TreeNode.root` | 132 | 8.8 |
| `it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap.get` | 96 | 6.4 |
| `net.minecraft.world.chunk.Chunk.getEntitiesOfTypeWithinAABB` | 74 | 4.9 |
| `net.minecraft.world.chunk.Chunk.getBlockState` | 56 | 3.7 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks` | 47 | 3.1 |
| `com.google.common.base.Predicates$InstanceOfPredicate.apply` | 44 | 2.9 |
| `com.google.common.collect.Iterators$6.computeNext` | 41 | 2.7 |
| `java.util.IdentityHashMap.containsKey` | 37 | 2.5 |
| `net.minecraft.world.World.handleMaterialAcceleration` | 34 | 2.3 |
| `net.minecraft.entity.EntityLiving.updateEntityActionState` | 28 | 1.9 |
| `java.util.Collections$SetFromMap.contains` | 28 | 1.9 |
| `net.minecraft.util.ClassInheritanceMultiMap.initializeClassLookup` | 26 | 1.7 |
| `net.minecraft.util.math.AxisAlignedBB.offset` | 24 | 1.6 |
| `net.minecraft.world.World.getCollisionBoxes` | 20 | 1.3 |
| `net.minecraft.entity.Entity.isInLava` | 18 | 1.2 |
| `java.util.HashMap$HashIterator.<init>` | 17 | 1.1 |
| `noppes.npcs.api.wrapper.ItemStackWrapper.MCItem` | 17 | 1.1 |
| `net.minecraft.entity.EntityLivingBase.onEntityUpdate` | 17 | 1.1 |
| `net.minecraft.entity.EntityLivingBase.onUpdate` | 17 | 1.1 |
| `java.util.Arrays.copyOf` | 17 | 1.1 |
| `noppes.npcs.controllers.ScriptContainer.run` | 16 | 1.1 |
| `net.minecraft.entity.ai.EntityAITasks.canUse` | 15 | 1.0 |
| `net.minecraft.entity.Entity.setFlag` | 15 | 1.0 |
| `net.minecraft.block.state.BlockStateContainer$StateImplementation.addCollisionBoxToList` | 15 | 1.0 |

### CPU: lastni cas po vrstici

| mesto | vzorcev | % |
|---|---:|---:|
| `net.minecraft.world.chunk.Chunk.getEntitiesWithinAABBForEntity:989` | 298 | 19.9 |
| `java.util.HashMap$TreeNode.root:1848` | 132 | 8.8 |
| `it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap.get:454` | 86 | 5.7 |
| `net.minecraft.world.chunk.Chunk.getEntitiesOfTypeWithinAABB:1012` | 69 | 4.6 |
| `net.minecraft.world.chunk.Chunk.getBlockState:515` | 56 | 3.7 |
| `net.minecraft.world.chunk.Chunk.getEntitiesWithinAABBForEntity:971` | 49 | 3.3 |
| `com.google.common.base.Predicates$InstanceOfPredicate.apply:502` | 44 | 2.9 |
| `java.util.IdentityHashMap.containsKey:363` | 37 | 2.5 |
| `com.google.common.collect.Iterators$6.computeNext:615` | 32 | 2.1 |
| `net.minecraft.world.World.handleMaterialAcceleration:2416` | 29 | 1.9 |
| `java.util.Collections$SetFromMap.contains:5513` | 28 | 1.9 |
| `net.minecraft.util.ClassInheritanceMultiMap.initializeClassLookup:52` | 26 | 1.7 |
| `net.minecraft.util.math.AxisAlignedBB.offset:339` | 24 | 1.6 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks:97` | 24 | 1.6 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks:81` | 21 | 1.4 |
| `net.minecraft.entity.EntityLiving.updateEntityActionState:840` | 18 | 1.2 |
| `net.minecraft.entity.Entity.isInLava:1462` | 18 | 1.2 |
| `java.util.HashMap$HashIterator.<init>:1457` | 17 | 1.1 |
| `noppes.npcs.api.wrapper.ItemStackWrapper.MCItem:353` | 17 | 1.1 |
| `java.util.Arrays.copyOf:3181` | 17 | 1.1 |
| `noppes.npcs.controllers.ScriptContainer.run:133` | 16 | 1.1 |
| `net.minecraft.entity.Entity.setFlag:2669` | 15 | 1.0 |
| `net.minecraft.block.state.BlockStateContainer$StateImplementation.addCollisionBoxToList:463` | 15 | 1.0 |
| `noppes.npcs.ai.selector.NPCAttackSelector.apply:89` | 15 | 1.0 |
| `net.minecraft.world.World.getCollisionBoxes:1465` | 14 | 0.9 |

### CPU: vkljucni cas (noppes.* in net.minecraft*)

| metoda | vzorcev | % |
|---|---:|---:|
| `net.minecraft.server.MinecraftServer.tick` | 1501 | 100.0 |
| `net.minecraft.server.MinecraftServer.run` | 1501 | 100.0 |
| `net.minecraft.server.MinecraftServer.updateTimeLightAndEntities` | 1499 | 99.9 |
| `net.minecraft.server.dedicated.DedicatedServer.updateTimeLightAndEntities` | 1499 | 99.9 |
| `net.minecraft.world.World.updateEntities` | 1471 | 98.0 |
| `net.minecraft.world.WorldServer.updateEntities` | 1471 | 98.0 |
| `net.minecraft.world.WorldServer.updateEntityWithOptionalForce` | 1466 | 97.7 |
| `net.minecraft.world.World.updateEntity` | 1466 | 97.7 |
| `net.minecraft.world.World.updateEntityWithOptionalForce` | 1460 | 97.3 |
| `noppes.npcs.entity.EntityCustomNpc.onUpdate` | 1446 | 96.3 |
| `noppes.npcs.entity.EntityNPCInterface.onUpdate` | 1334 | 88.9 |
| `net.minecraft.entity.EntityLivingBase.onUpdate` | 1315 | 87.6 |
| `net.minecraft.entity.EntityLiving.onUpdate` | 1315 | 87.6 |
| `noppes.npcs.entity.EntityNPCInterface.onLivingUpdate` | 1103 | 73.5 |
| `net.minecraft.entity.EntityLiving.onLivingUpdate` | 845 | 56.3 |
| `net.minecraft.entity.EntityLivingBase.onLivingUpdate` | 843 | 56.2 |
| `net.minecraft.entity.EntityLivingBase.travel` | 519 | 34.6 |
| `noppes.npcs.entity.EntityNPCInterface.travel` | 519 | 34.6 |
| `noppes.npcs.entity.EntityNPCFlying.travel` | 519 | 34.6 |
| `net.minecraft.entity.Entity.move` | 472 | 31.4 |
| `net.minecraft.world.World.getCollisionBoxes` | 384 | 25.6 |
| `net.minecraft.world.World.getEntitiesInAABBexcluding` | 370 | 24.7 |
| `net.minecraft.world.chunk.Chunk.getEntitiesWithinAABBForEntity` | 361 | 24.1 |
| `net.minecraft.world.World.getEntitiesWithinAABB` | 302 | 20.1 |
| `net.minecraft.world.chunk.Chunk.getEntitiesOfTypeWithinAABB` | 282 | 18.8 |
| `net.minecraft.world.World.getEntitiesWithinAABBExcludingEntity` | 230 | 15.3 |
| `noppes.npcs.entity.EntityNPCInterface.onCollide` | 226 | 15.1 |
| `net.minecraft.entity.EntityLiving.updateEntityActionState` | 170 | 11.3 |
| `net.minecraft.entity.EntityLivingBase.collideWithNearbyEntities` | 144 | 9.6 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks` | 142 | 9.5 |
| `net.minecraft.world.World.getBlockState` | 139 | 9.3 |
| `net.minecraft.entity.Entity.onUpdate` | 139 | 9.3 |
| `net.minecraft.entity.EntityLivingBase.onEntityUpdate` | 127 | 8.5 |
| `net.minecraft.entity.EntityLiving.onEntityUpdate` | 127 | 8.5 |
| `net.minecraft.network.datasync.EntityDataManager.getEntry` | 124 | 8.3 |
| `net.minecraft.network.datasync.EntityDataManager.get` | 124 | 8.3 |
| `noppes.npcs.ModelEyeData.update` | 105 | 7.0 |
| `net.minecraft.world.gen.ChunkProviderServer.getLoadedChunk` | 96 | 6.4 |
| `net.minecraft.world.gen.ChunkProviderServer.loadChunk` | 96 | 6.4 |
| `net.minecraft.world.gen.ChunkProviderServer.provideChunk` | 96 | 6.4 |
| `net.minecraft.world.World.getChunkFromChunkCoords` | 96 | 6.4 |
| `net.minecraft.util.ClassInheritanceMultiMap.initializeClassLookup` | 91 | 6.1 |
| `net.minecraft.util.ClassInheritanceMultiMap$1.iterator` | 91 | 6.1 |
| `noppes.npcs.Server.sendAssociatedData` | 90 | 6.0 |
| `net.minecraft.world.World.getChunkFromBlockCoords` | 83 | 5.5 |
| `net.minecraft.entity.EntityLivingBase.getHealth` | 73 | 4.9 |
| `net.minecraft.entity.EntityLivingBase.isEntityAlive` | 68 | 4.5 |
| `noppes.npcs.entity.EntityNPCInterface.isEntityAlive` | 68 | 4.5 |
| `net.minecraft.entity.Entity.handleWaterMovement` | 56 | 3.7 |
| `net.minecraft.world.chunk.Chunk.getBlockState` | 56 | 3.7 |

### CPU: po prvem noppes okvirju od vrha sklada

| metoda | vzorcev | % |
|---|---:|---:|
| `noppes.npcs.entity.EntityNPCInterface.travel` | 497 | 33.1 |
| `noppes.npcs.entity.EntityNPCInterface.onLivingUpdate` | 266 | 17.7 |
| `noppes.npcs.entity.EntityNPCInterface.onCollide` | 163 | 10.9 |
| `noppes.npcs.entity.EntityNPCInterface.onUpdate` | 156 | 10.4 |
| `noppes.npcs.Server.sendAssociatedData` | 90 | 6.0 |
| `noppes.npcs.entity.EntityNPCInterface.isEntityAlive` | 68 | 4.5 |
| `(brez noppes okvirja)` | 53 | 3.5 |
| `noppes.npcs.ai.target.EntityAIClosestTarget.shouldExecute` | 37 | 2.5 |
| `noppes.npcs.controllers.ScriptContainer.run` | 27 | 1.8 |
| `noppes.npcs.entity.EntityNPCFlying.updateFallState` | 21 | 1.4 |
| `noppes.npcs.api.wrapper.ItemStackWrapper.MCItem` | 17 | 1.1 |
| `noppes.npcs.ai.selector.NPCAttackSelector.apply` | 15 | 1.0 |
| `noppes.npcs.rework.diag.DiagEventCollector.observePath` | 11 | 0.7 |
| `noppes.npcs.entity.EntityNPCInterface.isKilled` | 11 | 0.7 |
| `noppes.npcs.ai.EntityAIWaterNav.shouldExecute` | 9 | 0.6 |
| `noppes.npcs.ModelEyeData.update` | 9 | 0.6 |
| `noppes.npcs.entity.EntityCustomNpc.onUpdate` | 8 | 0.5 |
| `noppes.npcs.entity.EntityNPCInterface.isAttacking` | 7 | 0.5 |
| `noppes.npcs.entity.data.DataScript.runScript` | 5 | 0.3 |
| `noppes.npcs.entity.EntityNPCInterface.calculateStartYPos` | 4 | 0.3 |
| `noppes.npcs.entity.data.DataTimers.update` | 3 | 0.2 |
| `noppes.npcs.ScriptPlayerEventHandler$ForgeEventHandler.forgeEntity` | 3 | 0.2 |
| `noppes.npcs.ai.EntityAIAnimation.shouldExecute` | 3 | 0.2 |
| `noppes.npcs.entity.EntityNPCInterface.isInteracting` | 3 | 0.2 |
| `noppes.npcs.entity.EntityNPCInterface.getFaction` | 2 | 0.1 |

### Alokacije: po razredu objekta

| razred | MB | % |
|---|---:|---:|
| `net.minecraft.util.math.AxisAlignedBB` | 3402.8 | 43.7 |
| `net.minecraft.util.ClassInheritanceMultiMap$1` | 1372.8 | 17.6 |
| `java.util.LinkedHashMap$LinkedKeyIterator` | 596.0 | 7.7 |
| `java.util.ArrayList$Itr` | 432.8 | 5.6 |
| `java.util.ArrayList` | 301.2 | 3.9 |
| `java.lang.Object[]` | 257.4 | 3.3 |
| `noppes.npcs.api.event.NpcEvent$CollideEvent` | 232.4 | 3.0 |
| `net.minecraft.util.math.BlockPos` | 219.8 | 2.8 |
| `net.minecraft.inventory.EntityEquipmentSlot[]` | 146.2 | 1.9 |
| `net.minecraftforge.event.world.GetCollisionBoxesEvent` | 126.1 | 1.6 |
| `java.util.HashMap$ValueIterator` | 110.8 | 1.4 |
| `net.minecraftforge.event.entity.living.LivingEvent$LivingUpdateEvent` | 84.4 | 1.1 |
| `java.util.Arrays$ArrayList` | 68.9 | 0.9 |
| `net.minecraft.util.EntitySelectors$7` | 58.2 | 0.7 |
| `com.google.common.base.Predicate[]` | 48.8 | 0.6 |
| `java.lang.Integer` | 46.6 | 0.6 |
| `com.google.common.collect.Iterators$6` | 33.1 | 0.4 |
| `java.util.HashMap$Node` | 27.0 | 0.3 |
| `com.google.common.base.Predicates$AndPredicate` | 26.6 | 0.3 |
| `char[]` | 22.1 | 0.3 |
| `com.google.common.base.Predicates$InstanceOfPredicate` | 22.0 | 0.3 |
| `java.lang.Double` | 19.9 | 0.3 |
| `java.util.HashMap$Node[]` | 15.6 | 0.2 |
| `java.io.PrintWriter` | 15.4 | 0.2 |
| `java.io.StringWriter` | 13.2 | 0.2 |

### Alokacije: po prvem ne-JDK okvirju

| mesto | MB | % |
|---|---:|---:|
| `net.minecraft.util.math.AxisAlignedBB.offset:339` | 1815.5 | 23.3 |
| `net.minecraft.util.ClassInheritanceMultiMap.getByClass:122` | 1372.8 | 17.6 |
| `net.minecraft.util.math.AxisAlignedBB.grow:288` | 1244.0 | 16.0 |
| `net.minecraft.entity.ai.EntityAITasks.canUse:145` | 316.6 | 4.1 |
| `com.google.common.collect.Lists.newArrayList:88` | 237.0 | 3.0 |
| `net.minecraft.util.ClassInheritanceMultiMap.iterator:143` | 235.3 | 3.0 |
| `noppes.npcs.EventHooks.onNPCCollide:115` | 232.4 | 3.0 |
| `noppes.npcs.entity.data.DataTimers.update:91` | 212.5 | 2.7 |
| `net.minecraft.util.math.AxisAlignedBB.offset:334` | 176.7 | 2.3 |
| `noppes.npcs.entity.data.DataScript.runScript:57` | 173.1 | 2.2 |
| `net.minecraft.util.math.AxisAlignedBB.expand:245` | 166.5 | 2.1 |
| `net.minecraft.block.Block.addCollisionBoxToList:575` | 157.5 | 2.0 |
| `net.minecraft.inventory.EntityEquipmentSlot.values:3` | 146.2 | 1.9 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks:91` | 128.6 | 1.7 |
| `net.minecraft.world.World.getCollisionBoxes:1523` | 126.1 | 1.6 |
| `com.google.common.base.Predicates.asList:709` | 117.7 | 1.5 |
| `net.minecraft.entity.EntityLivingBase.onEntityUpdate:348` | 95.4 | 1.2 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks:112` | 88.7 | 1.1 |
| `net.minecraftforge.common.ForgeHooks.onLivingUpdate:581` | 84.4 | 1.1 |
| `net.minecraft.entity.Entity.move:987` | 75.6 | 1.0 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks:70` | 62.1 | 0.8 |
| `net.minecraft.util.EntitySelectors.getTeamCollisionPredicate:71` | 58.2 | 0.7 |
| `net.minecraft.entity.Entity.isInsideOfMaterial:1443` | 48.8 | 0.6 |
| `net.minecraft.entity.Entity.setAir:2680` | 46.6 | 0.6 |
| `noppes.npcs.controllers.ScriptContainer.run:169` | 46.6 | 0.6 |

### Alokacije: vkljucno (noppes.* in net.minecraft*)

| metoda | MB | % |
|---|---:|---:|
| `net.minecraft.server.MinecraftServer.tick` | 7782.3 | 100.0 |
| `net.minecraft.server.MinecraftServer.run` | 7782.3 | 100.0 |
| `net.minecraft.server.MinecraftServer.updateTimeLightAndEntities` | 7702.1 | 99.0 |
| `net.minecraft.server.dedicated.DedicatedServer.updateTimeLightAndEntities` | 7702.1 | 99.0 |
| `noppes.npcs.entity.EntityCustomNpc.onUpdate` | 7682.1 | 98.7 |
| `net.minecraft.world.World.updateEntityWithOptionalForce` | 7682.1 | 98.7 |
| `net.minecraft.world.WorldServer.updateEntityWithOptionalForce` | 7682.1 | 98.7 |
| `net.minecraft.world.World.updateEntity` | 7682.1 | 98.7 |
| `net.minecraft.world.World.updateEntities` | 7682.1 | 98.7 |
| `net.minecraft.world.WorldServer.updateEntities` | 7682.1 | 98.7 |
| `noppes.npcs.entity.EntityNPCInterface.onUpdate` | 6656.7 | 85.5 |
| `net.minecraft.entity.EntityLivingBase.onUpdate` | 6315.7 | 81.2 |
| `net.minecraft.entity.EntityLiving.onUpdate` | 6315.7 | 81.2 |
| `noppes.npcs.entity.EntityNPCInterface.onLivingUpdate` | 5565.8 | 71.5 |
| `net.minecraft.entity.EntityLivingBase.onLivingUpdate` | 4956.9 | 63.7 |
| `net.minecraft.entity.EntityLiving.onLivingUpdate` | 4956.9 | 63.7 |
| `net.minecraft.entity.EntityLivingBase.travel` | 3529.7 | 45.4 |
| `noppes.npcs.entity.EntityNPCInterface.travel` | 3529.7 | 45.4 |
| `noppes.npcs.entity.EntityNPCFlying.travel` | 3529.7 | 45.4 |
| `net.minecraft.entity.Entity.move` | 3346.1 | 43.0 |
| `net.minecraft.world.World.getCollisionBoxes` | 2556.4 | 32.8 |
| `net.minecraft.util.math.AxisAlignedBB.offset` | 1992.2 | 25.6 |
| `net.minecraft.block.Block.addCollisionBoxToList` | 1973.0 | 25.4 |
| `net.minecraft.block.state.BlockStateContainer$StateImplementation.addCollisionBoxToList` | 1973.0 | 25.4 |
| `net.minecraft.world.World.getEntitiesWithinAABB` | 1516.6 | 19.5 |
| `net.minecraft.world.chunk.Chunk.getEntitiesOfTypeWithinAABB` | 1488.0 | 19.1 |
| `net.minecraft.util.ClassInheritanceMultiMap.getByClass` | 1372.8 | 17.6 |
| `net.minecraft.util.math.AxisAlignedBB.grow` | 1244.0 | 16.0 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks` | 1031.8 | 13.3 |
| `net.minecraft.entity.EntityLiving.updateEntityActionState` | 1031.8 | 13.3 |
| `noppes.npcs.Server.sendAssociatedData` | 1025.4 | 13.2 |
| `noppes.npcs.ModelEyeData.update` | 1025.4 | 13.2 |
| `noppes.npcs.entity.EntityNPCInterface.onCollide` | 608.9 | 7.8 |
| `net.minecraft.util.math.AxisAlignedBB.shrink` | 532.9 | 6.8 |
| `net.minecraft.entity.EntityLivingBase.onEntityUpdate` | 519.3 | 6.7 |
| `net.minecraft.entity.EntityLiving.onEntityUpdate` | 519.3 | 6.7 |
| `net.minecraft.entity.Entity.onUpdate` | 519.3 | 6.7 |
| `net.minecraft.entity.Entity.isInLava` | 429.5 | 5.5 |
| `noppes.npcs.EventHooks.onNPCCollide` | 401.1 | 5.2 |
| `net.minecraft.entity.EntityLivingBase.collideWithNearbyEntities` | 395.4 | 5.1 |
| `net.minecraft.world.World.getEntitiesInAABBexcluding` | 385.9 | 5.0 |
| `noppes.npcs.ai.target.EntityAIClosestTarget.shouldExecute` | 351.8 | 4.5 |
| `net.minecraft.entity.Entity.handleWaterMovement` | 344.7 | 4.4 |
| `net.minecraft.entity.Entity.onEntityUpdate` | 328.4 | 4.2 |
| `net.minecraft.entity.ai.EntityAITasks.canUse` | 316.6 | 4.1 |
| `noppes.npcs.entity.data.DataScript.runScript` | 288.4 | 3.7 |
| `net.minecraft.util.ClassInheritanceMultiMap.iterator` | 235.3 | 3.0 |
| `net.minecraft.world.chunk.Chunk.getEntitiesWithinAABBForEntity` | 235.3 | 3.0 |
| `noppes.npcs.entity.data.DataTimers.update` | 212.5 | 2.7 |
| `net.minecraft.util.EntitySelectors.getTeamCollisionPredicate` | 202.5 | 2.6 |

### Alokacije: po prvem noppes okvirju od vrha sklada

| metoda | MB | % |
|---|---:|---:|
| `noppes.npcs.entity.EntityNPCInterface.travel` | 3346.9 | 43.0 |
| `noppes.npcs.Server.sendAssociatedData` | 1025.4 | 13.2 |
| `noppes.npcs.entity.EntityNPCInterface.onLivingUpdate` | 991.4 | 12.7 |
| `noppes.npcs.entity.EntityNPCInterface.onUpdate` | 749.9 | 9.6 |
| `noppes.npcs.ai.target.EntityAIClosestTarget.shouldExecute` | 351.8 | 4.5 |
| `noppes.npcs.EventHooks.onNPCCollide` | 232.4 | 3.0 |
| `noppes.npcs.entity.data.DataTimers.update` | 212.5 | 2.7 |
| `noppes.npcs.entity.EntityNPCInterface.onCollide` | 207.8 | 2.7 |
| `noppes.npcs.entity.EntityNPCFlying.updateFallState` | 182.8 | 2.3 |
| `noppes.npcs.entity.data.DataScript.runScript` | 173.1 | 2.2 |
| `noppes.npcs.controllers.ScriptContainer.run` | 115.3 | 1.5 |
| `noppes.npcs.ai.EntityAIWaterNav.shouldExecute` | 79.5 | 1.0 |
| `(brez noppes okvirja)` | 24.6 | 0.3 |
| `noppes.npcs.entity.EntityNPCInterface.writeEntityToNBT` | 17.6 | 0.2 |
| `noppes.npcs.ModelPartConfig.writeToNBT` | 9.0 | 0.1 |
| `noppes.npcs.EventHooks.onNPCTick` | 8.8 | 0.1 |
| `noppes.npcs.entity.data.DataAI.writeToNBT` | 6.8 | 0.1 |
| `noppes.npcs.controllers.data.Lines.writeToNBT` | 6.6 | 0.1 |
| `noppes.npcs.controllers.data.DataTransform.writeOptions` | 6.6 | 0.1 |
| `noppes.npcs.entity.data.DataRanged.writeToNBT` | 6.6 | 0.1 |
| `noppes.npcs.ai.EntityAIWatchClosest.shouldExecute` | 4.5 | 0.1 |
| `noppes.npcs.entity.EntityCustomNpc.writeToNBTOptional` | 4.4 | 0.1 |
| `noppes.npcs.entity.data.DataStats.writeToNBT` | 4.4 | 0.1 |
| `noppes.npcs.entity.data.DataDisplay.writeToNBT` | 2.4 | 0.0 |
| `noppes.npcs.entity.data.DataScenes.writeToNBT` | 2.4 | 0.0 |

### Druge niti (vzorcev CPU)

| nit | vzorcev |
|---|---:|
| `File IO Thread` | 2 |
