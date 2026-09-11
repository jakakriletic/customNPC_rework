/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.math.BlockPos
 */
package noppes.npcs.controllers.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.api.entity.data.role.IRoleTransporter;
import noppes.npcs.controllers.data.TransportCategory;

public class TransportLocation
implements IRoleTransporter.ITransportLocation {
    public int id = -1;
    public String name = "default name";
    public BlockPos pos;
    public int type = 0;
    public int dimension = 0;
    public TransportCategory category;

    public void readNBT(NBTTagCompound compound) {
        if (compound == null) {
            return;
        }
        this.id = compound.func_74762_e("Id");
        this.pos = new BlockPos(compound.func_74769_h("PosX"), compound.func_74769_h("PosY"), compound.func_74769_h("PosZ"));
        this.type = compound.func_74762_e("Type");
        this.dimension = compound.func_74762_e("Dimension");
        this.name = compound.func_74779_i("Name");
    }

    public NBTTagCompound writeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.func_74768_a("Id", this.id);
        compound.func_74780_a("PosX", (double)this.pos.func_177958_n());
        compound.func_74780_a("PosY", (double)this.pos.func_177956_o());
        compound.func_74780_a("PosZ", (double)this.pos.func_177952_p());
        compound.func_74768_a("Type", this.type);
        compound.func_74768_a("Dimension", this.dimension);
        compound.func_74778_a("Name", this.name);
        return compound;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public int getDimension() {
        return this.dimension;
    }

    @Override
    public int getX() {
        return this.pos.func_177958_n();
    }

    @Override
    public int getY() {
        return this.pos.func_177956_o();
    }

    @Override
    public int getZ() {
        return this.pos.func_177952_p();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getType() {
        return this.type;
    }

    public boolean isDefault() {
        return this.type == 1;
    }
}

