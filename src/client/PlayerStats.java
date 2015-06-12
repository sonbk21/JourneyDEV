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
import client.properties.AbilityLine;
import client.properties.AbilityLine.AbilityStat;
import client.properties.MapleBuffStat;
import client.properties.MapleJob;
import client.properties.MapleSkinColor;
import client.properties.MapleStat;
import client.properties.Skill;
import client.properties.SkillFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import tools.Randomizer;

/**
 * JourneyMS
 * 
 */
public class PlayerStats {

    private final EnumMap<MapleStat, Short> stats = new EnumMap<>(MapleStat.class);
    private final EnumMap<EquipStat, Short> localstats = new EnumMap<>(EquipStat.class);
    
    private final LinkedList<AbilityLine> lines = new LinkedList<>();
    private final EnumMap<AbilityLine.AbilityStat, Short> tempstats = new EnumMap<>(AbilityLine.AbilityStat.class);
    private Rank rank;
    private boolean addedStats = false;
    private boolean updateStats = false;
    
    private float bossdamage = 1;
    public short level;
    public int exp;
    public int meso;
    
    public short face;
    public int hair;
    public MapleSkinColor skincolor = MapleSkinColor.NORMAL;
    public MapleJob job = MapleJob.BEGINNER;
    public MapleGender gender;
    public String name;
    
    public enum MapleGender {
        MALE, FEMALE;
    }
    
    public enum Rank {
        NULL, BASIC, INTERMEDIATE, ADVANCED, LEGENDARY, GODLY;
        
        public byte getValue() {
            return (byte) this.ordinal();
        }
    }
    
    public PlayerStats(short str, short dex, short lnt, short luk, short maxhp, short maxmp, short ap, short sp) {
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
    }
    
    public short getStat(MapleStat type) {
        return stats.containsKey(type)?stats.get(type):0;
    }
    
    public short addToStat(MapleStat type, int amount) {
        updateStats = true;
        
        if (stats.containsKey(type))
            stats.put(type, (short) (stats.get(type) + amount));
        else
            stats.put(type, (short) amount);
        return stats.get(type);
    }
    
    public short decreaseStat(MapleStat type, short amount) {
        updateStats = true;
        
        if (stats.containsKey(type)) {
            stats.put(type, (short) (stats.get(type) - amount));
            return stats.get(type);
        } else {
            return 0;
        }
    }
    
