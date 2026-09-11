/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.scoreboard.Score
 */
package noppes.npcs.api.wrapper;

import net.minecraft.scoreboard.Score;
import noppes.npcs.api.IScoreboardScore;

public class ScoreboardScoreWrapper
implements IScoreboardScore {
    private Score score;

    public ScoreboardScoreWrapper(Score score) {
        this.score = score;
    }

    @Override
    public int getValue() {
        return this.score.func_96652_c();
    }

    @Override
    public void setValue(int val) {
        this.score.func_96647_c(val);
    }

    @Override
    public String getPlayerName() {
        return this.score.func_96653_e();
    }
}

