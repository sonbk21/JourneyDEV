package server.life;

import client.MapleCharacter;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;
import tools.Randomizer;

/*
JourneyMS
I will make an abstractspawnpoint for this, if I need this file at all, only real difference is the randomized spawn point
*/

public class SpawnPointBoss {

    private final int monster, mobTime, fh, f;
    private final Point[] pos;
    private long nextPossibleSpawn;
    private final AtomicInteger spawnedMonsters = new AtomicInteger(0);
    private boolean spawned = false;

    public SpawnPointBoss(final MapleMonster monster, Point[] pos, int mobTime) {
        this.monster = monster.getId();
        this.pos = pos;
        this.mobTime = mobTime;
        this.fh = monster.getFh();
        this.f = monster.getF();
        this.nextPossibleSpawn = System.currentTimeMillis();
    }

    public boolean shouldSpawn() {
        if (mobTime < 0 || spawnedMonsters.get() > 0) {
            return false;
        }
        return nextPossibleSpawn <= System.currentTimeMillis();
    }

    public MapleMonster getMonster() {
        MapleMonster mob = new MapleMonster(MapleLifeFactory.getMonster(monster));
        mob.setPosition(pos[Randomizer.nextInt(pos.length)]);
        mob.setTeam(-1);
        mob.setFh(fh);
        mob.setF(f);
        spawnedMonsters.incrementAndGet();
        spawned = true;
        mob.addListener(new MonsterListener() {
            @Override
            public void monsterKilled(MapleMonster monster, MapleCharacter highestDamageChar) {
                spawned = false;
                nextPossibleSpawn = System.currentTimeMillis();
                if (mobTime > 0) {
                    nextPossibleSpawn += mobTime * 1000;
                } else {
                    nextPossibleSpawn += monster.getAnimationTime("die1");
                }
                spawnedMonsters.decrementAndGet();
            }
        });
        if (mobTime == 0) {
            nextPossibleSpawn = System.currentTimeMillis();
        }
        return mob;
    }

    public final int getF() {
        return f;
    }

    public final int getFh() {
        return fh;
    }
    
    public boolean isSpawned() {
        return spawned;
    }
}

