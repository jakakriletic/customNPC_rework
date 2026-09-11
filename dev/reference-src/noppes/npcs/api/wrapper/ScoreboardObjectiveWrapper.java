/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.scoreboard.Score
 *  net.minecraft.scoreboard.ScoreObjective
 *  net.minecraft.scoreboard.Scoreboard
 */
package noppes.npcs.api.wrapper;

import java.util.Collection;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IScoreboardObjective;
import noppes.npcs.api.IScoreboardScore;
import noppes.npcs.api.wrapper.ScoreboardScoreWrapper;

public class ScoreboardObjectiveWrapper
implements IScoreboardObjective {
    private ScoreObjective objective;
    private Scoreboard board;

    protected ScoreboardObjectiveWrapper(Scoreboard board, ScoreObjective objective) {
        this.objective = objective;
        this.board = board;
    }

    @Override
    public String getName() {
        return this.objective.getName();
    }

    @Override
    public String getDisplayName() {
        return this.objective.getDisplayName();
    }

    @Override
    public void setDisplayName(String name) {
        if (name.length() <= 0 || name.length() > 32) {
            throw new CustomNPCsException("Score objective display name must be between 1-32 characters: %s", name);
        }
        this.objective.setDisplayName(name);
    }

    @Override
    public String getCriteria() {
        return this.objective.getCriteria().getName();
    }

    @Override
    public boolean isReadyOnly() {
        return this.objective.getCriteria().isReadOnly();
    }

    @Override
    public IScoreboardScore[] getScores() {
        Collection list = this.board.getSortedScores(this.objective);
        IScoreboardScore[] scores = new IScoreboardScore[list.size()];
        int i = 0;
        for (Score score : list) {
            scores[i] = new ScoreboardScoreWrapper(score);
            ++i;
        }
        return scores;
    }

    @Override
    public IScoreboardScore getScore(String player) {
        if (!this.hasScore(player)) {
            return null;
        }
        return new ScoreboardScoreWrapper(this.board.getOrCreateScore(player, this.objective));
    }

    @Override
    public IScoreboardScore createScore(String player) {
        return new ScoreboardScoreWrapper(this.board.getOrCreateScore(player, this.objective));
    }

    @Override
    public void removeScore(String player) {
        this.board.removeObjectiveFromEntity(player, this.objective);
    }

    @Override
    public boolean hasScore(String player) {
        return this.board.entityHasObjective(player, this.objective);
    }
}

