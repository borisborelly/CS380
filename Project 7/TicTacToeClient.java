import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.SocketFactory;
import java.util.Scanner;

public class TicTacToeClient implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String host = "45.50.5.238";
	private static final int port = 38007;
	
	private ObjectInputStream socketIn;
	private ObjectOutputStream out;
	private Socket socket = new Socket();
	private Scanner kb = new Scanner(System.in);
	
	private CommandMessage commandMessage;
	private ConnectMessage connectMessage;
	private MoveMessage moveMessage;
	
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException { 
		TicTacToeClient ttt = new TicTacToeClient();
		ttt.run();
	}
	
	/**
	 * Initialized everything and contains server listener
	 * Server listener handles all responses with approrpriate actions
	 * If username is taken, program ends
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void run() throws UnknownHostException, IOException, InterruptedException { 
		socket = SocketFactory.getDefault().createSocket(host, port);
		socketIn = new ObjectInputStream(socket.getInputStream());
		out = new ObjectOutputStream(socket.getOutputStream());
		System.out.println("Connected to " + host + ":" + port + "!\n");
		
		Runnable listen = () -> { //server listener
			Object incoming;
			try {
				while ( (incoming = socketIn.readObject()) != null) {
					System.out.println("\n[DEBUG] Listener: " + ((Message) incoming).getType());
					switch ( ((Message) incoming).getType() ) {
						case CONNECT:
							System.out.println("Connect message");
							break;
						case COMMAND:
							System.out.println("Command message");
							break;
						case BOARD:
							displayBoard((BoardMessage) incoming); //prints board
							boardHandler((BoardMessage) incoming); //handles the board messages
							play();
							break;
						case MOVE:
							System.out.println("Move message");
							break;
						case ERROR:
							System.out.println( ((ErrorMessage) incoming).getError() );
							errorHandler(((ErrorMessage) incoming));
							break;
						default:
							System.out.println("Unexpected response from server!");
							break;
					} //end switch
				} // end while
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		
		Thread listener = new Thread(listen);
		listener.start();	//reads incoming messages from the server
		
		System.out.println("Enter username");
		String user = kb.nextLine();
		connectMessage = new ConnectMessage(user);
		out.writeObject(connectMessage); //send username request
		
		commandMessage = new CommandMessage(CommandMessage.Command.NEW_GAME);
		out.writeObject(commandMessage); //start new game
	}
	
	/**
	 * Prints direction menu and does corresponding action
	 * @throws IOException
	 */
	private void play() throws IOException { 
		System.out.println("\n***** MENU *****");
		System.out.println("1 - Move");
		System.out.println("2 - Surrender");
		System.out.println("3 - Quit");
		String input = kb.nextLine();
		
		switch(input) { 
			case "1":
				System.out.println("Input row,column using comma as delimeter");
				String[] move = kb.next().split(",");
				byte row = Byte.parseByte(move[0]);
				byte col = Byte.parseByte(move[1]);
				
				moveMessage = new MoveMessage(row, col);
				out.writeObject(moveMessage);
				break;
			case "2":
				System.out.println("You surrendered! You lose...");
				commandMessage = new CommandMessage(CommandMessage.Command.SURRENDER);
				out.writeObject(commandMessage);
				exit();
				break;
			case "3":
				System.out.println("Force exiting...");
				exit();
				break;
			default:
				System.out.println("Bad Input.." + input);
				kb.nextLine();
				play();
				break;
		} //end switch
		kb.nextLine();
	}
	
	/**
	 * Prints the 3x3 ttt board
	 * @param msg
	 */
	private void displayBoard(BoardMessage msg) { 
		byte[][] board = msg.getBoard();

		System.out.println();
		System.out.println("   0  1  2");
		for (int i = 0; i < board.length; i++) {
			System.out.print(i + " ");
			for (int j = 0; j < board.length; j++) {
				switch (board[i][j]) {
					case 0:
						System.out.print(" _ ");
						break;
					case 1:
						System.out.print(" O ");
						break;
					case 2:
						System.out.print(" X ");
						break;
				}
			}
			System.out.println();
		}
	}
	
	/**
	 * Board status handler
	 * Exits when game is no longer in progress (stalemate or someone won)
	 * @param msg
	 * @throws IOException
	 */
	private void boardHandler(BoardMessage msg) throws IOException { 
		//PLAYER1_SURRENDER, PLAYER2_SURRENDER, PLAYER1_VICTORY, 
		//PLAYER2_VICTORY, STALEMATE, IN_PROGRESS, ERROR;
		switch( msg.getStatus() ) { 
			case PLAYER1_SURRENDER:
				System.out.println("Quitter... You lose! ");
				exit();
			break;
			
			case PLAYER2_SURRENDER:
				System.out.println("Player 2 forfeited... You win! ");
				exit();
			break;
			
			case PLAYER1_VICTORY:
				System.out.println("Congratulations! You win!");
				exit();
			break;
			
			case PLAYER2_VICTORY:
				System.out.println("Player 2 won! You lose.");
				exit();
			break;
			
			case STALEMATE:
				System.out.println("Match was a draw... Exiting program");
				exit();
			break;
			
			case IN_PROGRESS: //allow game to continue
				
			break;
			
			case ERROR:
				System.out.println("ERROR with BoardMEssage");
				exit();
			break;
			
			default:
				System.out.println("Unknown status...");
				exit();
			break;
		}
	}
	
	/**
	 * Handles error messages from server
	 * TODO: Re-prompt user for new name if in use
	 * @param e
	 * @throws IOException
	 */
	private void errorHandler(ErrorMessage e) throws IOException { 
		String err = e.getError();
		switch(err) { 
			case "Name in use.":
				exit();
				break;
			case "That move is illegal.":
				System.out.println("Try again...");
				play();
				break;
			default:
				System.out.println("ERROR DEBUG - " + err);
				break;
		}
	
	}
	
	/**
	 * Closes everything and ends program.
	 * @throws IOException
	 */
	private void exit() throws IOException { 
		System.out.println("Exiting Program...");
		
		commandMessage = new CommandMessage(CommandMessage.Command.EXIT);
		out.writeObject(commandMessage);
		
		socketIn.close();
		out.close();
		kb.close();
		System.exit(0);
	}
}
