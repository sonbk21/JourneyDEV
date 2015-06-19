/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server.maps;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import server.MaplePortal;
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
    
    private final Map<Byte, MaplePortal> portals = new HashMap<>();
    private Pair<Integer, String> timeMob;
    private final Stack<MapleMapObject> mapobjects = new Stack<>();
    
    private MapleFootholdTree footholds = null;
    
    public MapleMapData(int mapid, int returnMapId, int forcedReturnId,  float monsterRate, String onEnterF, String onEnter, int fieldType, int fieldLimit, int mobCapacity, short mobInterval, boolean clock, boolean town, boolean everlast, boolean boat, int decHp, int protectItem, int timeLimit) {
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
    }
    
    public void addPortal(MaplePortal myPortal) {
        portals.put((byte) myPortal.getId(), myPortal);
    }
    
    public void addMapObject(MapleMapObject mapobject) {
        mapobjects.push(mapobject);
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

    public void setFootholds(MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public MapleFootholdTree getFootholds() {
        return footholds;
    }
    
    public void addTimeMob(int id, String msg) {
        timeMob = new Pair<>(id, msg);
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
    
    public Stack<MapleMapObject> getMapObjects() {
        return mapobjects;
    }
}
