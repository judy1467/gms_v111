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
package client.commands;

import java.util.ArrayList;
import client.MapleCharacter;
import client.MapleClient;
import client.commands.*;
import client.commands.InternCommand;
import constants.ServerConstants.CommandType;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import tools.FileoutputUtil;
import constants.ServerConstants;
import java.sql.Connection;


public class CommandProcessor {

    private final static HashMap<String, CommandObject> commands = new HashMap<String, CommandObject>();
    private final static HashMap<Integer, ArrayList<String>> commandList = new HashMap<Integer, ArrayList<String>>();

    static {

        Class<?>[] CommandFiles = {
            PlayerCommand.class, InternCommand.class, GMCommand.class, AdminCommand.class, DonatorCommand.class, SuperDonatorCommand.class, SuperGMCommand.class
        };

        for (Class<?> clasz : CommandFiles) {
            try {
                PlayerGMRank rankNeeded = (PlayerGMRank) clasz.getMethod("getPlayerLevelRequired", new Class<?>[]{}).invoke(null, (Object[]) null);
                Class<?>[] a = clasz.getDeclaredClasses();
                ArrayList<String> cL = new ArrayList<String>();
                for (Class<?> c : a) {
                    try {
                        if (!Modifier.isAbstract(c.getModifiers()) && !c.isSynthetic()) {
                            Object o = c.newInstance();
                            boolean enabled;
                            try {
                                enabled = c.getDeclaredField("enabled").getBoolean(c.getDeclaredField("enabled"));
                            } catch (NoSuchFieldException ex) {
                                enabled = true; //Enable all coded commands by default.
                            }
                            if (o instanceof CommandExecute && enabled) {
                                cL.add(rankNeeded.getCommandPrefix() + c.getSimpleName().toLowerCase());
                                commands.put(rankNeeded.getCommandPrefix() + c.getSimpleName().toLowerCase(), new CommandObject((CommandExecute) o, rankNeeded.getLevel()));
				if (rankNeeded.getCommandPrefix() != PlayerGMRank.GM.getCommandPrefix() && rankNeeded.getCommandPrefix() != PlayerGMRank.NORMAL.getCommandPrefix()) { //add it again for GM
                                    commands.put("!" + c.getSimpleName().toLowerCase(), new CommandObject((CommandExecute) o, PlayerGMRank.GM.getLevel()));
				}
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
                    }
                }
                Collections.sort(cL);
                commandList.put(rankNeeded.getLevel(), cL);
            } catch (Exception ex) {
                ex.printStackTrace();
                FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            }
        }
    }

    private static void sendDisplayMessage(MapleClient c, String msg, CommandType type) {
	if (c.getPlayer() == null) {
	    return;
	}
        switch (type) {
            case NORMAL:
                c.getPlayer().dropMessage(6, msg);
                break;
            case TRADE:
                c.getPlayer().dropMessage(-2, "에러 : " + msg);
                break;
	    case POKEMON:
		c.getPlayer().dropMessage(-3, "(..." + msg + "..)");
		break;
        }

    }

    public static void 도움말(MapleClient c) {
	final StringBuilder sb = new StringBuilder("명령 도움말: ");
	for (int i = 0; i <= c.getPlayer().getGMLevel(); i++) {
	    if (commandList.containsKey(i)) {
	        for (String s : commandList.get(i)) {
		    sb.append(s);
		    sb.append(" ");
	        }
	    }
	}
	c.getPlayer().dropMessage(6, sb.toString());
    }

    public static boolean processCommand(MapleClient c, String line, CommandType type) {
        if (line.charAt(0) == PlayerGMRank.NORMAL.getCommandPrefix() || (c.getPlayer().getGMLevel() > PlayerGMRank.NORMAL.getLevel() && line.charAt(0) == PlayerGMRank.DONATOR.getCommandPrefix())) {
            String[] splitted = line.split(" ");
            splitted[0] = splitted[0].toLowerCase();

            CommandObject co = commands.get(splitted[0]);
            if (co == null || co.getType() != type) {
                c.getPlayer().dropMessage(6, "명령어 " + splitted[0] + " 은(는) 존재하지 않거나 실행권한이 없습니다.");
                return true;
            }
            try {
                int ret = co.execute(c, splitted); //Don't really care about the return value. ;D
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "명령어 처리에 오류가 발생했습니다: " + e.getMessage());
                if (c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(6, "명령어 처리에 오류가 발생했습니다: " + e.getMessage());
		    e.printStackTrace();
		    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                }
            }
            return true;
        }

        if (c.getPlayer().getGMLevel() > PlayerGMRank.NORMAL.getLevel()) {
            if (line.charAt(0) == PlayerGMRank.SUPERGM.getCommandPrefix() || line.charAt(0) == PlayerGMRank.INTERN.getCommandPrefix() || line.charAt(0) == PlayerGMRank.GM.getCommandPrefix() || line.charAt(0) == PlayerGMRank.ADMIN.getCommandPrefix()) { //Redundant for now, but in case we change symbols later. This will become extensible.
                String[] splitted = line.split(" ");
                splitted[0] = splitted[0].toLowerCase();

                CommandObject co = commands.get(splitted[0]);
                if (co == null) {
		    if (splitted[0].equals(line.charAt(0) + "help")) {
 		        도움말(c);
		        return true;
		    }
                    c.getPlayer().dropMessage(6, "명령어 " + splitted[0] + " 은(는) 존재하지 않거나 실행권한이 없습니다.");
                    return true;
                }
                if (c.getPlayer().getGMLevel() >= co.getReqGMLevel()) {
                    int ret = 0;
		    try {
			ret = co.execute(c, splitted);
		    } catch (ArrayIndexOutOfBoundsException x) {
                        c.getPlayer().dropMessage(6, "명령어 처리에 오류가 발생했습니다: " + x.getMessage());
		    } catch (Exception e) {
			FileoutputUtil.outputFileError(FileoutputUtil.CommandEx_Log, e);
		    }
                    if (ret > 0 && c.getPlayer() != null) { //incase d/c after command or something
			if (c.getPlayer().isGM()) {
                            logCommandToDB(c.getPlayer(), line, "gmlog");
			} else {
			    logCommandToDB(c.getPlayer(), line, "internlog");
			}
                    }
                } else {
                    c.getPlayer().dropMessage(6, "명령어 " + splitted[0] + " 은(는) 실행권한이 없습니다.");
                }
                return true;
            }
        }
        return false;
    }

    private static void logCommandToDB(MapleCharacter player, String command, String table) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO " + table + " (cid, command, mapid) VALUES (?, ?, ?)");
            ps.setInt(1, player.getId());
            ps.setString(2, command);
            ps.setInt(3, player.getMap().getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
            ex.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
   }
}
