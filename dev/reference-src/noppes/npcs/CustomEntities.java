/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.event.RegistryEvent$Register
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.registry.EntityEntry
 *  net.minecraftforge.fml.common.registry.EntityEntryBuilder
 *  net.minecraftforge.fml.common.registry.ForgeRegistries
 *  net.minecraftforge.registries.IForgeRegistryEntry
 */
package noppes.npcs;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityChairMount;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPC64x32;
import noppes.npcs.entity.EntityNPCGolem;
import noppes.npcs.entity.EntityNpcAlex;
import noppes.npcs.entity.EntityNpcClassicPlayer;
import noppes.npcs.entity.EntityNpcCrystal;
import noppes.npcs.entity.EntityNpcDragon;
import noppes.npcs.entity.EntityNpcPony;
import noppes.npcs.entity.EntityNpcSlime;
import noppes.npcs.entity.EntityProjectile;
import noppes.npcs.entity.old.EntityNPCDwarfFemale;
import noppes.npcs.entity.old.EntityNPCDwarfMale;
import noppes.npcs.entity.old.EntityNPCElfFemale;
import noppes.npcs.entity.old.EntityNPCElfMale;
import noppes.npcs.entity.old.EntityNPCEnderman;
import noppes.npcs.entity.old.EntityNPCFurryFemale;
import noppes.npcs.entity.old.EntityNPCFurryMale;
import noppes.npcs.entity.old.EntityNPCHumanFemale;
import noppes.npcs.entity.old.EntityNPCHumanMale;
import noppes.npcs.entity.old.EntityNPCOrcFemale;
import noppes.npcs.entity.old.EntityNPCOrcMale;
import noppes.npcs.entity.old.EntityNPCVillager;
import noppes.npcs.entity.old.EntityNpcEnderchibi;
import noppes.npcs.entity.old.EntityNpcMonsterFemale;
import noppes.npcs.entity.old.EntityNpcMonsterMale;
import noppes.npcs.entity.old.EntityNpcNagaFemale;
import noppes.npcs.entity.old.EntityNpcNagaMale;
import noppes.npcs.entity.old.EntityNpcSkeleton;

public class CustomEntities {
    private int newEntityStartId = 0;

    @SubscribeEvent
    public void register(RegistryEvent.Register<EntityEntry> event) {
        EntityEntry[] entries = new EntityEntry[]{this.registerNpc(EntityNPCHumanMale.class, "npchumanmale"), this.registerNpc(EntityNPCVillager.class, "npcvillager"), this.registerNpc(EntityNpcPony.class, "npcpony"), this.registerNpc(EntityNPCHumanFemale.class, "npchumanfemale"), this.registerNpc(EntityNPCDwarfMale.class, "npcdwarfmale"), this.registerNpc(EntityNPCFurryMale.class, "npcfurrymale"), this.registerNpc(EntityNpcMonsterMale.class, "npczombiemale"), this.registerNpc(EntityNpcMonsterFemale.class, "npczombiefemale"), this.registerNpc(EntityNpcSkeleton.class, "npcskeleton"), this.registerNpc(EntityNPCDwarfFemale.class, "npcdwarffemale"), this.registerNpc(EntityNPCFurryFemale.class, "npcfurryfemale"), this.registerNpc(EntityNPCOrcMale.class, "npcorcfmale"), this.registerNpc(EntityNPCOrcFemale.class, "npcorcfemale"), this.registerNpc(EntityNPCElfMale.class, "npcelfmale"), this.registerNpc(EntityNPCElfFemale.class, "npcelffemale"), this.registerNpc(EntityNpcCrystal.class, "npccrystal"), this.registerNpc(EntityNpcEnderchibi.class, "npcenderchibi"), this.registerNpc(EntityNpcNagaMale.class, "npcnagamale"), this.registerNpc(EntityNpcNagaFemale.class, "npcnagafemale"), this.registerNpc(EntityNpcSlime.class, "NpcSlime"), this.registerNpc(EntityNpcDragon.class, "NpcDragon"), this.registerNpc(EntityNPCEnderman.class, "npcEnderman"), this.registerNpc(EntityNPCGolem.class, "npcGolem"), this.registerNpc(EntityCustomNpc.class, "CustomNpc"), this.registerNpc(EntityNPC64x32.class, "CustomNpc64x32"), this.registerNpc(EntityNpcAlex.class, "CustomNpcAlex"), this.registerNpc(EntityNpcClassicPlayer.class, "CustomNpcClassic"), this.registerNewentity("CustomNpcChairMount", 64, 10, false).entity(EntityChairMount.class).build(), this.registerNewentity("CustomNpcProjectile", 64, 3, true).entity(EntityProjectile.class).build()};
        event.getRegistry().registerAll((IForgeRegistryEntry[])entries);
    }

    private EntityEntry registerNpc(Class<? extends Entity> cl, String name) {
        if (CustomNpcs.FixUpdateFromPre_1_12) {
            ForgeRegistries.ENTITIES.register(new EntityEntry(cl, name).setRegistryName(new ResourceLocation("customnpcs." + name)));
        }
        return this.registerNewentity(name, 64, 3, true).entity(cl).build();
    }

    private <E extends Entity> EntityEntryBuilder<E> registerNewentity(String name, int range, int update, boolean velocity) {
        EntityEntryBuilder builder = EntityEntryBuilder.create();
        ResourceLocation registryName = new ResourceLocation("customnpcs", name);
        return builder.id(registryName, this.newEntityStartId++).name(name).tracker(range, update, velocity);
    }
}

