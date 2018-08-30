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

import io.netty.channel.EventLoop
import org.xnio._
import org.xnio.channels.{AcceptingChannel,ConnectedChannel}
import java.io.IOException
import java.net.SocketAddress

/**
  * {@link io.netty.channel.socket.ServerSocketChannel} which uses XNIO.
  *
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
final class XnioServerSocketChannel extends AbstractXnioServerSocketChannel {
  final private val options = OptionMap.builder
  private var channel: AcceptingChannel[_ <: ConnectedChannel] = null

  override protected def isCompatible(loop: EventLoop): Boolean = loop.isInstanceOf[XnioEventLoop]

  @throws[Exception]
  override protected def doBind(localAddress: SocketAddress): Unit = {
    val worker = this.eventLoop.asInstanceOf[XnioEventLoop].ioThread.getWorker
    // use the same thread count as the XnioWorker
    val map = options.set(Options.WORKER_IO_THREADS, worker.getIoThreadCount).getMap
    val eventLoop = this.eventLoop.asInstanceOf[XnioEventLoop]
    this.synchronized {
      channel = eventLoop.ioThread.getWorker.createStreamConnectionServer(localAddress, new AcceptListener, map)
    }
    // start accepting
    channel.resumeAccepts()
  }

  @throws[IOException]
  override protected def getOption0[T](option: Option[T]): T = {
    if (channel != null) return channel.getOption(option)
    options.getMap.get(option)
  }

  @throws[IOException]
  override protected def setOption0[T](option: Option[T], value: T): Unit = if (channel != null) channel.setOption(option, value)
  else options.set(option, value)

  override protected def xnioChannel: AcceptingChannel[_ <: ConnectedChannel] = channel
}