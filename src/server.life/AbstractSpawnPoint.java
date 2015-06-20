/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server.life;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JourneyMS
 * 
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
