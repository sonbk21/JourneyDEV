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

package net.server.handlers;

import client.MapleClient;
import constants.ServerConstants;
import net.AbstractPacketHandler;
import net.server.Server;
import net.server.world.World;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */
public class LoginHandlers {
    public final static ServerlistHandler serverlist = new ServerlistHandler();
    public final static LoginHandler login = new LoginHandler();
    
    public final static class ServerlistHandler extends AbstractPacketHandler {
        @Override
        public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
            World oneWorld = Server.getInstance().getWorld(0);
            c.announce(PacketCreator.ServerList(0, ServerConstants.WORLD_NAMES[0], oneWorld.getFlag(), oneWorld.getEventMessage(), oneWorld.getChannels()));
        }
    }
    
    public final static class LoginHandler extends AbstractPacketHandler {
        @Override
        public boolean validateState(MapleClient c) {
            return !c.isLoggedIn() && c.passedWZCheck();
        }
        
        @Override
        public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
            String account = slea.readMapleAsciiString();
            String pass = slea.readMapleAsciiString();
            c.setAccountName(account);
            byte reason = c.tryLogin(account, pass);
            c.announce(PacketCreator.LoginStatus(c, reason, c.isMuted()));
        }
    }
}
