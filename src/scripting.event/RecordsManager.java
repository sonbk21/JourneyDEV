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

package scripting.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.DatabaseConnection;
import tools.Pair;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class RecordsManager {
    
    private final EnumMap<RecordEvent, List<Pair<String, Integer>>> records = new EnumMap<>(RecordEvent.class);
    private final byte world;
    private final ReentrantLock recordsLoadLock;
    private final ReentrantLock recordsUpdateLock;
    
    public enum RecordEvent {
        BOSSPQEASY, BOSSPQMED, BOSSPQHARD, BOSSPQHELL;
    }
    
    public RecordsManager(byte world) {
        this.world = world;
        recordsLoadLock = new ReentrantLock(false);
        recordsUpdateLock = new ReentrantLock(true);
    }
    
    public byte checkRecord(RecordEvent event, String names, int time) {
        List<Pair<String, Integer>> entries = loadRecords(event);
        if (entries.size() > 14) {
            if (time > entries.get(15).getRight()) {
                return 0;
            }
        }
        return (byte) (updateRecords(event, names, time) + 1);
    }
    
    public byte updateRecords(RecordEvent event, String names, int time) {
        recordsUpdateLock.lock();
        try {
            Pair<String, Integer> toDelete = null;
            byte rank = 0;
            
            if (!records.containsKey(event)) {
                List<Pair<String, Integer>> entries = new LinkedList<>();
                entries.add(new Pair<>(names, time));
                records.put(event, entries);
            } else {
                List<Pair<String, Integer>> entries = records.get(event);
                Pair<String, Integer> newEntry = new Pair(names, time);
                entries.add(newEntry);
                Collections.sort(entries, (a, b) -> a.getRight().compareTo(b.getRight()));
                if (entries.size() > 15) {
                    toDelete = entries.remove(15);
                }
                records.put(event, entries);
            }
        
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps;
                if (toDelete != null) {
                    try {
                        ps = con.prepareStatement("DELETE FROM records WHERE world = ? AND event = ? AND names = ? AND time = ?");
                        ps.setInt(1, world);
                        ps.setInt(2, event.ordinal());
                        ps.setString(3, toDelete.getLeft());
                        ps.setInt(4, toDelete.getRight());
                        ps.execute();
                        ps.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                ps = con.prepareStatement("INSERT INTO records (`world`,`event`,`time`,`charnames`) VALUES (?, ?, ?, ?)");
                ps.setInt(1, world);
                ps.setInt(2, event.ordinal());
                ps.setInt(3, time);
                ps.setString(4, names);
                ps.executeUpdate();
                ps.close();
                return rank;
            } catch (SQLException ex) {
                Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
                return -1;
            }
        } finally {
            recordsUpdateLock.unlock();
        }
    }
    
    public List<Pair<String, Integer>> loadRecords(RecordEvent event) {
        if (records.containsKey(event)) {
            return records.get(event);
        } else {
            recordsLoadLock.lock();
            try {
                if (records.containsKey(event)) {
                    return records.get(event);
                }
                List<Pair<String, Integer>> entries = new LinkedList<>();
                try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT time, charnames FROM records WHERE world = ? AND event = ? ORDER BY time ASC")) {
                    ps.setByte(1, world);
                    ps.setInt(2, event.ordinal());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            entries.add( new Pair<>(rs.getString("charnames"), rs.getInt("time")));
                        }
                        rs.close();
                    }
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }
                records.put(event, entries);
                return entries;
            } finally {
                recordsLoadLock.unlock();
            }
        }
    }
}
