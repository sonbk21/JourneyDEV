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
import client.inventory.Equip.PotentialRank;
import tools.Randomizer;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class PotentialLine {

    private final EquipStat effect;
    private final PotentialLevel superior;
    
    public enum PotentialLevel {
        ZERO, ONE, TWO, THREE;
    }
    
    public PotentialLine(EquipStat effect, PotentialLevel superior) {
        this.effect = effect;
        this.superior = superior;
    }
    
    public PotentialLine(int line) {
        this.effect = EquipStat.values()[line%100];
        this.superior = PotentialLevel.values()[line/100 - (line/1000)*10];
    }
    
    public PotentialLine(EquipStat effect) {
        this.effect = effect;
        this.superior = PotentialLevel.values()[Randomizer.nextInt(PotentialLevel.values().length)];
    }
    
    public int getValue(PotentialRank rank) {
        if (effect == EquipStat.HANDS || effect == EquipStat.WATK || effect == EquipStat.MAGIC)
            return rank.ordinal();
        else if (effect == EquipStat.HP || effect == EquipStat.MP || effect == EquipStat.WDEF || effect == EquipStat.MDEF)
            return 10*rank.ordinal() - (superior.ordinal()/2)*5;
        else
            return 2*rank.ordinal() - superior.ordinal()/2;
    }
    
    public EquipStat getEffect() {
        return effect;
    }
    
    public PotentialLevel getSuperior() {
        return superior;
    }
}
