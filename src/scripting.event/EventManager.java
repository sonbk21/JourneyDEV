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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptException;
import net.server.channel.Channel;
import net.server.world.MapleParty;
import scripting.event.RecordsManager.RecordEvent;
import server.TimerManager;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import tools.Pair;

/**
 *
 * @author Matze, modified by SYJourney
 */
public class EventManager {
    private final Invocable iv;
    private final Channel cserv;
    private final Map<String, EventInstanceManager> instances = new HashMap<>();
    private final Properties props = new Properties();
    private final String name;
    private ScheduledFuture<?> schedule = null;

    public EventManager(Channel cserv, Invocable iv, String name) {
        this.iv = iv;
        this.cserv = cserv;
        this.name = name;
    }

    public void cancel() {
        try {
            iv.invokeFunction("cancelSchedule", (Object) null);
        } catch (ScriptException | NoSuchMethodException ex) {
        }
    }

    public void schedule(String methodName, long delay) {
        schedule(methodName, null, delay);
    }

    public void schedule(final String methodName, final EventInstanceManager eim, long delay) {
        schedule = TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    iv.invokeFunction(methodName, eim);
                } catch (ScriptException | NoSuchMethodException ex) {
                    Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, delay);
    }

    public void cancelSchedule() {
        schedule.cancel(true);
    }

    public ScheduledFuture<?> scheduleAtTimestamp(final String methodName, long timestamp) {
        return TimerManager.getInstance().scheduleAtTimestamp(new Runnable() {
            @Override
            public void run() {
                try {
                    iv.invokeFunction(methodName, (Object) null);
                } catch (ScriptException | NoSuchMethodException ex) {
                    Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, timestamp);
    }

    public Channel getChannelServer() {
        return cserv;
    }
    
    public MapleMonster getMob(int id) { //New, just so I don't have to import
        return MapleLifeFactory.getMonster(id);
    }

    public EventInstanceManager getInstance(String name) {
        return instances.get(name);
    }

    public Collection<EventInstanceManager> getInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public EventInstanceManager newInstance(String name) {
        EventInstanceManager ret = new EventInstanceManager(this, name);
        instances.put(name, ret);
        return ret;
    }

    public void disposeInstance(String name) {
        instances.remove(name);
    }

    public Invocable getIv() {
        return iv;
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public String getName() {
        return name;
    }
    
    public List<Pair<String, Integer>> getWorldRecords() { //New functions, related to record system.
        return cserv.getRecordsManager().getRecords(RecordEvent.valueOf(name));
    }
    
    public RecordsManager getWorldRecordsManager() {
        return cserv.getRecordsManager();
    }
    
    public String getNpcTextRankings() {
        boolean loaded = true;
        
        if (getWorldRecords() == null)
            loaded = cserv.getRecordsManager().loadRecords(RecordEvent.valueOf(name));
        if (loaded) {
            System.out.println("loaded");
            List<Pair<String, Integer>> records = getWorldRecords();
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (Pair<String, Integer> ere : records) {
                sb.append(String.valueOf(i+1));
                sb.append(". ");
                sb.append(String.valueOf((int) Math.floor(ere.getRight()/60)));
                sb.append(":");
                if (ere.getRight()%60 < 10)
                    sb.append("0");
                sb.append(String.valueOf(ere.getRight()%60));
                sb.append(" by ");
                sb.append(ere.getLeft());
                sb.append("\r\n");
                i++;
            }
            return sb.toString();
        } else {
            return "";
        }
    } //End of modifications

    //PQ method: starts a PQ
    public void startInstance(MapleParty party, MapleMap map) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", (Object) null));
            eim.registerParty(party, map);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //non-PQ method for starting instance
    public void startInstance(EventInstanceManager eim, String leader) {
        try {
            iv.invokeFunction("setup", eim);
            eim.setProperty("leader", leader);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
