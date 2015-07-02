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

import client.MapleClient;
import client.command.PlayerCommand.*;
import client.command.PlayerCommand.type2Command.*;
import java.util.EnumMap;
import java.util.Map;
import tools.MaplePacketCreator;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class CommandProcessor {
    
    private final static Map<Commands, Command> playerCommands = new EnumMap<>(Commands.class);
    private final static CommandProcessor instance = new CommandProcessor();
    
    public enum Commands {
        DISPOSE, HELP, INFO, TIME, GM, BOSSHP, FM, PROFESSION, ADMIN, GOTO, STR, DEX, INT, LUK;
        
        public byte getGMLevel() {
            return (byte) ((this.compareTo(ADMIN) > 0)? 1 : 0);
        }
        
        public static Commands fromString(String type, char heading) {
            return valueOf(type.replace(heading+"", ""));
        }
    }
    
    private CommandProcessor() {
        playerCommands.put(Commands.DISPOSE, dispose.instance);
        playerCommands.put(Commands.HELP, help.instance);
        playerCommands.put(Commands.INFO, info.instance);
        playerCommands.put(Commands.TIME, time.instance);
        playerCommands.put(Commands.BOSSHP, bosshp.instance);
        playerCommands.put(Commands.FM, fm.instance);
        playerCommands.put(Commands.PROFESSION, profession.instance);
        playerCommands.put(Commands.ADMIN, admin.instance);
        playerCommands.put(Commands.GM, gm.instance);
    }
    
    public static CommandProcessor getInstance() {
        return instance;
    }
    
    public void process(MapleClient c, String[] args, char heading) {
        try {
            Commands type = Commands.fromString(args[0].toUpperCase(), heading);
            byte gmlevel = type.getGMLevel();
            if (c.getPlayer().getGMLevel() < gmlevel) {
                c.getPlayer().message("You may not use this command.");
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
                        
            switch (type.getGMLevel()) {
                case 0:
                    playerCommands.get(type).execute(c, args);
                    break;
                case 1:
                    //GMCommand.getInstance().execute(type, c, args);
                    break;
                case 2:
                    //AdminCommand.getInstance().execute(type, c, args);
                    break;
                default:
                    c.announce(MaplePacketCreator.enableActions());
            }
        } catch (IllegalArgumentException iae) { //Hmm...
            c.getPlayer().message("Command "+args[0]+" does not exist.");
            c.announce(MaplePacketCreator.enableActions());
        }
    }
}
