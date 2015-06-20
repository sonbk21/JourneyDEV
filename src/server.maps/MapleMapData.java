/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server.maps;

import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;
import server.MaplePortal;
import server.life.MapleNPC;
import tools.Pair;

/**
 * JourneyMS
 * 
 */
public class MapleMapData {
    
    private final int mapid;
    private final int returnMapId;
    private final int forcedReturnId;
    private final byte monsterRate;
    private final String onEnterF;
    private final String onEnter;
    private final int fieldType;
    private final int fieldLimit;
    private final int mobCapacity;
    private final short mobInterval;
    private final boolean clock;
    private final boolean town;
    private final boolean everlast;
    private final boolean boat;
    private final int decHp;
    private final int protectItem;
    private final int timeLimit;
    
    private int oid;
    
    private final Map<Byte, MaplePortal> portals = new HashMap<>();
    private final Pair<Integer, String> timeMob;
    private final Map<Integer, MapleMapObject> mapobjects = new HashMap<>();
    
    private final MapleFootholdTree footholds;
    
    public MapleMapData(int mapid, int returnMapId, int forcedReturnId,  float monsterRate, String onEnterF, String onEnter, int fieldType, int fieldLimit, int mobCapacity, short mobInterval, boolean clock, boolean town, boolean everlast, boolean boat, int decHp, int protectItem, int timeLimit, Pair<Integer, String> timeMob, MapleFootholdTree footholds) {
        this.mapid = mapid;
        this.returnMapId = returnMapId;
        this.forcedReturnId = forcedReturnId;
        this.monsterRate = (byte) ((Math.round(monsterRate) == 0)? 1 : Math.round(monsterRate));
        this.onEnterF = onEnterF;
        this.onEnter = onEnter;
        this.fieldType = fieldType;
        this.fieldLimit = fieldLimit;
        this.mobCapacity = mobCapacity;
        this.mobInterval = mobInterval;
        this.clock = clock;
        this.town = town;
        this.everlast = everlast;
        this.boat = boat;
        this.decHp = decHp;
        this.protectItem = protectItem;
        this.timeLimit = timeLimit;
        this.timeMob = timeMob;
        this.footholds = footholds;
    }
    
    public void addPortal(MaplePortal myPortal) {
        portals.put((byte) myPortal.getId(), myPortal);
    }
    
    public void addMapObject(MapleMapObject mapobject) {
        mapobject.setObjectId(oid);
        mapobjects.put(oid, mapobject);
        oid++;
    }

    public MaplePortal getPortal(String portalname) {
        for (MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public MaplePortal getPortal(byte portalid) {
        return portals.get(portalid);
    }
    
    public Map<Byte, MaplePortal> getPortals() {
        return portals;
    }

    public MapleFootholdTree getFootholds() {
        return footholds;
    }

    public Pair<Integer, String> getTimeMob() {
        return timeMob;
    }
    
    public int getReturnMapId() {
        return returnMapId;
    }
    
    public int getForcedReturnId() {
        return forcedReturnId;
    }
    
    public int getMobCapacity() {
        return mobCapacity;
    }
    
    public short getMobInterval() {
        return mobInterval;
    }
    
    public int getFieldType() {
        return fieldType;
    }
    
    public int getTimeLimit() {
        return timeLimit;
    }
    
    public int getFieldLimit() {
        return fieldLimit;
    }
    
    public int getHpDec() {
        return decHp;
    }
    
    public int getProtectItem() {
        return protectItem;
    }
    
    public byte getMonsterRate() {
        return monsterRate;
    }
    
    public boolean getEverlast() {
        return everlast;
    }
    
    public boolean getClock() {
        return clock;
    }
    
    public boolean getBoat() {
        return boat;
    }
    
    public boolean getTown() {
        return town;
    }
    
    public String getUserEnterF() {
        return onEnterF;
    }
    
    public String getUserEnter() {
        return onEnter;
    }
    
    public int getId() {
        return mapid;
    }
    
    public Collection<MapleMapObject> getMapObjects() {
        return mapobjects.values();
    }
    
    public Stream<MapleMapObject> getNpcs() {
        return mapobjects.values().stream().filter((mmo) -> mmo instanceof MapleNPC);
    }
    
    public MapleMapObject getObject(int oid) {
        return mapobjects.get(oid);
    }
    
    public Point calcPointBelow(Point initial) {
        MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            double s1 = Math.abs(fh.getY2() - fh.getY1());
            double s2 = Math.abs(fh.getX2() - fh.getX1());
            double s5 = Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2)));
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) s5;
            } else {
                dropY = fh.getY1() + (int) s5;
            }
        }
        return new Point(initial.x, dropY);
    }
    
    public Point[] calcBossSpawnPosition(Point[] initial) {
        Stack<Point> newPoints = new Stack<>();
        for (Point initial1 : initial) {
            Point newpos = calcPointBelow(initial1);
            if (newpos != null)
                newPoints.add(newpos);
        }
        final byte size = (byte) newPoints.size();
        Point[] newPositions = new Point[size];
        for (byte i = 0; i < size; i++) {
            newPositions[i] = newPoints.pop();
        }
        return newPositions;
    }
}
