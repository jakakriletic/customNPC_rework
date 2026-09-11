/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Vec3i
 */
package noppes.npcs.api.wrapper;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import noppes.npcs.api.IPos;

public class BlockPosWrapper
implements IPos {
    private BlockPos blockPos;

    public BlockPosWrapper(BlockPos pos) {
        this.blockPos = pos;
    }

    @Override
    public int getX() {
        return this.blockPos.func_177958_n();
    }

    @Override
    public int getY() {
        return this.blockPos.func_177956_o();
    }

    @Override
    public int getZ() {
        return this.blockPos.func_177952_p();
    }

    @Override
    public IPos up() {
        return new BlockPosWrapper(this.blockPos.func_177984_a());
    }

    @Override
    public IPos up(int n) {
        return new BlockPosWrapper(this.blockPos.func_177981_b(n));
    }

    @Override
    public IPos down() {
        return new BlockPosWrapper(this.blockPos.func_177977_b());
    }

    @Override
    public IPos down(int n) {
        return new BlockPosWrapper(this.blockPos.func_177979_c(n));
    }

    @Override
    public IPos north() {
        return new BlockPosWrapper(this.blockPos.func_177978_c());
    }

    @Override
    public IPos north(int n) {
        return new BlockPosWrapper(this.blockPos.func_177964_d(n));
    }

    @Override
    public IPos east() {
        return new BlockPosWrapper(this.blockPos.func_177978_c());
    }

    @Override
    public IPos east(int n) {
        return new BlockPosWrapper(this.blockPos.func_177964_d(n));
    }

    @Override
    public IPos south() {
        return new BlockPosWrapper(this.blockPos.func_177978_c());
    }

    @Override
    public IPos south(int n) {
        return new BlockPosWrapper(this.blockPos.func_177964_d(n));
    }

    @Override
    public IPos west() {
        return new BlockPosWrapper(this.blockPos.func_177978_c());
    }

    @Override
    public IPos west(int n) {
        return new BlockPosWrapper(this.blockPos.func_177964_d(n));
    }

    @Override
    public IPos add(int x, int y, int z) {
        return new BlockPosWrapper(this.blockPos.func_177982_a(x, y, z));
    }

    @Override
    public IPos add(IPos pos) {
        return new BlockPosWrapper(this.blockPos.func_177971_a((Vec3i)pos.getMCBlockPos()));
    }

    @Override
    public IPos subtract(int x, int y, int z) {
        return new BlockPosWrapper(this.blockPos.func_177982_a(-x, -y, -z));
    }

    @Override
    public IPos subtract(IPos pos) {
        return new BlockPosWrapper(this.blockPos.func_177982_a(-pos.getX(), -pos.getY(), -pos.getZ()));
    }

    @Override
    public IPos offset(int direction) {
        return new BlockPosWrapper(this.blockPos.func_177972_a(EnumFacing.field_82609_l[direction]));
    }

    @Override
    public IPos offset(int direction, int n) {
        return new BlockPosWrapper(this.blockPos.func_177967_a(EnumFacing.field_82609_l[direction], n));
    }

    @Override
    public BlockPos getMCBlockPos() {
        return this.blockPos;
    }

    @Override
    public double[] normalize() {
        double d = Math.sqrt(this.blockPos.func_177958_n() * this.blockPos.func_177958_n() + this.blockPos.func_177956_o() * this.blockPos.func_177956_o() + this.blockPos.func_177952_p() * this.blockPos.func_177952_p());
        return new double[]{(double)this.getX() / d, (double)this.getY() / d, (double)this.getZ() / d};
    }

    @Override
    public double distanceTo(IPos pos) {
        double d0 = this.getX() - pos.getX();
        double d1 = this.getY() - pos.getY();
        double d2 = this.getZ() - pos.getZ();
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }
}

