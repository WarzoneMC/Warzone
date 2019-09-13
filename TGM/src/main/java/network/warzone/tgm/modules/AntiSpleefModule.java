package network.warzone.tgm.modules;

import network.warzone.tgm.TGM;
import network.warzone.tgm.match.Match;
import network.warzone.tgm.match.MatchModule;
import network.warzone.tgm.modules.team.MatchTeam;
import network.warzone.tgm.modules.team.TeamManagerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class AntiSpleefModule extends MatchModule implements Listener {
    private TeamManagerModule teamManagerModule;

    @Override
    public void load(Match match) {
        teamManagerModule = TGM.get().getModule(TeamManagerModule.class);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        MatchTeam playerTeam = teamManagerModule.getTeam(event.getPlayer());
        if(playerTeam.isSpectator()) return;
        String playerTeamID = playerTeam.getId();
        Location locOfBreaker = event.getPlayer().getLocation().clone();
        locOfBreaker.setY(locOfBreaker.getY() + 1);
        World playerWorld = event.getPlayer().getWorld();
        for(Player p : Bukkit.getOnlinePlayers()) {
            if(p.getWorld() != playerWorld) continue;
            if(p.getLocation().equals(locOfBreaker) && teamManagerModule.getTeam(p).getId().equalsIgnoreCase(playerTeamID)) {
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot break blocks under your teammates!");
                event.setCancelled(true);
                return;
            }
        }
    }
}
