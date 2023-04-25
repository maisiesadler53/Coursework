package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {


	@Nonnull @Override public Model build(GameSetup setup,
										  Player mrX,
										  ImmutableList<Player> detectives) {
		// new factory, state

		return new Model() {
			MyGameStateFactory factory = new MyGameStateFactory();
			// first board built  (State)
			Board.GameState state = factory.build(setup, mrX, detectives);
			// observers - works
			List<Model.Observer> observerList = new ArrayList<>();
			@Nonnull
			@Override
			public Board getCurrentBoard() {
				return state;
			}
			@Override
			public void registerObserver(Observer observer) {
				if (observer == null) { throw new NullPointerException("Null observer"); }
				if (observerList.contains(observer)) { throw new IllegalArgumentException("Observer already registered");}
				else { observerList.add(observer);}

			}

			@Override
			public void unregisterObserver(Observer observer) {
				if (observer == null) { throw new NullPointerException("Null observer"); }
				if (! observerList.contains(observer)) { throw new IllegalArgumentException("Observer not previously registered");}
				else  { observerList.remove(observer); }
			}
			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return ImmutableSet.copyOf(observerList);
			}

			@Override
			public void chooseMove(@Nonnull Move move){
				// first move
				// error : not computing new moves once advanced
				// new state -> advanced -> MRx first advance - works as normal
				// chooseMove called again -> advance called -> state does not include ?
				Board.GameState newState = state.advance(move);
				for (Observer observer : observerList) {
					// if the new get winner is empty -> so no win
					if(newState.getWinner().isEmpty()) {
						observer.onModelChanged(newState, Observer.Event.MOVE_MADE);
					}
					else if(! newState.getWinner().isEmpty()) {
						observer.onModelChanged(newState, Observer.Event.GAME_OVER);
					}
				}
				this.state = newState;
			}
		};
	}



}