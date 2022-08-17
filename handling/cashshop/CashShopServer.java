/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.cashshop;

import constants.ServerConstants;
import java.net.InetSocketAddress;

import handling.MapleServerHandler;
import handling.channel.PlayerStorage;
import handling.mina.MapleCodecFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;

import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import server.MTSStorage;
import server.ServerProperties;

public class CashShopServer { // 월드아이피 설정

    private static String ip;
    private static InetSocketAddress InetSocketadd;
    private final static int PORT = 7575;
    private static IoAcceptor acceptor;
    private static PlayerStorage players, playersMTS;
    private static boolean finishedShutdown = false;

    public static final void run_startup_configurations() {
        ip = ServerConstants.ip + ":" + PORT;

        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());

        acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
        players = new PlayerStorage(-10);
        playersMTS = new PlayerStorage(-20);

        try {
            InetSocketadd = new InetSocketAddress(PORT);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
        acceptor.setHandler((IoHandler) new MapleServerHandler(-1, true));
            acceptor.bind(InetSocketadd);
//System.out.println("서버 세션이 " + PORT + "포트로 대기중입니다..");
        } catch (final Exception e) {
            System.err.println("이미 " + PORT + "포트가 사용중이어서 구동에 실패하였습니다.");
            e.printStackTrace();
            throw new RuntimeException("Binding failed.", e);
        }
    }

    public static final String getIP() {
        return ip;
    }

    public static final PlayerStorage getPlayerStorage() {
        return players;
    }

    public static final PlayerStorage getPlayerStorageMTS() {
        return playersMTS;
    }

    public static final void shutdown() {
        if (finishedShutdown) {
            return;
        }
        System.out.println("Saving all connected clients (CS)...");
        players.disconnectAll();
	playersMTS.disconnectAll();
        MTSStorage.getInstance().saveBuyNow(true);
        System.out.println("Shutting down CS...");
	acceptor.unbind();
        finishedShutdown = true;
    }

    public static boolean isShutdown() {
	return finishedShutdown;
    }
}
