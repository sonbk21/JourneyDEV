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
import client.properties.MapleStat;
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
                chr.dropMessage(6, "~admin : Calls the mighty Maple Administrator. (Universal NPC)");
                if (chr.getProfessions().getProfession(true) != null)
                    chr.dropMessage(6, "~profession : Calls Grant, to inform you about your professions.");
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
                chr.dropMessage(6, "Uptime: "+d+" Days : "+h+" Hours : "+m+" Minutes");
                break;
            case "admin":
                NPCScriptManager.getInstance().start(c, 9010000, "admin", chr);
                break;
            case "profession":
                if (chr.getProfessions().getProfession(true) != null) {
                    NPCScriptManager.getInstance().start(c, 9031000, "9031000", chr);
                } else {
                    chr.message("You have yet to learn a profession.");
                }
                break;
            case "goto":
                if (args.length < 2) {
                    chr.message("Please use ~goto <mapname/mapid>");
                }
                MapleMap target = null;
                        
                int targetId = MapleMapDataFactory.getInstance().searchMapByName(args);
                if (targetId > 0)
                    target = c.getChannelServer().getMapFactory().getMap(targetId);
                
                try {
                    if (target == null)
                        target = c.getChannelServer().getMapFactory().getMap(Integer.valueOf(args[1]));
                    MaplePortal targetPortal = target.getPortal(0);
                    chr.changeMap(target, targetPortal);
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
                break;
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
                break;
            default:
                if (chr.getGMLevel() == 0) {
                    chr.yellowMessage("Player Command "+ args[0] + " does not exist.");
                }
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
}
