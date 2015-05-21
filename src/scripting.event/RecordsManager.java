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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.DatabaseConnection;

/**
 * JourneyMS
 * 
 */
public class RecordsManager {
    
    private final Map<String, List<EventRecordEntry>> records = new HashMap<>();
    private final int world;
    
    public RecordsManager(int world) {
        this.world = world;
    }
    
    public List<EventRecordEntry> getRecords(String event) {
        return records.containsKey(event)?Collections.unmodifiableList(records.get(event)):null;
    }
    
    public synchronized boolean updateRecords(String event, String names, int time, int rank) {
        EventRecordEntry toDelete = null;
        
        if (!records.containsKey(event)) {
            List <EventRecordEntry> entries = new LinkedList<>();
            entries.add(rank, new EventRecordEntry(names, time));
            records.put(event, entries);
        } else {
            List <EventRecordEntry> entries = records.get(event);
            entries.add( new EventRecordEntry(names, time));
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
                ps.setString(2, event);
                ps.setString(3, toDelete.getNames());
                ps.setInt(4, toDelete.getTime());
                ps.execute();
                ps.close();
            } catch (NullPointerException | SQLException ex) {
            }
            ps = con.prepareStatement("INSERT INTO records (`world`,`event`,`time`,`charnames`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, world);
            ps.setString(2, event);
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
    
    public boolean loadRecords(String event) {
        List <EventRecordEntry> entries = new LinkedList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT time, charnames FROM records WHERE world = ?, event = ? ORDER BY time ASC")) {
            ps.setInt(1, world);
            ps.setString(2, event);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add( new EventRecordEntry(rs.getString("charnames"), rs.getInt("time")));
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
