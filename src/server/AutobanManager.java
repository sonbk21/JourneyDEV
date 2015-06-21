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

package client;

import java.io.File;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;

/**
 * JourneyMS
 * 
 */
public class AutobanManager {
    
    private static final EnumMap<WZCheck, HashSet<Integer>> legitIDs = new EnumMap<>(WZCheck.class);
    private static final ReentrantLock dataLoadLock = new ReentrantLock(false);
    
    public enum WZCheck {
        MAKECHARINFO;
    }

    public static boolean checkWZNotLegit(WZCheck img, Stream<Integer> ids) {
        final HashSet<Integer> legitIds = getLegitIDs(img);
        return !ids.allMatch((id) -> legitIds.contains(id));
    }
    
    public static HashSet<Integer> getLegitIDs(WZCheck img) {
        if (legitIDs.containsKey(img)) {
            return legitIDs.get(img);
        } else {
            dataLoadLock.lock();
            try {
                if (legitIDs.containsKey(img)) {
                    return legitIDs.get(img);
                }
                
                final HashSet<Integer> ret = new HashSet<>();
                final MapleDataProvider source;
                final MapleData data;
                switch (img) {
                    case MAKECHARINFO:
                        source = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Etc.wz"));
                        data = source.getData("MakeCharInfo.img").getChildByPath("Info");
                        data.getChildren().stream().forEach((gender) -> {
                            gender.getChildren().stream().forEach((cat) -> {
                                cat.getChildren().stream().forEach((id) -> {
                                    ret.add(MapleDataTool.getIntConvert(id));
                                });
                            });
                        });
                        break;
                }
                legitIDs.put(img, ret);
                return ret;
            } finally {
                dataLoadLock.unlock();
            }
        }
    }
}
