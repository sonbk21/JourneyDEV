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
import client.properties.MapleSkinColor;
import client.properties.MapleStat;
import client.properties.Skill;
import client.properties.SkillFactory;
import java.util.EnumMap;

/**
 * JourneyMS
 * 
 */
public class PlayerStats {

    private final EnumMap<MapleStat, Short> stats = new EnumMap<>(MapleStat.class);
    private final EnumMap<EquipStat, Short> localstats = new EnumMap<>(EquipStat.class);
    private float bossdamage = 1;
    public short level, rebirths = 0;
    public int hair;
    public short face;
    public MapleSkinColor skincolor = MapleSkinColor.NORMAL;
    public MapleJob job = MapleJob.BEGINNER;
    public MapleGender gender;
    public String name;
    
    public enum MapleGender {
        MALE, FEMALE;
    }
    
    public PlayerStats(short str, short dex, short lnt, short luk, short hp, short maxhp, short mp, short maxmp) {
        stats.put(MapleStat.STR, str);
        stats.put(MapleStat.DEX, dex);
        stats.put(MapleStat.INT, lnt);
        stats.put(MapleStat.LUK, luk);
        stats.put(MapleStat.HP, hp);
        stats.put(MapleStat.MAXHP, maxhp);
        stats.put(MapleStat.MP, mp);
        stats.put(MapleStat.MAXMP, maxmp);
    }
    
    public short getStat(MapleStat type) {
        return stats.containsKey(type)?stats.get(type):0;
    }
    
    public short addToStat(MapleStat type, short amount) {
        if (stats.containsKey(type))
            stats.put(type, (short) (stats.get(type) + amount));
        else
            stats.put(type, amount);
        return stats.get(type);
    }
    
    public short decreaseStat(MapleStat type, short amount) {
        if (stats.containsKey(type)) {
            stats.put(type, (short) (stats.get(type) - amount));
            return stats.get(type);
        } else {
            return 0;
        }
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
        
        for (EnumMap.Entry<MapleStat, Short> entry : stats.entrySet()) {
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
}
