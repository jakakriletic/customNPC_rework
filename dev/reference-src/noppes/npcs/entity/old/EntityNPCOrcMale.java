/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.world.World
 */
package noppes.npcs.entity.old;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import noppes.npcs.ModelData;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityNPCOrcMale
extends EntityNPCInterface {
    public EntityNPCOrcMale(World world) {
        super(world);
        this.scaleY = 1.0f;
        this.scaleZ = 1.2f;
        this.scaleX = 1.2f;
        this.display.setSkinTexture("customnpcs:textures/entity/orcmale/StrandedOrc.png");
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
            data.getPartConfig(EnumParts.LEG_LEFT).setScale(1.2f, 1.05f);
            data.getPartConfig(EnumParts.ARM_LEFT).setScale(1.2f, 1.05f);
            data.getPartConfig(EnumParts.BODY).setScale(1.4f, 1.1f, 1.5f);
            data.getPartConfig(EnumParts.HEAD).setScale(1.2f, 1.1f);
            this.world.spawnEntity((Entity)npc);
        }
        super.onUpdate();
    }
}

