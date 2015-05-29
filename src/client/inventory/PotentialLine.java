/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.inventory;

import client.inventory.Equip.EquipStat;
import tools.Randomizer;

/**
 * JourneyMS
 * 
 */
public class PotentialLine {

    private final EquipStat effect;
    private final PotentialLevel superior;
    
    public enum PotentialLevel {
        ZERO, ONE, TWO, THREE
    }
    
    public PotentialLine(EquipStat effect, PotentialLevel superior) {
        this.effect = effect;
        this.superior = superior;
    }
    
    public PotentialLine(int line) {
        this.effect = EquipStat.values()[line%100];
        this.superior = PotentialLevel.values()[line/100 - (line/1000)*10];
    }
    
    public PotentialLine(EquipStat effect) {
        this.effect = effect;
        this.superior = PotentialLevel.values()[Randomizer.nextInt(PotentialLevel.values().length)];
    }
    
    public int getValue(int rank) {
        if (effect == EquipStat.WATK || effect == EquipStat.MAGIC)
            return 2*rank - superior.ordinal()/2;
        else if (effect == EquipStat.HANDS)
            return rank;
        else
            return 4*rank - superior.ordinal();
    }
    
    public EquipStat getEffect() {
        return effect;
    }
    
    public PotentialLevel getSuperior() {
        return superior;
    }
}
