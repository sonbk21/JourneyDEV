/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.life;

import java.awt.Point;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.DatabaseConnection;

/*
JourneyMS
BossFactory - loads areabosses from db (somehow theyre not in .wz files? i just assumed that at first)
*/

public class BossFactory {
    
    private static final Map<Integer, BossData> areabosses = new HashMap<>();
    
    public static BossData getBoss(int mapid) {
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
                    areabosses.put(mapid, new BossData(bossid, intervall, position, msg));
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(BossFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }        
}
