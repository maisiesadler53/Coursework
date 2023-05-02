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

			// initialise factory, state and observerList as in observer pattern
			MyGameStateFactory factory = new MyGameStateFactory();
			Board.GameState state = factory.build(setup, mrX, detectives);
			List<Model.Observer> observerList = new ArrayList<>();
			@Nonnull
			@Override
			public Board getCurrentBoard() {
				return state;
			}
			@Override

			// add observer to list
			public void registerObserver(Observer observer) {
				if (observer == null) { throw new NullPointerException("Null observer"); }
				if (observerList.contains(observer)) { throw new IllegalArgumentException("Observer already registered");}
				else { observerList.add(observer);}

			}


			// remove observer from list
			@Override
			public void unregisterObserver(Observer observer) {
				if (observer == null) { throw new NullPointerException("Null observer"); }
				if (! observerList.contains(observer)) { throw new IllegalArgumentException("Observer not previously registered");}
				else  { observerList.remove(observer); }
			}

			// return list of observers
			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return ImmutableSet.copyOf(observerList);
			}

			// continue the game method
			@Override
			public void chooseMove(@Nonnull Move move){
				// make move using current state
				state = state.advance(move);
				for (Observer observer : observerList) {
					// move made if no winner
					if (state.getWinner().isEmpty()) { observer.onModelChanged(state, Observer.Event.MOVE_MADE); }
					// if winner, then game over
					else if (! state.getWinner().isEmpty()) { observer.onModelChanged(state, Observer.Event.GAME_OVER);}
				}
				// update current state to allow continuation
			}
		};
	}



}