    public void setStat(MapleStat type, short amount) {
        stats.put(type, amount);
        updateStats = true;
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
    
    public void changeJob(MapleJob newJob) {
        job = newJob;
        
        addToStat(MapleStat.AVAILABLESP, (short) 1);
        
        if (job.getId() % 10 == 2)
            addToStat(MapleStat.AVAILABLESP, (short) 2);
        
        if (job.getId() % 10 > 1)
            addToStat(MapleStat.AVAILABLEAP, (short) 5);
        
        switch (job.getBaseJob()) { //not gms-like, who the fk cares
            case WARRIOR: 
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
        updateStats = true;
    }
    
    public int getAbilityLines() {
        return lines.size();
    }
    
    public void addLine(short line) {
        lines.add( new AbilityLine(line));
    }
    
    public void setRank(short rank) {
        this.rank = Rank.values()[rank];
    }
    
    private void clearTempStats() {
        if (addedStats)
            return;
        
        MapleStat newtype;
        for (Map.Entry<AbilityStat, Short> entry : tempstats.entrySet()) {
            switch (entry.getKey()) {
                case BDM: bossdamage = (bossdamage - ((float) entry.getValue())/100); return;
                case HP: newtype = MapleStat.MAXHP; break;
                case MP: newtype = MapleStat.MAXMP; break;
                default: newtype = MapleStat.values()[entry.getKey().ordinal() + 5];
            }
            addToStat(newtype, -entry.getValue());
        }
        addedStats = true;
    }
    
    private void addTempStats() {
        if (!addedStats)
            return;
        
        MapleStat newtype;
        for (Map.Entry<AbilityLine.AbilityStat, Short> entry : tempstats.entrySet()) {
            switch (entry.getKey()) {
                case BDM: bossdamage = (bossdamage + ((float) entry.getValue())/100); return;
                case HP: newtype = MapleStat.MAXHP; break;
                case MP: newtype = MapleStat.MAXMP; break;
                default: newtype = MapleStat.values()[entry.getKey().ordinal() + 5];
            }
            addToStat(newtype, entry.getValue());
        }
        addedStats = false;
    }
    
    public void resetStats(MapleCharacter chr, boolean add) {
        clearTempStats();
        tempstats.clear();
        if (add) {
            for (AbilityLine line : lines) {
                float value = (float) line.getValue(rank.getValue());
                AbilityStat effect = line.getEffect();
                short tmp = 0;
                switch (effect) {
                    case HP: tmp += (value / 100) * chr.getStat(MapleStat.MAXHP); break;
                    case MP: tmp += (value / 100) * chr.getStat(MapleStat.MAXMP); break;
                    case BDM: tmp += (int) value; break;
                    default: tmp += (value / 100) * chr.getStat(MapleStat.values()[effect.ordinal() + 5]);
                }
                if (tempstats.containsKey(effect))
                    tempstats.put(effect, (short) (tmp + tempstats.get(effect)));
                else
                    tempstats.put(effect, tmp);
            }
            addTempStats();
        }
        updateStats = true;
    }
    
    public void resetAbility() {
        rank = Rank.values()[(lines.isEmpty() || rank == Rank.NULL)?1:(rank.equals(Rank.GODLY))?5:(rank.getValue())+Randomizer.nextInt(51)/50];
        for (int i = 0; i < lines.size(); i++) {
            lines.add(new AbilityLine());
            lines.removeFirst();
        }
        if ((lines.size() < 3 && Randomizer.nextInt(51) == 0) || lines.isEmpty()) //Lines increase or first potential
            lines.add(new AbilityLine());
    }
    
    public short getTempStat(AbilityLine.AbilityStat effect) {
        return tempstats.containsKey(effect)?tempstats.get(effect):0;
    }
    
    public String getAbilityName() {
        return String.valueOf(rank).charAt(0) + String.valueOf(rank).toLowerCase().substring(1);
    }
    
    public byte getAbilityRank() {
        return rank.getValue();
    }
    
    public AbilityLine getAbilityLine(byte line) {
        return lines.get(line);
    }
    
    public boolean updateStats() {
        return updateStats;
    }
    
    public void disableUpdate() {
        updateStats = false;
    }
    
    public PreparedStatement getStatement(Connection con, int charid) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE characters SET level = ?, str = ?, dex = ?, luk = ?, `int` = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, skincolor = ?, job = ?, hair = ?, face = ?, ability0 = ?, ability1 = ?, ability2 = ? WHERE id = ?");
        ps.setShort(1, level);
        ps.setShort(2, getStat(MapleStat.STR));
        ps.setShort(3, getStat(MapleStat.DEX));
        ps.setShort(4, getStat(MapleStat.LUK));
        ps.setShort(5, getStat(MapleStat.INT));
        ps.setShort(6, getStat(MapleStat.MAXHP));
        ps.setShort(7, getStat(MapleStat.MAXMP));
        ps.setShort(8, getStat(MapleStat.AVAILABLESP));
        ps.setShort(9, getStat(MapleStat.AVAILABLEAP));
        ps.setByte(10, (byte) skincolor.getId());
        ps.setShort(11, (short) job.getId());
        ps.setInt(12, hair);
        ps.setShort(13, face);
        ps.setShort(14, (short) (rank.getValue()*1000 + lines.get(0).getSuperior().ordinal()*100 + lines.get(0).getEffect().ordinal()));
        ps.setShort(15, (short) (lines.get(1).getSuperior().ordinal()*100 + lines.get(1).getEffect().ordinal()));
        ps.setShort(16, (short) (lines.get(2).getSuperior().ordinal()*100 + lines.get(2).getEffect().ordinal()));
        ps.setInt(17, charid);
        return ps;
    }
}
