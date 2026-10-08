# Povzetek JFR: 2026-10-08-0925-idle-500.jfr

Nit: `Server thread`. Trajanje posnetka: 301 s.
Vzorcev CPU niti: 1469 (vseh niti 1473). Alokacij niti (ocena iz TLAB vzorcev): 6197.2 MB = 20.6 MB/s (vse niti 6222.3 MB, dogodkov niti 651).

### CPU: lastni cas po metodi (vrh sklada)

| metoda | vzorcev | % |
|---|---:|---:|
| `net.minecraft.world.chunk.Chunk.getEntitiesWithinAABBForEntity` | 225 | 15.3 |
| `java.util.HashMap$TreeNode.root` | 135 | 9.2 |
| `it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap.get` | 86 | 5.9 |
| `net.minecraft.world.chunk.Chunk.getEntitiesOfTypeWithinAABB` | 63 | 4.3 |
| `com.google.common.base.Predicates$InstanceOfPredicate.apply` | 48 | 3.3 |
| `java.util.ArrayList.iterator` | 48 | 3.3 |
| `java.util.concurrent.locks.ReentrantReadWriteLock$NonfairSync.readerShouldBlock` | 47 | 3.2 |
| `java.util.IdentityHashMap.containsKey` | 41 | 2.8 |
| `net.minecraft.world.chunk.Chunk.getBlockState` | 38 | 2.6 |
| `java.util.Collections$SetFromMap.contains` | 38 | 2.6 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks` | 36 | 2.5 |
| `com.google.common.collect.Iterators$6.computeNext` | 30 | 2.0 |
| `net.minecraft.entity.EntityLiving.updateEntityActionState` | 28 | 1.9 |
| `noppes.npcs.ModelEyeData.update` | 27 | 1.8 |
| `net.minecraft.block.state.BlockStateContainer$StateImplementation.addCollisionBoxToList` | 27 | 1.8 |
| `net.minecraft.entity.Entity.setFlag` | 25 | 1.7 |
| `net.minecraft.world.World.handleMaterialAcceleration` | 24 | 1.6 |
| `net.minecraft.entity.EntityLivingBase.onUpdate` | 24 | 1.6 |
| `net.minecraft.world.World.getCollisionBoxes` | 24 | 1.6 |
| `noppes.npcs.api.wrapper.ItemStackWrapper.MCItem` | 21 | 1.4 |
| `org.apache.commons.lang3.ObjectUtils.equals` | 20 | 1.4 |
| `net.minecraft.entity.EntityLivingBase.isElytraFlying` | 19 | 1.3 |
| `net.minecraft.world.World.getEntitiesInAABBexcluding` | 18 | 1.2 |
| `net.minecraft.util.math.Vec3i.equals` | 18 | 1.2 |
| `java.util.HashMap$HashIterator.<init>` | 16 | 1.1 |

### CPU: lastni cas po vrstici

| mesto | vzorcev | % |
|---|---:|---:|
| `net.minecraft.world.chunk.Chunk.getEntitiesWithinAABBForEntity:969` | 223 | 15.2 |
| `java.util.HashMap$TreeNode.root:1848` | 135 | 9.2 |
| `it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap.get:454` | 82 | 5.6 |
| `com.google.common.base.Predicates$InstanceOfPredicate.apply:502` | 48 | 3.3 |
| `java.util.ArrayList.iterator:842` | 48 | 3.3 |
| `net.minecraft.world.chunk.Chunk.getEntitiesOfTypeWithinAABB:1012` | 47 | 3.2 |
| `java.util.concurrent.locks.ReentrantReadWriteLock$NonfairSync.readerShouldBlock:682` | 47 | 3.2 |
| `java.util.IdentityHashMap.containsKey:363` | 41 | 2.8 |
| `net.minecraft.world.chunk.Chunk.getBlockState:515` | 38 | 2.6 |
| `java.util.Collections$SetFromMap.contains:5513` | 38 | 2.6 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks:81` | 36 | 2.5 |
| `noppes.npcs.ModelEyeData.update:58` | 27 | 1.8 |
| `net.minecraft.block.state.BlockStateContainer$StateImplementation.addCollisionBoxToList:463` | 27 | 1.8 |
| `net.minecraft.entity.Entity.setFlag:2669` | 25 | 1.7 |
| `net.minecraft.entity.EntityLivingBase.onUpdate:2398` | 24 | 1.6 |
| `com.google.common.collect.Iterators$6.computeNext:615` | 24 | 1.6 |
| `net.minecraft.entity.EntityLiving.updateEntityActionState:840` | 23 | 1.6 |
| `noppes.npcs.api.wrapper.ItemStackWrapper.MCItem:353` | 21 | 1.4 |
| `org.apache.commons.lang3.ObjectUtils.equals:228` | 20 | 1.4 |
| `net.minecraft.world.World.handleMaterialAcceleration:2416` | 20 | 1.4 |
| `net.minecraft.world.World.getCollisionBoxes:1465` | 20 | 1.4 |
| `net.minecraft.entity.EntityLivingBase.isElytraFlying:3143` | 19 | 1.3 |
| `net.minecraft.util.math.Vec3i.equals:54` | 18 | 1.2 |
| `net.minecraft.world.World.getEntitiesInAABBexcluding:3281` | 17 | 1.2 |
| `java.util.HashMap$HashIterator.<init>:1457` | 16 | 1.1 |

