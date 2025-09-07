import javax.swing.*;
import javax.swing.plaf.basic.BasicTreeUI;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class Main {
    public static void main(String[] args) {

        JFrame frame = new JFrame();
        frame.setSize(800,600);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton JoinGameButton = new JButton();
        JoinGameButton.setSize(200, 100);
        JoinGameButton.setLocation(50, 50);
        JoinGameButton.setText("Join Game");
        JoinGameButton.setVisible(true);

        JButton HostGameButton = new JButton();
        HostGameButton.setSize(200, 100);
        HostGameButton.setLocation(50, 200);
        HostGameButton.setText("Host Game");
        HostGameButton.setVisible(true);

        frame.add(JoinGameButton);
        frame.add(HostGameButton);

        HostGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Hosting hosting = new Hosting();
                frame.setVisible(false);
                hosting.Start();
            }
        });



    }
}