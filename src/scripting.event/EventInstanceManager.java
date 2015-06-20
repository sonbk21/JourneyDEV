/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package scripting.event;

import client.MapleCharacter;
import java.awt.Point;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import net.server.world.MapleParty;
import scripting.event.RecordsManager.RecordEvent;
import server.MapleInventoryManipulator;
import server.TimerManager;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.Pair;

/**
 *
 * @author Matze, modified by SYJourney
 */
public class EventInstanceManager {
    private final List<MapleCharacter> chars = new ArrayList<>();
    private final List<MapleMonster> mobs = new LinkedList<>();
    private final Map<MapleCharacter, Integer> killCount = new HashMap<>();
    private EventManager em;
    private MapleMapFactory mapFactory;
    private final String name;
    private final Properties props = new Properties();
    private long timeStarted = 0;
    private long eventTime = 0;
    private final Map<String, Integer> deathcountholder = new HashMap<>();

    public EventInstanceManager(EventManager em, String name) {
        this.em = em;
        this.name = name;
        mapFactory = new MapleMapFactory(em.getChannelServer().getId(), (byte) 1);//Fk this <- yup
    }

    public EventManager getEm() {
        return em;
    }
    
    public void registerPlayer(MapleCharacter chr) {
        try {
            chars.add(chr);
            chr.setEventInstance(this);
            em.getIv().invokeFunction("playerEntry", this, chr);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void startEventTimer(long time) {
        timeStarted = System.currentTimeMillis();
        eventTime = time;
    }

    public boolean isTimerStarted() {
        return eventTime > 0 && timeStarted > 0;
    }

    public long getTimeLeft() {
        return eventTime - (System.currentTimeMillis() - timeStarted);
    }

    public void registerParty(MapleParty party, MapleMap mapleMap) {
        party.getMembers().stream().map((pc) -> mapleMap.getCharacterById(pc.getId())).forEach((c) -> registerPlayer(c));
    }

    public void unregisterPlayer(MapleCharacter chr) {
        chars.remove(chr);
        chr.setEventInstance(null);
    }

    public int getPlayerCount() {
        return chars.size();
    }

    public List<MapleCharacter> getPlayers() {
        return new ArrayList<>(chars);
    }
    
    public void giveItem(MapleCharacter chr, int id, short qty) { //New functions, most just make it so that I don't have to import stuff in my scripts.
        MapleInventoryManipulator.addById(chr.getClient(), id, qty);
    }
    
    public long getTime() {
        return System.currentTimeMillis();
    }
    
    public void broadcastClock(int timelimit) {
        List <MapleCharacter> pl = getPlayers();
        pl.stream().forEach((chr) -> {
            chr.getClient().getSession().write(MaplePacketCreator.getClock(timelimit));
        });
    }
    
    public void broadcastClock(MapleCharacter chr, int timelimit) {
        chr.getClient().getSession().write(MaplePacketCreator.getClock(timelimit));
    }
    
    public void broadcastClock(final MapleCharacter chr, final int timelimit, int delay) {
        TimerManager.getInstance().schedule(() -> {
            chr.getClient().getSession().write(MaplePacketCreator.getClock(timelimit));
        }, delay);
    }
    
    public int setRecord(int time) {
        RecordEvent event = RecordEvent.valueOf(em.getName().toUpperCase());
        List<Pair<String, Integer>> records = em.getWorldRecordsManager().loadRecords(event);
        
        if (records == null) {
            return 0;
        }
        
        if (!records.isEmpty()) {
            for (Pair<String, Integer> ere : records) {
                if (time < ere.getRight()) {
                    if (em.getWorldRecordsManager().updateRecords(event, getPartyNames(), time, records.indexOf(ere)))
                        return records.indexOf(ere) + 1;
                    else
                        return 0;
                }
            }
            if (records.size() > 14)
                return 0;
            em.getWorldRecordsManager().updateRecords(event, getPartyNames(), time, records.size());
            return records.size();
        } else {
            em.getWorldRecordsManager().updateRecords(event, getPartyNames(), time, 0);
            return 1;
        }
    }
    
    public String getPartyNames() {
        StringBuilder names = new StringBuilder();
        getPlayers().stream().forEach((chr) -> {
            names.append(chr.getName());
            names.append(", ");
        });
        return names.substring(0, names.length()-2);
    }
    
    public void spawnMonster(MapleMap map, MapleMonster mob, int x, int y) {
        map.spawnMonsterOnGroudBelow(mob, new Point(x, y));
    }
    
    public void addToDeathCount(MapleCharacter chr) {
        if (deathcountholder.containsKey(chr.getName()))
            deathcountholder.put(chr.getName(), deathcountholder.get(chr.getName())+1);
        else
            deathcountholder.put(chr.getName(), 1);
    }
    
    public int getDeathCount(MapleCharacter chr) {
        return deathcountholder.containsKey(chr.getName())?deathcountholder.get(chr.getName()):0;
    } 
    
    public void addDropToMonster(MapleMonster mob, int itemid, int max) {
        if (mobs.contains(mob)) {
            mobs.remove(mob);
            mob.addCustomDrop(itemid, max);
            mob.setEventInstance(this);
            mobs.add(mob);
        } else {
            mob.addCustomDrop(itemid, max);
        }
    }//End of modification

    public void registerMonster(MapleMonster mob) {
        mob.setEventInstance(this);
        mobs.add(mob);
    }

    public void unregisterMonster(MapleMonster mob) {
        mob.setEventInstance(null);
        mobs.remove(mob);
        if (mobs.isEmpty()) {
            try {
                em.getIv().invokeFunction("allMonstersDead", this);
            } catch (ScriptException | NoSuchMethodException ex) {
                Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void playerKilled(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerDead", this, chr);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean revivePlayer(MapleCharacter chr) {
        try {
            Object b = em.getIv().invokeFunction("playerRevive", this, chr);
            if (b instanceof Boolean) {
                return (Boolean) b;
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    public void playerDisconnected(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerDisconnected", this, chr);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void mapChanged(MapleCharacter chr, int mapid) {
        try {
            em.getIv().invokeFunction("mapChanged", this, chr, mapid);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void monsterKilled(MapleCharacter chr, MapleMonster mob) {
        try {
            Integer kc = killCount.get(chr);
            int inc = (Integer) em.getIv().invokeFunction("monsterValue", this, mob.getId());
            if (kc == null) {
                kc = inc;
            } else {
                kc += inc;
            }
            killCount.put(chr, kc);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getKillCount(MapleCharacter chr) {
        Integer kc = killCount.get(chr);
        if (kc == null) {
            return 0;
        } else {
            return kc;
        }
    }

    public void dispose() {
        try {
            chars.clear();
            mobs.clear();
            killCount.clear();
            deathcountholder.clear();
            mapFactory = null;
            em.disposeInstance(name);
            em = null;
        } catch (NullPointerException ne) {
            //Not sure how else to prevent this
        }
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public void schedule(final String methodName, long delay) {
        TimerManager.getInstance().schedule(() -> {
            try {
                em.getIv().invokeFunction(methodName, EventInstanceManager.this);
            } catch (ScriptException | NoSuchMethodException ex) {
                Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, delay);
    }

    public String getName() {
        return name;
    }

    public void saveWinner(MapleCharacter chr) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO eventstats (event, instance, characterid, channel) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, em.getName());
                ps.setString(2, getName());
                ps.setInt(3, chr.getId());
                ps.setInt(4, chr.getClient().getChannel());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public MapleMap getMapInstance(int mapId) {
        MapleMap map = mapFactory.getMap(mapId);
        if (!mapFactory.isMapLoaded(mapId)) {
            if (em.getProperty("shuffleReactors") != null && em.getProperty("shuffleReactors").equals("true")) {
                map.shuffleReactors();
            }
        }
        return map;
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public Object setProperty(String key, String value, boolean prev) {
        return props.setProperty(key, value);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public void leftParty(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("leftParty", this, chr);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void disbandParty() {
        try {
            em.getIv().invokeFunction("disbandParty", this);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);        }
    }

    public void finishPQ() {
        try {
            em.getIv().invokeFunction("clearPQ", this);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void removePlayer(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerExit", this, chr);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventInstanceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean isLeader(MapleCharacter chr) {
        return (chr.getParty().getLeader().getId() == chr.getId());
    }
}