### CPU: vkljucni cas (noppes.* in net.minecraft*)

| metoda | vzorcev | % |
|---|---:|---:|
| `net.minecraft.server.MinecraftServer.tick` | 1469 | 100.0 |
| `net.minecraft.server.MinecraftServer.run` | 1469 | 100.0 |
| `net.minecraft.server.MinecraftServer.updateTimeLightAndEntities` | 1467 | 99.9 |
| `net.minecraft.server.dedicated.DedicatedServer.updateTimeLightAndEntities` | 1467 | 99.9 |
| `net.minecraft.world.WorldServer.updateEntities` | 1386 | 94.3 |
| `net.minecraft.world.World.updateEntities` | 1385 | 94.3 |
| `net.minecraft.world.WorldServer.updateEntityWithOptionalForce` | 1381 | 94.0 |
| `net.minecraft.world.World.updateEntity` | 1381 | 94.0 |
| `net.minecraft.world.World.updateEntityWithOptionalForce` | 1379 | 93.9 |
| `noppes.npcs.entity.EntityCustomNpc.onUpdate` | 1373 | 93.5 |
| `noppes.npcs.entity.EntityNPCInterface.onUpdate` | 1236 | 84.1 |
| `net.minecraft.entity.EntityLiving.onUpdate` | 1226 | 83.5 |
| `net.minecraft.entity.EntityLivingBase.onUpdate` | 1220 | 83.0 |
| `noppes.npcs.entity.EntityNPCInterface.onLivingUpdate` | 978 | 66.6 |
| `net.minecraft.entity.EntityLivingBase.onLivingUpdate` | 757 | 51.5 |
| `net.minecraft.entity.EntityLiving.onLivingUpdate` | 757 | 51.5 |
| `noppes.npcs.entity.EntityNPCInterface.travel` | 428 | 29.1 |
| `noppes.npcs.entity.EntityNPCFlying.travel` | 428 | 29.1 |
| `net.minecraft.entity.EntityLivingBase.travel` | 427 | 29.1 |
| `net.minecraft.entity.Entity.move` | 385 | 26.2 |
| `net.minecraft.world.World.getEntitiesInAABBexcluding` | 291 | 19.8 |
| `net.minecraft.world.World.getCollisionBoxes` | 289 | 19.7 |
| `net.minecraft.world.World.getEntitiesWithinAABB` | 276 | 18.8 |
| `net.minecraft.world.chunk.Chunk.getEntitiesWithinAABBForEntity` | 273 | 18.6 |
| `net.minecraft.world.chunk.Chunk.getEntitiesOfTypeWithinAABB` | 253 | 17.2 |
| `net.minecraft.entity.EntityLiving.updateEntityActionState` | 195 | 13.3 |
| `noppes.npcs.entity.EntityNPCInterface.onCollide` | 177 | 12.0 |
| `net.minecraft.world.World.getEntitiesWithinAABBExcludingEntity` | 174 | 11.8 |
| `net.minecraft.network.datasync.EntityDataManager.getEntry` | 171 | 11.6 |
| `net.minecraft.network.datasync.EntityDataManager.get` | 171 | 11.6 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks` | 167 | 11.4 |
| `net.minecraft.entity.Entity.onUpdate` | 163 | 11.1 |
| `net.minecraft.entity.EntityLiving.onEntityUpdate` | 138 | 9.4 |
| `noppes.npcs.ModelEyeData.update` | 137 | 9.3 |
| `net.minecraft.entity.EntityLivingBase.onEntityUpdate` | 136 | 9.3 |
| `net.minecraft.entity.EntityLivingBase.collideWithNearbyEntities` | 124 | 8.4 |
| `net.minecraft.world.World.getBlockState` | 108 | 7.4 |
| `noppes.npcs.Server.sendAssociatedData` | 96 | 6.5 |
| `net.minecraft.util.ClassInheritanceMultiMap.initializeClassLookup` | 94 | 6.4 |
| `net.minecraft.util.ClassInheritanceMultiMap$1.iterator` | 94 | 6.4 |
| `net.minecraft.world.World.getChunkFromChunkCoords` | 87 | 5.9 |
| `net.minecraft.world.gen.ChunkProviderServer.getLoadedChunk` | 86 | 5.9 |
| `net.minecraft.world.gen.ChunkProviderServer.loadChunk` | 86 | 5.9 |
| `net.minecraft.world.gen.ChunkProviderServer.provideChunk` | 86 | 5.9 |
| `net.minecraft.world.World.getChunkFromBlockCoords` | 71 | 4.8 |
| `noppes.npcs.entity.EntityNPCInterface.isAttacking` | 61 | 4.2 |
| `net.minecraft.world.WorldServer.tick` | 57 | 3.9 |
| `net.minecraft.entity.EntityLivingBase.getHealth` | 54 | 3.7 |
| `net.minecraft.entity.EntityLivingBase.isEntityAlive` | 52 | 3.5 |
| `noppes.npcs.entity.EntityNPCInterface.isEntityAlive` | 52 | 3.5 |

### CPU: po prvem noppes okvirju od vrha sklada

| metoda | vzorcev | % |
|---|---:|---:|
| `noppes.npcs.entity.EntityNPCInterface.travel` | 405 | 27.6 |
| `noppes.npcs.entity.EntityNPCInterface.onLivingUpdate` | 233 | 15.9 |
| `noppes.npcs.entity.EntityNPCInterface.onUpdate` | 200 | 13.6 |
| `noppes.npcs.entity.EntityNPCInterface.onCollide` | 155 | 10.6 |
| `noppes.npcs.Server.sendAssociatedData` | 96 | 6.5 |
| `(brez noppes okvirja)` | 92 | 6.3 |
| `noppes.npcs.entity.EntityNPCInterface.isAttacking` | 61 | 4.2 |
| `noppes.npcs.entity.EntityNPCInterface.isEntityAlive` | 52 | 3.5 |
| `noppes.npcs.ModelEyeData.update` | 38 | 2.6 |
| `noppes.npcs.ai.target.EntityAIClosestTarget.shouldExecute` | 26 | 1.8 |
| `noppes.npcs.entity.EntityNPCFlying.updateFallState` | 22 | 1.5 |
| `noppes.npcs.api.wrapper.ItemStackWrapper.MCItem` | 21 | 1.4 |
| `noppes.npcs.entity.EntityNPCInterface.isKilled` | 15 | 1.0 |
| `noppes.npcs.entity.data.DataTimers.update` | 10 | 0.7 |
| `noppes.npcs.entity.EntityNPCInterface.isVeryNearAssignedPlace` | 6 | 0.4 |
| `noppes.npcs.ai.EntityAILook.updateTask` | 6 | 0.4 |
| `noppes.npcs.rework.diag.DiagEventCollector.observePath` | 6 | 0.4 |
| `noppes.npcs.ai.EntityAIWaterNav.shouldExecute` | 6 | 0.4 |
| `noppes.npcs.entity.EntityNPCInterface.updateClient` | 3 | 0.2 |
| `noppes.npcs.entity.EntityNPCInterface.isFollower` | 3 | 0.2 |
| `noppes.npcs.ai.EntityAIWatchClosest.shouldExecute` | 3 | 0.2 |
| `noppes.npcs.entity.EntityNPCInterface.isInteracting` | 2 | 0.1 |
| `noppes.npcs.ai.EntityAILook.shouldExecute` | 2 | 0.1 |
| `noppes.npcs.ai.EntityAIAnimation.shouldExecute` | 1 | 0.1 |
| `noppes.npcs.ai.EntityAIJob.getMutexBits` | 1 | 0.1 |

### Alokacije: po razredu objekta

| razred | MB | % |
|---|---:|---:|
| `net.minecraft.util.math.AxisAlignedBB` | 3358.9 | 54.2 |
| `java.util.LinkedHashMap$LinkedKeyIterator` | 661.7 | 10.7 |
| `java.util.ArrayList$Itr` | 346.3 | 5.6 |
| `java.util.ArrayList` | 334.3 | 5.4 |
| `java.lang.Object[]` | 261.3 | 4.2 |
| `net.minecraft.util.math.BlockPos` | 220.2 | 3.6 |
| `java.util.HashMap$ValueIterator` | 146.1 | 2.4 |
| `net.minecraftforge.event.entity.living.LivingEvent$LivingUpdateEvent` | 137.0 | 2.2 |
| `noppes.npcs.api.event.NpcEvent$CollideEvent` | 115.4 | 1.9 |
| `java.lang.Integer` | 109.0 | 1.8 |
| `net.minecraftforge.event.world.GetCollisionBoxesEvent` | 98.5 | 1.6 |
| `net.minecraft.inventory.EntityEquipmentSlot[]` | 96.2 | 1.6 |
| `net.minecraft.util.EntitySelectors$7` | 77.6 | 1.3 |
| `com.google.common.base.Predicate[]` | 59.3 | 1.0 |
| `com.google.common.base.Predicates$AndPredicate` | 48.2 | 0.8 |
| `java.util.Arrays$ArrayList` | 39.0 | 0.6 |
| `java.util.HashMap$Node` | 29.5 | 0.5 |
| `com.google.common.collect.Iterators$6` | 20.1 | 0.3 |
| `java.util.HashMap$Node[]` | 19.5 | 0.3 |
| `com.google.common.base.Predicates$InstanceOfPredicate` | 19.1 | 0.3 |

### Alokacije: po prvem ne-JDK okvirju

| mesto | MB | % |
|---|---:|---:|
| `net.minecraft.util.math.AxisAlignedBB.offset:339` | 1773.1 | 28.6 |
| `net.minecraft.util.math.AxisAlignedBB.grow:288` | 1328.6 | 21.4 |
| `net.minecraft.entity.ai.EntityAITasks.canUse:145` | 397.5 | 6.4 |
| `net.minecraft.util.ClassInheritanceMultiMap.iterator:143` | 306.4 | 4.9 |
| `com.google.common.collect.Lists.newArrayList:88` | 275.7 | 4.4 |
| `noppes.npcs.entity.data.DataTimers.update:91` | 233.5 | 3.8 |
| `net.minecraft.block.Block.addCollisionBoxToList:575` | 194.8 | 3.1 |
| `net.minecraft.util.math.AxisAlignedBB.expand:245` | 139.8 | 2.3 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks:91` | 138.3 | 2.2 |
| `net.minecraftforge.common.ForgeHooks.onLivingUpdate:581` | 137.0 | 2.2 |
| `net.minecraft.util.math.AxisAlignedBB.offset:334` | 117.4 | 1.9 |
| `noppes.npcs.EventHooks.onNPCCollide:115` | 115.4 | 1.9 |
| `net.minecraft.entity.Entity.setAir:2680` | 109.0 | 1.8 |
| `net.minecraft.world.World.getCollisionBoxes:1523` | 98.5 | 1.6 |
| `com.google.common.base.Predicates.asList:709` | 98.3 | 1.6 |
| `net.minecraft.inventory.EntityEquipmentSlot.values:3` | 96.2 | 1.6 |
| `net.minecraft.entity.Entity.isInsideOfMaterial:1443` | 86.5 | 1.4 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks:112` | 86.4 | 1.4 |
| `net.minecraft.util.EntitySelectors.getTeamCollisionPredicate:71` | 77.6 | 1.3 |
| `net.minecraft.entity.EntityLivingBase.onEntityUpdate:348` | 68.1 | 1.1 |
| `net.minecraft.entity.Entity.move:987` | 65.6 | 1.1 |
| `com.google.common.base.Predicates.and:121` | 48.2 | 0.8 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks:70` | 39.5 | 0.6 |
| `net.minecraft.util.ClassInheritanceMultiMap$1.iterator:134` | 29.9 | 0.5 |
| `net.minecraft.nbt.NBTTagCompound.setInteger:123` | 29.8 | 0.5 |

