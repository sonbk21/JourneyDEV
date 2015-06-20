/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * JourneyMS
 * 
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
    
    public List<Pair<String, Integer>> getRecords(RecordEvent event) {
        return records.containsKey(event)? Collections.unmodifiableList(records.get(event)) : null;
    }
    
    public boolean updateRecords(RecordEvent event, String names, int time, int rank) {
        recordsUpdateLock.lock();
        try {
            Pair<String, Integer> toDelete = null;
            if (!records.containsKey(event)) {
                List<Pair<String, Integer>> entries = new LinkedList<>();
                entries.add(new Pair<>(names, time));
                records.put(event, entries);
            } else {
                List<Pair<String, Integer>> entries = records.get(event);
                
                if (entries.get(rank).getRight() < time) //Check if rank was taken while locked
                    entries.add(rank + 1, new Pair<>(names, time));
                else
                    entries.add(rank, new Pair<>(names, time));
                if (records.get(event).size() > 15)
                    toDelete = entries.remove(15);
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
                return true;
            } catch (SQLException ex) {
                Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
                return false;
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
