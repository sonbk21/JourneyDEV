/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import client.properties.MapleJob;
import client.properties.MapleStat;
import java.util.HashMap;
import java.util.Map;

/**
 * JourneyMS
 * 
 */
public class PlayerStats {

    private final Map<MapleStat, Integer> stats = new HashMap<>();
    private float bossdamage = 1;
    private MapleJob job;
    
    public PlayerStats(final int[] defaults, MapleJob job) {
        for (int i = 0; i < defaults.length; i++)
            stats.put(MapleStat.values()[i+3], defaults[i]);
        this.job = job;
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
    
    public MapleJob getJob() {
        return job;
    }
    
    public void setJob(MapleJob job) {
        this.job = job;
    }
}
