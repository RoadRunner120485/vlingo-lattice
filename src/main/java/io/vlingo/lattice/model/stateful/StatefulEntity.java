// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.lattice.model.stateful;

import java.util.function.Supplier;

import io.vlingo.actors.Actor;
import io.vlingo.common.Outcome;
import io.vlingo.lattice.model.CompletionSupplier;
import io.vlingo.lattice.model.stateful.StatefulTypeRegistry.Info;
import io.vlingo.symbio.Metadata;
import io.vlingo.symbio.State;
import io.vlingo.symbio.store.Result;
import io.vlingo.symbio.store.StorageException;
import io.vlingo.symbio.store.state.StateStore.ReadResultInterest;
import io.vlingo.symbio.store.state.StateStore.WriteResultInterest;

/**
 * Abstract base for all entity types that require the id-clob/blob state storage
 * typical of a CQRS Command Model and CQRS Query Model. Therefore, extend me
 * for both your Command Model and CQRS Query Model, or for your CQRS Query Model
 * only when your Command Model uses the {@code EventSourced} or {@code EventSourced}.
 */
public abstract class StatefulEntity<S> extends Actor
    implements ReadResultInterest, WriteResultInterest {

  private int currentVersion;
  private final Info<S,State<?>> info;
  private final ReadResultInterest readInterest;
  private final WriteResultInterest writeInterest;

  /**
   * Construct my default state.
   */
  protected StatefulEntity() {
    this.currentVersion = 0;
    this.info = stage().world().resolveDynamic(StatefulTypeRegistry.INTERNAL_NAME, StatefulTypeRegistry.class).info(stateType());
    this.readInterest = selfAs(ReadResultInterest.class);
    this.writeInterest = selfAs(WriteResultInterest.class);
  }

  /*
   * @see io.vlingo.actors.Actor#start()
   */
  @Override
  public void start() {
    super.start();

    restore(true); // ignore not found (possible first time start)
  }

  /**
   * Answer my currentVersion, which, if zero, indicates that the
   * receiver is being initially constructed or reconstituted.
   * @return int
   */
  protected int currentVersion() {
    return currentVersion;
  }

  /**
   * Answer my unique identity, which much be provided by
   * my concrete extender by overriding.
   * @return String
   */
  protected abstract String id();

  /**
   * Preserve my current {@code state} and {@code metadataValye} that was modified
   * due to the descriptive {@code operation} and supply an eventual outcome by means
   * of the given {@code andThen} function.
   * @param state the S typed state to preserve
   * @param metadataValue the String metadata value to preserve along with the state
   * @param operation the String descriptive name of the operation that caused the state modification
   * @param andThen the {@code Supplier<RT>} that will provide the fully updated state following this operation,
   * and which will used to answer an eventual outcome to the client of this entity
   * @param <RT> the return type of the Supplier function, which is the type of the completed state
   */
  protected <RT> void preserve(final S state, final String metadataValue, final String operation, final Supplier<RT> andThen) {
    final Metadata metadata = Metadata.with(state, metadataValue == null ? "" : metadataValue, operation == null ? "" : operation);
    stowMessages(WriteResultInterest.class);
    info.store.write(id(), state, nextVersion(), metadata, writeInterest, CompletionSupplier.supplierOrNull(andThen, completesEventually()));
  }

  /**
   * Preserve my current {@code state} that was modified due to the descriptive {@code operation}
   * and supply an eventual outcome by means of the given {@code andThen} function.
   * @param state the S typed state to preserve
   * @param operation the String descriptive name of the operation that caused the state modification
   * @param andThen the {@code Supplier<RT>} that will provide the fully updated state following this operation,
   * and which will used to answer an eventual outcome to the client of this entity
   * @param <RT> the return type of the Supplier function, which is the type of the completed state
   */
  protected <RT> void preserve(final S state, final String operation, final Supplier<RT> andThen) {
    preserve(state, "", operation, andThen);
  }

  /**
   * Preserve my current {@code state} and supply an eventual outcome by means of the given
   * {@code andThen} function.
   * @param state the S typed state to preserve
   * @param andThen the {@code Supplier<RT>} that will provide the fully updated state following this operation,
   * and which will used to answer an eventual outcome to the client of this entity
   * @param <RT> the return type of the Supplier function, which is the type of the completed state
   */
  protected <RT> void preserve(final S state, final Supplier<RT> andThen) {
    preserve(state, "", "", andThen);
  }

  /**
   * Preserve my current {@code state} and {@code metadataValye} that was modified
   * due to the descriptive {@code operation}.
   * @param state the S typed state to preserve
   * @param metadataValue the String metadata value to preserve along with the state
   * @param operation the String descriptive name of the operation that caused the state modification
   */
  protected void preserve(final S state, final String metadataValue, final String operation) {
    preserve(state, metadataValue, operation, null);
  }

  /**
   * Preserve my current {@code state} that was modified due to the descriptive {@code operation}.
   * @param state the S typed state to preserve
   * @param operation the String descriptive name of the operation that caused the state modification
   */
  protected void preserve(final S state, final String operation) {
    preserve(state, "", operation, null);
  }

  /**
   * Preserve my current {@code state}.
   * @param state the S typed state to preserve
   */
  protected void preserve(final S state) {
    preserve(state, "", "", null);
  }

  /**
   * Restore my current state, dispatching to {@code state(final S state)} when completed.
   */
  protected void restore() {
    restore(false);
  }

  /**
   * Received by my extender when my current state has been preserved and restored.
   * Must be overridden by my extender.
   * @param state the S typed state
   */
  protected abstract void state(final S state);

  /**
   * Received by my extender when I must know it's state type.
   * Must be overridden by my extender.
   * @return {@code Class<S>}
   */
  protected abstract Class<S> stateType();

  //=====================================
  // FOR INTERNAL USE ONLY.
  //=====================================

  /*
   * @see io.vlingo.symbio.store.state.StateStore.ReadResultInterest#readResultedIn(io.vlingo.common.Outcome, java.lang.String, java.lang.Object, int, io.vlingo.symbio.Metadata, java.lang.Object)
   */
  @Override
  @SuppressWarnings("unchecked")
  final public <ST> void readResultedIn(final Outcome<StorageException, Result> outcome, final String id, final ST state, final int stateVersion, final Metadata metadata, final Object object) {
    outcome
      .andThen(result -> {
        state((S) state);
        currentVersion = stateVersion;
        disperseStowedMessages();
        return result;
      })
      .otherwise(cause -> {
        final String message = "State not restored for: " + getClass() + "(" + id + ") because: " + cause.result + " with: " + cause.getMessage();
        logger().log(message, cause);
        throw new IllegalStateException(message, cause);
      });
  }

  /*
   * @see io.vlingo.symbio.store.state.StateStore.WriteResultInterest#writeResultedIn(io.vlingo.common.Outcome, java.lang.String, java.lang.Object, int, java.lang.Object)
   */
  @Override
  @SuppressWarnings("unchecked")
  final public <ST> void writeResultedIn(final Outcome<StorageException, Result> outcome, final String id, final ST state, final int stateVersion, final Object supplier) {
    outcome
      .andThen(result -> {
        state((S) state);
        currentVersion = stateVersion;
        completeUsing(supplier);
        disperseStowedMessages();
        return result;
      })
      .otherwise(cause -> {
        final String message = "State not preserved for: " + getClass() + "(" + id + ") because: " + cause.result + " with: " + cause.getMessage();
        logger().log(message, cause);
        throw new IllegalStateException(message, cause);
      });
  }

  /**
   * Dispatches to the {@code supplier} to complete my protocol
   * message that answers an eventual outcome.
   * @param supplier the Object that is cast to a {@code CompletionSupplier<?>} and then completed
   */
  private void completeUsing(final Object supplier) {
    if (supplier != null) {
      ((CompletionSupplier<?>) supplier).complete();
    }
  }

  /**
   * Answer my {@code nextVersion}, which is one greater than my {@code currentVersion}.
   * @return int
   */
  private int nextVersion() {
    return currentVersion + 1;
  }

  /**
   * Cause state restoration and indicate whether a not found
   * condition can be safely ignored.
   * @param ignoreNotFound the boolean indicating whether or not a not found condition may be ignored
   */
  private void restore(final boolean ignoreNotFound) {
    stowMessages(ReadResultInterest.class);
    info.store.read(id(), info.storeType, readInterest);
  }
}
