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

package server.properties;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class MapleRewardEntry {

    public final int id;
    public final short min, max;
    public final byte rarity;
    
    public MapleRewardEntry(int id, short min, short max, byte rarity) {
        this.id = id;
        this.min = min;
        this.max = max;
        this.rarity = rarity;
    }
}
