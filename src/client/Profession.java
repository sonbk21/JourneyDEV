/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.DatabaseConnection;

/**
 * JourneyMS
 * 
 */
public class Professions {
    
    private MapleProfession primary;
    private MapleProfession secondary;
    
    public Professions(MapleProfession primary, MapleProfession secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }
    
    public Professions() {
        primary = null;
        secondary = null;
    }
    
    public enum ProfessionType {
        NONE, MINING, HERBALISM, SMITHING, CRAFTING, ALCHEMY;
    }
    
    public enum ProfessionLevel {
        NEWBIE((short) 0, (byte) 10), APPRENTICE((short) 500, (byte) 30), ASSISTANT((short) 1200, (byte) 50), FOREMAN((short) 2500, (byte) 70), 
        EXPERT((short) 6000, (byte) 100), MASTER((short) 18000, (byte) 120);
        private final short threshold;
        private final byte levelreq;
        
        ProfessionLevel(short threshold, byte levelreq) {
            this.threshold = threshold;
            this.levelreq = levelreq;
        }
        
        public short getThreshold() {
            return threshold;
        }
        
        public byte getLevelReq() {
            return levelreq;
        }
        
        public ProfessionLevel getNext() {
            return (this == MASTER)? MASTER : ProfessionLevel.values()[this.ordinal() + 1];
        }
        
        public String getName() {
            return this.toString().substring(0, 1).concat(this.toString().substring(1).toLowerCase());
        }
    }
    
    public class MapleProfession {
        
        private final ProfessionType type;
        private ProfessionLevel level;
        private short exp;
        
        public MapleProfession(ProfessionType type, ProfessionLevel level, short exp) {
            this.type = type;
            this.level = level;
            this.exp = exp;
        }
        
        public boolean gainExp(int gain) {
            if (level == ProfessionLevel.MASTER)
                return false;
            
            exp += gain;
            
            if (exp >= level.getNext().getThreshold()) {
                exp = level.getNext().getThreshold();
                return true;
            }
            return false;
        }
        
        public void increaseLevel() {
            level = level.getNext();
            exp = 0;
        }
        
        public String getName() {
            return type.toString().substring(0, 1).concat(type.toString().substring(1).toLowerCase());
        }
        
        public String getHarvestableName() {
            return (type == ProfessionType.HERBALISM)? "Herb" : "Vein";
        }
        
        public ProfessionType getType() {
            return type;
        }
        
        public ProfessionLevel getLevel() {
            return level;
        }
        
        public int getExp() {
            return exp;
        }
    }
    
    public void changeProfession(boolean prim, ProfessionType newtype) {
        if (prim)
            primary = new MapleProfession(newtype, ProfessionLevel.NEWBIE, (short) 0);
        else
            secondary = new MapleProfession(newtype, ProfessionLevel.NEWBIE, (short) 0);
    }
    
    public MapleProfession getProfession(boolean prim) {
        if (primary == null)
            return null;
        
        if (prim)
            return primary;
        else
            return secondary;
    }
    
    public MapleProfession getProfession(ProfessionType prof) {
        if (primary == null)
            return null;
        
        if (primary.getType() == prof)
            return primary;
        else if (secondary.getType() == prof)
            return secondary;
        else
            return null;
    }
    
    public void loadProfessions(final int charid) {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT type, level, exp FROM professions WHERE charid = ?")) {
            ps.setInt(1, charid);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    MapleProfession loaded = new MapleProfession(ProfessionType.values()[rs.getByte("type")], ProfessionLevel.values()[rs.getByte("level")], rs.getShort("exp"));
                    if (first) {
                        primary = loaded;
                        first = false;
                    } else {
                        secondary = loaded;
                    }
                }
                rs.close();
            }
            ps.close();
        } catch (SQLException sqle) {
            System.out.println("Error."+sqle);
        }
    }

    public void saveProfessions(final int charid) {
        if (primary == null) {
            return;
        }
        
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM professions WHERE charid = ?");
            ps.setInt(1, charid);
            ps.execute();
            ps.close();
            StringBuilder query = new StringBuilder();
            query.append("INSERT INTO professions VALUES (");
            query.append(charid);
            query.append(", ");
            query.append(primary.getType().ordinal());
            query.append(", ");
            query.append(primary.getLevel().ordinal());
            query.append(", ");
            query.append(primary.getExp());
            query.append(")");
            if (secondary != null) {
                query.append(",(");
                query.append(charid);
                query.append(", ");
                query.append(secondary.getType().ordinal());
                query.append(", ");
                query.append(secondary.getLevel().ordinal());
                query.append(", ");
                query.append(secondary.getExp());
                query.append(")");
            }
            ps = con.prepareStatement(query.toString());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.out.println("Error."+e);
        }
    }
}
