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

import java.awt.Point;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.PortalFactory;
import server.ProfessionFactory;
import server.life.AbstractLoadedMapleLife;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import tools.DatabaseConnection;
import tools.StringUtil;

public class MapleMapDataFactory {

    private final MapleDataProvider source;
    private final MapleData nameData;
    
    private final Map<String, Integer> mapNames = new HashMap<>();
    private static final Map<Integer, MapleMapData> mapdata = new HashMap<>();
    
    private static MapleMapDataFactory instance;
    private final ReentrantLock dataLoadLock;

    private MapleMapDataFactory() {
        this.source = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map.wz")); 
        this.nameData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz")).getData("Map.img");
        dataLoadLock = new ReentrantLock(false);
    }
    
    public static MapleMapDataFactory getInstance() {
        if (instance == null) {
            instance = new MapleMapDataFactory();
        }
        return instance;
    }
    
    public boolean isMapDataLoaded(int mapid) {
        return mapdata.containsKey(mapid);
    }

    public MapleMapData getMapData(int mapid) {
        if (mapdata.containsKey(mapid)) {
            return mapdata.get(mapid);
        } else  {
            dataLoadLock.lock();
            try {
                MapleMapData map = mapdata.get(mapid);
                if (map != null) {
                    return map;
                }
                
                String mapName = getMapName(mapid);
                MapleData mapData = source.getData(mapName);
                if (mapData == null)
                    return null;
                
                String link = MapleDataTool.getString(mapData.getChildByPath("info/link"), "");
                if (!link.isEmpty()) {
                    mapName = getMapName(Integer.parseInt(link));
                    mapData = source.getData(mapName);
                }
                
                map = new MapleMapData(mapid, MapleDataTool.getInt("info/returnMap", mapData),
                                        MapleDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999),
                                        (mapData.getChildByPath("info/mobRate") != null)? (Float) mapData.getChildByPath("info/mobRate").getData() : 0,
                                        MapleDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), String.valueOf(mapid)),
                                        MapleDataTool.getString(mapData.getChildByPath("info/onUserEnter"), String.valueOf(mapid)), 
                                        MapleDataTool.getIntConvert("info/fieldType", mapData, 0),
                                        MapleDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0),
                                        MapleDataTool.getIntConvert("fixedMobCapacity", mapData.getChildByPath("info"), 500),
                                        (short) MapleDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), 5000),
                                        mapData.getChildByPath("clock") != null,
                                        mapData.getChildByPath("everlast") != null,
                                        mapData.getChildByPath("town") != null,
                                        mapData.getChildByPath("shipObj") != null,
                                        MapleDataTool.getIntConvert("decHP", mapData, 0),
                                        MapleDataTool.getIntConvert("protectItem", mapData, 0),
                                        MapleDataTool.getIntConvert("timeLimit", mapData.getChildByPath("info"), -1));
                
                PortalFactory portalFactory = new PortalFactory();
                for (MapleData portal : mapData.getChildByPath("portal")) {
                    map.addPortal(portalFactory.makePortal(MapleDataTool.getInt(portal.getChildByPath("pt")), portal));
                }
                
                MapleData timeMob = mapData.getChildByPath("info/timeMob");
                if (timeMob != null) {
                    map.addTimeMob(MapleDataTool.getInt(timeMob.getChildByPath("id")), MapleDataTool.getString(timeMob.getChildByPath("message")));
                }

                List<MapleFoothold> allFootholds = new LinkedList<>();
                Point lBound = new Point();
                Point uBound = new Point();
                for (MapleData footRoot : mapData.getChildByPath("foothold")) {
                    for (MapleData footCat : footRoot) {
                        for (MapleData footHold : footCat) {
                            int x1 = MapleDataTool.getInt(footHold.getChildByPath("x1"));
                            int y1 = MapleDataTool.getInt(footHold.getChildByPath("y1"));
                            int x2 = MapleDataTool.getInt(footHold.getChildByPath("x2"));
                            int y2 = MapleDataTool.getInt(footHold.getChildByPath("y2"));
                            MapleFoothold fh = new MapleFoothold(new Point(x1, y1), new Point(x2, y2), Integer.parseInt(footHold.getName()));
                            fh.setPrev(MapleDataTool.getInt(footHold.getChildByPath("prev")));
                            fh.setNext(MapleDataTool.getInt(footHold.getChildByPath("next")));
                            if (fh.getX1() < lBound.x) {
                                lBound.x = fh.getX1();
                            }
                            if (fh.getX2() > uBound.x) {
                                uBound.x = fh.getX2();
                            }
                            if (fh.getY1() < lBound.y) {
                                lBound.y = fh.getY1();
                            }
                            if (fh.getY2() > uBound.y) {
                                uBound.y = fh.getY2();
                            }
                            allFootholds.add(fh);
                        }
                    }
                }
                MapleFootholdTree fTree = new MapleFootholdTree(lBound, uBound);
                allFootholds.stream().forEach((fh) -> { fTree.insert(fh); });
                map.setFootholds(fTree);
                
                try { 
                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM playernpcs WHERE map = ?")) {
                        ps.setInt(1, mapid);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                map.addMapObject(new PlayerNPCs(rs));
                            }
                            rs.close();
                        }
                        ps.close();
                    }
                } catch (Exception e) {
                }
                
                if (mapData.getChildByPath("reactor") != null) {
                    for (MapleData reactor : mapData.getChildByPath("reactor")) {
                        String id = MapleDataTool.getString(reactor.getChildByPath("id"));
                        if (id != null) {
                            if (ProfessionFactory.getInstance().isHarvestable(Integer.valueOf(id))) {
                                MapleHarvestable newHarvestable = loadHarvestable(reactor, id);
                                map.addMapObject(newHarvestable);
                            } else {
                                MapleReactor newReactor = loadReactor(reactor, id);
                                map.addMapObject(newReactor);
                            }
                        }
                    }
                }
                
                for (MapleData life : mapData.getChildByPath("life")) {
                    String id = MapleDataTool.getString(life.getChildByPath("id"));
                    String type = MapleDataTool.getString(life.getChildByPath("type"));
                    
                    AbstractLoadedMapleLife myLife = loadLife(life, id, type);
                    if (myLife instanceof MapleMonster) {
                        MapleMonster monster = (MapleMonster) myLife;
                        int mobTime = MapleDataTool.getInt("mobTime", life, 0);
                        int team = MapleDataTool.getInt("team", life, -1);
                        myLife.setMobtime(mobTime);
                        myLife.setTeam(team);
                        map.addMapObject(monster);
                    } else {
                        map.addMapObject(myLife);
                    }
                }
                
                mapdata.put(mapid, map);
                return map;
            } finally {
                dataLoadLock.unlock();
            }
        }
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
    
    private AbstractLoadedMapleLife loadLife(MapleData life, String id, String type) {
        AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(Integer.parseInt(id), type);
        myLife.setCy(MapleDataTool.getInt(life.getChildByPath("cy")));
        MapleData dF = life.getChildByPath("f");
        if (dF != null) {
            myLife.setF(MapleDataTool.getInt(dF));
        }
        myLife.setFh(MapleDataTool.getInt(life.getChildByPath("fh")));
        myLife.setRx0(MapleDataTool.getInt(life.getChildByPath("rx0")));
        myLife.setRx1(MapleDataTool.getInt(life.getChildByPath("rx1")));
        int x = MapleDataTool.getInt(life.getChildByPath("x"));
        int y = MapleDataTool.getInt(life.getChildByPath("y"));
        myLife.setPosition(new Point(x, y));
        int hide = MapleDataTool.getInt("hide", life, 0);
        if (hide == 1) {
            myLife.setHide(true);
        }
        return myLife;
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
        myReactor.setDelay(10000);
        myReactor.setState((byte) 0);
        myReactor.setName(MapleDataTool.getString(reactor.getChildByPath("viewName"), ""));
        return myReactor;
    }
    
    public void loadMapNames() {
        for (MapleData parents : nameData.getChildren()) {
            for (MapleData data : parents.getChildren()) {
                String name = MapleDataTool.getString("mapName", data, "");
                if (name != null)
                    mapNames.put(name, Integer.valueOf(data.getName()));
            }
        }
    }
    
    public int searchMapByName(String[] name) {
        if (mapNames.containsKey(StringUtil.joinStringFrom(name, 1)))
            return getMapData(mapNames.get(StringUtil.joinStringFrom(name, 1))).getId();
        
        int matchId = 0;
        Double matches;
        Double prevMatches = 0.0;
        for (String fullName : mapNames.keySet()) {
            String[] SplitNames = fullName.split(" ");
            matches = 0.0;
            for (String SplitName : SplitNames) {
                for (int k = 1; k < name.length; k++) {
                    if (SplitName.equalsIgnoreCase(name[k])) {
                        matches += 1;
                    }
                }
            }
            matches /= (double) SplitNames.length;
            if (matches > prevMatches) {
                prevMatches = matches;
                matchId = mapNames.get(fullName);
            }
        }
        
        return matchId;
    }
}
