/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityCreature
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.entity.ai.RandomPositionGenerator
 *  net.minecraft.util.math.Vec3d
 */
package noppes.npcs.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIDodgeShoot
extends EntityAIBase {
    private EntityNPCInterface entity;
    private double x;
    private double y;
    private double zPosition;

    public EntityAIDodgeShoot(EntityNPCInterface iNpc) {
        this.entity = iNpc;
        this.func_75248_a(AiMutex.PASSIVE);
    }

    public boolean func_75250_a() {
        Vec3d vec;
        EntityLivingBase var1 = this.entity.func_70638_az();
        if (var1 == null || !var1.func_70089_S()) {
            return false;
        }
        if (this.entity.inventory.getProjectile() == null) {
            return false;
        }
        if (this.entity.getRangedTask() == null) {
            return false;
        }
        Vec3d vec3d = vec = this.entity.getRangedTask().hasFired() ? RandomPositionGenerator.func_75463_a((EntityCreature)this.entity, (int)4, (int)1) : null;
        if (vec == null) {
            return false;
        }
        this.x = vec.field_72450_a;
        this.y = vec.field_72448_b;
        this.zPosition = vec.field_72449_c;
        return true;
    }

    public boolean func_75253_b() {
        return !this.entity.func_70661_as().func_75500_f();
    }

    public void func_75249_e() {
        this.entity.func_70661_as().func_75492_a(this.x, this.y, this.zPosition, 1.2);
    }

    public void func_75246_d() {
        if (this.entity.func_70638_az() != null) {
            this.entity.func_70671_ap().func_75651_a((Entity)this.entity.func_70638_az(), 30.0f, 30.0f);
        }
    }
}

