/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import client.properties.MapleStat;
import client.properties.PotentialLine;
import client.properties.PotentialLine.PotentialStat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import tools.Randomizer;

/**
 * JourneyMS
 * 
 */
public class Potentials {
    
    private final LinkedList<PotentialLine> lines = new LinkedList<>();
    private final HashMap<PotentialStat, Integer> tempstats = new HashMap<>();
    private Rank rank;
    private boolean addedStats = false;
    
    public enum Rank {
        NULL, BASIC, INTERMEDIATE, ADVANCED, LEGENDARY, GODLY;
    }
    
    public Potentials(int line1, int line2, int line3) {
        if (line1 > 0) {
            lines.add(new PotentialLine(line1));
            if (line2 > 0) {
                lines.add(new PotentialLine(line2));
                if (line3 > 0)
                    lines.add(new PotentialLine(line3));
            }
        }
        this.rank = Rank.values()[line1/1000];
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
        for (Map.Entry<PotentialStat, Integer> entry : tempstats.entrySet()) {
            chr.addPotentialStat(entry.getKey(), -entry.getValue());
        }
        addedStats = true;
    }
    
    private void addStats(MapleCharacter chr) {
        if (!addedStats)
            return;
        for (Map.Entry<PotentialStat, Integer> entry : tempstats.entrySet()) {
            chr.addPotentialStat(entry.getKey(), entry.getValue());
        }
        addedStats = false;
    }
    
    public void resetStats(MapleCharacter chr, boolean add) {
        clearStats(chr);
        tempstats.clear();
        if (add) {
            for (PotentialLine line : lines) {
                float value = (float) line.getValue(rank.ordinal());
                PotentialStat effect = line.getEffect();
                int tmp = 0;
                switch (effect) {
                    case HP: tmp += (value / 100) * chr.getStat(MapleStat.MAXHP); break;
                    case MP: tmp += (value / 100) * chr.getStat(MapleStat.MAXMP); break;
                    case BDM: tmp += (int) value; break;
                    default: tmp += (value / 100) * chr.getStat(MapleStat.values()[effect.ordinal() + 5]);
                }
                if (tempstats.containsKey(effect))
                    tempstats.put(effect, tmp + tempstats.get(effect));
                else
                    tempstats.put(effect, tmp);
            }
            addStats(chr);
        }
    }
    
    public void resetLines() {
        rank = Rank.values()[(lines.isEmpty())?1:(rank.equals(Rank.GODLY))?5:(rank.ordinal())+Randomizer.nextInt(51)/50];
        for (int i = 0; i < lines.size(); i++) {
            lines.add(new PotentialLine());
            lines.removeFirst();
        }
        if ((lines.size() < 3 && Randomizer.nextInt(51) == 0) || lines.isEmpty()) //Lines increase or first potential
            lines.add(new PotentialLine());
    }
    
    public int getTempStat(PotentialStat effect) {
        return tempstats.containsKey(effect)?tempstats.get(effect):0;
    }
    
    public String getName() {
        return String.valueOf(rank).charAt(0) + String.valueOf(rank).toLowerCase().substring(1);
    }
    
    public int getRank() {
        return rank.ordinal();
    }
    
    public PotentialLine getLine(int line) {
        return lines.get(line);
    }
}
