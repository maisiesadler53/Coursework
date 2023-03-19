package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableSet;
import com.sun.media.jfxmedia.events.PlayerStateEvent;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.sql.Array;
import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
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
			for (int i = 0; i < detectives.size(); i++) {
				for (int j = i + 1; j < detectives.size(); j++) {
					if (detectives.get(i).piece() == detectives.get(j).piece())
						throw new IllegalArgumentException("2 detectives are the same colour");
					if (detectives.get(i).location() == detectives.get(j).location())
						throw new IllegalArgumentException("2 detectives are the same colour");
				}
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

		public ImmutableSet<Move> makeMoves() {
			List<Move> tempmoves = new ArrayList<>();
			if(this.remaining.containsAll(detectives)) {
				for (Player temp : this.detectives) {
					tempmoves.addAll((makeSingleMoves(this.setup, this.detectives, temp, temp.location())));
				}
			}

			else if (this.remaining.contains(MrX) && !this.remaining.containsAll(detectives)){
				tempmoves.addAll(makeSingleMoves(this.setup, this.detectives, MrX, MrX.location()));
				tempmoves.addAll(makeDoubleMoves(this.setup, this.detectives, MrX, MrX.location()));
			}

			return ImmutableSet.copyOf(tempmoves);

		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			for (Player temp : detectives) {
				TicketBoard ticketboard = new TicketBoard() {
					public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
						return (temp.tickets()).get(ticket);
					}
				};
				if (temp.piece() == piece) return Optional.of(ticketboard);
			}
			TicketBoard ticketboard = new TicketBoard() {
				public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
					return (MrX.tickets()).get(ticket);
				}
			};
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
			return ImmutableSet.of();
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return this.moves;
		}

		@Override
		public GameSetup getSetup() {
			return this.setup;
		}


		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> temp = new HashSet<>();
			temp.add(this.MrX.piece());
			;
			for (Player detective : this.detectives) {
				temp.add(detective.piece());
			}
			return ImmutableSet.copyOf(temp);

		}


		@Nonnull
		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			Set<Move.SingleMove> possibleMoves = new HashSet<>();
			boolean occupied = false;
			for (int destination : setup.graph.adjacentNodes(source)) {
				for (Player temp : detectives) {
					if (temp.location() == destination) occupied = true;
				}

				if (occupied) continue;
				for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if ((player.tickets().containsValue(t.requiredTicket()))) {
						possibleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
					}
					else if (player.isMrX() && player.has(SECRET)) {
						possibleMoves.add(new Move.SingleMove(player.piece(), source, SECRET, destination));
					}
				}

			}
			return possibleMoves;
		}


		@Nonnull
		private Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			if (player.has(DOUBLE) && player.isMrX()) {
				Set<Move.SingleMove> possibleMoves = makeSingleMoves(setup, detectives, player, source);
				Set<Move.DoubleMove> possibleDoubleMoves = new HashSet<>();
				for (Move.SingleMove singleMove : possibleMoves) {
					Set<Move.SingleMove> possibleMoveTwo = makeSingleMoves(setup, detectives, player, singleMove.destination);
					for (Move.SingleMove moveTwo : possibleMoveTwo) {
						if ((moveTwo.ticket != singleMove.ticket) || (player.hasAtLeast(moveTwo.ticket, 2))) {
							possibleDoubleMoves.add(new Move.DoubleMove(player.piece(), source, singleMove.ticket, singleMove.destination, moveTwo.ticket, moveTwo.destination));
						}
					}
				}
				for (Move.DoubleMove moves : possibleDoubleMoves) {
					System.out.println(moves.toString());
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
			if (move.commencedBy() == MrX.piece()) {
				return move.accept(new Move.Visitor<GameState>() {
					@Override
					public GameState visit(Move.SingleMove move) {
						// add to log - hidden and reveals
						if (setup.moves.contains(true)) {
							log.add(LogEntry.reveal((move.ticket), move.destination));
						} else if (setup.moves.contains(false)) {
							log.add(LogEntry.hidden(move.ticket));
						}

						// removed tickets
						MrX.use(move.ticket);
						// moved to new location
						MrX.at(move.destination);

						remaining.remove(Piece.MrX.MRX);

						GameState gameState = new MyGameState(setup, remaining, log, MrX, detectives);
						return gameState;

					}

					@Override
					public GameState visit(Move.DoubleMove move) {
						// add to logs
						if (setup.moves.contains(true)) {
							log.add(LogEntry.reveal((move.ticket1), move.destination1));
						} else if (setup.moves.contains(false)) {
							log.add(LogEntry.hidden(move.ticket1));
							log.add(LogEntry.hidden(move.ticket2));
						}

						// removed tickets
						MrX.use(move.ticket1);
						MrX.at(move.destination1);
						// moved to new location
						MrX.use(move.ticket2);
						MrX.at(move.destination2);

						remaining.remove(Piece.MrX.MRX);

						GameState gameState = new MyGameState(setup, remaining, log, MrX, detectives);
						return gameState;
					}
				});
			}

			if (move.commencedBy().isDetective()) {
				return move.accept(new Move.Visitor<GameState>() {
					@Override
					public GameState visit(Move.SingleMove move) {
						// removed tickets
						for (Player temp : detectives) {
							if (temp.piece() == move.commencedBy()) ;
							{
								Player player = temp;
								player.use(move.ticket);
								MrX.give(move.ticket);
								player.at(move.destination);
							}
						}

						remaining.remove(move.commencedBy());

						GameState gameState = new MyGameState(setup, remaining, log, MrX, detectives);
						return gameState;
					}

					public GameState visit(Move.DoubleMove move) {
						return null;
					}

				});
			}

			else return null;

		}
	}


	@Nonnull
	@Override
	public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);


	}
}


