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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import server.life.AreaBossData;
import server.life.AreaBossFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;

public class MapleMapFactory {

    private final Map<Integer, MapleMap> maps = new HashMap<>();
    private final byte world;
    private final byte channel;
    private final ReentrantLock mapLoadLock;

    public MapleMapFactory(byte world, byte channel) {
        this.world = world;
        this.channel = channel;
        mapLoadLock = new ReentrantLock(false);
    }

    public MapleMap getMap(int mapid) {
        if (maps.containsKey(mapid)) {
            return maps.get(mapid);
        } else {
            mapLoadLock.lock();
            try {
                if (maps.containsKey(mapid)) {
                    return maps.get(mapid);
                }
                final MapleMap map = new MapleMap(mapid, world, channel);
                
                MapleMapData data = MapleMapDataFactory.getInstance().getMapData(mapid);
                map.setTimeLimit(data.getTimeLimit());
                
                if (AreaBossFactory.hasBoss(map.getId())) {
                    AreaBossData ab = AreaBossFactory.getBossData(map.getId());
                    map.addBossSpawn(MapleLifeFactory.getMonster(ab.getId()), data.calcBossSpawnPosition(ab.getPosition()), ab.getIntervall(), ab.getMsg());
                }
                
                data.getMapObjects().stream().forEach((mmo) -> {
                    if (mmo instanceof MapleMonster) {
                        MapleMonster mob = (MapleMonster) mmo;
                        if (mob.getMobtime() == -1) {
                            map.spawnMonster(mob);
                        } else {
                            map.addMonsterSpawn(mob, mob.getMobtime(), mob.getTeam(), data.calcPointBelow(mob.getPosition()));
                        }
                    } else if (mmo instanceof MapleReactor) {
                        MapleReactor reactor = (MapleReactor) mmo;
                        map.spawnReactor(reactor);
                    } else if (mmo instanceof MapleHarvestable) {
                        MapleHarvestable harvestable = (MapleHarvestable) mmo;
                        map.spawnReactor(harvestable);
                    } else if (mmo instanceof MapleNPC) {
                        map.incrementRunningOid();
                    }
                });
                
                maps.put(mapid, map);
                return map;
            } finally {
                mapLoadLock.unlock();
            }
        }
    }

    public boolean isMapLoaded(int mapId) {
        return maps.containsKey(mapId);
    }

    public Map<Integer, MapleMap> getMaps() {
        return Collections.unmodifiableMap(maps);
    }
}
