package server;

import client.MapleCharacter;
import constants.ServerConstants;
import handling.channel.ChannelServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class StartFrame extends JFrame implements ActionListener {
    String type;
    private int selected_c_row = -1; // 선택된 캐릭터 행 값, 초기 값 -1
    ArrayList <String> nameList = null;
    public StartFrame(){
        Container cp = getContentPane();

        // panel 1
        JPanel p1 = new JPanel();
        JLabel expLabel = new JLabel("경험치 배율: "+ ServerConstants.expRate);
        JLabel dropLabel = new JLabel("드롭 배율: "+ ServerConstants.dropRate);
        JLabel mesoLabel = new JLabel("메소 배율: "+ ServerConstants.mesoRate);
        p1.add(expLabel);
        p1.add(dropLabel);
        p1.add(mesoLabel);

        // panel 2
        JPanel p2 = new JPanel();
        nameList = null;
        DefaultListModel<String> l1 = new DefaultListModel<>();
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                l1.addElement(mch.getName());
                try{
                    nameList.add(mch.getName()); // selected_c_row 를 가져오기 위함
                }
                catch (NullPointerException e){
                    System.out.println("nameList.add Error!");
                }
            }
        }
        JList<String> list = new JList<>(l1);
        list.setBounds(100,100, 75,75);
        JButton setGMButton = new JButton("GM 설정");
        setGMButton.addActionListener(this);

        p2.add(list);
        // ArrayList<MapleCharacter> (idToChar.values()) - getAllCharacters() 의 반환 값


        cp.add(p1);
        cp.add(p2);
        setSize(500, 500);
        setTitle("test");
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent e){
        if (type.equals("GM 설정")) {
            //nameList
        }
    }

}
