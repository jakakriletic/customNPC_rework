/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Predicate
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityCreature
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.ai.EntityAINearestAttackableTarget$Sorter
 *  net.minecraft.entity.ai.EntityAITarget
 *  net.minecraft.entity.monster.EntityMob
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.ai.target;

import com.google.common.base.Predicate;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIClosestTarget
extends EntityAITarget {
    private final Class targetClass;
    private final int targetChance;
    private final EntityAINearestAttackableTarget.Sorter theNearestAttackableTargetSorter;
    private final Predicate field_82643_g;
    private EntityLivingBase targetEntity;
    private EntityNPCInterface npc;

    public EntityAIClosestTarget(EntityNPCInterface npc, Class par2Class, int par3, boolean par4, boolean par5, Predicate par6IEntitySelector) {
        super((EntityCreature)npc, par4, par5);
        this.targetClass = par2Class;
        this.targetChance = par3;
        this.theNearestAttackableTargetSorter = new EntityAINearestAttackableTarget.Sorter((Entity)npc);
        this.func_75248_a(1);
        this.field_82643_g = par6IEntitySelector;
        this.npc = npc;
    }

    public boolean func_75250_a() {
        if (this.targetChance > 0 && this.field_75299_d.func_70681_au().nextInt(this.targetChance) != 0) {
            return false;
        }
        double d0 = this.func_111175_f();
        List list = this.field_75299_d.field_70170_p.func_175647_a(this.targetClass, this.field_75299_d.func_174813_aQ().func_72314_b(d0, (double)MathHelper.func_76143_f((double)(d0 / 2.0)), d0), this.field_82643_g);
        Collections.sort(list, this.theNearestAttackableTargetSorter);
        if (list.isEmpty()) {
            return false;
        }
        this.targetEntity = (EntityLivingBase)list.get(0);
        return true;
    }

    public void func_75249_e() {
        this.field_75299_d.func_70624_b(this.targetEntity);
        if (this.targetEntity instanceof EntityMob && ((EntityMob)this.targetEntity).func_70638_az() == null) {
            ((EntityMob)this.targetEntity).func_70624_b((EntityLivingBase)this.field_75299_d);
        }
        super.func_75249_e();
    }
}

