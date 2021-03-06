// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.lattice.exchange;

import io.vlingo.common.message.Message;

public class ExternalType1 implements Message {
  public final String field1;
  public final String field2;

  public ExternalType1(final String value1, final int value2) {
    this.field1 = value1;
    this.field2 = Integer.toString(value2);
  }

  @Override
  public String toString() {
    return "ExternalType[field1=" + field1 + " field2=" + field2 + "]";
  }
}
