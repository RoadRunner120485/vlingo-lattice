// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.lattice.exchange;

import io.vlingo.common.message.Message;

public class ExchangeMessage implements Message {
  public final String type;
  public final String payload;

  public ExchangeMessage(final String type, final String payload) {
    this.type = type;
    this.payload = payload;
  }

  @Override
  public String toString() {
    return "ExchangeMessage[type=" + type + " payload=" + payload + "]";
  }
}