### Alokacije: vkljucno (noppes.* in net.minecraft*)

| metoda | MB | % |
|---|---:|---:|
| `net.minecraft.server.MinecraftServer.tick` | 6197.2 | 100.0 |
| `net.minecraft.server.MinecraftServer.run` | 6197.2 | 100.0 |
| `noppes.npcs.entity.EntityNPCInterface.onUpdate` | 6139.1 | 99.1 |
| `noppes.npcs.entity.EntityCustomNpc.onUpdate` | 6139.1 | 99.1 |
| `net.minecraft.world.World.updateEntityWithOptionalForce` | 6139.1 | 99.1 |
| `net.minecraft.world.WorldServer.updateEntityWithOptionalForce` | 6139.1 | 99.1 |
| `net.minecraft.world.World.updateEntity` | 6139.1 | 99.1 |
| `net.minecraft.world.World.updateEntities` | 6139.1 | 99.1 |
| `net.minecraft.world.WorldServer.updateEntities` | 6139.1 | 99.1 |
| `net.minecraft.server.MinecraftServer.updateTimeLightAndEntities` | 6139.1 | 99.1 |
| `net.minecraft.server.dedicated.DedicatedServer.updateTimeLightAndEntities` | 6139.1 | 99.1 |
| `net.minecraft.entity.EntityLivingBase.onUpdate` | 5905.6 | 95.3 |
| `net.minecraft.entity.EntityLiving.onUpdate` | 5905.6 | 95.3 |
| `noppes.npcs.entity.EntityNPCInterface.onLivingUpdate` | 5044.8 | 81.4 |
| `net.minecraft.entity.EntityLivingBase.onLivingUpdate` | 4792.6 | 77.3 |
| `net.minecraft.entity.EntityLiving.onLivingUpdate` | 4792.6 | 77.3 |
| `net.minecraft.entity.EntityLivingBase.travel` | 3648.6 | 58.9 |
| `noppes.npcs.entity.EntityNPCInterface.travel` | 3648.6 | 58.9 |
| `noppes.npcs.entity.EntityNPCFlying.travel` | 3648.6 | 58.9 |
| `net.minecraft.entity.Entity.move` | 3530.1 | 57.0 |
| `net.minecraft.world.World.getCollisionBoxes` | 2593.0 | 41.8 |
| `net.minecraft.block.Block.addCollisionBoxToList` | 1967.9 | 31.8 |
| `net.minecraft.block.state.BlockStateContainer$StateImplementation.addCollisionBoxToList` | 1967.9 | 31.8 |
| `net.minecraft.util.math.AxisAlignedBB.offset` | 1890.5 | 30.5 |
| `net.minecraft.util.math.AxisAlignedBB.grow` | 1328.6 | 21.4 |
| `net.minecraft.util.math.AxisAlignedBB.shrink` | 740.9 | 12.0 |
| `net.minecraft.entity.ai.EntityAITasks.onUpdateTasks` | 712.4 | 11.5 |
| `net.minecraft.entity.EntityLiving.updateEntityActionState` | 712.4 | 11.5 |
| `net.minecraft.entity.EntityLivingBase.onEntityUpdate` | 627.6 | 10.1 |
| `net.minecraft.entity.EntityLiving.onEntityUpdate` | 627.6 | 10.1 |
| `net.minecraft.entity.Entity.onUpdate` | 627.6 | 10.1 |
| `net.minecraft.world.World.getEntitiesInAABBexcluding` | 463.4 | 7.5 |
| `net.minecraft.entity.EntityLivingBase.collideWithNearbyEntities` | 431.6 | 7.0 |
| `net.minecraft.entity.ai.EntityAITasks.canUse` | 397.5 | 6.4 |
| `net.minecraft.entity.Entity.isInLava` | 396.6 | 6.4 |
| `net.minecraft.entity.Entity.handleWaterMovement` | 384.3 | 6.2 |
| `net.minecraft.entity.Entity.onEntityUpdate` | 364.0 | 5.9 |
| `net.minecraft.util.ClassInheritanceMultiMap.iterator` | 306.4 | 4.9 |
| `net.minecraft.world.chunk.Chunk.getEntitiesWithinAABBForEntity` | 306.4 | 4.9 |
| `net.minecraft.entity.EntityLivingBase.updateFallState` | 257.7 | 4.2 |
| `noppes.npcs.entity.EntityNPCFlying.updateFallState` | 257.7 | 4.2 |
| `net.minecraft.world.World.getEntitiesWithinAABBExcludingEntity` | 255.9 | 4.1 |
| `noppes.npcs.entity.EntityNPCInterface.onCollide` | 242.2 | 3.9 |
| `noppes.npcs.entity.data.DataTimers.update` | 233.5 | 3.8 |
| `net.minecraft.util.EntitySelectors.getTeamCollisionPredicate` | 224.1 | 3.6 |
| `net.minecraft.util.math.AxisAlignedBB.expand` | 139.8 | 2.3 |
| `net.minecraftforge.common.ForgeHooks.onLivingUpdate` | 137.0 | 2.2 |
| `net.minecraft.world.World.getEntitiesWithinAABB` | 136.8 | 2.2 |
| `noppes.npcs.EventHooks.onNPCCollide` | 115.4 | 1.9 |
| `net.minecraft.entity.Entity.setAir` | 109.0 | 1.8 |

