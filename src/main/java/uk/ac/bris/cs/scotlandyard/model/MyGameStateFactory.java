package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;
import java.util.stream.Collectors;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.*;

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
			this.moves = makeMoves();

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

			HashSet<Piece> pieces = new HashSet<>(this.detectivePieces());
			HashSet<Integer> locations = new HashSet<>(this.detectiveLocations());
			if (pieces.size() < this.detectivePieces().size()) {
				throw new IllegalArgumentException("2 detectives are the same colour!");
			}
			if (locations.size() < this.detectiveLocations().size()) {
				throw new IllegalArgumentException("2 detectives are the same location!");
			}

		}


		// streams to map detectives to their pieces and locations
		public List<Piece> detectivePieces() {
			List<Piece> detectivePieces = detectives.stream()
					.map(Player::piece)
					.collect(Collectors.toList());
			return detectivePieces;
		}
		public List<Integer> detectiveLocations() {
			List<Integer> detectiveLocations = detectives.stream()
					.map(Player::location)
					.collect(Collectors.toList());
			return detectiveLocations;
		}

		@Nonnull
		@Override
		// for each detective, find the correct location of the detective
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player temp : detectives) {
				if ((temp.piece()) == detective) return Optional.of(temp.location());
			}
			return Optional.empty();
		}



		public ImmutableSet<Move> makeMoves() {
			HashSet<Move> tempMoves = new HashSet<>();
			// if its not MrX's turn
			if (!this.remaining.contains(MRX)) {
				for (Player tempDetective : this.detectives) {
					if (remaining.contains(tempDetective.piece())) {
					tempMoves.addAll(makeSingleMoves(this.setup, this.detectives, tempDetective, tempDetective.location()));}
				}
			}

			else if (this.remaining.contains(MRX)) {
				tempMoves.addAll(makeSingleMoves(this.setup, this.detectives, MrX, MrX.location()));
				if ((makeDoubleMoves(this.setup, this.detectives, MrX, MrX.location())) != null) {
					tempMoves.addAll(makeDoubleMoves(this.setup, this.detectives, MrX, MrX.location()));}
			}
			return ImmutableSet.copyOf(tempMoves);
		}

		private boolean detectiveNoTickets() {
			boolean noTickets = true;
			for (Player detective : detectives) {
				if ((detective.tickets().get(TAXI) + detective.tickets().get(BUS) + detective.tickets().get(UNDERGROUND) > 0)) {
					noTickets = false;
				}
			}
			return noTickets;
		}

		public Player pieceToPlayer(Piece piece) {
			if (piece.isDetective()){
				for(Player tempDetective : detectives) {
					if (tempDetective.piece() == piece) { return tempDetective;}
				}
			}
			return MrX;
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			// goes through each detective,
			Player currPlayer = pieceToPlayer(piece);
			if (!detectives.contains(currPlayer) && !piece.isMrX()) { return Optional.empty(); }
			TicketBoard ticketBoard = ticket -> (currPlayer.tickets()).get(ticket);
			 return Optional.of(ticketBoard);

		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return this.log;
		}

		@Nonnull
		@Override
		// if it's MrX's turn, no more moves, or if detectives have got to MrX
		public ImmutableSet<Piece> getWinner() {
			if  ((makeSingleMoves(setup, detectives, MrX, MrX.location()).isEmpty()) ||  (this.detectiveLocations().contains(MrX.location()))){
				return winner = ImmutableSet.copyOf(this.detectivePieces());
			}
			// else detectives no tickets, or all moves used up
			else if (((this.detectiveNoTickets()) || log.size() == setup.moves.size()) ) { return winner = ImmutableSet.of(MrX.piece()); }
			else { return winner = ImmutableSet.of();}
		}



		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			if (getWinner().isEmpty()) {
				return this.moves;
			}
			return ImmutableSet.of();
		}

		@Override
		public GameSetup getSetup() {
			return this.setup;
		}


		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> players = new HashSet<>();
			players.add(this.MrX.piece());
			players.addAll(detectivePieces());
			return ImmutableSet.copyOf(players);
		}


		@Nonnull
		private Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			Set<Move.SingleMove> possibleMoves = new HashSet<>();
			boolean occupied = false;
			for (int destination : setup.graph.adjacentNodes(source)) {
				occupied = false;
				if (detectiveLocations().contains(destination)) occupied = true;
				if (occupied) continue;
				for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if ((player.has(t.requiredTicket()))) {possibleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));}
					if (player.isMrX() && player.has(SECRET)) {possibleMoves.add(new Move.SingleMove(player.piece(), source, SECRET, destination));}}}
			return possibleMoves;
		}


		@Nonnull
		private Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			if ((this.MrX.has(DOUBLE) && player.isMrX()) && (setup.moves.size() >= 2)){
				Set<Move.SingleMove> possibleFirstMoves = makeSingleMoves(setup, detectives, player, source);
				Set<Move.DoubleMove> possibleDoubleMoves = new HashSet<>();
				for (Move.SingleMove singleMove : possibleFirstMoves) {
					Set<Move.SingleMove> possibleMoveTwo = makeSingleMoves(setup, detectives, player, singleMove.destination);
					for (Move.SingleMove moveTwo : possibleMoveTwo) {
						if ((moveTwo.ticket != singleMove.ticket) || (player.hasAtLeast(moveTwo.ticket, 2))) {
							possibleDoubleMoves.add(new Move.DoubleMove(player.piece(), source, singleMove.ticket, singleMove.destination, moveTwo.ticket, moveTwo.destination));}
					}
				}

				return possibleDoubleMoves;
			} else return null;
		}

		@Override
		// accept -> analyses visitor, then returns visit correct with pattern of visitor type
		// return move.accept ( case -> single case -> double) gamestate objects
		public GameState advance(Move move) {
			if (!this.moves.contains(move)) {
				throw new IllegalArgumentException("Illegal move: " + move);
			}
			// mr x's advance method
				return move.accept(new Move.Visitor<GameState>() {
					@Override
					public GameState visit(Move.SingleMove move) {
						Set<Piece> newRemaining = new HashSet<>(remaining);
						// mrx log stuff
						List<LogEntry> newLog = new ArrayList<>(log);
						Set<Player> newDetectives = new HashSet<>(detectives);
						if (move.commencedBy() == MrX.piece()) {
							if (setup.moves.get(log.size()) == (true)) {
								newLog.add(LogEntry.reveal((move.ticket), move.destination));
							} else if (setup.moves.get(log.size()) == false) {
								newLog.add(LogEntry.hidden(move.ticket));
							}
							// removed tickets
							MrX = MrX.use(move.ticket);
							// moved to new location
							MrX = MrX.at(move.destination);
						}
						else {
							Player player = pieceToPlayer(move.commencedBy());
							if (player.has(move.ticket)) {
								newDetectives.remove(player);
								player = player.use(move.ticket);
								MrX = MrX.give(move.ticket);
								player = player.at(move.destination);
								newDetectives.add(player);
							}
						}

						newRemaining.remove(move.commencedBy());
						if (newRemaining.isEmpty()) {
							newRemaining.addAll(detectivePieces());
							if (move.commencedBy().isDetective()) {
								newRemaining.add(MRX);
							}
						}
						GameState gameState = new MyGameState(setup, ImmutableSet.copyOf(newRemaining), ImmutableList.copyOf(newLog), MrX, ImmutableList.copyOf(newDetectives));
						return gameState;

					}

					@Override
					public GameState visit(Move.DoubleMove move) {
						// add to logs

						if (move.commencedBy().isDetective()) { return null; }
						List<LogEntry> newLog = new ArrayList<>(log);
						List<Boolean> newMoves = new ArrayList<>(setup.moves);
						if (newMoves.get(log.size()) == (true)) {
							newLog.add(LogEntry.reveal((move.ticket1), move.destination1));

						}
						else if (newMoves.get(log.size()) == (false)) {
							newLog.add(LogEntry.hidden(move.ticket1));
						}


						if (newMoves.get(newLog.size()) == (true)) {
							newLog.add(LogEntry.reveal((move.ticket2), move.destination2));
						}
						else if (newMoves.get(newLog.size()) == (false)) {
							newLog.add(LogEntry.hidden(move.ticket2));
						}


						GameSetup newGameSetup = new GameSetup(setup.graph, ImmutableList.copyOf(newMoves));

						// removed tickets
						MrX = MrX.use(move.ticket1);
						MrX = MrX.at(move.destination1);
						// moved to new location
						MrX = MrX.use(move.ticket2);
						MrX = MrX.at(move.destination2);
						MrX = MrX.use(DOUBLE);

						Set<Piece> newRemaining = new HashSet<>(remaining);
						newRemaining.remove(MRX);
						if (newRemaining.isEmpty()) {
							for (Player detective:detectives) {
								newRemaining.add(detective.piece()) ;
							}
						}

						GameState gameState = new MyGameState(newGameSetup, ImmutableSet.copyOf(newRemaining), ImmutableList.copyOf(newLog), MrX, detectives);
						return gameState;
					};
			});

		}
	}


	@Nonnull
	public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, detectives);


	}
}


