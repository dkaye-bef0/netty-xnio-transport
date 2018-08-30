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

import org.xnio.Option
import org.xnio.XnioIoThread
import org.xnio.channels.{AcceptingChannel,ConnectedChannel}
import java.io.IOException
import java.net.SocketAddress

/**
  * {@link AbstractXnioServerSocketChannel} implementation which allows to use a {@link AcceptingChannel} and wrap it.
  *
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
final class WrappingXnioServerSocketChannel @SuppressWarnings(Array("unchecked"))(val channel: AcceptingChannel[_ <: ConnectedChannel])

/**
  * Create a new instance wrapping the given {@link AcceptingChannel}
  */
  extends AbstractXnioServerSocketChannel with IoThreadPowered {
  if (channel == null) throw new NullPointerException("channel")
  thread = channel.getIoThread
  channel.getAcceptSetter.set(new AcceptListener)
  // register a EventLoop and start read
  unsafe.register(new XnioEventLoop(channel.getWorker.getIoThread), unsafe.voidPromise)
  read
  final private var thread:XnioIoThread = null

  override def ioThread: XnioIoThread = thread

  @throws[IOException]
  override protected def setOption0[T](option: Option[T], value: T): Unit = channel.setOption(option, value)

  @throws[IOException]
  override protected def getOption0[T](option: Option[T]): T = channel.getOption(option)

  @throws[Exception]
  override protected def doBind(localAddress: SocketAddress) = throw new UnsupportedOperationException("Wrapped XNIO Channel")

  override protected def xnioChannel: AcceptingChannel[_ <: ConnectedChannel] = channel
}