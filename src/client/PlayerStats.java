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

import client.inventory.Equip;
import client.inventory.Equip.EquipStat;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.properties.MapleBuffStat;
import client.properties.MapleJob;
import client.properties.MapleStat;
import client.properties.Skill;
import client.properties.SkillFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.EnumMap;
import tools.Randomizer;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class PlayerStats {

    private final EnumMap<MapleStat, Short> stats = new EnumMap<>(MapleStat.class);
    private final EnumMap<EquipStat, Short> localstats = new EnumMap<>(EquipStat.class);
    private float bossdamage = 1;
    
    public PlayerStats(short level, short str, short dex, short lnt, short luk, short maxhp, short maxmp, short ap, short sp, short job) {
        stats.put(MapleStat.LEVEL, level);
        stats.put(MapleStat.STR, str);
        stats.put(MapleStat.DEX, dex);
        stats.put(MapleStat.INT, lnt);
        stats.put(MapleStat.LUK, luk);
        stats.put(MapleStat.HP, maxhp);
        stats.put(MapleStat.MAXHP, maxhp);
        stats.put(MapleStat.MP, maxmp);
        stats.put(MapleStat.MAXMP, maxmp);
        stats.put(MapleStat.AVAILABLEAP, ap);
        stats.put(MapleStat.AVAILABLESP, sp);
        stats.put(MapleStat.JOB, job);
    }
    
    public MapleJob getJob() {
        return MapleJob.getById(stats.get(MapleStat.JOB));
    }
    
    public void setJob(MapleJob job) {
        stats.put(MapleStat.JOB, (short) job.getId());
    }
    
    public short getStat(MapleStat type) {
        return stats.containsKey(type)?stats.get(type):0;
    }
    
    public short addToStat(MapleStat type, int amount) {
        if (stats.containsKey(type))
            stats.put(type, (short) (stats.get(type) + amount));
        else
            stats.put(type, (short) amount);
        return stats.get(type);
    }
    
    public void setStat(MapleStat type, short amount) {
        stats.put(type, amount);
    }
    
    public float getBDM() {
        return bossdamage;
    }
    
    public void setBDM(float bdm) {
        bossdamage = bdm;
    }
    
    public void recalcLocalStats(MapleCharacter chr) {
        int oldmaxhp = getLocal(EquipStat.HP);
        
        localstats.put(EquipStat.SPEED, (short) 100);
        localstats.put(EquipStat.JUMP, (short) 100);
        localstats.put(EquipStat.STR, getStat(MapleStat.STR));
        localstats.put(EquipStat.DEX, getStat(MapleStat.DEX));
        localstats.put(EquipStat.INT, getStat(MapleStat.INT));
        localstats.put(EquipStat.LUK, getStat(MapleStat.LUK));
        localstats.put(EquipStat.HP, getStat(MapleStat.MAXHP));
        localstats.put(EquipStat.MP, getStat(MapleStat.MAXMP));
        
        for (Item item : chr.getInventory(MapleInventoryType.EQUIPPED)) {
            Equip equip = (Equip) item;
            for (EnumMap.Entry<EquipStat, Short> entry : equip.getAllStats().entrySet()) {
                switch (entry.getKey()) {
                    case WDEF:
                    case MDEF:
                    case ACC:
                    case AVOID:
                        continue;
                    default:
                        addLocalStat(entry.getKey(), entry.getValue());
                }
            }
        }
        localstats.put(EquipStat.MAGIC, localstats.get(EquipStat.INT));
        
        Short hbhp = chr.getBuffedValue(MapleBuffStat.HYPERBODYHP);
        if (hbhp != null) {
            addLocalStat(EquipStat.HP, (short) ((hbhp.doubleValue() / 100) * getLocal(EquipStat.HP)));
        }
        Short hbmp = chr.getBuffedValue(MapleBuffStat.HYPERBODYMP);
        if (hbmp != null) {
            addLocalStat(EquipStat.MP, (short) ((hbmp.doubleValue() / 100) * getLocal(EquipStat.MP)));
        }
        Short watkbuff = chr.getBuffedValue(MapleBuffStat.WATK);
        if (watkbuff != null) {
            addLocalStat(EquipStat.WATK, watkbuff);
        }
        
        MapleJob job = MapleJob.getById(getStat(MapleStat.JOB));
        if (job == MapleJob.BOWMAN) {
            Skill expert = null;
            if (job == MapleJob.MARKSMAN) {
                expert = SkillFactory.getSkill(3220004);
            } else if (job == MapleJob.BOWMASTER) {
                expert = SkillFactory.getSkill(3120005);
            }
            if (expert != null) {
                int boostLevel = chr.getSkillLevel(expert);
                if (boostLevel > 0) {
                    addLocalStat(EquipStat.WATK, (short) expert.getEffect(boostLevel).getX());
                }
            }
        }
        Short matkbuff = chr.getBuffedValue(MapleBuffStat.MATK);
        if (matkbuff != null) {
            addLocalStat(EquipStat.MAGIC,  matkbuff);
        }
        Short speedbuff = chr.getBuffedValue(MapleBuffStat.SPEED);
        if (speedbuff != null) {
            addLocalStat(EquipStat.SPEED,  speedbuff);
        }
        Short jumpbuff = chr.getBuffedValue(MapleBuffStat.JUMP);
        if (jumpbuff != null) {
            addLocalStat(EquipStat.JUMP,  jumpbuff);
        }
        
        if (oldmaxhp != 0 && oldmaxhp != getLocal(EquipStat.HP)) {
            chr.updatePartyMemberHP();
        }
    }
    
    private void addLocalStat(EquipStat type, short up) {
        if (localstats.containsKey(type)) {
            short newval = (short) (getLocal(type) + up);
            switch (type) {
                case MAGIC:
                    if (newval > 2000)
                        newval = 2000; 
                    break;
                case SPEED:
                    if (newval > 140)
                        newval = 140; 
                    break;
                case JUMP:
                    if (newval > 123)
                        newval = 123; 
                    break;
                case MP:
                case HP: 
                    if (newval > 30000)
                        newval = 30000; 
                    break;
            }
            localstats.put(type, newval);
        } else {
            localstats.put(type, up);
        }
    }
    
    public short getLocal(EquipStat type) {
        return (localstats.containsKey(type))?localstats.get(type):0;
    }
    
    public void changeJob(MapleJob newJob) {
        stats.put(MapleStat.JOB, (short) newJob.getId());
        
        addToStat(MapleStat.AVAILABLESP, (short) 1);
        
        if (newJob.getId() % 10 == 2)
            addToStat(MapleStat.AVAILABLESP, (short) 2);
        
        if (newJob.getId() % 10 > 1)
            addToStat(MapleStat.AVAILABLEAP, (short) 5);
        
        switch (newJob.getBaseJob()) { //not gms-like, who the fk cares
            case WARRIOR: 
            case ARAN1:
                addToStat(MapleStat.MAXHP, (short) Randomizer.rand(200, 250));
                addToStat(MapleStat.MAXMP, (short) Randomizer.rand(20, 25));
                break;
            case MAGICIAN:
                addToStat(MapleStat.MAXHP, (short) Randomizer.rand(20, 25));
                addToStat(MapleStat.MAXMP, (short) Randomizer.rand(200, 250));
            case BOWMAN:
                addToStat(MapleStat.MAXHP, (short) Randomizer.rand(50, 100));
                addToStat(MapleStat.MAXMP, (short) Randomizer.rand(50, 100));
                break;
            case THIEF:
                addToStat(MapleStat.MAXHP, (short) Randomizer.rand(50, 100));
                addToStat(MapleStat.MAXMP, (short) Randomizer.rand(50, 100));
                break;
            case PIRATE:
                addToStat(MapleStat.MAXHP, (short) Randomizer.rand(50, 100));
                addToStat(MapleStat.MAXMP, (short) Randomizer.rand(50, 100));
                break;
        }
    }
    
    public void levelUp(int impMaxHp, int impMaxMp) {
        addToStat(MapleStat.LEVEL, (short) 1);
        
        MapleJob job = MapleJob.getById(getStat(MapleStat.JOB));
        if (job == MapleJob.BEGINNER || job == MapleJob.LEGEND) {
            stats.put(MapleStat.AVAILABLEAP, (short) 0);
            if (stats.get(MapleStat.LEVEL) < 6) {
                addToStat(MapleStat.STR, (short) 5);
            } else {
                addToStat(MapleStat.STR, (short) 4);
                addToStat(MapleStat.DEX, (short) 1);
            }
        } else {
            addToStat(MapleStat.AVAILABLEAP, (short) 5);
        }
        
        switch (job.getBaseJob()) {
            case BEGINNER:
            case LEGEND:
                addToStat(MapleStat.MAXHP, (short) Randomizer.rand(12, 16));
                addToStat(MapleStat.MAXMP, (short) Randomizer.rand(10, 12));
                break;
            case WARRIOR:
            case ARAN1:
                addToStat(MapleStat.MAXHP, (short) Randomizer.rand(24 + (impMaxHp * 4), 28 + (impMaxHp * 4)));
                addToStat(MapleStat.MAXMP, (short) Randomizer.rand(4, 6));
                break;
            case MAGICIAN:
                addToStat(MapleStat.MAXHP, (short) Randomizer.rand(10, 14));
                addToStat(MapleStat.MAXMP, (short) Randomizer.rand(22 + (impMaxMp * 2), 24 + (impMaxMp * 2)));
                break;
            case THIEF:
            case BOWMAN:
                addToStat(MapleStat.MAXHP, (short) Randomizer.rand(20, 24));
                addToStat(MapleStat.MAXMP, (short) Randomizer.rand(14, 16));
                break;
            case PIRATE:
                addToStat(MapleStat.MAXHP, (short) Randomizer.rand(22 + (impMaxHp * 3), 28 + (impMaxHp * 3)));
                addToStat(MapleStat.MAXMP, (short) Randomizer.rand(18, 23));
                break;
        }
        addToStat(MapleStat.MAXHP, (short) (getLocal(EquipStat.STR) / 10)); //...shhh
        addToStat(MapleStat.MAXMP, (short) (getLocal(EquipStat.INT) / 10));
        
        if (job.getId() % 1000 > 0) {
            addToStat(MapleStat.AVAILABLESP, (short) 3);
        }
    }
    
    public PreparedStatement getStatement(Connection con, int charid) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE characters SET level = ?, str = ?, dex = ?, luk = ?, `int` = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, job = ? WHERE id = ?");
        ps.setShort(1, getStat(MapleStat.LEVEL));
        ps.setShort(2, getStat(MapleStat.STR));
        ps.setShort(3, getStat(MapleStat.DEX));
        ps.setShort(4, getStat(MapleStat.LUK));
        ps.setShort(5, getStat(MapleStat.INT));
        ps.setShort(6, getStat(MapleStat.MAXHP));
        ps.setShort(7, getStat(MapleStat.MAXMP));
        ps.setShort(8, getStat(MapleStat.AVAILABLESP));
        ps.setShort(9, getStat(MapleStat.AVAILABLEAP));
        ps.setShort(10, getStat(MapleStat.JOB));
        ps.setInt(11, charid);
        return ps;
    }
}
