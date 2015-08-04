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

package tools;

import client.MapleClient;
import constants.ServerConstants;
import java.util.List;
import net.SendOpcode;
import net.server.channel.Channel;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */
public class PacketCreator {

    public static byte[] LoginStatus(MapleClient c, byte reason, boolean muted) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.LOGIN_STATUS.getValue());
        mplew.write(reason);
        if (reason > 0) {
            return mplew.getPacket();
        }
        mplew.writeInt(c.getAccID());
        mplew.write(c.getGender());
        mplew.write(c.gmLevel());
        mplew.writeMapleAsciiString(c.getAccountName());
        mplew.writeBool(muted);
        mplew.writeShort(2);
        return mplew.getPacket();
    }
    
    public static byte[] ServerList(int serverId, String serverName, int flag, String eventmsg, List<Channel> channelLoad) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SERVERLIST.getValue());
        mplew.write(serverId);
        mplew.writeMapleAsciiString(serverName);
        mplew.write(flag);
        mplew.writeMapleAsciiString(eventmsg);
        mplew.write(channelLoad.size());
        channelLoad.stream().forEach((ch) -> mplew.write((int) (14 * (((float)ch.getConnectedClients())/ServerConstants.CHANNEL_LOAD))));
        return mplew.getPacket();
    }
}
