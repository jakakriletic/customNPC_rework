package net.minecraft.pathfinding;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * P3 (Q256): vanilla 1.12 {@link PathFinder} z nastavljivo mejo vozlisc. Vanilla ima mejo 200
 * vpisano v zasebno {@code findPath} [K], zato je algoritem tu prepisan vrstico za vrstico (isti
 * vrstni red sosedov, ista kopica, isti pogoji); razlika je samo {@link #maxNodes}. Pri 200 je izid
 * enak vanilli ({@code RwPathFinderTest}).
 *
 * <p>Uporablja ga samo {@code RwPathNavigateFlying} pri {@code RwPathResume=1}, kadar obnova poti z
 * vanilla iskanjem ne napreduje (vogal M4.16: pot se konca ob steni, ker 200 vozlisc ne zadostuje za
 * krak 20 x 16, Q82). Razred je v paketu {@code net.minecraft.pathfinding} zaradi polj
 * {@link PathPoint} brez dostopa izven paketa (vzorec {@link RwNavigatorAccess}).
 */
public final class RwPathFinder {
    /** Vanilla meja vozlisc v {@code PathFinder.findPath}. */
    public static final int VANILLA_NODES = 200;

    private final PathHeap path = new PathHeap();
    private final Set<PathPoint> closedSet = new HashSet<PathPoint>();
    private final PathPoint[] pathOptions = new PathPoint[32];
    private final NodeProcessor nodeProcessor;
    private final int maxNodes;

    public RwPathFinder(NodeProcessor processor, int maxNodes) {
        this.nodeProcessor = processor;
        this.maxNodes = maxNodes;
    }

    public int maxNodes() {
        return this.maxNodes;
    }

    /** Enako {@code PathFinder.findPath(IBlockAccess, EntityLiving, BlockPos, float)}. */
    @Nullable
    public Path findPath(IBlockAccess worldIn, EntityLiving entityIn, BlockPos targetPos, float maxDistance) {
        this.path.clearPath();
        this.nodeProcessor.init(worldIn, entityIn);
        PathPoint from = this.nodeProcessor.getStart();
        PathPoint to = this.nodeProcessor.getPathPointToCoords((double) ((float) targetPos.getX() + 0.5F),
                (double) ((float) targetPos.getY() + 0.5F), (double) ((float) targetPos.getZ() + 0.5F));
        Path result = this.findPath(from, to, maxDistance);
        this.nodeProcessor.postProcess();
        return result;
    }

    /** Vanilla {@code PathFinder.findPath(PathPoint, PathPoint, float)}, meja {@link #maxNodes}. */
    @Nullable
    Path findPath(PathPoint pathFrom, PathPoint pathTo, float maxDistance) {
        pathFrom.totalPathDistance = 0.0F;
        pathFrom.distanceToNext = pathFrom.distanceManhattan(pathTo);
        pathFrom.distanceToTarget = pathFrom.distanceToNext;
        this.path.clearPath();
        this.closedSet.clear();
        this.path.addPoint(pathFrom);
        PathPoint best = pathFrom;
        int i = 0;

        while (!this.path.isPathEmpty()) {
            ++i;
            if (i >= this.maxNodes) {
                break;
            }
            PathPoint current = this.path.dequeue();
            if (current.equals(pathTo)) {
                best = pathTo;
                break;
            }
            if (current.distanceManhattan(pathTo) < best.distanceManhattan(pathTo)) {
                best = current;
            }
            current.visited = true;
            int j = this.nodeProcessor.findPathOptions(this.pathOptions, current, pathTo, maxDistance);
            for (int k = 0; k < j; ++k) {
                PathPoint next = this.pathOptions[k];
                float f = current.distanceManhattan(next);
                next.distanceFromOrigin = current.distanceFromOrigin + f;
                next.cost = f + next.costMalus;
                float f1 = current.totalPathDistance + next.cost;
                if (next.distanceFromOrigin < maxDistance && (!next.isAssigned() || f1 < next.totalPathDistance)) {
                    next.previous = current;
                    next.totalPathDistance = f1;
                    next.distanceToNext = next.distanceManhattan(pathTo) + next.costMalus;
                    if (next.isAssigned()) {
                        this.path.changeDistance(next, next.totalPathDistance + next.distanceToNext);
                    } else {
                        next.distanceToTarget = next.totalPathDistance + next.distanceToNext;
                        this.path.addPoint(next);
                    }
                }
            }
        }

        if (best == pathFrom) {
            return null;
        }
        return createPath(best);
    }

    private static Path createPath(PathPoint end) {
        int i = 1;
        for (PathPoint p = end; p.previous != null; p = p.previous) {
            ++i;
        }
        PathPoint[] points = new PathPoint[i];
        PathPoint p = end;
        --i;
        for (points[i] = end; p.previous != null; points[i] = p) {
            p = p.previous;
            --i;
        }
        return new Path(points);
    }
}
