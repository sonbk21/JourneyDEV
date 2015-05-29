/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import scripting.event.EventManager;
import tools.DatabaseConnection;
import tools.Randomizer;

/**
 * JourneyMS
 * 
 */

public class MapleRewardManager {
    
    private static MapleRewardManager instance = null;
    private final EnumMap<RewardEvent, List<MapleRewardEntry>> rewards = new EnumMap<>(RewardEvent.class);
    private final EnumMap<RewardEvent, List<MapleRewardEntry>> swrewards = new EnumMap<>(RewardEvent.class);
    
    private MapleRewardManager() {
    }
    
    public static MapleRewardManager getInstance() {
        if (instance == null) {
            instance = new MapleRewardManager();
        }
        return instance;
    }
    
    public enum RewardEvent {
        GACHAPON, FISHING, TREASURE;
    }
    
    public MapleRewardEntry chooseRandomItem(RewardEvent event, boolean supremeWorld) {
        List<MapleRewardEntry> rewardlist = (supremeWorld)? swrewards.get(event) : rewards.get(event);
        if (rewardlist.isEmpty())
            return null;
        MapleRewardEntry ret;
        do {
            ret = rewardlist.get(Randomizer.nextInt(rewardlist.size()));
            if (Randomizer.nextInt(ret.rarity) != 0)
                ret = null;
        } while (ret == null);
        return ret;
    }
    
    public boolean loadEventRewards(RewardEvent event, boolean supreme) {
        List<MapleRewardEntry> entries = new LinkedList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid FROM rewards WHERE world = ? AND event = ?")) {
            ps.setByte(1, (byte) ((supreme)?1:0));
            ps.setInt(2, event.ordinal());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add( new MapleRewardEntry(rs.getInt("itemid"), rs.getShort("min"), rs.getShort("max"), rs.getByte("rarity")));
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        rewards.put(event, entries);
        return !entries.isEmpty();
    }
    
    public int loadAllRewards() {
        int i = 0;
        List<MapleRewardEntry> entries;
        
        for (RewardEvent event : RewardEvent.values()) {
            entries = new LinkedList<>();
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid, min, max, rarity FROM rewards WHERE world = ? AND event = ?")) {
                ps.setByte(1, (byte) 0);
                ps.setInt(2, event.ordinal());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add( new MapleRewardEntry(rs.getInt("itemid"), rs.getShort("min"), rs.getShort("max"), rs.getByte("rarity")));
                        i++;
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            rewards.put(event, entries);
        }
        
        for (RewardEvent event : RewardEvent.values()) {
            entries = new LinkedList<>();
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid, min, max, rarity FROM rewards WHERE world = ? AND event = ?")) {
                ps.setByte(1, (byte) 1);
                ps.setInt(2, event.ordinal());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add( new MapleRewardEntry(rs.getInt("itemid"), rs.getShort("min"), rs.getShort("max"), rs.getByte("rarity")));
                        i++;
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            swrewards.put(event, entries);
        }
        
        return i;
    }
}
