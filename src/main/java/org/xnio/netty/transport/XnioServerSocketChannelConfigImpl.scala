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

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelOption
import io.netty.channel.DefaultChannelConfig
import io.netty.channel.MessageSizeEstimator
import io.netty.channel.RecvByteBufAllocator
import org.xnio.Options
import java.util

/**
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
final class XnioServerSocketChannelConfigImpl private[transport](channel: AbstractXnioServerSocketChannel) extends DefaultChannelConfig(channel) with XnioServerSocketChannelConfig {
  override def getOptions: util.Map[ChannelOption[_], AnyRef] = getOptions(super.getOptions, XnioChannelOption.BALANCING_CONNECTIONS, XnioChannelOption.BALANCING_TOKENS, XnioChannelOption.CONNECTION_HIGH_WATER, XnioChannelOption.CONNECTION_LOW_WATER)

  @SuppressWarnings(Array("unchecked")) override def getOption[T](option: ChannelOption[T]): T = {
    if (option eq XnioChannelOption.BALANCING_CONNECTIONS) return Integer.valueOf(getBalancingConnections).asInstanceOf[T]
    if (option eq XnioChannelOption.BALANCING_TOKENS) return Integer.valueOf(getBalancingTokens).asInstanceOf[T]
    if (option eq XnioChannelOption.CONNECTION_HIGH_WATER) return Integer.valueOf(getConnectionHighWater).asInstanceOf[T]
    if (option eq XnioChannelOption.CONNECTION_LOW_WATER) return Integer.valueOf(getConnectionLowWater).asInstanceOf[T]
    super.getOption(option)
  }

  override def setOption[T](option: ChannelOption[T], value: T): Boolean = {
    validate(option, value)
    if (option eq XnioChannelOption.BALANCING_CONNECTIONS) setBalancingConnections(value.asInstanceOf[Integer])
    else if (option eq XnioChannelOption.BALANCING_TOKENS) setBalancingTokens(value.asInstanceOf[Integer])
    else if (option eq XnioChannelOption.CONNECTION_HIGH_WATER) setConnectionHighWater(value.asInstanceOf[Integer])
    else if (option eq XnioChannelOption.CONNECTION_LOW_WATER) setConnectionLowWater(value.asInstanceOf[Integer])
    else return super.setOption(option, value)
    true
  }

  override def getBacklog: Int = channel.getOption(Options.BACKLOG)

  override def setBacklog(backlog: Int): XnioServerSocketChannelConfig = {
    channel.setOption(Options.BACKLOG, backlog:Integer)
    this
  }

  override def isReuseAddress: Boolean = channel.getOption(Options.REUSE_ADDRESSES)

  override def setReuseAddress(reuseAddress: Boolean): XnioServerSocketChannelConfig = {
    channel.setOption(Options.REUSE_ADDRESSES, reuseAddress:java.lang.Boolean)
    this
  }

  override def getReceiveBufferSize: Int = channel.getOption(Options.RECEIVE_BUFFER)

  override def setReceiveBufferSize(receiveBufferSize: Int): XnioServerSocketChannelConfig = {
    channel.setOption(Options.RECEIVE_BUFFER, receiveBufferSize:Integer)
    this
  }

  override def setPerformancePreferences(connectionTime: Int, latency: Int, bandwidth: Int) = throw new UnsupportedOperationException

  override def setConnectTimeoutMillis(connectTimeoutMillis: Int): XnioServerSocketChannelConfig = {
    super.setConnectTimeoutMillis(connectTimeoutMillis)
    this
  }

  override def setMaxMessagesPerRead(maxMessagesPerRead: Int): XnioServerSocketChannelConfig = {
    super.setMaxMessagesPerRead(maxMessagesPerRead)
    this
  }

  override def setWriteSpinCount(writeSpinCount: Int): XnioServerSocketChannelConfig = {
    super.setWriteSpinCount(writeSpinCount)
    this
  }

  override def setAllocator(allocator: ByteBufAllocator): XnioServerSocketChannelConfig = {
    super.setAllocator(allocator)
    this
  }

  override def setRecvByteBufAllocator(allocator: RecvByteBufAllocator): XnioServerSocketChannelConfig = {
    super.setRecvByteBufAllocator(allocator)
    this
  }

  override def setAutoRead(autoRead: Boolean): XnioServerSocketChannelConfig = {
    super.setAutoRead(autoRead)
    this
  }

  override def setWriteBufferHighWaterMark(writeBufferHighWaterMark: Int): XnioServerSocketChannelConfig = {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark)
    this
  }

  override def setWriteBufferLowWaterMark(writeBufferLowWaterMark: Int): XnioServerSocketChannelConfig = {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark)
    this
  }

  override def setMessageSizeEstimator(estimator: MessageSizeEstimator): XnioServerSocketChannelConfig = {
    super.setMessageSizeEstimator(estimator)
    this
  }

  override def setAutoClose(autoClose: Boolean): XnioServerSocketChannelConfig = {
    super.setAutoClose(autoClose)
    this
  }

  override def setConnectionHighWater(connectionHighWater: Int): XnioServerSocketChannelConfig = {
    channel.setOption(Options.CONNECTION_HIGH_WATER, connectionHighWater:Integer)
    this
  }

  override def setConnectionLowWater(connectionLowWater: Int): XnioServerSocketChannelConfig = {
    channel.setOption(Options.CONNECTION_LOW_WATER, connectionLowWater:Integer)
    this
  }

  override def setBalancingTokens(balancingTokens: Int): XnioServerSocketChannelConfig = {
    channel.setOption(Options.BALANCING_TOKENS, balancingTokens:Integer)
    this
  }

  override def setBalancingConnections(connections: Int): XnioServerSocketChannelConfig = {
    channel.setOption(Options.BALANCING_CONNECTIONS, connections:Integer)
    this
  }

  override def getConnectionHighWater: Int = channel.getOption(Options.CONNECTION_HIGH_WATER)

  override def getConnectionLowWater: Int = channel.getOption(Options.CONNECTION_LOW_WATER)

  override def getBalancingTokens: Int = channel.getOption(Options.BALANCING_TOKENS)

  override def getBalancingConnections: Int = channel.getOption(Options.BALANCING_CONNECTIONS)
}