// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.lattice.model;

import io.vlingo.symbio.Source;

/**
 * A abstract base for commands, which are considered a type of {@code Source}.
 */
public abstract class Command extends Source<Command> {
  /**
   * Construct my default state with a type version of 1.
   */
  protected Command() {
    super();
  }

  /**
   * Construct my default state with a {@code commandTypeVersion} greater than 1.
   * @param commandTypeVersion the int version of this command type
   */
  protected Command(final int commandTypeVersion) {
    super(commandTypeVersion);
  }
}
