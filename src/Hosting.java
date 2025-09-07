import javax.swing.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.io.*;
import java.net.*;
import java.util.*;

public class Hosting {

    public List<String> Players = new ArrayList<>();

    public Hosting (){
        Card[] Deck = new Card[52];


    }
    public void Start(){
        JFrame frame = new JFrame();
        frame.setSize(800,600);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextArea PlayerListTitle = new JTextArea();
        PlayerListTitle.setSize(200, 30);
        PlayerListTitle.setLocation(25, 10);
        PlayerListTitle.setEditable(false);
        PlayerListTitle.setVisible(true);
        PlayerListTitle.setText("Players:");

        JTextArea PlayerList = new JTextArea();
        PlayerList.setSize(200, 300);
        PlayerList.setLocation(25, 50);
        PlayerList.setEditable(false);
        PlayerList.setVisible(true);

        frame.add(PlayerList);
        frame.add(PlayerListTitle);


    }

    public void UpdatePlayers(JTextArea Text)
    {
        Text.setText("");

        for (int i = 0; i < Players.size(); i++) {
            Text.append(Players.get(i).toString() + "\n");
        }

    }

}
