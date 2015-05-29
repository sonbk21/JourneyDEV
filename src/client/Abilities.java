/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import client.properties.MapleStat;
import client.properties.AbilityLine;
import client.properties.AbilityLine.AbilityStat;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import tools.Randomizer;

/**
 * JourneyMS
 * 
 */
public class Abilities {
    
    private final LinkedList<AbilityLine> lines = new LinkedList<>();
    private final EnumMap<AbilityStat, Short> tempstats = new EnumMap<>(AbilityStat.class);
    private Rank rank;
    private boolean addedStats = false;
    
    public enum Rank {
        NULL, BASIC, INTERMEDIATE, ADVANCED, LEGENDARY, GODLY;
        
        public byte getValue() {
            return (byte) this.ordinal();
        }
    }
    
    public Abilities(byte rank, short line1, short line2, short line3) {
        if (line1 > 0) {
            lines.add(new AbilityLine(line1));
            if (line2 > 0) {
                lines.add(new AbilityLine(line2));
                if (line3 > 0)
                    lines.add(new AbilityLine(line3));
            }
        }
        this.rank = Rank.values()[rank];
    }
    
    public int getLines() {
        return lines.size();
    }
    
    public boolean getAdded() {
        return addedStats;
    }
    
    private void clearStats(MapleCharacter chr) {
        if (addedStats)
            return;
        for (Map.Entry<AbilityStat, Short> entry : tempstats.entrySet()) {
            chr.addPotentialStat(entry.getKey(), (short) -entry.getValue());
        }
        addedStats = true;
    }
    
    private void addStats(MapleCharacter chr) {
        if (!addedStats)
            return;
        for (Map.Entry<AbilityStat, Short> entry : tempstats.entrySet()) {
            chr.addPotentialStat(entry.getKey(), entry.getValue());
        }
        addedStats = false;
    }
    
    public void resetStats(MapleCharacter chr, boolean add) {
        clearStats(chr);
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
            addStats(chr);
        }
    }
    
    public void resetLines() {
        rank = Rank.values()[(lines.isEmpty())?1:(rank.equals(Rank.GODLY))?5:(rank.getValue())+Randomizer.nextInt(51)/50];
        for (int i = 0; i < lines.size(); i++) {
            lines.add(new AbilityLine());
            lines.removeFirst();
        }
        if ((lines.size() < 3 && Randomizer.nextInt(51) == 0) || lines.isEmpty()) //Lines increase or first potential
            lines.add(new AbilityLine());
    }
    
    public short getTempStat(AbilityStat effect) {
        return tempstats.containsKey(effect)?tempstats.get(effect):0;
    }
    
    public String getName() {
        return String.valueOf(rank).charAt(0) + String.valueOf(rank).toLowerCase().substring(1);
    }
    
    public byte getRank() {
        return rank.getValue();
    }
    
    public AbilityLine getLine(byte line) {
        return lines.get(line);
    }
}
