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

package client.inventory;

import client.inventory.Equip.EquipStat;
import java.util.LinkedList;
import java.util.List;
import tools.Pair;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class CustomScroll extends Item {
    
    List<Pair<EquipStat, Integer>> effects = new LinkedList<>();
    ScrollType type;
    private int success;
    private int cursed;
    
    public enum ScrollType {
        ALL, WEAPON, ARMOR, ACCESSORY, HELMET, TOP, BOTTOM, OVERALL, GLOVES, SHOES, CAPE, EARRING, BELT, PENDANT;
    }

    public CustomScroll(int id, byte position, short quantity) {
        super(id, position, quantity);
        type = ScrollType.ALL;
        success = 100;
        cursed = 0;
    }
    
    public boolean addEffect(EquipStat se, boolean highvalue, boolean dark) { //add a one stat effect, return wether it can be added or not
        success = (int) (success * ((highvalue)?(dark)?0.3:0.1:(dark)?0.7:0.6));
        cursed += (dark)?(highvalue)?35:15:0;
        if (success < 1 || cursed > 99)
            return false;
        effects.add( new Pair(se, calcValue(highvalue)));
        return true;
    }
    
    public boolean addSpecialEffect(List<Pair<EquipStat, Integer>> effects, int success, int cursed) { //add a special effect, for example dragon stone
        this.success = (int) (success * ((float) success)/100);
        this.cursed += cursed;
        if (this.success < 1 || this.cursed > 99)
            return false;
        this.effects.addAll(effects);
        return true;
    }
    
    public int calcValue(boolean highvalue) {
        if (type == ScrollType.WEAPON || type == ScrollType.ARMOR || type == ScrollType.OVERALL)
            return (highvalue)?5:2;
        else
            return (highvalue)?3:2;
    }
    
    public Equip applyEffects(Equip eq) {
        for (Pair<EquipStat, Integer> line : effects) {
            eq.setStat(line.getLeft(), (short) (eq.getStat(line.getLeft()) + line.getRight()));
        }
        return eq;
    }
    
    public ScrollType getScrollType() {
        return type;
    }
    
    public int getSuccess() {
        return success;
    }
    
    public int getCursed() {
        return cursed;
    }
}
