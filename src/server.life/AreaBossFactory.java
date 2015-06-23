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

package server.life;

import server.properties.AreaBossEntry;
import java.awt.Point;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.DatabaseConnection;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class AreaBossFactory {
    
    private static final Map<Integer, AreaBossEntry> areabosses = new HashMap<>();
    
    public static AreaBossEntry getBossData(int mapid) {
        return areabosses.get(mapid);
    }
    
    public static boolean hasBoss(int mapid) {
        return areabosses.containsKey(mapid);
    }
    
    public static void loadBosses() {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT mapid, mobid, intervall, x1, y1, x2, y2, x3, y3, msg FROM areabosses ORDER BY mapid ASC")) {
            try (ResultSet rs = ps.executeQuery()) {
                int mapid, bossid, intervall;
                Point[] position = new Point[3];
                String msg;
                while (rs.next()) {
                    mapid = rs.getInt("mapid");
                    bossid = rs.getInt("mobid");
                    intervall = rs.getInt("intervall");
                    position[0] = new Point(rs.getInt("x1"), rs.getInt("y1"));
                    if (rs.getObject("x2") != null)
                        position[1] = new Point(rs.getInt("x2"), rs.getInt("y2"));
                    if (rs.getObject("x3") != null)
                        position[2] = new Point(rs.getInt("x3"), rs.getInt("y3"));
                    msg = rs.getString("msg");
                    areabosses.put(mapid, new AreaBossEntry(bossid, intervall, position, msg));
                }
                rs.close();
            }
            ps.close();
        } catch (SQLException ex) {
            Logger.getLogger(AreaBossFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
