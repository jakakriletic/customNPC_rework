/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandBase
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.Entity
 *  net.minecraft.nbt.JsonToNBT
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTUtil
 *  net.minecraft.scoreboard.IScoreCriteria
 *  net.minecraft.scoreboard.Score
 *  net.minecraft.scoreboard.ScoreObjective
 *  net.minecraft.scoreboard.ScorePlayerTeam
 *  net.minecraft.scoreboard.Scoreboard
 *  net.minecraft.server.MinecraftServer
 */
package noppes.npcs.api.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IScoreboard;
import noppes.npcs.api.IScoreboardObjective;
import noppes.npcs.api.IScoreboardTeam;
import noppes.npcs.api.wrapper.ScoreboardObjectiveWrapper;
import noppes.npcs.api.wrapper.ScoreboardTeamWrapper;

public class ScoreboardWrapper
implements IScoreboard {
    private Scoreboard board;
    private MinecraftServer server;

    protected ScoreboardWrapper(MinecraftServer server) {
        this.server = server;
        this.board = server.getWorld(0).getScoreboard();
    }

    @Override
    public IScoreboardObjective[] getObjectives() {
        ArrayList collection = new ArrayList(this.board.getScoreObjectives());
        IScoreboardObjective[] objectives = new IScoreboardObjective[collection.size()];
        for (int i = 0; i < collection.size(); ++i) {
            objectives[i] = new ScoreboardObjectiveWrapper(this.board, (ScoreObjective)collection.get(i));
        }
        return objectives;
    }

    @Override
    public String[] getPlayerList() {
        Collection collection = this.board.getObjectiveNames();
        return collection.toArray(new String[collection.size()]);
    }

    @Override
    public IScoreboardObjective getObjective(String name) {
        ScoreObjective obj = this.board.getObjective(name);
        if (obj == null) {
            return null;
        }
        return new ScoreboardObjectiveWrapper(this.board, obj);
    }

    @Override
    public boolean hasObjective(String objective) {
        return this.board.getObjective(objective) != null;
    }

    @Override
    public void removeObjective(String objective) {
        ScoreObjective obj = this.board.getObjective(objective);
        if (obj != null) {
            this.board.removeObjective(obj);
        }
    }

    @Override
    public IScoreboardObjective addObjective(String objective, String criteria) {
        IScoreCriteria icriteria = (IScoreCriteria)IScoreCriteria.INSTANCES.get(criteria);
        if (icriteria == null) {
            throw new CustomNPCsException("Unknown score criteria: %s", criteria);
        }
        if (objective.length() <= 0 || objective.length() > 16) {
            throw new CustomNPCsException("Score objective must be between 1-16 characters: %s", objective);
        }
        ScoreObjective obj = this.board.addScoreObjective(objective, icriteria);
        return new ScoreboardObjectiveWrapper(this.board, obj);
    }

    @Override
    public void setPlayerScore(String player, String objective, int score, String datatag) {
        ScoreObjective objec = this.getObjectiveWithException(objective);
        if (objec.getCriteria().isReadOnly() || score < Integer.MIN_VALUE || score > Integer.MAX_VALUE || !this.test(datatag)) {
            return;
        }
        Score sco = this.board.getOrCreateScore(player, objec);
        sco.setScorePoints(score);
    }

    private boolean test(String datatag) {
        if (datatag.isEmpty()) {
            return true;
        }
        try {
            Entity entity = CommandBase.getEntity((MinecraftServer)this.server, (ICommandSender)this.server, (String)datatag);
            NBTTagCompound nbttagcompound = JsonToNBT.getTagFromJson((String)datatag);
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            entity.writeToNBT(nbttagcompound1);
            return NBTUtil.areNBTEquals((NBTBase)nbttagcompound, (NBTBase)nbttagcompound1, (boolean)true);
        }
        catch (Exception e) {
            return false;
        }
    }

    private ScoreObjective getObjectiveWithException(String objective) {
        ScoreObjective objec = this.board.getObjective(objective);
        if (objec == null) {
            throw new CustomNPCsException("Score objective does not exist: %s", objective);
        }
        return objec;
    }

    @Override
    public int getPlayerScore(String player, String objective, String datatag) {
        ScoreObjective objec = this.getObjectiveWithException(objective);
        if (objec.getCriteria().isReadOnly() || !this.test(datatag)) {
            return 0;
        }
        return this.board.getOrCreateScore(player, objec).getScorePoints();
    }

    @Override
    public boolean hasPlayerObjective(String player, String objective, String datatag) {
        ScoreObjective objec = this.getObjectiveWithException(objective);
        if (!this.test(datatag)) {
            return false;
        }
        return this.board.getObjectivesForEntity(player).get(objec) != null;
    }

    @Override
    public void deletePlayerScore(String player, String objective, String datatag) {
        ScoreObjective objec = this.getObjectiveWithException(objective);
        if (!this.test(datatag)) {
            return;
        }
        if (this.board.getObjectivesForEntity(player).remove(objec) != null) {
            this.board.removePlayerFromTeams(player);
        }
    }

    @Override
    public IScoreboardTeam[] getTeams() {
        ArrayList list = new ArrayList(this.board.getTeams());
        IScoreboardTeam[] teams = new IScoreboardTeam[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            teams[i] = new ScoreboardTeamWrapper((ScorePlayerTeam)list.get(i), this.board);
        }
        return teams;
    }

    @Override
    public boolean hasTeam(String name) {
        return this.board.getTeam(name) != null;
    }

    @Override
    public IScoreboardTeam addTeam(String name) {
        if (this.hasTeam(name)) {
            throw new CustomNPCsException("Team %s already exists", name);
        }
        return new ScoreboardTeamWrapper(this.board.createTeam(name), this.board);
    }

    @Override
    public IScoreboardTeam getTeam(String name) {
        ScorePlayerTeam team = this.board.getTeam(name);
        if (team == null) {
            return null;
        }
        return new ScoreboardTeamWrapper(team, this.board);
    }

    @Override
    public void removeTeam(String name) {
        ScorePlayerTeam team = this.board.getTeam(name);
        if (team != null) {
            this.board.removeTeam(team);
        }
    }

    @Override
    public IScoreboardTeam getPlayerTeam(String player) {
        ScorePlayerTeam team = this.board.getPlayersTeam(player);
        if (team == null) {
            return null;
        }
        return new ScoreboardTeamWrapper(team, this.board);
    }

    @Override
    public void removePlayerTeam(String player) {
        this.board.removePlayerFromTeams(player);
    }
}

