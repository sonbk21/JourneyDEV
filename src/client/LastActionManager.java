/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import java.util.EnumMap;

/**
 * JourneyMS
 * 
 */
public class LastActionManager {
    
    private final EnumMap<MapleAction, Long> lastActionHolder;
    private short observedAction; 
    
    public enum MapleAction {
        NPCTALK(500), HARVEST(1000);
        private final int actionLimit;
        
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
                if (observedAction > 1000)
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
