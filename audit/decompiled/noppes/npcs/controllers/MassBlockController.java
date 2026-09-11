/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package noppes.npcs.controllers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.controllers.data.BlockData;
import noppes.npcs.entity.EntityNPCInterface;

public class MassBlockController {
    private static Queue<IMassBlock> queue;
    private static MassBlockController Instance;

    public MassBlockController() {
        queue = new LinkedList<IMassBlock>();
        Instance = this;
    }

    public static void Update() {
        if (queue.isEmpty()) {
            return;
        }
        IMassBlock imb = queue.remove();
        World world = imb.getNpc().field_70170_p;
        BlockPos pos = imb.getNpc().func_180425_c();
        int range = imb.getRange();
        ArrayList<BlockData> list = new ArrayList<BlockData>();
        for (int x = -range; x < range; ++x) {
            for (int z = -range; z < range; ++z) {
                if (!world.func_175667_e(new BlockPos(x + pos.func_177958_n(), 64, z + pos.func_177952_p()))) continue;
                for (int y = 0; y < range; ++y) {
                    BlockPos blockPos = pos.func_177982_a(x, y - range / 2, z);
                    list.add(new BlockData(blockPos, world.func_180495_p(blockPos), null));
                }
            }
        }
        imb.processed(list);
    }

    public static void Queue(IMassBlock imb) {
        queue.add(imb);
    }

    public static interface IMassBlock {
        public EntityNPCInterface getNpc();

        public int getRange();

        public void processed(List<BlockData> var1);
    }
}

