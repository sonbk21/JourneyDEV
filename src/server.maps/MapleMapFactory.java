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
package server.maps;

import constants.ServerConstants;
import java.awt.Point;
import java.awt.Rectangle;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataTool;
import server.PortalFactory;
import server.ProfessionFactory;
import server.life.AbstractLoadedMapleLife;
import server.life.AreaBossData;
import server.life.AreaBossFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import tools.DatabaseConnection;
import tools.StringUtil;

public class MapleMapFactory {

    private final MapleDataProvider source;
    private final MapleData nameData;
    private final Map<Integer, MapleMap> maps = new HashMap<>();
    private final Map<Integer, Integer> chaosIdCache = new HashMap<>();
    private final byte world;
    private final byte channel;
    private final byte helllevel;

    public MapleMapFactory(MapleDataProvider source, MapleDataProvider stringSource, byte world, byte channel) {
        this.source = source;
        this.nameData = stringSource.getData("Map.img");
        this.world = world;
        this.channel = channel;
        if (ServerConstants.HELL_EVENT)
            helllevel = (byte) ((channel-1) * 10);
        else
            helllevel = 0;
    }

    public MapleMap getMap(int mapid) {
        Integer omapid = mapid;
        MapleMap map = maps.get(omapid);
        if (map == null) {
            synchronized (this) {
                map = maps.get(omapid);
                if (map != null) {
                    return map;
                }
                String mapName = getMapName(mapid);
                MapleData mapData = source.getData(mapName);
                if (mapData == null)
                    return null;
                
                map = new MapleMap(mapid, world, channel);
                
                MapleMapData data = MapleMapDataFactory.getInstance().getMapData(mapid);
                map.setEverlast(data.getEverlast());
                map.setBoat(data.getBoat());
                map.setTown(data.getTown());
                map.setClock(data.getClock());
                map.setReturnMapId(data.getReturnMapId());
                map.setMonsterRate(data.getMonsterRate());
                map.setMobCapacity(data.getMobCapacity());
                map.setMobInterval(data.getMobInterval());
                map.setFieldLimit(data.getFieldLimit());
                map.setFieldType(data.getFieldType());
                map.setForcedReturnMap(data.getForcedReturnId());
                map.setHPDec(data.getHpDec());
                map.setHPDecProtect(data.getProtectItem());
                map.setOnFirstUserEnter(data.getUserEnterF());
                map.setOnUserEnter(data.getUserEnter());
                map.setTimeLimit(data.getTimeLimit());
                
                if (AreaBossFactory.hasBoss(map.getId())) { //add areabosses
                    AreaBossData ab = AreaBossFactory.getBossData(map.getId());
                    map.addBossSpawn(MapleLifeFactory.getMonster(ab.getId()), ab.getPosition(), ab.getIntervall(), ab.getMsg());
                }
                
                if (mapData.getChildByPath("reactor") != null) {
                    for (MapleData reactor : mapData.getChildByPath("reactor")) {
                        String id = MapleDataTool.getString(reactor.getChildByPath("id"));
                        if (id != null) {
                            if (ProfessionFactory.getInstance().isHarvestable(Integer.valueOf(id))) {
                                MapleHarvestable newHarvestable = loadHarvestable(reactor, id);
                                map.spawnReactor(newHarvestable);
                            } else {
                                MapleReactor newReactor = loadReactor(reactor, id);
                                map.spawnReactor(newReactor);
                            }
                        }
                    }
                }
                
                for (MapleMapObject mmo : data.getMapObjects()) {
                    if (mmo instanceof MapleMonster) {
                        MapleMonster mob = (MapleMonster) mmo;
                        if (mob.getMobtime() == -1) {
                            map.spawnMonster(mob);
                        } else {
                            map.addMonsterSpawn(mob, mob.getMobtime(), mob.getTeam());
                        }
                    } else {
                        map.addMapObject(mmo);
                    }
                }
                
                maps.put(omapid, map);
            }
        }
        return map;
    }

    public boolean isMapLoaded(int mapId) {
        return maps.containsKey(mapId);
    }

    private MapleReactor loadReactor(MapleData reactor, String id) {
        MapleReactor myReactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(id)), Integer.parseInt(id));
        int x = MapleDataTool.getInt(reactor.getChildByPath("x"));
        int y = MapleDataTool.getInt(reactor.getChildByPath("y"));
        myReactor.setPosition(new Point(x, y));
        myReactor.setDelay(MapleDataTool.getInt(reactor.getChildByPath("reactorTime")) * 1000);
        myReactor.setState((byte) 0);
        myReactor.setName(MapleDataTool.getString(reactor.getChildByPath("name"), ""));
        return myReactor;
    }
    
    private MapleHarvestable loadHarvestable(MapleData reactor, String id) {
        MapleHarvestable myReactor = new MapleHarvestable(MapleReactorFactory.getReactor(Integer.parseInt(id)), Integer.parseInt(id), ProfessionFactory.getInstance().getStats(Integer.valueOf(id)));
        int x = MapleDataTool.getInt(reactor.getChildByPath("x"));
        int y = MapleDataTool.getInt(reactor.getChildByPath("y"));
        myReactor.setPosition(new Point(x, y));
        myReactor.setDelay(1000);
        myReactor.setState((byte) 0);
        myReactor.setName(MapleDataTool.getString(reactor.getChildByPath("viewName"), ""));
        return myReactor;
    }

    private String getMapName(int mapid) {
        String mapName = StringUtil.getLeftPaddedStr(Integer.toString(mapid), '0', 9);
        StringBuilder builder = new StringBuilder("Map/Map");
        int area = mapid / 100000000;
        builder.append(area);
        builder.append("/");
        builder.append(mapName);
        builder.append(".img");
        mapName = builder.toString();
        return mapName;
    }

    private String getMapStringName(int mapid) {
        StringBuilder builder = new StringBuilder();
        if (mapid < 100000000) {
            builder.append("maple");
        } else if (mapid >= 100000000 && mapid < 200000000) {
            builder.append("victoria");
        } else if (mapid >= 200000000 && mapid < 300000000) {
            builder.append("ossyria");
        } else if (mapid >= 540000000 && mapid < 541010110) {
            builder.append("singapore");
        } else if (mapid >= 600000000 && mapid < 620000000) {
            builder.append("MasteriaGL");
        } else if (mapid >= 670000000 && mapid < 682000000) {
            builder.append("weddingGL");
        } else if (mapid >= 682000000 && mapid < 683000000) {
            builder.append("HalloweenGL");
        } else if (mapid >= 800000000 && mapid < 900000000) {
            builder.append("jp");
        } else {
            builder.append("etc");
        }
        builder.append("/").append(mapid);
        return builder.toString();
    }

    public Map<Integer, MapleMap> getMaps() {
        return Collections.unmodifiableMap(maps);
    }
}
