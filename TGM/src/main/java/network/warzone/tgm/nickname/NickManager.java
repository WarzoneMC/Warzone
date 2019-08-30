package network.warzone.tgm.nickname;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_14_R1.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_14_R1.PacketPlayOutPlayerInfo;
import network.warzone.tgm.TGM;
import network.warzone.tgm.modules.team.MatchTeam;
import network.warzone.tgm.modules.team.TeamManagerModule;
import network.warzone.tgm.user.PlayerContext;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

public class NickManager {

    @Getter @Setter @AllArgsConstructor
    private class Skin {
        public String value;
        public String signature;
    }

    public HashMap<UUID, String> nickNames = new HashMap<>();
    public HashMap<UUID, Skin> skins = new HashMap<>();

    public HashMap<String, UUID> uuidCache = new HashMap<>();
    public HashMap<String, Skin> skinCache = new HashMap<>();

    public void setName(Player player, String newName) {
        EntityPlayer entityPlayer = getEntityPlayer(player);

        PacketPlayOutPlayerInfo playerInfo1 = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer);
        entityPlayer.playerConnection.sendPacket(playerInfo1);

        // Get player's old team.
        TeamManagerModule teamManager = TGM.get().getModule(TeamManagerModule.class);
        MatchTeam team = teamManager.getTeam(player);
        PlayerContext context = TGM.get().getPlayerManager().getPlayerContext(player);
        team.removePlayer(context);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player) && p.canSee(player)) {
                EntityPlayer entityOther = getEntityPlayer(p);

                // Remove the old player.
                PacketPlayOutPlayerInfo playerInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer);
                entityOther.playerConnection.sendPacket(playerInfo);


                // Modify the player's game profile.
                GameProfile profile = entityPlayer.getProfile();
                try {
                    Field field = GameProfile.class.getDeclaredField("name");
                    field.setAccessible(true);

                    field.set(profile, newName);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                // Add the player back.
                PacketPlayOutPlayerInfo playerAddBack = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer);
                PacketPlayOutEntityDestroy entityDestroy = new PacketPlayOutEntityDestroy(player.getEntityId());
                PacketPlayOutNamedEntitySpawn namedEntitySpawn = new PacketPlayOutNamedEntitySpawn(entityPlayer);
                entityOther.playerConnection.sendPacket(playerAddBack);
                entityOther.playerConnection.sendPacket(entityDestroy);
                entityOther.playerConnection.sendPacket(namedEntitySpawn);
            }
        }

        PacketPlayOutPlayerInfo playerAddBack = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer);
        entityPlayer.playerConnection.sendPacket(playerAddBack);
        nickNames.put(player.getUniqueId(), newName);

        TGM.get().getModule(TeamManagerModule.class).joinTeam(context, team);
    }

    public void setSkin(Player player, String nameOfPlayer) {
        EntityPlayer entityPlayer = getEntityPlayer(player);

        UUID uuid = getUUID(nameOfPlayer);
        if (uuid == null) return;
        Skin skin = getSkin(uuid);

        PacketPlayOutPlayerInfo playerInfo1 = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer);
        entityPlayer.playerConnection.sendPacket(playerInfo1);

        if (skin != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player) && p.canSee(player)) {
                    EntityPlayer entityOther = getEntityPlayer(p);

                    // Remove the old player.
                    PacketPlayOutPlayerInfo playerInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer);
                    entityOther.playerConnection.sendPacket(playerInfo);

                    entityPlayer.getProfile().getProperties().put("textures", new Property("textures", skin.value, skin.signature));

                    // Add the player back.
                    PacketPlayOutPlayerInfo playerAddBack = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer);
                    PacketPlayOutEntityDestroy entityDestroy = new PacketPlayOutEntityDestroy(player.getEntityId());
                    PacketPlayOutNamedEntitySpawn namedEntitySpawn = new PacketPlayOutNamedEntitySpawn(entityPlayer);
                    entityOther.playerConnection.sendPacket(playerAddBack);
                    entityOther.playerConnection.sendPacket(entityDestroy);
                    entityOther.playerConnection.sendPacket(namedEntitySpawn);
                }
            }

            PacketPlayOutPlayerInfo playerAddBack = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer);
            entityPlayer.playerConnection.sendPacket(playerAddBack);
            skins.put(player.getUniqueId(), skin);
        }
    }

    private UUID getUUID(String name) {
        if (uuidCache.containsKey(name)) {
            return uuidCache.get(name);
        } else {
            UUID uuid = fetchUUID(name);
            uuidCache.put(name, uuid);
            return uuid;
        }
    }

    private Skin getSkin(UUID uuid) {
        if (skinCache.containsKey(uuid.toString())) {
            return skinCache.get(uuid.toString());
        } else {
            Skin skin = fetchSkin(uuid);
            skinCache.put(uuid.toString(), skin);
            return skin;
        }
    }

    private UUID fetchUUID(String name) {
        try {
            HttpResponse<String> response = Unirest.get("https://api.mojang.com/users/profiles/minecraft/" + name).asString();
            if (response.getStatus() == 200) {
                return UUID.fromString(insertDashUUID(new JSONObject(response.getBody()).getString("id")));
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String insertDashUUID(String uuid) {
        StringBuilder sb = new StringBuilder(uuid);
        sb.insert(8, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(13, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(18, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(23, "-");

        return sb.toString();
    }

    private Skin fetchSkin(UUID uuid) {
        try {
            HttpResponse<String> response = Unirest.get(String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false", UUIDTypeAdapter.fromUUID(uuid))).asString();
            if (response.getStatus() == 200) {
                JSONObject object = new JSONObject(response.getBody());
                JSONObject properties = object.getJSONArray("properties").getJSONObject(0);
                return new Skin(properties.getString("value"), properties.getString("signature"));
            } else {
                System.out.println("Connection couldn't be established code=" + response.getStatus());
                return null;
            }
        } catch (UnirestException e) {
            e.printStackTrace();
            return null;
        }
    }

    private EntityPlayer getEntityPlayer(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

}