/*
 * Copyright (C) 2015 SYJourney
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package client.command;

import client.Abilities;
import client.MapleCharacter;
import client.MapleClient;
import client.properties.MapleJob;
import client.properties.MapleStat;
import constants.GameConstants;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import scripting.npc.NPCScriptManager;
import server.MaplePortal;
import server.Timer.WorldTimer;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapDataFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class PlayerCommand extends Command {

    public PlayerCommand(MapleClient c, String[] args) {
        this.c = c;
        this.args = args;
    }
    
    @Override
    public void execute() {
        
        MapleCharacter chr = c.getPlayer();
        
        switch(args[0]) {
            case "dispose":
                NPCScriptManager.getInstance().dispose(c);
                c.announce(MaplePacketCreator.enableActions());
                chr.dropMessage(6, "Disposed.");
                break;
            case "help":
                chr.dropMessage(6, "Available Commands:");
                chr.dropMessage(6, "~dispose : Use it when you can't attack or talk to an NPC.");
                chr.dropMessage(6, "~info : Display information about your character.");
                chr.dropMessage(6, "~time : Display the server time and uptime.");
                chr.dropMessage(6, "~fm : Transports you to free market.");
                chr.dropMessage(6, "~admin : Calls the mighty Maple Administrator. (Universal NPC)");
                if (chr.getProfessions().getProfession(true) != null) {
                    chr.dropMessage(6, "~profession : Calls Grant, to inform you about your professions.");
                }
                chr.dropMessage(6, "~bosshp : Display the HP of nearby bosses.");
                break;
            case "info":
                chr.dropMessage(6, "Vote Points: "+c.getVPoints());
                chr.dropMessage(6, "Cash: "+chr.getCashShop().getCash(1));
                chr.dropMessage(6, "Rank: "+chr.getRank());
                if (chr.getAbilities().getLinesSize() > 0) {
                    Abilities abs = chr.getAbilities();
                    StringBuilder sb = new StringBuilder("Inner Ability");
                    sb.append(abs.getName());
                    sb.append("): ");
                    for (byte i = 0; i < chr.getAbilities().getLinesSize(); i++) {
                        sb.append(abs.getLine(i).getName(chr.getAbilities().getRank()));
                        sb.append("  ");
                    }
                    chr.dropMessage(6, sb.toString());
                }
                break;
            case "time":
                final SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
                long uptime = WorldTimer.getInstance().getUptime();
                int d = (int) uptime/(24 * 60 * 60);
                int h = (int) (uptime % (24 * 60 * 60))/(60 * 60);
                int m = (int) (uptime % (60 * 60))/(60);
                chr.dropMessage(6, "Server Time: "+time.format(Calendar.getInstance().getTime()));
                StringBuilder sb = new StringBuilder("Uptime: ");
                sb.append(d);
                sb.append((d == 1)? " Day : " : " Days : ");
                sb.append(h);
                sb.append((h == 1)? " Hour : " : " Hours : ");
                sb.append(m);
                sb.append((m == 1)? " Minute : " : " Minutes : ");
                chr.dropMessage(6, sb.toString());
                return;
            case "gm":
                return;
            case "goto": //Command only used for testing stuff, will not be in beta
                if (args.length < 2) {
                    chr.message("Please use ~goto <mapname/mapid>");
                }
                MapleMap target2 = null;
                int targetId = MapleMapDataFactory.getInstance().searchMapByName(args);
                if (targetId > 0)
                    target2 = c.getChannelServer().getMapFactory().getMap(targetId);
                try {
                    if (target2 == null) {
                        target2 = c.getChannelServer().getMapFactory().getMap(Integer.valueOf(args[1]));
                    }
                    MaplePortal targetPortal2 = target2.getPortal(0);
                    chr.changeMap(target2, targetPortal2);
                } catch (NumberFormatException | NullPointerException e) {
                    chr.dropMessage(6, "Map does not exist.");
                }
                break;
            case "bosshp":
                boolean found = false;
                List<MapleMapObject> monsters = chr.getMap().getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                for (MapleMapObject mob : monsters) {
                    MapleMonster monster = (MapleMonster) mob;
                    if (monster.getStats().isBoss()) {
                        c.getPlayer().dropMessage(6, monster.getName()+" with "+formatHpOutput(monster.getHp())+"/"+formatHpOutput(monster.getMaxHp())+"HP");
                        found = true;
                    }
                }
                if (!found) {
                    c.getPlayer().dropMessage(6, "No monster was found.");
                }
                return;
            case "str":
            case "dex":
            case "int":
            case "luk":
                try {
                    MapleStat type = MapleStat.getByString(args[0].toUpperCase());
                    if (c.getPlayer().getStat(MapleStat.AVAILABLEAP) >= Integer.valueOf(args[1])) {
                        c.getPlayer().addStat(type, Short.valueOf(args[1]));
                        c.getPlayer().addStat(MapleStat.AVAILABLEAP, (short) -Integer.valueOf(args[1]));
                    } else {
                        c.getPlayer().dropMessage(6, "You don't have enough AP left.");
                    }
                } catch (NumberFormatException nfe ){
                    c.getPlayer().dropMessage(6, "Please use ~<stat> <amount>.");
                }
                return;
        }
        
        byte deniedReason = isAllowedType2Commands(chr.getMapId(), chr.getStat(MapleStat.LEVEL), chr.getMapleJob(), chr.isAlive());
        
        if (deniedReason == 0) {
            switch (args[0]) {
                case "fm":
                    chr.saveLocation("FREE_MARKET");
                    MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.valueOf(910000000));
                    MaplePortal targetPortal = target.getPortal(0);
                    chr.changeMap(target, targetPortal);
                    return;
                case "profession":
                    if (chr.getProfessions().getProfession(true) != null) {
                        NPCScriptManager.getInstance().start(c, 9031000, "9031000", chr);
                    } else {
                        chr.message("You have yet to learn a profession.");
                    }
                    return;
                case "admin":
                    NPCScriptManager.getInstance().start(c, 9010000, "admin", chr);
                    return;
            }   
        }
        
        switch (deniedReason) {
            case 1: 
                chr.dropMessage("You cannot use this command now.");
                return;
            case 2:
                chr.dropMessage("You need to be atleast Level 10 to use this command.");
                return;
            case 3:
                chr.dropMessage("You need to make your first job advancement to use this command.");
                return;
            case 4:
                chr.dropMessage("You cannot use this command in a PQ Map.");
                return;
        }
        
        if (chr.getGMLevel() == 0) {
            chr.dropMessage("Player Command "+ args[0] + " does not exist.");
        }
    }
    
    private static String formatHpOutput(long in) {
        StringBuilder output = new StringBuilder();
        String temp = Long.toString(in);
        int ind;
        for (ind = temp.length()-1; ind >= 0; ind --) {
            if ((temp.length()-1-ind)%3 == 0 && (temp.length()-1-ind) > 0)
                output.append(",");
            output.append(temp.charAt(ind));
        }
        temp = output.toString();
        output.delete(0, output.length());
        for (ind = temp.length()-1; ind >= 0; ind --) {
            output.append(temp.charAt(ind));
        }
        return output.toString();
    }
    
    private static byte isAllowedType2Commands(int mapid, short level, MapleJob job, boolean alive) {
        if (!alive || (mapid >=  910000000 && mapid <=  910000022)) {
            return 1;
        }
        if (level < 10) {
            return 2;
        }
        if (job == MapleJob.BEGINNER || job == MapleJob.LEGEND) {
            return 3;
        }
        if (GameConstants.isPQMap(mapid)) {
            return 4;
        }
        return 0;
    }
}
