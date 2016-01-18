import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * 클라이언트 정보를 입력하는 스윙 클래스로
 * 	서버 정보 및 변경을 처리할수 있는 UI
 * 클라이언트와 통신이 시작되면
 * AutoBot 클래스를 이용하여 수신된값에 따라
 * 마우스혹은 키보드 이벤트를 처리한다. 
 */
public class ServerWindow implements ActionListener{
	
	private RemoteDataServer server;
	
	private Thread sThread; //server thread
	
	private static final int WINDOW_HEIGHT = 200;
	private static final int WINDOW_WIDTH = 350;
	
	private String ipAddress;
	
	private JFrame window = new JFrame("원격 마우스 서버");
	
	private JLabel addressLabel = new JLabel("");
	private JLabel portLabel = new JLabel("포트: ");
	private JTextArea[] buffers = new JTextArea[3];
	private JTextField portTxt = new JTextField(5);
	private JLabel serverMessages = new JLabel("연결되지 않음");
	
	private JButton connectButton = new JButton("연결");
	private JButton disconnectButton = new JButton("연결종료");
	
	public ServerWindow(){
		server = new RemoteDataServer();
		
		window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		connectButton.addActionListener(this);
		disconnectButton.addActionListener(this);
		
		Container c = window.getContentPane();
		c.setLayout(new FlowLayout());
		
		try{
			InetAddress ip = InetAddress.getLocalHost();
			String ip2 = InetAddress.getLocalHost().getHostAddress();

			addressLabel.setText("IP Address: "+ip2);
		}
		catch(Exception e){addressLabel.setText("IP Address Could Not be Resolved");}
		
		int x;
		for(x = 0; x < 3; x++){
			buffers[x] = new JTextArea("", 1, 30);
			buffers[x].setEditable(false);
			buffers[x].setBackground(window.getBackground());
		}
		
		c.add(addressLabel);
		c.add(buffers[0]);
		c.add(portLabel);
		portTxt.setText("5444");
		c.add(portTxt);
		c.add(buffers[1]);
		c.add(connectButton);
		c.add(disconnectButton);
		c.add(buffers[2]);
		c.add(serverMessages);
		
		window.setLocationRelativeTo(null);
		window.setVisible(true);
		window.setResizable(false);
	}
	
	public void actionPerformed(ActionEvent e){
		Object src = e.getSource();
		
		if(src instanceof JButton){
			if((JButton)src == connectButton){
				int port = Integer.parseInt(portTxt.getText());
				runServer(port);
			}
				
			else if((JButton)src == disconnectButton){
				closeServer();
			}
		}
	}
	
	public void runServer(int port){
		if(port <= 9999){
			server.setPort(port);
			sThread = new Thread(server);
			sThread.start();
		}
		else{
			serverMessages.setText("포트번호르 10000 이하로 해주세요");
		}
	}
	
	public void closeServer(){
		serverMessages.setText("연결종료");
		server.shutdown();
		connectButton.setEnabled(true);
	}
	
	public static void main(String[] args){
		new ServerWindow();
	}
	
	public class RemoteDataServer implements Runnable{
		int PORT;
		private DatagramSocket server;
		private byte[] buf;
		private DatagramPacket dgp;
		
		private String message;
		private AutoBot bot;
		
		public RemoteDataServer(int port){
			PORT = port;
			buf = new byte[1000];
			dgp = new DatagramPacket(buf, buf.length);
			bot = new AutoBot();
			serverMessages.setText("Not Connected");
		}
		
		public RemoteDataServer(){
			buf = new byte[1000];
			dgp = new DatagramPacket(buf, buf.length);
			bot = new AutoBot();
			serverMessages.setText("Not Connected");
		}
		
		public String getIpAddress(){
			String returnStr;
			try{
					InetAddress ip = InetAddress.getLocalHost();
					returnStr = ip.getCanonicalHostName();
			}
			catch(Exception e){ returnStr = new String("Could Not Resolve Ip Address");}
			return returnStr;
		}
		
		public void setPort(int port){
			PORT = port;
		}
		
		public void shutdown(){
			try{server.close();
				serverMessages.setText("Disconnected");}
			catch(Exception e){}
		}
		
		public void run(){
			boolean connected = false;
			try {InetAddress ip = InetAddress.getLocalHost(); 
				serverMessages.setText("Waiting for connection on " + ip.getCanonicalHostName());
				
				server = new DatagramSocket(PORT, ip);
				
				connected = true;
				connectButton.setEnabled(false);
			}
			catch(BindException e){ serverMessages.setText("Port "+PORT+" is already in use. Use a different Port"); }
			catch(Exception e){serverMessages.setText("Unable to connect");}
			
			while(connected){
				// get message from sender
				try{ server.receive(dgp);
				
					// translate and use the message to automate the desktop
					message = new String(dgp.getData(), 0, dgp.getLength());
					if (message.equals("Connectivity")){
						//send response to confirm connectivity
						serverMessages.setText("Trying to Connect");
						server.send(dgp); //echo the message back
					}else if(message.equals("Connected")){
						server.send(dgp); //echo the message back
					}else if(message.equals("Close")){
						serverMessages.setText("Controller has Disconnected. Trying to reconnect."); //echo the message back
					}else{
						serverMessages.setText("Connected to Controller");
						bot.handleMessage(message);
					}
				}catch(Exception e){
					serverMessages.setText("Disconnected");
					connected = false;}
			}
		}
	}
}
