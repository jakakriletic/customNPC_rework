/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.world.World
 */
package noppes.npcs.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityNpcCrystal
extends EntityNPCInterface {
    public EntityNpcCrystal(World world) {
        super(world);
        this.scaleX = 0.7f;
        this.scaleY = 0.7f;
        this.scaleZ = 0.7f;
        this.display.setSkinTexture("customnpcs:textures/entity/crystal/EnderCrystal.png");
    }

    @Override
    public void onUpdate() {
        this.isDead = true;
        this.setNoAI(true);
        if (!this.world.isRemote) {
            NBTTagCompound compound = new NBTTagCompound();
            this.writeToNBT(compound);
            EntityCustomNpc npc = new EntityCustomNpc(this.world);
            npc.readFromNBT(compound);
            ModelData data = npc.modelData;
            data.setEntityClass(EntityNpcCrystal.class);
            this.world.spawnEntity((Entity)npc);
        }
        super.onUpdate();
    }
}