### Alokacije: po prvem noppes okvirju od vrha sklada

| metoda | MB | % |
|---|---:|---:|
| `noppes.npcs.entity.EntityNPCInterface.travel` | 3390.9 | 54.7 |
| `noppes.npcs.entity.EntityNPCInterface.onLivingUpdate` | 1093.3 | 17.6 |
| `noppes.npcs.entity.EntityNPCInterface.onUpdate` | 860.8 | 13.9 |
| `noppes.npcs.entity.EntityNPCFlying.updateFallState` | 257.7 | 4.2 |
| `noppes.npcs.entity.data.DataTimers.update` | 233.5 | 3.8 |
| `noppes.npcs.entity.EntityNPCInterface.onCollide` | 126.8 | 2.0 |
| `noppes.npcs.EventHooks.onNPCCollide` | 115.4 | 1.9 |
| `noppes.npcs.ai.EntityAIWaterNav.shouldExecute` | 40.7 | 0.7 |
| `noppes.npcs.entity.data.DataRanged.writeToNBT` | 29.8 | 0.5 |
| `noppes.npcs.ai.target.EntityAIClosestTarget.shouldExecute` | 10.0 | 0.2 |
| `noppes.npcs.entity.data.DataScenes.update` | 10.0 | 0.2 |
| `noppes.npcs.entity.EntityNPCInterface.writeEntityToNBT` | 9.9 | 0.2 |
| `noppes.npcs.ModelEyeData.writeToNBT` | 9.3 | 0.2 |
| `(brez noppes okvirja)` | 9.1 | 0.1 |

### Druge niti (vzorcev CPU)

| nit | vzorcev |
|---|---:|
| `File IO Thread` | 4 |
