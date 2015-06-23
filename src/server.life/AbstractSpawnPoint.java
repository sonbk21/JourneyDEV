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

package server.life;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public abstract class AbstractSpawnPoint {
    
    protected final int monster, mobTime, team, fh, f;
    protected final Point pos;
    protected long nextPossibleSpawn;
    protected int mobInterval = 5000;
    protected final AtomicInteger spawnedMonsters = new AtomicInteger(0);
    protected final boolean immobile;
    
    AbstractSpawnPoint(final MapleMonster monster, Point pos, boolean immobile, int mobTime, int mobInterval, int team) {
        this.monster = monster.getId();
        this.pos = pos;
        this.mobTime = mobTime;
        this.team = team;
        this.fh = monster.getFh();
        this.f = monster.getF();
        this.immobile = immobile;
        this.mobInterval = mobInterval;
        this.nextPossibleSpawn = System.currentTimeMillis();
    }
    
    public abstract boolean shouldSpawn();
    
    public abstract MapleMonster getMonster();
    
    abstract int getF();
    
    abstract int getFh();

}
