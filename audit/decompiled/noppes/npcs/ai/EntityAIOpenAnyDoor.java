/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockDoor
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.properties.PropertyBool
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.pathfinding.Path
 *  net.minecraft.pathfinding.PathPoint
 *  net.minecraft.util.math.BlockPos
 */
package noppes.npcs.ai;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIOpenAnyDoor
extends EntityAIBase {
    private EntityNPCInterface npc;
    private BlockPos position;
    private Block door;
    private IProperty property;
    private boolean hasStoppedDoorInteraction;
    private float entityX;
    private float entityZ;
    private int closeDoorTemporisation;

    public EntityAIOpenAnyDoor(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public boolean func_75250_a() {
        if (!this.npc.field_70123_F) {
            return false;
        }
        Path pathentity = this.npc.func_70661_as().func_75505_d();
        if (pathentity != null && !pathentity.func_75879_b()) {
            for (int i = 0; i < Math.min(pathentity.func_75873_e() + 2, pathentity.func_75874_d()); ++i) {
                PathPoint pathpoint = pathentity.func_75877_a(i);
                this.position = new BlockPos(pathpoint.field_75839_a, pathpoint.field_75837_b + 1, pathpoint.field_75838_c);
                if (!(this.npc.func_70092_e(this.position.func_177958_n(), this.npc.field_70163_u, this.position.func_177952_p()) <= 2.25)) continue;
                this.door = this.getDoor(this.position);
                if (this.door == null) continue;
                return true;
            }
            this.position = new BlockPos((Entity)this.npc).func_177984_a();
            this.door = this.getDoor(this.position);
            return this.door != null;
        }
        return false;
    }

    public boolean func_75253_b() {
        return this.closeDoorTemporisation > 0 && !this.hasStoppedDoorInteraction;
    }

    public void func_75249_e() {
        this.hasStoppedDoorInteraction = false;
        this.entityX = (float)((double)((float)this.position.func_177958_n() + 0.5f) - this.npc.field_70165_t);
        this.entityZ = (float)((double)((float)this.position.func_177952_p() + 0.5f) - this.npc.field_70161_v);
        this.closeDoorTemporisation = 20;
        this.setDoorState(this.door, this.position, true);
    }

    public void func_75251_c() {
        this.setDoorState(this.door, this.position, false);
    }

    public void func_75246_d() {
        float f1;
        --this.closeDoorTemporisation;
        float f = (float)((double)((float)this.position.func_177958_n() + 0.5f) - this.npc.field_70165_t);
        float f2 = this.entityX * f + this.entityZ * (f1 = (float)((double)((float)this.position.func_177952_p() + 0.5f) - this.npc.field_70161_v));
        if (f2 < 0.0f) {
            this.hasStoppedDoorInteraction = true;
        }
    }

    public Block getDoor(BlockPos pos) {
        IBlockState state = this.npc.field_70170_p.func_180495_p(pos);
        Block block = state.func_177230_c();
        if (state.func_185913_b() || block == Blocks.field_150454_av) {
            return null;
        }
        if (block instanceof BlockDoor) {
            return block;
        }
        ImmutableSet set = state.func_177228_b().keySet();
        for (IProperty prop : set) {
            if (!(prop instanceof PropertyBool) || !prop.func_177701_a().equals("open")) continue;
            this.property = prop;
            return block;
        }
        return null;
    }

    public void setDoorState(Block block, BlockPos position, boolean open) {
        if (block instanceof BlockDoor) {
            ((BlockDoor)block).func_176512_a(this.npc.field_70170_p, position, open);
        } else {
            IBlockState state = this.npc.field_70170_p.func_180495_p(position);
            if (state.func_177230_c() != block) {
                return;
            }
            this.npc.field_70170_p.func_175656_a(position, state.func_177226_a(this.property, (Comparable)Boolean.valueOf(open)));
            this.npc.field_70170_p.func_180498_a((EntityPlayer)null, open ? 1003 : 1006, position, 0);
        }
    }
}

