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

import io.netty.channel.ChannelPromise
import org.xnio.Option
import org.xnio.StreamConnection
import org.xnio.XnioIoThread
import org.xnio.channels.AcceptingChannel
import java.io.IOException
import java.net.SocketAddress

/**
  * {@link AbstractXnioSocketChannel} implementation which allows you to wrap a pre-created XNIO channel.
  *
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
final class WrappingXnioSocketChannel private[transport](parent: AbstractXnioServerSocketChannel, val channel: StreamConnection) extends AbstractXnioSocketChannel(parent) with IoThreadPowered {
  if (channel == null) throw new NullPointerException("channel")
  this.thread = channel.getIoThread
  config.setTcpNoDelay(true)
  channel.getSourceChannel.getReadSetter.set(new AbstractXnioSocketChannel#ReadListener)
  final private var thread:XnioIoThread = null

  /**
    * Create a new {@link WrappingXnioSocketChannel} which was created via the given {@link AcceptingChannel} and uses
    * the given {@link StreamConnection} under the covers.
    */
  def this(parent: AcceptingChannel[StreamConnection], channel: StreamConnection) {
    this(new WrappingXnioServerSocketChannel(parent), channel)
    // register a EventLoop and start read
    unsafe.register(new XnioEventLoop(thread), unsafe.voidPromise)
    read
  }

  /**
    * Create a {@link WrappingXnioSocketChannel} which uses the given {@link StreamConnection} under the covers.
    */
  def this(channel: StreamConnection) {
    this(null.asInstanceOf[AbstractXnioServerSocketChannel], channel)
    unsafe.register(new XnioEventLoop(thread), unsafe.voidPromise)
    read
  }

  override def ioThread: XnioIoThread = thread

  override protected def newUnsafe = new WrappingXnioSocketChannel#XnioUnsafe

  @throws[Exception]
  override protected def doBind(localAddress: SocketAddress) = throw new UnsupportedOperationException("Wrapped XNIO Channel")

  final private class XnioUnsafe extends AbstractXnioSocketChannel#AbstractXnioUnsafe {
    override def connect(remoteAddress: SocketAddress, localAddress: SocketAddress, promise: ChannelPromise): Unit = promise.setFailure(new UnsupportedOperationException("Wrapped XNIO Channel"))
  }

  @throws[IOException]
  override protected def setOption0[T](option: Option[T], value: T): Unit = channel.setOption(option, value)

  @throws[IOException]
  override protected def getOption0[T](option: Option[T]): T = channel.getOption(option)

  override protected def connection: StreamConnection = channel
}