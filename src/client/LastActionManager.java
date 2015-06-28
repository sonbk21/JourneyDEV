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

package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.DatabaseConnection;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class LastActionManager {
    
    private final EnumMap<MapleAction, Long> timeActionHolder;
    private final EnumMap<MapleAction, Byte> countActionHolder;
    private short observedAction; 
    
    public enum MapleAction {
        NPCTALK(0.5), 
        HARVEST, 
        HEAL(1.5), 
        ITEMSORT, 
        PETFOOD, 
        CATCHITEM, 
        SPECIALMOVE(0.5), 
        ENTERHARVEST(3600 * 24, 3),
        CPQ(3600 * 12, 3),
        CPQ2(3600 * 12, 3),
        LUDI(3600 * 12, 3),
        LUDIMAZE(3600 * 12, 3),
        PIRATE(3600 * 12, 3),
        DOJO(3600 * 24, 5),
        BPQEASY(3600 * 24, 5),
        BPQMED(3600 * 24, 5),
        BPQHARD(3600 * 24, 5),
        BPQHELL(3600 * 24, 5),
        NEOTOKYO(3600 * 12, 2),
        ARMORIA(3600 * 12, 1),
        ZAK(3600 * 24, 1), 
        HT(3600 * 24, 1),
        PB(3600 * 24, 1);
        private final double timeLimit;
        private final int countLimit;
        
        MapleAction() {
            this(1, 0);
        }
        
        MapleAction(double timeLimit) {
            this(timeLimit, 0);
        }
        
        MapleAction(double timeLimit, int countLimit) {
            this.timeLimit = timeLimit;
            this.countLimit = countLimit;
        }
        
        private double getTimeLimit() {
            return timeLimit;
        }
        
        private int getCountLimit() {
            return countLimit;
        }
    }
    
    public enum ActionResult {
        ALLOW, DISALLOW, DISCONNECT;
    }
    
    public LastActionManager() {
        timeActionHolder = new EnumMap<>(MapleAction.class);
        countActionHolder = new EnumMap<>(MapleAction.class);
        observedAction = 0;
    }
    
    public ActionResult tryAction(MapleAction ma) {
        if (ma.getCountLimit() > 0) {
            if (countActionHolder.containsKey(ma)) {
                byte tries = countActionHolder.get(ma);
                if (tries < ma.getCountLimit()) {
                    countActionHolder.put(ma, (byte) (tries + 1));
                    if (tries + 1 == ma.getCountLimit()) {
                        timeActionHolder.put(ma, System.currentTimeMillis());
                    }
                    return ActionResult.ALLOW;
                } else if (ma.getTimeLimit() == 0) {
                    return ActionResult.DISALLOW;
                }
            } else {
                countActionHolder.put(ma, (byte) 1);
                return ActionResult.ALLOW;
            }
        }
        
        if (timeActionHolder.containsKey(ma)) {
            if (System.currentTimeMillis() - timeActionHolder.get(ma) < 200) {
                observedAction ++;
                if (observedAction > 50)
                    return ActionResult.DISCONNECT;
            } else {
                observedAction = (short) (observedAction/2);
            }
            
            long timeLimit = timeActionHolder.get(ma) + (long) Math.floor(ma.getTimeLimit() * 1000);
            if (System.currentTimeMillis() > timeLimit) {
                if (countActionHolder.containsKey(ma)) {
                    countActionHolder.put(ma, (byte) 1);
                }
                return ActionResult.ALLOW;
            } else {
                return ActionResult.DISALLOW;
            }
        } else {
            return ActionResult.ALLOW;
        }
    }
    
    public void setLastAction(MapleAction ma) {
        timeActionHolder.put(ma, System.currentTimeMillis());
    }
    
    public long getRemaining(MapleAction ma) {
        return timeActionHolder.get(ma) + (long) Math.floor(ma.getTimeLimit() * 1000) - System.currentTimeMillis();
    }
    
    public void saveActions(int charid) {
        if (countActionHolder.isEmpty()) {
            return;
        }
        
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement psd;
            psd = con.prepareStatement("DELETE FROM lastactions WHERE charid = ?");
            psd.setInt(1, charid);
            psd.execute();
            psd.close();
            
            final PreparedStatement ps;
            ps = con.prepareStatement("INSERT INTO lastactions VALUES (?, ?, ?, ?)");
            ps.setInt(1, charid);
            countActionHolder.entrySet().stream().forEach((entry) -> {
                try {
                    ps.setByte(2, (byte) entry.getKey().ordinal());
                    ps.setLong(3, timeActionHolder.containsKey(entry.getKey())? timeActionHolder.get(entry.getKey()) : 0);
                    ps.setByte(4, (byte) entry.getValue());
                    ps.addBatch();
                } catch (SQLException ex) {
                    Logger.getLogger(LastActionManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            ps.executeBatch();
            ps.close();
        } catch (SQLException e) {
            Logger.getLogger(LastActionManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    public void loadActions(int charid) {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT action, lastaction, count FROM lastactions WHERE charid = ?")) {
            ps.setInt(1, charid);
            try (ResultSet rs = ps.executeQuery()) {
                MapleAction ma;
                byte count;
                long time;
                while (rs.next()) {
                    ma = MapleAction.values()[rs.getByte("action")];
                    count = rs.getByte("count");
                    time = rs.getLong("lastaction");
                    countActionHolder.put(ma, count);
                    if (time > 0)
                        timeActionHolder.put(ma, time);
                }
                rs.close();
            }
            ps.close();
        } catch (SQLException sqle) {
            Logger.getLogger(LastActionManager.class.getName()).log(Level.SEVERE, null, sqle);
        }
    }
}
