package noppes.npcs.rework.nav;

import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.world.IBlockAccess;

/**
 * M5.11 (S14b): node procesor kopenskega NPC-ja. Od {@link WalkNodeProcessor} se razlikuje samo
 * med {@link #beginMemo()} in {@link #endMemo()}: tam si posamezno oceno
 * {@link #getPathNodeType(IBlockAccess, int, int, int)} zapomni po poziciji.
 *
 * <p>Zakaj je to enako originalu (Forge 14.23.5.2847): posamezna ocena je odvisna od sveta,
 * pozicije in polja {@code currentEntity} (Forge kavelj {@code getAiPathNodeType}). Pomnjenje je
 * vklopljeno samo za cas enega {@code PathNavigateGround.isDirectPathBetweenPoints}; tam vse ocene
 * pridejo iz kvadrne {@code getPathNodeType(..., entity, sizeX, ...)}, ki {@code currentEntity}
 * sama nastavi na entiteto navigatorja, svet je {@code navigator.world} in se med klicem ne
 * spremeni (ena nit, brez postavljanja blokov). Preslikave vrat in tirnic so v kvadrni zanki,
 * izven pomnjene metode, torej se izvedejo kot v originalu. Iskanje poti ({@code PathFinder},
 * {@code ChunkCache}) tece izven tega okna in pomnilnika ne vidi.
 *
 * <p>Pomnjena metoda ne klice sama sebe ({@code getPathNodeTypeRaw}, {@code checkNeighborBlocks}),
 * zato pomnilnik ne vsebuje vmesnih izidov.
 */
public class RwWalkNodeProcessor extends WalkNodeProcessor {
    private final NodeTypeMemo<PathNodeType> memo = new NodeTypeMemo<PathNodeType>(256, false);
    private boolean active;
    /** M5.17: v preverbi se vsaka ocena izracuna in primerja s pomnjeno. */
    private boolean verify;
    private long mismatches;

    /** Zacetek klica isDirectPathBetweenPoints: prazen pomnilnik, pomnjenje vklopljeno. */
    void beginMemo() {
        this.beginMemo(false);
    }

    /** M5.17: zacetek okna pomnjenja; v preverbi se ocene primerjajo, ne prihranijo. */
    void beginMemo(boolean verifyMode) {
        this.memo.reset();
        this.memo.setMaxCapacity(PathSearchMemo.MAX_SLOTS);
        this.active = true;
        this.verify = verifyMode;
    }

    /** Konec klica: pomnjenje izklopljeno, pomnilnik prazen. */
    void endMemo() {
        this.active = false;
        this.verify = false;
        this.memo.reset();
    }

    long memoMismatches() {
        return this.mismatches;
    }

    long memoLookups() {
        return this.memo.lookups();
    }

    long memoMisses() {
        return this.memo.misses();
    }

    @Override
    public PathNodeType getPathNodeType(IBlockAccess blockaccessIn, int x, int y, int z) {
        if (!this.active) {
            return super.getPathNodeType(blockaccessIn, x, y, z);
        }
        PathNodeType type = this.memo.get(x, y, z);
        if (this.verify) {
            PathNodeType fresh = super.getPathNodeType(blockaccessIn, x, y, z);
            if (type == null) {
                this.memo.put(x, y, z, fresh);
            } else if (type != fresh) {
                ++this.mismatches;
            }
            return fresh;
        }
        if (type == null) {
            type = super.getPathNodeType(blockaccessIn, x, y, z);
            this.memo.put(x, y, z, type);
        }
        return type;
    }
}
