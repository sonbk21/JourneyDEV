/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import java.util.EnumMap;

/**
 * JourneyMS
 * 
 */
public class PlayerStats {

    private final EnumMap<MapleStat, Integer> stats = new EnumMap<>(MapleStat.class);
    private final EnumMap<EquipStat, Integer> localstats = new EnumMap<>(EquipStat.class);
    private float bossdamage = 1;
    
    public enum LocalStat {
        STR, DEX, INT, LUK, MHP, MMP, WATK, MAGIC, SPEED, JUMP;
    }
    
    public PlayerStats(final int[] defaults) {
        for (int i = 0; i < defaults.length; i++)
            stats.put(MapleStat.values()[i], defaults[i]);
    }
    
    public PlayerStats(final int[] defaults, int offset) {
        for (int i = 0; i < defaults.length; i++)
            stats.put(MapleStat.values()[i + offset], defaults[i]);
    }
    
    public int getStat(MapleStat type) {
        return stats.containsKey(type)?stats.get(type):0;
    }
    
    public int addToStat(MapleStat type, int amount) {
        if (stats.containsKey(type))
            stats.put(type, stats.get(type) + amount);
        else
            stats.put(type, amount);
        return stats.get(type);
    }
    
    public int decreaseStat(MapleStat type, int amount) {
        if (stats.containsKey(type))
            stats.put(type, stats.get(type) - amount);
        return stats.get(type);
    }
    
    public void setStat(MapleStat type, int amount) {
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
        localstats.put(EquipStat.SPEED, 100);
        localstats.put(EquipStat.JUMP, 100);
        
        for (EnumMap.Entry<MapleStat, Integer> entry : stats.entrySet()) {
            switch (entry.getKey()) {
                case STR:
                case DEX:
                case LUK:
                case INT:
                case MAXHP:
                case MAXMP:
                    localstats.put(entry.getKey().convertToEquipStat(), entry.getValue());
                    break;
            }
        }
        localstats.put(EquipStat.MAGIC, localstats.get(EquipStat.INT));
        
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
        
        Integer hbhp = chr.getBuffedValue(MapleBuffStat.HYPERBODYHP);
        if (hbhp != null) {
            addLocalStat(EquipStat.HP, (int) ((hbhp.doubleValue() / 100) * getLocal(EquipStat.HP)));
        }
        Integer hbmp = chr.getBuffedValue(MapleBuffStat.HYPERBODYMP);
        if (hbmp != null) {
            addLocalStat(EquipStat.MP, (int) ((hbmp.doubleValue() / 100) * getLocal(EquipStat.MP)));
        }
        Integer watkbuff = chr.getBuffedValue(MapleBuffStat.WATK);
        if (watkbuff != null) {
            addLocalStat(EquipStat.WATK, watkbuff);
        }
        if (MapleJob.getById(stats.get(MapleStat.JOB)) == MapleJob.BOWMAN) {
            Skill expert = null;
            if (MapleJob.getById(stats.get(MapleStat.JOB)) == MapleJob.MARKSMAN) {
                expert = SkillFactory.getSkill(3220004);
            } else if (MapleJob.getById(stats.get(MapleStat.JOB)) == MapleJob.BOWMASTER) {
                expert = SkillFactory.getSkill(3120005);
            }
            if (expert != null) {
                int boostLevel = chr.getSkillLevel(expert);
                if (boostLevel > 0) {
                    addLocalStat(EquipStat.WATK,  expert.getEffect(boostLevel).getX());
                }
            }
        }
        Integer matkbuff = chr.getBuffedValue(MapleBuffStat.MATK);
        if (matkbuff != null) {
            addLocalStat(EquipStat.MAGIC,  matkbuff);
        }
        Integer speedbuff = chr.getBuffedValue(MapleBuffStat.SPEED);
        if (speedbuff != null) {
            addLocalStat(EquipStat.SPEED,  speedbuff);
        }
        Integer jumpbuff = chr.getBuffedValue(MapleBuffStat.JUMP);
        if (jumpbuff != null) {
            addLocalStat(EquipStat.JUMP,  jumpbuff);
        }
        
        if (oldmaxhp != 0 && oldmaxhp != getLocal(EquipStat.HP)) {
            chr.updatePartyMemberHP();
        }
    }
    
    private void addLocalStat(EquipStat type, int up) {
        if (localstats.containsKey(type)) {
            int cur = getLocal(type);
            switch (type) {
                case MAGIC:
                    up = Math.min(2000 - cur, up); 
                    break;
                case SPEED:
                    up = Math.min(140 - cur, up); 
                    break;
                case JUMP:
                    up = Math.min(123 - cur, up); 
                    break;
                case MP:
                case HP: 
                    up = Math.min(30000 - cur, up); 
                    break;
            }
            localstats.put(type, cur + up);
        } else {
            localstats.put(type, up);
        }
    }
    
    public int getLocal(EquipStat type) {
        return (localstats.containsKey(type))?localstats.get(type):0;
    }
}
