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

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelPromise
import org.xnio.IoFuture
import org.xnio.Option
import org.xnio.OptionMap
import org.xnio.StreamConnection
import org.xnio.XnioIoThread
import java.io.IOException
import java.net.SocketAddress
import java.util.concurrent.CancellationException

/**
  * {@link io.netty.channel.socket.SocketChannel} which uses XNIO.
  *
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
class XnioSocketChannel() extends AbstractXnioSocketChannel(null) {
  config.setTcpNoDelay(true)
  final private val options = OptionMap.builder
  private var channel: StreamConnection = null

  override protected def newUnsafe = new XnioSocketChannel#XnioUnsafe

  @throws[IOException]
  override protected def setOption0[T](option: Option[T], value: T): Unit = if (channel == null) options.set(option, value)
  else channel.setOption(option, value)

  @throws[IOException]
  override protected def getOption0[T](option: Option[T]): T = if (channel == null) options.getMap.get(option)
  else channel.getOption(option)

  override protected def connection: StreamConnection = channel

  @throws[Exception]
  override protected def doBind(localAddress: SocketAddress) = throw new UnsupportedOperationException("Not supported to bind in a separate step")

  final private class XnioUnsafe extends AbstractXnioSocketChannel#AbstractXnioUnsafe {
    override def connect(remoteAddress: SocketAddress, localAddress: SocketAddress, promise: ChannelPromise): Unit = {
      if (!ensureOpen(promise)) return
      val wasActive = isActive
      val thread = eventLoop.asInstanceOf[XnioEventLoop].ioThread
      var future:IoFuture[StreamConnection] = null
      if (localAddress == null) future = thread.openStreamConnection(remoteAddress, null, null, options.getMap)
      else future = thread.openStreamConnection(localAddress, remoteAddress, null, null, options.getMap)
      promise.addListener(new ChannelFutureListener() {
        @throws[Exception]
        override def operationComplete(channelFuture: ChannelFuture): Unit = if (channelFuture.isSuccess) if (!wasActive && isActive) pipeline.fireChannelActive
        else closeIfClosed()
      })
      future.addNotifier(new IoFuture.Notifier[StreamConnection, ChannelPromise]() {
        override def notify(ioFuture: IoFuture[_ <: StreamConnection], promise: ChannelPromise): Unit = {
          val status = ioFuture.getStatus
          if (status eq IoFuture.Status.DONE) try {
            channel = ioFuture.get
            channel.getSourceChannel.getReadSetter.set(new AbstractXnioSocketChannel#ReadListener)
            promise.setSuccess
            channel.getSourceChannel.resumeReads()
          } catch {
            case cause: Throwable =>
              promise.setFailure(cause)
          }
          else {
            var error:Exception = null
            if (status eq IoFuture.Status.FAILED) error = ioFuture.getException
            else error = new CancellationException
            promise.setFailure(error)
          }
        }
      }, promise)
    }
  }

}