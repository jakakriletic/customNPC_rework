package noppes.npcs.rework.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * M5-S S1: predzavrnitev v iskalniku tarc ne sme spremeniti izida predikata.
 *
 * <p>{@link #original} je model {@code NPCAttackSelector.isEntityApplicable} (reference-src) kot
 * funkcija pogojev; izjema originala ob neskladnem stanju je modelirana kot {@code null}.
 * Test pregleda vse kombinacije in za vsako vrednost UNKNOWN obe mozni razresitvi:
 * kadar predzavrnitev reče „zavrni“, original nikoli ne vrne {@code true} in nikoli ne vrze
 * izjeme, ne glede na vidnost, nevidnost in razdaljo od doma.
 */
public class TargetPrefilterTest {
    private static final int[] TRI = {TargetPrefilter.NO, TargetPrefilter.YES, TargetPrefilter.UNKNOWN};
    private static final int[] KINDS = {TargetPrefilter.KIND_OTHER, TargetPrefilter.KIND_PLAYER,
            TargetPrefilter.KIND_NPC};
    private static final boolean[] BOOL = {false, true};

    @After
    public void restoreDefault() {
        TargetPrefilter.setMode(TargetPrefilter.ORIGINAL);
        TargetPrefilter.resetRejected();
    }

    /**
     * Model originala za kandidata, ki je prestal prvo preverjanje (ziv, v dosegu, zdravje).
     * {@code guard}/{@code companionGuard}/{@code aggressiveToNpc} so tu ze razreseni:
     * YES, NO ali UNKNOWN = izjema ob dostopu (vrne {@code null}).
     */
    static Boolean original(boolean visibleOk, boolean invisibleOk, boolean homeOk, int guard,
            int companionGuard, int kind, boolean creative, boolean disableDamage,
            boolean factionPointsAggressive, boolean npcKilled, boolean attackOtherFactions,
            int aggressiveToNpc) {
        if (!visibleOk || !invisibleOk || !homeOk) {
            return false;
        }
        if (guard == TargetPrefilter.UNKNOWN) {
            return null;
        }
        if (guard == TargetPrefilter.YES) {
            return true;
        }
        if (companionGuard == TargetPrefilter.UNKNOWN) {
            return null;
        }
        if (companionGuard == TargetPrefilter.YES) {
            return true;
        }
        if (kind == TargetPrefilter.KIND_PLAYER) {
            // Faction.isAggressiveToPlayer: kreativni nacin false, sicer tocke (stranski ucinek).
            boolean aggressive = !creative && factionPointsAggressive;
            return aggressive && !disableDamage;
        }
        if (kind == TargetPrefilter.KIND_NPC) {
            if (npcKilled) {
                return false;
            }
            if (attackOtherFactions) {
                if (aggressiveToNpc == TargetPrefilter.UNKNOWN) {
                    return null;
                }
                return aggressiveToNpc == TargetPrefilter.YES;
            }
        }
        return false;
    }

    @Test
    public void rejectionImpliesOriginalFalseWithoutException() {
        int combos = 0;
        int rejectedCombos = 0;
        for (int guard : TRI) {
            for (int comp : TRI) {
                for (int kind : KINDS) {
                    for (boolean creative : BOOL) {
                        for (boolean disableDamage : BOOL) {
                            for (boolean killed : BOOL) {
                                for (boolean attackOther : BOOL) {
                                    for (int aggrNpc : TRI) {
                                        boolean exempt = kind == TargetPrefilter.KIND_PLAYER
                                                && (creative || disableDamage);
                                        boolean r = TargetPrefilter.rejects(guard, comp, kind, exempt,
                                                killed, attackOther, aggrNpc);
                                        combos++;
                                        if (!r) {
                                            continue;
                                        }
                                        rejectedCombos++;
                                        for (boolean vis : BOOL) {
                                            for (boolean inv : BOOL) {
                                                for (boolean home : BOOL) {
                                                    for (boolean points : BOOL) {
                                                        Boolean o = original(vis, inv, home, guard, comp,
                                                                kind, creative, disableDamage, points,
                                                                killed, attackOther, aggrNpc);
                                                        assertEquals("guard=" + guard + " comp=" + comp
                                                                + " kind=" + kind + " vis=" + vis,
                                                                Boolean.FALSE, o);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertEquals(3 * 3 * 3 * 2 * 2 * 2 * 2 * 3, combos);
        assertTrue("predzavrnitev mora nekaj zavrniti", rejectedCombos > 0);
    }

    /** Igralca, ki ga NPC lahko napade, predzavrnitev ne odloca: PlayerData ostane za vidnostjo. */
    @Test
    public void attackablePlayerKeepsOriginalOrder() {
        for (int aggrNpc : TRI) {
            for (boolean killed : BOOL) {
                for (boolean attackOther : BOOL) {
                    assertFalse(TargetPrefilter.rejects(TargetPrefilter.NO, TargetPrefilter.NO,
                            TargetPrefilter.KIND_PLAYER, false, killed, attackOther, aggrNpc));
                }
            }
        }
    }

    /** Glavni primer idle scenarija: prijazen NPC (frakcija ne napada) in drug NPC v dosegu. */
    @Test
    public void friendlyNpcCandidateIsRejected() {
        assertTrue(TargetPrefilter.rejects(TargetPrefilter.NO, TargetPrefilter.NO,
                TargetPrefilter.KIND_NPC, false, false, false, TargetPrefilter.NO));
        assertTrue(TargetPrefilter.rejects(TargetPrefilter.NO, TargetPrefilter.NO,
                TargetPrefilter.KIND_NPC, false, false, true, TargetPrefilter.NO));
        assertTrue(TargetPrefilter.rejects(TargetPrefilter.NO, TargetPrefilter.NO,
                TargetPrefilter.KIND_OTHER, false, false, false, TargetPrefilter.NO));
    }

    /** Sovrazni NPC in neskladno stanje gresta po originalni poti. */
    @Test
    public void hostileOrUnknownIsNotRejected() {
        assertFalse(TargetPrefilter.rejects(TargetPrefilter.NO, TargetPrefilter.NO,
                TargetPrefilter.KIND_NPC, false, false, true, TargetPrefilter.YES));
        assertFalse(TargetPrefilter.rejects(TargetPrefilter.NO, TargetPrefilter.NO,
                TargetPrefilter.KIND_NPC, false, false, true, TargetPrefilter.UNKNOWN));
        assertFalse(TargetPrefilter.rejects(TargetPrefilter.YES, TargetPrefilter.NO,
                TargetPrefilter.KIND_OTHER, false, false, false, TargetPrefilter.NO));
        assertFalse(TargetPrefilter.rejects(TargetPrefilter.UNKNOWN, TargetPrefilter.NO,
                TargetPrefilter.KIND_OTHER, false, false, false, TargetPrefilter.NO));
        assertFalse(TargetPrefilter.rejects(TargetPrefilter.NO, TargetPrefilter.UNKNOWN,
                TargetPrefilter.KIND_OTHER, false, false, false, TargetPrefilter.NO));
    }

    @Test
    public void defaultModeIsOriginalAndDoesNothing() {
        assertEquals(TargetPrefilter.ORIGINAL, TargetPrefilter.mode());
        // V nacinu 0 se metoda vrne pred dostopom do argumentov.
        assertFalse(TargetPrefilter.rejectsEarly(null, null));
        assertEquals(0L, TargetPrefilter.rejected());
    }

    @Test
    public void invalidModeFallsBackToOriginal() {
        assertEquals(TargetPrefilter.HOSTILITY_FIRST, TargetPrefilter.setMode(1));
        assertEquals(TargetPrefilter.ORIGINAL, TargetPrefilter.setMode(7));
        assertEquals(TargetPrefilter.ORIGINAL, TargetPrefilter.setMode(-1));
        assertEquals(TargetPrefilter.PLAYERS_ONLY_SCAN, TargetPrefilter.setMode(2));
        assertFalse(TargetPrefilter.isValidMode(3));
    }

    /**
     * M5-S S2: ko {@link TargetPrefilter#onlyPlayers} rece "samo igralci", original za vsakega
     * kandidata, ki ni igralec (KIND_OTHER, KIND_NPC), vrne {@code false} brez izjeme - v vseh
     * kombinacijah, ki jih nastavitve NPC-ja dopuscajo. Poizvedba samo po igralcih zato da isti
     * seznam po predikatu.
     */
    @Test
    public void playersOnlyScanDropsOnlyCandidatesOriginalRejects() {
        int narrowedCombos = 0;
        for (boolean guardJob : BOOL) {
            for (int compJob : TRI) {
                for (boolean attackOther : BOOL) {
                    if (!TargetPrefilter.onlyPlayers(guardJob, compJob, attackOther)) {
                        continue;
                    }
                    narrowedCombos++;
                    // Brez strazarja je guard vedno NO; brez spremljevalca-strazarja tudi companionGuard.
                    int[] guards = guardJob ? TRI : new int[] {TargetPrefilter.NO};
                    int[] comps = compJob == TargetPrefilter.NO ? new int[] {TargetPrefilter.NO} : TRI;
                    for (int guard : guards) {
                        for (int comp : comps) {
                            for (int kind : new int[] {TargetPrefilter.KIND_OTHER, TargetPrefilter.KIND_NPC}) {
                                for (boolean killed : BOOL) {
                                    for (int aggrNpc : TRI) {
                                        for (boolean vis : BOOL) {
                                            for (boolean inv : BOOL) {
                                                for (boolean home : BOOL) {
                                                    assertEquals(Boolean.FALSE, original(vis, inv, home, guard, comp,
                                                            kind, false, false, true, killed, attackOther, aggrNpc));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertEquals("samo NPC brez strazarja, spremljevalca-strazarja in AttackOtherFactions", 1, narrowedCombos);
    }

    @Test
    public void playersOnlyScanKeepsEveryNpcThatMayTargetOthers() {
        assertFalse(TargetPrefilter.onlyPlayers(true, TargetPrefilter.NO, false));
        assertFalse(TargetPrefilter.onlyPlayers(false, TargetPrefilter.YES, false));
        assertFalse(TargetPrefilter.onlyPlayers(false, TargetPrefilter.UNKNOWN, false));
        assertFalse(TargetPrefilter.onlyPlayers(false, TargetPrefilter.NO, true));
        assertTrue(TargetPrefilter.onlyPlayers(false, TargetPrefilter.NO, false));
    }

    @Test
    public void scanClassIsUnchangedOutsideMode2() {
        // V nacinih 0 in 1 se metoda vrne pred dostopom do NPC-ja.
        assertEquals(net.minecraft.entity.EntityLivingBase.class,
                TargetPrefilter.scanClass(null, net.minecraft.entity.EntityLivingBase.class));
        TargetPrefilter.setMode(TargetPrefilter.HOSTILITY_FIRST);
        assertEquals(net.minecraft.entity.EntityLivingBase.class,
                TargetPrefilter.scanClass(null, net.minecraft.entity.EntityLivingBase.class));
        assertEquals(0L, TargetPrefilter.narrowed());
    }
}
