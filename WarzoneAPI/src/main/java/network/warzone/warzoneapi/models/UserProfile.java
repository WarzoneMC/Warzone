package network.warzone.warzoneapi.models;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.types.ObjectId;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luke on 4/27/17.
 */
@AllArgsConstructor
public class UserProfile {

    public static final int XP_PER_KILL = 1,
                            XP_PER_WIN = 10,
                            XP_PER_LOSS = 5,
                            XP_PER_WOOL_BREAK = 7;

    @SerializedName("_id")
    @Getter private ObjectId id;

    @Getter private String name;
    @Getter private String nameLower;
    @Getter private String uuid;
    @Getter private long initialJoinDate;
    @Getter private long lastOnlineDate;

    @Getter private List<String> ips;
    private List<String> ranks;
    private List<Rank> ranksLoaded;
    @Getter private int wins = 0;
    @Getter private int losses = 0;
    @Getter private int kills = 0;
    @Getter private int deaths = 0;
    @Getter private int wool_destroys = 0;
    @Getter private List<String> matches;

    public void addWin() {
        wins++;
    }

    public void addKill() {
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    public void addLoss() {
        losses++;
    }

    public void addWoolDestroy() {
        wool_destroys++;
    }

    public int getXP() {
        return (getWins() * XP_PER_WIN) + (getLosses() * XP_PER_LOSS) + (getWool_destroys() * XP_PER_WOOL_BREAK) + (getKills() * XP_PER_KILL);
    }

    public int getLevel() {
        return (int) getLevelRaw();
    }

    public double getLevelRaw() {
        return (0.6 * Math.sqrt(getXP())) + 1;
    }

    public List<String> getRanks() {
        if (ranks == null) ranks = new ArrayList<>();
        return ranks;
    }

    public List<Rank> getRanksLoaded() {
        if (ranksLoaded == null) ranksLoaded = new ArrayList<>();
        return ranksLoaded;
    }

    public void addRank(Rank rank) {
        if (ranksLoaded == null) ranksLoaded = new ArrayList<>();
        ranksLoaded.add(rank);
    }

    public void removeRank(Rank r) {
        if (ranksLoaded == null) ranksLoaded = new ArrayList<>();
        for (Rank rank : ranksLoaded) {
            if (rank.getId().equals(r.getId())) {
                ranksLoaded.remove(rank);
                return;
            }
        }
    }

    public boolean isStaff() {
        if (!ranksLoaded.isEmpty()) {
            Rank highest = ranksLoaded.get(0);
            for (Rank rank : ranksLoaded) {
                if (highest.getPriority() < rank.getPriority()) highest = rank;
            }
            return highest.isStaff();
        }
        else return false;
    }

    public String getKDR() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);
        if (getDeaths() == 0) return nf.format((double) getKills());
        return nf.format((double) getKills()/getDeaths());
    }

    public String getWLR() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);
        if (getLosses() == 0) return nf.format((double) getWins());
        return nf.format((double) getWins()/getLosses());
    }

    public String getPrefix() {
        if (!ranksLoaded.isEmpty()) {
            Rank highest = ranksLoaded.get(0);
            for (Rank rank : ranksLoaded) {
                if (highest.getPriority() < rank.getPriority()) highest = rank;
            }
            return highest.getPrefix() != null && !highest.getPrefix().isEmpty() ? highest.getPrefix() : null;
        }
        else return null;
    }
}
