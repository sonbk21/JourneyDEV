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

package client.properties;

import tools.Randomizer;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class AbilityLine {

    private final AbilityStat effect;
    private final AbilityLevel superior;
    
    public enum AbilityStat {
        STR, DEX, INT, LUK, HP, MP, BDM;
    }
    
    public enum AbilityLevel {
        ZERO, ONE, TWO;
    }
    
    public AbilityLine(AbilityStat effect, AbilityLevel superior) {
        this.effect = effect;
        this.superior = superior;
    }
    
    public AbilityLine(short line) {
        this.effect = AbilityStat.values()[line%100];
        this.superior = AbilityLevel.values()[line/100 - (line/1000)*10];
    }
    
    public AbilityLine() {
        this.effect = AbilityStat.values()[Randomizer.nextInt(AbilityStat.values().length)];
        this.superior = AbilityLevel.values()[Randomizer.nextInt(AbilityLevel.values().length)];
    }
    
    public int getValue(byte rank) {
        return (effect.equals(AbilityStat.BDM)?2:1)*(3*rank - superior.ordinal());
    }
    
    public String getName(byte rank) {
        return String.valueOf(effect).replaceFirst("BDM", "Boss Damage").concat(" ").concat(String.valueOf(getValue(rank))).concat("%");
    }
    
    public AbilityStat getEffect() {
        return effect;
    }
    
    public AbilityLevel getSuperior() {
        return superior;
    }
}
