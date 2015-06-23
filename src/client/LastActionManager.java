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

import java.util.EnumMap;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class LastActionManager {
    
    private final EnumMap<MapleAction, Long> lastActionHolder;
    private short observedAction; 
    
    public enum MapleAction {
        NPCTALK(500), HARVEST, HEAL(1500), ITEMSORT, PETFOOD, CATCHITEM, SPECIALMOVE(500);
        private final int actionLimit;
        
        MapleAction() {
            this(1000);
        }
        
        MapleAction(int actionLimit) {
            this.actionLimit = actionLimit;
        }
        
        public int getActionLimit() {
            return actionLimit;
        }
    }
    
    public enum ActionResult {
        ALLOW, DISALLOW, DISCONNECT;
    }
    
    public LastActionManager() {
        lastActionHolder = new EnumMap<>(MapleAction.class);
        observedAction = 0;
    }
    
    public ActionResult tryAction(MapleAction ma) {
        if (lastActionHolder.containsKey(ma)) {
            if (System.currentTimeMillis() - lastActionHolder.get(ma) < 200) {
                observedAction ++;
                if (observedAction > 50)
                    return ActionResult.DISCONNECT;
            } else {
                observedAction = (short) (observedAction/2);
            }
            
            if (System.currentTimeMillis() > lastActionHolder.get(ma) + ma.getActionLimit())
                return ActionResult.ALLOW;
            else
                return ActionResult.DISALLOW;
        } else {
            return ActionResult.ALLOW;
        }
    }
    
    public void setLastAction(MapleAction ma) {
        lastActionHolder.put(ma, System.currentTimeMillis());
    }
}
