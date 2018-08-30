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
import io.netty.channel.MessageSizeEstimator
import io.netty.channel.RecvByteBufAllocator
import io.netty.channel.socket.ServerSocketChannelConfig

/**
  * {@link ServerSocketChannelConfig} which expose configuration settings which are specific to the XNIO transport.
  *
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
trait XnioServerSocketChannelConfig extends ServerSocketChannelConfig {
  /**
    * @see { @link XnioChannelOption#CONNECTION_HIGH_WATER}
    */
    def setConnectionHighWater(connectionHighWater: Int): XnioServerSocketChannelConfig

  def getConnectionHighWater: Int

  /**
    * @see { @link XnioChannelOption#CONNECTION_LOW_WATER}
    */
  def setConnectionLowWater(connectionLowWater: Int): XnioServerSocketChannelConfig

  def getConnectionLowWater: Int

  /**
    * @see { @link XnioChannelOption#BALANCING_TOKENS}
    */
  def setBalancingTokens(balancingTokens: Int): XnioServerSocketChannelConfig

  def getBalancingTokens: Int

  /**
    * @see { @link XnioChannelOption#BALANCING_CONNECTIONS}
    */
  def setBalancingConnections(connections: Int): XnioServerSocketChannelConfig

  def getBalancingConnections: Int

  override def setConnectTimeoutMillis(connectTimeoutMillis: Int): XnioServerSocketChannelConfig

  override def setMaxMessagesPerRead(maxMessagesPerRead: Int): XnioServerSocketChannelConfig

  override def setWriteSpinCount(writeSpinCount: Int): XnioServerSocketChannelConfig

  override def setAllocator(allocator: ByteBufAllocator): XnioServerSocketChannelConfig

  override def setRecvByteBufAllocator(allocator: RecvByteBufAllocator): XnioServerSocketChannelConfig

  override def setAutoRead(autoRead: Boolean): XnioServerSocketChannelConfig

  @deprecated override def setAutoClose(autoClose: Boolean): XnioServerSocketChannelConfig

  override def setWriteBufferHighWaterMark(writeBufferHighWaterMark: Int): XnioServerSocketChannelConfig

  override def setWriteBufferLowWaterMark(writeBufferLowWaterMark: Int): XnioServerSocketChannelConfig

  override def setMessageSizeEstimator(estimator: MessageSizeEstimator): XnioServerSocketChannelConfig

  override def setBacklog(backlog: Int): XnioServerSocketChannelConfig

  override def setReuseAddress(reuseAddress: Boolean): XnioServerSocketChannelConfig

  override def setReceiveBufferSize(receiveBufferSize: Int): XnioServerSocketChannelConfig

  override def setPerformancePreferences(connectionTime: Int, latency: Int, bandwidth: Int): XnioServerSocketChannelConfig
}