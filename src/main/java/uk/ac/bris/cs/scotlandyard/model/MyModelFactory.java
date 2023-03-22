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
		MyGameStateFactory factory = new MyGameStateFactory();
		Board.GameState state = factory.build(setup, mrX, detectives);
		// observers - works
		List<Model.Observer> observerList = new ArrayList<>();


	return new Model() {


		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;

		}
		@Override
		public void registerObserver(Observer observer) {
			if (observer == null) { throw new NullPointerException("Null observer"); }

			if (observerList.contains(observer)) {
				throw new IllegalArgumentException("Observer already registered");
			}
			else {
				observerList.add(observer);
			}

		}

		@Override
		public void unregisterObserver(Observer observer) {
			if (observer == null) { throw new NullPointerException("Null observer"); }

			if (! observerList.contains(observer)) {
				throw new IllegalArgumentException("Observer not previously registered");
			}
			else  {
				observerList.remove(observer);
			}
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observerList);
		}

		@Override
		public void chooseMove(@Nonnull Move move){
			state.advance(move);
			for (Observer observer : observerList) {
				if(!state.getWinner().isEmpty()) {
					observer.onModelChanged(state, Observer.Event.MOVE_MADE);
				}
				else {observer.onModelChanged(state, Observer.Event.MOVE_MADE); }
			}
		}
	};
	}



}
