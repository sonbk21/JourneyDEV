/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

/**
 * JourneyMS
 * 
 */
public class MapleRewardEntry {

    public final int id;
    public final short min, max;
    public final byte rarity;
    
    public MapleRewardEntry(int id, short min, short max, byte rarity) {
        this.id = id;
        this.min = min;
        this.max = max;
        this.rarity = rarity;
    }
    
}
