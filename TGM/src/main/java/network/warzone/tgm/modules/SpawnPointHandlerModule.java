package network.warzone.tgm.modules;

import lombok.Getter;
import network.warzone.tgm.TGM;
import network.warzone.tgm.gametype.GameType;
import network.warzone.tgm.map.SpawnPoint;
import network.warzone.tgm.match.Match;
import network.warzone.tgm.match.MatchManager;
import network.warzone.tgm.match.MatchModule;
import network.warzone.tgm.match.MatchStatus;
import network.warzone.tgm.modules.kit.classes.GameClass;
import network.warzone.tgm.modules.kit.classes.GameClassModule;
import network.warzone.tgm.modules.team.MatchTeam;
import network.warzone.tgm.modules.team.TeamChangeEvent;
import network.warzone.tgm.modules.team.TeamManagerModule;
import network.warzone.tgm.player.event.TGMPlayerRespawnEvent;
import network.warzone.tgm.user.PlayerContext;
import network.warzone.tgm.util.Players;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

@Getter
public class SpawnPointHandlerModule extends MatchModule implements Listener {
    private TeamManagerModule teamManagerModule;
    private SpectatorModule spectatorModule;
    private GameClassModule gameClassModule;

    @Override
    public void load(Match match) {
        this.teamManagerModule = match.getModule(TeamManagerModule.class);
        this.spectatorModule = match.getModule(SpectatorModule.class);
        gameClassModule = match.getMapContainer().getMapInfo().isUsingClasses() ? TGM.get().getModule(GameClassModule.class) : null;
    }

    @EventHandler
    public void onTeamChange(TeamChangeEvent event) {
        if (event.isCancelled()) return;
        if (TGM.get().getMatchManager().getMatch().getMatchStatus() == MatchStatus.MID) {
            spawnPlayer(event.getPlayerContext(), event.getTeam(), true);
        }
        //player is joining the server
        else if (event.getOldTeam() == null) {
            spawnPlayer(event.getPlayerContext(), event.getTeam(), true);
        }
        //player is swapping teams pre/post match.
        else {
            //we don't need to teleport them in this case. Let them stay in their position.
        }
    }

    @EventHandler
    public void onRespawn(TGMPlayerRespawnEvent event) {
        PlayerContext playerContext = TGM.get().getPlayerManager().getPlayerContext(event.getPlayer());
        MatchTeam matchTeam = teamManagerModule.getTeam(event.getPlayer());

        spawnPlayer(playerContext, matchTeam, true);
    }

    public void spawnPlayer(PlayerContext playerContext, MatchTeam matchTeam, boolean teleport) {
        Players.reset(playerContext.getPlayer(), true);

        if (teleport) {
            MatchManager matchManager = TGM.get().getMatchManager();
            GameType gameType = matchManager.getMatch().getMapContainer().getMapInfo().getGametype();

            playerContext.getPlayer().setVelocity(new Vector(0, 0, 0));
            playerContext.getPlayer().setAllowFlight(true);
            playerContext.getPlayer().setFlying(true);
            playerContext.getPlayer().teleport(getTeamSpawn(matchTeam).getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            if (!matchTeam.isSpectator() && !gameType.equals(GameType.Infected)) playerContext.getPlayer().setGameMode(GameMode.SURVIVAL);
        }

        if (gameClassModule != null) {
            if (playerContext.getCurrentClass() == null) playerContext.setCurrentClass(gameClassModule.getDefaultClass());
            Bukkit.getScheduler().runTaskLater(TGM.get(), () -> {
                if (matchTeam.isSpectator()) {
                    spectatorModule.applySpectatorKit(playerContext);
                } else {
                    gameClassModule.performSwitch(playerContext);
                    GameClass gameClass = gameClassModule.getGameClass(playerContext.getCurrentClass());
                    if (gameClass != null) gameClass.apply(playerContext.getPlayer(), matchTeam.getColor());
                    playerContext.getPlayer().updateInventory();
                }
            }, 1L);
        } else
            Bukkit.getScheduler().runTaskLater(TGM.get(), () -> {
                playerContext.getPlayer().setFlying(false);
                playerContext.getPlayer().setAllowFlight(false);

                if (matchTeam.isSpectator()) {
                    spectatorModule.applySpectatorKit(playerContext);
                } else {
                    matchTeam.getKits().forEach(kit -> kit.apply(playerContext.getPlayer(), matchTeam));
                    playerContext.getPlayer().updateInventory();
                }
            }, 1L);
    }

    @Override
    public void enable() {
        for (MatchTeam matchTeam : TGM.get().getModule(TeamManagerModule.class).getTeams()) {
            if (!matchTeam.isSpectator()) {
                for (PlayerContext player : matchTeam.getMembers()) {
                    spawnPlayer(player, matchTeam, true);
                }
            }
        }
    }

    public SpawnPoint getTeamSpawn(MatchTeam matchTeam) {
        int i = (int) (Math.random() * (matchTeam.getSpawnPoints().size() - 1));
        return matchTeam.getSpawnPoints().get(i);
    }
}
