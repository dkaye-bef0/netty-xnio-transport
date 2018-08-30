/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 *//*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package org.xnio.netty.transport

import io.netty.channel.ChannelOption

/**
  * {@link ChannelOption}'s specific for the XNIO transport.
  *
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
object XnioChannelOption {
  import ChannelOption.valueOf
  /**
    * @see { @link org.xnio.Options#CONNECTION_HIGH_WATER}
    */
    val CONNECTION_HIGH_WATER: ChannelOption[Integer] = valueOf("CONNECTION_HIGH_WATER")
  /**
    * @see { @link org.xnio.Options#CONNECTION_LOW_WATER}
    */
  val CONNECTION_LOW_WATER: ChannelOption[Integer] = valueOf("CONNECTION_LOW_WATER")
  /**
    * @see { @link org.xnio.Options#BALANCING_TOKENS}
    */
  val BALANCING_TOKENS: ChannelOption[Integer] = valueOf("BALANCING_TOKENS")
  /**
    * @see { @link org.xnio.Options#BALANCING_CONNECTIONS}
    */
  val BALANCING_CONNECTIONS: ChannelOption[Integer] = valueOf("BALANCING_CONNECTIONS")
}

final class XnioChannelOption[T] @SuppressWarnings(Array("unused")) private(name: String) extends ChannelOption[T](name) {
}