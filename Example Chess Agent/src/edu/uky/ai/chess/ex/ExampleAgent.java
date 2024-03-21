package edu.uky.ai.chess.ex;

import java.util.ArrayList;
import java.util.Random;

import edu.uky.ai.SearchBudgetExceededException;
import edu.uky.ai.chess.Agent;
import edu.uky.ai.chess.state.Board;
import edu.uky.ai.chess.state.Bishop;
import edu.uky.ai.chess.state.Knight;
import edu.uky.ai.chess.state.Outcome;
import edu.uky.ai.chess.state.Pawn;
import edu.uky.ai.chess.state.Queen;
import edu.uky.ai.chess.state.Rook;
import edu.uky.ai.chess.state.State;
import edu.uky.ai.chess.state.Piece;
import edu.uky.ai.chess.state.Player;

/**
 * This agent chooses a next move randomly from among the possible legal moves.
 * 
 * @author Connor Leslie
 */
public class ExampleAgent extends Agent {

	/** A random number generator */
	private final Random random = new Random(0);
	/**
	 * Constructs a new random agent. You should change the string below from
	 * "Example" to your ID. You should also change the name of this class. In
	 * Eclipse, you can do that easily by right-clicking on this file
	 * (RandomAgent.java) in the Package Explorer and choosing Refactor > Rename.
	 */
	public ExampleAgent() {
		super("ctle235");
	}

	
	//copied from Class Website
	
	private static int material(Board board, Player player) {
		int material = 0;
		for(Piece piece : board)
			if(piece.player == player)
				material += value(piece);
		return material;
	}
	
	private static int value(Piece piece) {
		if(piece instanceof Pawn)
			return 1;
		else if(piece instanceof Knight)
			return 3;
		else if(piece instanceof Bishop)
			return 3;
		else if(piece instanceof Rook)
			return 5;
		else if(piece instanceof Queen)
			return 9;
		// The piece must be a King.
		else
			return 100;
	}
	
	//helper methods
	
	
	private double materialValue(State state) {
		if(state.outcome == Outcome.BLACK_WINS)
			return Double.NEGATIVE_INFINITY;
		if(state.outcome == Outcome.WHITE_WINS)
			return Double.POSITIVE_INFINITY;
		if(state.outcome == Outcome.DRAW) {
			//if(material(state.board, Player.WHITE) - material(state.board, Player.BLACK) > 0)
				//return Double.NEGATIVE_INFINITY+1;
			//if(material(state.board, Player.WHITE) - material(state.board, Player.BLACK) < 0)
				//return Double.POSITIVE_INFINITY-1;
			return 0.0;
		}
		
		//start with materialScore
		double returnValue = material(state.board, Player.WHITE) - material(state.board, Player.BLACK);
		
		
		
		
		for(Piece piece : state.board) {
			if(piece.player == Player.WHITE) {
				if(piece instanceof Knight || piece instanceof Bishop || piece instanceof Rook) {
					if(piece.file > 1 && piece.file < 6) 
						returnValue += .1;
					if(piece.rank > 1 && piece.rank < 6) 
						returnValue += .1;
				}
			}else {
				if(piece instanceof Knight|| piece instanceof Bishop || piece instanceof Rook) {
					if(piece.file > 1 && piece.file < 6) 
						returnValue -= .1;
					if(piece.rank > 1 && piece.rank < 6) 
						returnValue -= .1;
				}
			}
		}
		
		//sqew weights towards less material
		returnValue = returnValue/(material(state.board, Player.WHITE) + material(state.board, Player.BLACK));
		
		return returnValue;
	}

	//Class to hold a State and it's value associated with it's value
	public class valuedState{
		public State state;
		public double value;
		public String name = "undefined";
		public valuedState (State _state) {
			state = _state;
			value = materialValue(state);
		}
		public valuedState() {
			state = null;
			value = 0;
		}
		public valuedState(double _value, String _name) {
			state = null;
			value = _value;
			name = _name;
		}
		
	}
	

	private valuedState minMaxAB(State current, int depth, double alpha, double beta, boolean isWhite, boolean debug) {
		if(depth <= 0 || current.outcome != null) {
			return new valuedState(current);
		}
		try {
			if(isWhite) {
				valuedState max = new valuedState(Double.NEGATIVE_INFINITY, "max");
				
				for(State nextMove : current.next()) {
					valuedState nextValuedState = minMaxAB(nextMove, depth-1, alpha, beta, false, debug);
					max = max.value >= nextValuedState.value ? max : nextValuedState; // if incorrect may break on checkmates
					if(max.state == null) {
						max = nextValuedState;
					}
					if(debug && current.turn >33) {
						System.out.println("isWhiteBlock:    value: "+ nextValuedState.value + "    state: " + nextValuedState.state);
						System.out.println("maxVal:                 " + max.value + "    state: " + max.state);
					}
					if(max.value >= beta) {//MAX and BETA may be set to the same value
						return max;
					}
					alpha = alpha > max.value ? alpha : max.value;
				}
				return max;
			}
			
			//isBlack
			valuedState min = new valuedState(Double.POSITIVE_INFINITY, "min");
			for(State nextMove : current.next()) {
				valuedState nextValuedState = minMaxAB(nextMove, depth-1, alpha, beta, true, debug);
				min = min.value <= nextValuedState.value ? min : nextValuedState; //if incorrect may break on checkmates
				if(min.state == null) {
					min = nextValuedState;
				}
				if(debug && current.turn >33) {
					System.out.println("isBlackBlock:    value: "+ nextValuedState.value + "    state: " + nextValuedState.state);
					System.out.println("minval:                 "+ min.value+ "    state: " + min.state);
				}
				if(min.value <=alpha) { //MAX and ALPHA may be set to the same value
					return min;
				}
				beta = beta < min.value ? beta : min.value;
			}
			return min;
		}catch(SearchBudgetExceededException e) {
			
			e.printStackTrace();
			System.out.println("SearchBudgetExceededException");
			if(isWhite)
				return new valuedState(Double.NEGATIVE_INFINITY, "max catch");
			return new valuedState(Double.POSITIVE_INFINITY, "min catch");
		}
	}
	
	private State cleanValuedToNext(State current, valuedState valued) {
		State next = valued.state;
		while(!current.equals(next.previous)) {
			next = next.previous;
		}
		return next;
	}
	
	
	
	@Override
	protected State chooseMove(State current) {
		//inefficent way of getting player information
		boolean isWhite = current.player == Player.WHITE;
		
		final int DEPTH = 4; //always make even
		
		//System.out.println("ChooseMoveCall");
		// This list will hold all the children state (all possible next moves).
		//ArrayList<State> moves = new ArrayList<>();
		// Iterate through each child and put it in the list. We wrap this in
		// a try/catch block in case the search budget runs out and causes an
		// exception to be thrown.
		
		valuedState bestMoveDepth = null;
		try {
			bestMoveDepth = minMaxAB(current, DEPTH, Double.NEGATIVE_INFINITY+1, Double.POSITIVE_INFINITY-1, isWhite, false);
		}catch(SearchBudgetExceededException e) {
			
			e.printStackTrace();
			System.out.println("SearchBudgetExceededException");
			
		}
		if(bestMoveDepth.state == null) {
			System.out.println(bestMoveDepth.name);
			return null;
			//for(State move : current.next()) 
				//return move;
			
		}
		return cleanValuedToNext(current, bestMoveDepth);

	}
}
