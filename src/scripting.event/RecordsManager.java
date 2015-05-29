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
    
    public enum RecordEvent {
        BOSSPQEASY, BOSSPQMED, BOSSPQHARD, BOSSPQHELL;
    }
    
    public RecordsManager(byte world) {
        this.world = world;
    }
    
    public List<Pair<String, Integer>> getRecords(RecordEvent event) {
        return records.containsKey(event)?Collections.unmodifiableList(records.get(event)):null;
    }
    
    public synchronized boolean updateRecords(RecordEvent event, String names, int time, int rank) {
        Pair<String, Integer> toDelete = null;
        
        if (!records.containsKey(event)) {
            List <Pair<String, Integer>> entries = new LinkedList<>();
            entries.add(rank, new Pair<>(names, time));
            records.put(event, entries);
        } else {
            List <Pair<String, Integer>> entries = records.get(event);
            entries.add( new Pair<>(names, time));
            if (records.get(event).size() > 15)
                toDelete = entries.remove(15);
            records.put(event, entries);
        }
        
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            try {
                ps = con.prepareStatement("DELETE FROM records WHERE world = ?, event = ?, names = ?, time = ?");
                ps.setInt(1, world);
                ps.setInt(2, event.ordinal());
                ps.setString(3, toDelete.getLeft());
                ps.setInt(4, toDelete.getRight());
                ps.execute();
                ps.close();
            } catch (NullPointerException | SQLException ex) {
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
    }
    
    public boolean loadRecords(RecordEvent event) {
        List <Pair<String, Integer>> entries = new LinkedList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT time, charnames FROM records WHERE world = ? AND event = ? ORDER BY time ASC")) {
            ps.setByte(1, world);
            ps.setInt(2, event.ordinal());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add( new Pair<>(rs.getString("charnames"), rs.getInt("time")));
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        records.put(event, entries);
        return !entries.isEmpty();
    }
}
