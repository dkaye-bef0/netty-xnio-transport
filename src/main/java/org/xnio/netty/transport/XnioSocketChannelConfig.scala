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
import io.netty.channel.DefaultChannelConfig
import io.netty.channel.MessageSizeEstimator
import io.netty.channel.RecvByteBufAllocator
import io.netty.channel.socket.SocketChannelConfig
import org.xnio.Options

/**
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
final class XnioSocketChannelConfig private[transport](channel: AbstractXnioSocketChannel) extends DefaultChannelConfig(channel) with SocketChannelConfig {
  override def isTcpNoDelay: Boolean = channel.getOption(Options.TCP_NODELAY)

  override def setTcpNoDelay(tcpNoDelay: Boolean): SocketChannelConfig = {
    channel.setOption(Options.TCP_NODELAY, tcpNoDelay:java.lang.Boolean)
    this
  }

  override def getSoLinger = 0

  override def setSoLinger(soLinger: Int) = throw new UnsupportedOperationException

  override def getSendBufferSize: Int = channel.getOption(Options.SEND_BUFFER)

  override def setSendBufferSize(sendBufferSize: Int): SocketChannelConfig = {
    channel.setOption(Options.SEND_BUFFER, sendBufferSize:Integer)
    this
  }

  override def getReceiveBufferSize: Int = channel.getOption(Options.RECEIVE_BUFFER)

  override def setReceiveBufferSize(receiveBufferSize: Int): SocketChannelConfig = {
    channel.setOption(Options.RECEIVE_BUFFER, receiveBufferSize:Integer)
    this
  }

  override def isKeepAlive: Boolean = channel.getOption(Options.KEEP_ALIVE)

  override def setKeepAlive(keepAlive: Boolean): SocketChannelConfig = {
    channel.setOption(Options.KEEP_ALIVE, keepAlive:java.lang.Boolean)
    this
  }

  override def getTrafficClass: Int = channel.getOption(Options.IP_TRAFFIC_CLASS)

  override def setTrafficClass(trafficClass: Int): SocketChannelConfig = {
    channel.setOption(Options.IP_TRAFFIC_CLASS, trafficClass:Integer)
    this
  }

  override def isReuseAddress: Boolean = channel.getOption(Options.REUSE_ADDRESSES)

  override def setReuseAddress(reuseAddress: Boolean): SocketChannelConfig = {
    channel.setOption(Options.REUSE_ADDRESSES, reuseAddress:java.lang.Boolean)
    this
  }

  override def setPerformancePreferences(connectionTime: Int, latency: Int, bandwidth: Int) = throw new UnsupportedOperationException

  override def isAllowHalfClosure = false

  override def setAllowHalfClosure(allowHalfClosure: Boolean) = throw new UnsupportedOperationException

  override def setConnectTimeoutMillis(connectTimeoutMillis: Int): SocketChannelConfig = {
    super.setConnectTimeoutMillis(connectTimeoutMillis)
    this
  }

  override def setMaxMessagesPerRead(maxMessagesPerRead: Int): SocketChannelConfig = {
    super.setMaxMessagesPerRead(maxMessagesPerRead)
    this
  }

  override def setWriteSpinCount(writeSpinCount: Int): SocketChannelConfig = {
    super.setWriteSpinCount(writeSpinCount)
    this
  }

  override def setAllocator(allocator: ByteBufAllocator): SocketChannelConfig = {
    super.setAllocator(allocator)
    this
  }

  override def setRecvByteBufAllocator(allocator: RecvByteBufAllocator): SocketChannelConfig = {
    super.setRecvByteBufAllocator(allocator)
    this
  }

  override def setAutoRead(autoRead: Boolean): SocketChannelConfig = {
    super.setAutoRead(autoRead)
    this
  }

  override def setWriteBufferHighWaterMark(writeBufferHighWaterMark: Int): SocketChannelConfig = {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark)
    this
  }

  override def setMessageSizeEstimator(estimator: MessageSizeEstimator): SocketChannelConfig = {
    super.setMessageSizeEstimator(estimator)
    this
  }

  override def setWriteBufferLowWaterMark(writeBufferLowWaterMark: Int): SocketChannelConfig = {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark)
    this
  }

  override def setAutoClose(autoClose: Boolean): SocketChannelConfig = {
    super.setAutoClose(autoClose)
    this
  }
}