/*
 * Copyright (C) 2015 SYJourney
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package server;

import server.properties.MapleRewardEntry;
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
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class MapleRewardManager {
    
    private static MapleRewardManager instance = null;
    private final EnumMap<RewardEvent, List<MapleRewardEntry>> rewards = new EnumMap<>(RewardEvent.class);
    
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
    
    public MapleRewardEntry chooseRandomItem(RewardEvent event) {
        List<MapleRewardEntry> rewardlist = rewards.get(event);
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
    
    public boolean loadEventRewards(RewardEvent event) {
        List<MapleRewardEntry> entries = new LinkedList<>();
        
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid FROM rewards WHERE world = ? AND event = ?")) {
            ps.setBoolean(1, false);
            ps.setInt(2, event.ordinal());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add( new MapleRewardEntry(rs.getInt("itemid"), rs.getShort("min"), rs.getShort("max"), rs.getByte("rarity")));
                }
                rs.close();
            }
            ps.close();
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
                ps.setBoolean(1, false);
                ps.setInt(2, event.ordinal());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add( new MapleRewardEntry(rs.getInt("itemid"), rs.getShort("min"), rs.getShort("max"), rs.getByte("rarity")));
                        i++;
                    }
                    rs.close();
                }
                ps.close();
            } catch (SQLException ex) {
                Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            rewards.put(event, entries);
        }
        return i;
    }
}
