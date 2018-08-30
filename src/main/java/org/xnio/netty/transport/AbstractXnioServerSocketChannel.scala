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

import io.netty.channel.AbstractServerChannel
import io.netty.channel.ChannelException
import io.netty.channel.EventLoop
import io.netty.channel.socket.ServerSocketChannel
import org.xnio.ChannelListener
import org.xnio.Option
import org.xnio.StreamConnection
import org.xnio.channels.{AcceptingChannel,ConnectedChannel}
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress

/**
  * {@link ServerSocketChannel} base class for our XNIO transport
  *
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
object AbstractXnioServerSocketChannel {
  private val connections = new ThreadLocal[Array[StreamConnection]]

  private def connectionsArray(size: Int) = {
    var array = connections.get
    if (array == null || array.length < size) {
      array = new Array[StreamConnection](size)
      connections.set(array)
    }
    array
  }
}

abstract class AbstractXnioServerSocketChannel extends AbstractServerChannel with ServerSocketChannel {
  override val config = new XnioServerSocketChannelConfigImpl(this)

  override protected def isCompatible(loop: EventLoop): Boolean = loop.isInstanceOf[XnioEventLoop]

  override def isActive: Boolean = isOpen

  override def localAddress: InetSocketAddress = super.localAddress.asInstanceOf[InetSocketAddress]

  override def remoteAddress: InetSocketAddress = super.remoteAddress.asInstanceOf[InetSocketAddress]

  private[transport] def getOption[T](option: Option[T]) = try getOption0(option)
  catch {
    case e: IOException =>
      throw new ChannelException(e)
  }

  private[transport] def setOption[T](option: Option[T], value: T) = try setOption0(option, value)
  catch {
    case e: IOException =>
      throw new ChannelException(e)
  }

  override protected def localAddress0: SocketAddress = {
    val channel = xnioChannel
    if (channel == null) return null
    channel.getLocalAddress
  }

  @throws[Exception]
  override protected def doClose(): Unit = {
    val channel = xnioChannel
    if (channel == null) return
    channel.suspendAccepts()
    channel.close()
  }

  @throws[Exception]
  override protected def doBeginRead(): Unit = {
    val channel = xnioChannel
    if (channel == null) return
    channel.resumeAccepts()
  }

  override def isOpen: Boolean = {
    val channel = xnioChannel
    channel == null || channel.isOpen
  }

  /**
    * Return the underyling {@link AcceptingChannel}
    */
  protected def xnioChannel: AcceptingChannel[_ <: ConnectedChannel]

  /**
    * Set the given {@link Option} to the given value.
    */
  @throws[IOException]
  protected def setOption0[T](option: Option[T], value: T): Unit

  /**
    * Return the value for the given {@link Option}.
    */
  @throws[IOException]
  protected def getOption0[T](option: Option[T]): T

  /**
    * {@link ChannelListener} implementation which takes care of accept connections and fire them through the
    * {@link io.netty.channel.ChannelPipeline}.
    */
  final class AcceptListener extends ChannelListener[AcceptingChannel[StreamConnection]] {
    override def handleEvent(channel: AcceptingChannel[StreamConnection]): Unit = {
      if (!config.isAutoRead) channel.suspendAccepts()
      val loop = eventLoop
      if (loop.inEventLoop) {
        try {
          val messagesToRead = config.getMaxMessagesPerRead
          var i = 0
          var s:StreamConnection = null
          while ( {
            (i < messagesToRead) &&
            (s = channel.accept()) != null
          }) {
            pipeline.fireChannelRead(new WrappingXnioSocketChannel(AbstractXnioServerSocketChannel.this, s))
            i += 1
          }
        } catch {
          case cause: Throwable =>
            pipeline.fireExceptionCaught(cause)
        }
        pipeline.fireChannelReadComplete
      }
      else {
        var cause:Throwable = null
        val messagesToRead = config.getMaxMessagesPerRead
        val array = AbstractXnioServerSocketChannel.connectionsArray(messagesToRead)
        try {
          var i = 0
          var s:StreamConnection = null
          while ( {
            (i < messagesToRead) &&
            (array(i) = channel.accept()) != null
          }) {
            i += 1
          }
          cause = null
        } catch {
          case e: Throwable =>
            cause = e
        }
        val acceptError = cause
        eventLoop.execute(new Runnable() {
          override def run(): Unit = {
            try {
              var i = 0
              var s:StreamConnection = null
              while ( {
                (i < array.length) &&
                (s = array(i)) != null
              }) {
                pipeline.fireChannelRead(new WrappingXnioSocketChannel(AbstractXnioServerSocketChannel.this, s))
                i += 1
              }
            } catch {
              case cause: Throwable =>
                pipeline.fireExceptionCaught(cause)
            }
            if (acceptError != null) pipeline.fireExceptionCaught(acceptError)
            pipeline.fireChannelReadComplete
          }
        })
      }
    }
  }
}