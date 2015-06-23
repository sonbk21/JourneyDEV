/*
 * JourneyMS
 * just an object to store boss data
 */
package server.properties;

import java.awt.Point;

public class AreaBossEntry {
    
    private final int id, intervall;
    private final Point[] position;
    private final String msg;
    
    public AreaBossEntry(int id, int intervall, Point[] position, String msg) {
        this.id = id;
        this.intervall = intervall;
        this.position = position;
        this.msg = msg;
    }
    
    public int getId() {
        return id;
    }
    
    public int getIntervall() {
        return intervall;
    }
    
    public Point[] getPosition() {
        return position;
    }
    
    public String getMsg() {
        return msg;
    }
}
