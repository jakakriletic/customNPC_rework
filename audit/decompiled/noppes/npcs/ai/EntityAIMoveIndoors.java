/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityCreature
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.world.World
 */
package noppes.npcs.ai;

import java.util.Random;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import noppes.npcs.constants.AiMutex;

public class EntityAIMoveIndoors
extends EntityAIBase {
    private EntityCreature theCreature;
    private double shelterX;
    private double shelterY;
    private double shelterZ;
    private World world;

    public EntityAIMoveIndoors(EntityCreature par1EntityCreature) {
        this.theCreature = par1EntityCreature;
        this.world = par1EntityCreature.field_70170_p;
        this.func_75248_a(AiMutex.PASSIVE);
    }

    public boolean func_75250_a() {
        if (this.theCreature.field_70170_p.func_72935_r() && !this.theCreature.field_70170_p.func_72896_J() || this.theCreature.field_70170_p.field_73011_w.func_191066_m()) {
            return false;
        }
        BlockPos pos = new BlockPos(this.theCreature.field_70165_t, this.theCreature.func_174813_aQ().field_72338_b, this.theCreature.field_70161_v);
        if (!this.world.func_175678_i(pos) && this.world.func_175699_k(pos) > 8) {
            return false;
        }
        Vec3d var1 = this.findPossibleShelter();
        if (var1 == null) {
            return false;
        }
        this.shelterX = var1.field_72450_a;
        this.shelterY = var1.field_72448_b;
        this.shelterZ = var1.field_72449_c;
        return true;
    }

    public boolean func_75253_b() {
        return !this.theCreature.func_70661_as().func_75500_f();
    }

    public void func_75249_e() {
        this.theCreature.func_70661_as().func_75492_a(this.shelterX, this.shelterY, this.shelterZ, 1.0);
    }

    private Vec3d findPossibleShelter() {
        Random random = this.theCreature.func_70681_au();
        BlockPos blockpos = new BlockPos(this.theCreature.field_70165_t, this.theCreature.func_174813_aQ().field_72338_b, this.theCreature.field_70161_v);
        for (int i = 0; i < 10; ++i) {
            BlockPos blockpos1 = blockpos.func_177982_a(random.nextInt(20) - 10, random.nextInt(6) - 3, random.nextInt(20) - 10);
            if (this.world.func_175678_i(blockpos1) || !(this.theCreature.func_180484_a(blockpos1) < 0.0f)) continue;
            return new Vec3d((double)blockpos1.func_177958_n(), (double)blockpos1.func_177956_o(), (double)blockpos1.func_177952_p());
        }
        return null;
    }
}

