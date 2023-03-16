package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.SECRET;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player MrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;

		private ImmutableSet<Piece> winner;

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {
			this.setup = new GameSetup(setup.graph, setup.moves);
			this.remaining = remaining;
			this.log = log;
			this.MrX = mrX;
			this.detectives = detectives;
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			if (!(mrX.isMrX())) throw new IllegalArgumentException("No MrX");
			if (detectives.isEmpty()) throw new IllegalArgumentException("No detectives");
			for (Player detective : detectives) {
				if (detective.isMrX()) throw new IllegalArgumentException("More than 1 MrX");
				if ((detective.tickets()).get(SECRET) != 0)
					throw new IllegalArgumentException("Detective has secret ticket");
				if ((detective.tickets()).get(DOUBLE) != 0)
					throw new IllegalArgumentException("Detective has double ticket");
			}
			for (int i = 0; i < detectives.size(); i++) {
				for (int j = i + 1; j < detectives.size(); j++) {
					if (detectives.get(i).piece() == detectives.get(j).piece())
						throw new IllegalArgumentException("2 detectives are the same colour");
					if (detectives.get(i).location() == detectives.get(j).location())
						throw new IllegalArgumentException("2 detectives are the same colour");
				}
			}
			if (!(remaining.contains(MrX)) && !remaining.contains(MrX)) {
				for (Player detective : detectives) {
					this.moves =ImmutableSet.copyOf(makeSingleMoves(this.setup, this.detectives, detective, detective.location()));

				}
			}

			if (remaining.contains(MrX)) {
				List<Move> new_moves = new ArrayList<>();
				new_moves.addAll(makeSingleMoves(this.setup, this.detectives, MrX, MrX.location()));
				new_moves.addAll(makeDoubleMoves(this.setup, this.detectives,MrX, MrX.location()));
				this.moves = ImmutableSet.copyOf(new_moves);

			}

		}
		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player temp : detectives) {
				if ((temp.piece()) == detective) return Optional.of(temp.location());
			}
			return Optional.empty();
		}



		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			for (Player temp : detectives) {
				TicketBoard ticketboard = new TicketBoard() {
					public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
						return (temp.tickets()).get(ticket);
					}}
					;
				if((temp.piece()) == piece) return Optional.of(ticketboard);
				}
			TicketBoard ticketboard = new TicketBoard() {
				public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
					return (MrX.tickets()).get(ticket);
				}}
					;
			if (piece.isMrX()) return Optional.of(ticketboard);

			return Optional.empty();
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return null;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			return null;
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return moves;
		}

		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			HashSet<Move.SingleMove> possibleMoves = new HashSet<>();
			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				boolean occupied = false;
				for (Player temp : detectives) {
					if (temp.location() == destination) occupied = true;
				}
				if (occupied) continue;
				for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
					if ((player.tickets().containsValue(t.requiredTicket())) || (player.tickets().containsValue(SECRET))) {
						possibleMoves.add(new Move.SingleMove(player.piece(),source, t.requiredTicket() , destination));
					}
					// TODO find out if the player has the required tickets
					//  if it does, construct a SingleMove and add it the collection of moves to return
				}

				//  add moves to the destination via a secret ticket if there are any left with the player
			}

			// TODO return the collection of moves
			return possibleMoves;
		}

		private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			Set<Move.SingleMove> possibleMoves = makeSingleMoves(setup, detectives,player, source);
			Set<Move.DoubleMove> possibleDoubleMoves = new HashSet<>();
			for (Move.SingleMove singleMove : possibleMoves) {
				Set<Move.SingleMove> possibleMoveTwo = makeSingleMoves(setup, detectives,player, singleMove.destination);
				for (Move.SingleMove moveTwo : possibleMoveTwo) {
						if ((moveTwo.ticket != singleMove.ticket) || (player.hasAtLeast(moveTwo.ticket, 2))) {
							possibleDoubleMoves.add(new Move.DoubleMove(player.piece(), source, singleMove.ticket, singleMove.destination, moveTwo.ticket, moveTwo.destination));
						}
				}
			}

			return possibleDoubleMoves;
		}




			@Override public GameSetup getSetup() {  return this.setup;}


		@Override  public ImmutableSet<Piece> getPlayers() {
			Set<Piece> temp = new HashSet<>();
			temp.add(this.MrX.piece());;
			for(Player detective : this.detectives) {temp.add(detective.piece());}
			return ImmutableSet.copyOf(temp);

		}
		@Override public GameState advance(Move move) {
			if (this.moves.contains(move)) {throw new IllegalArgumentException("illegal move " + move);}
			/*for (Player player : detectives) {

				if (this.remaining.contains(player.piece())){
					GameState nextState = move.accept(new Move.Visitor<GameState>(){
						@Override
						public GameState visit(Move.SingleMove move) {

						}

						@Override
						public GameState visit(Move.DoubleMove move) {
							return null;
						}


					});
				}

			}

			 */
			return null;
	}}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

	}


}
