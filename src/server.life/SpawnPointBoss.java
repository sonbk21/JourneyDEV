package server.life;

import client.MapleCharacter;
import java.awt.Point;
import tools.Randomizer;

/*
* JourneyMS
*
*/

public class SpawnPointBoss extends AbstractSpawnPoint {

    private final Point[] positions;
    private boolean spawned = false;

    public SpawnPointBoss(final MapleMonster monster, Point[] pos, int mobTime) {
        super(monster, null, false, mobTime, 0, -1);
        this.positions = pos;
    }

    @Override
    public boolean shouldSpawn() {
        if (mobTime < 0 || spawnedMonsters.get() > 0) {
            return false;
        }
        return nextPossibleSpawn <= System.currentTimeMillis();
    }

    @Override
    public MapleMonster getMonster() {
        MapleMonster mob = new MapleMonster(MapleLifeFactory.getMonster(monster));
        mob.setPosition( new Point(positions[Randomizer.nextInt(positions.length)]));
        mob.setTeam(-1);
        mob.setFh(fh);
        mob.setF(f);
        spawnedMonsters.incrementAndGet();
        spawned = true;
        mob.addListener((MapleMonster monster1, MapleCharacter highestDamageChar) -> {
            spawned = false;
            nextPossibleSpawn = System.currentTimeMillis();
            if (mobTime > 0) {
                nextPossibleSpawn += mobTime * 1000;
            } else {
                nextPossibleSpawn += monster1.getAnimationTime("die1");
            }
            spawnedMonsters.decrementAndGet();
        });
        if (mobTime == 0) {
            nextPossibleSpawn = System.currentTimeMillis();
        }
        return mob;
    }

    @Override
    public final int getF() {
        return f;
    }

    @Override
    public final int getFh() {
        return fh;
    }
    
    public boolean isSpawned() {
        return spawned;
    }
}

