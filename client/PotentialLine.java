/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.properties;

import tools.Randomizer;

/**
 * JourneyMS
 * 
 */
public class PotentialLine {

    private final PotentialStat effect;
    private final int superior;
    
    public enum PotentialStat {
        STR, DEX, INT, LUK, HP, MP, BDM;
    }
    
    public PotentialLine(PotentialStat effect, int superior) {
        this.effect = effect;
        this.superior = superior;
    }
    
    public PotentialLine(int line) {
        this.effect = PotentialStat.values()[line%100];
        this.superior = line/100 - (line/1000)*10;
    }
    
    public PotentialLine() {
        this.effect = PotentialStat.values()[Randomizer.nextInt(PotentialStat.values().length)];
        this.superior = Randomizer.nextInt(3);
    }
    
    public int getValue(int rank) {
        return (effect.equals(PotentialStat.BDM)?2:1)*(3*rank - superior);
    }
    
    public String getName(int rank) {
        return String.valueOf(effect).replaceFirst("BDM", "Boss Damage").concat(" ").concat(String.valueOf(getValue(rank))).concat("%");
    }
    
    public PotentialStat getEffect() {
        return effect;
    }
    
    public int getSuperior() {
        return superior;
    }
}
