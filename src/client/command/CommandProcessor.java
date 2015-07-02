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
import client.command.GMCommand.*;
import client.command.AdminCommand.*;
import java.util.EnumMap;
import java.util.Map;
import tools.MaplePacketCreator;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class CommandProcessor {
    
    private final static Map<Commands, Command> commands = new EnumMap<>(Commands.class);
    private final static CommandProcessor instance = new CommandProcessor();
    
    public enum Commands {
        DISPOSE, HELP, INFO, TIME, GM, BOSSHP, FM, PROFESSION, ADMIN, NOTICE, GOTO, SPAWN, ID, ITEM, JOB, MAXSKILLS, MESOS, SERVERNOTICE, NPC, SETGMLEVEL, SHUTDOWN;
        
        public byte getGMLevel() {
            return (byte) ((this.compareTo(SERVERNOTICE) > 0)? 2 : (this.compareTo(ADMIN) > 0)? 1 : 0);
        }
        
        public static Commands fromString(String type, char heading) {
            return valueOf(type.replace(heading+"", ""));
        }
    }
    
    private CommandProcessor() {
        commands.put(Commands.DISPOSE, dispose.instance);
        commands.put(Commands.HELP, help.instance);
        commands.put(Commands.INFO, info.instance);
        commands.put(Commands.TIME, time.instance);
        commands.put(Commands.BOSSHP, bosshp.instance);
        commands.put(Commands.FM, fm.instance);
        commands.put(Commands.PROFESSION, profession.instance);
        commands.put(Commands.ADMIN, admin.instance);
        commands.put(Commands.GM, gm.instance);
        commands.put(Commands.NOTICE, notice.instance);
        commands.put(Commands.GOTO, _goto.instance);
        commands.put(Commands.SPAWN, spawn.instance);
        commands.put(Commands.ID, id.instance);
        commands.put(Commands.ITEM, item.instance);
        commands.put(Commands.JOB, job.instance);
        commands.put(Commands.MAXSKILLS, maxskills.instance);
        commands.put(Commands.MESOS, mesos.instance);
        commands.put(Commands.SERVERNOTICE, servernotice.instance);
        commands.put(Commands.NPC, npc.instance);
        commands.put(Commands.SETGMLEVEL, setgmlevel.instance);
        commands.put(Commands.SHUTDOWN, shutdown.instance);
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
            commands.get(type).execute(c, args);
        } catch (IllegalArgumentException iae) { //Hmm...
            c.getPlayer().message("Command "+args[0]+" does not exist.");
            c.announce(MaplePacketCreator.enableActions());
        }
    }
}
