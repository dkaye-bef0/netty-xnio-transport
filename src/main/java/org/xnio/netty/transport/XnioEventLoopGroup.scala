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

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelPromise
import io.netty.channel.EventLoop
import io.netty.channel.EventLoopGroup
import io.netty.util.concurrent.AbstractEventExecutorGroup
import io.netty.util.concurrent.EventExecutor
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.ImmediateEventExecutor
import org.xnio.OptionMap
import org.xnio.Options
import org.xnio.Xnio
import org.xnio.XnioWorker
import java.io.IOException
import java.util
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
  * {@link EventLoopGroup} implementation which uses a {@link XnioWorker} under the covers. This means all operations
  * will be performed by it.
  */
final class XnioEventLoopGroup(val worker: XnioWorker)

/**
  * Create a new {@link XnioEventLoopGroup} using the provided {@link XnioWorker}.
  *
  */
  extends AbstractEventExecutorGroup with EventLoopGroup {
  if (worker == null) throw new NullPointerException("worker")

  /**
    * Create a new {@link XnioEventLoopGroup} which creates a new {@link XnioWorker} by itself and use it for all
    * operations. Using the given number of Threads to handle the IO.
    *
    * @throws IOException
    */
  def this(numThreads: Int) {
    this(Xnio.getInstance.createWorker(XnioEventLoopGroup.newIntOptionMap(Options.WORKER_IO_THREADS, numThreads)))
  }

  /**
    * Create a new {@link XnioEventLoopGroup} which creates a new {@link XnioWorker}
    * by itself and use it for all operations.
    *
    * @throws IOException
    */
  def this() {
    this(Runtime.getRuntime.availableProcessors * 2)
  }

  override def shutdown(): Unit = worker.shutdown()

  override def next = new XnioEventLoop(this, worker.getIoThread)

  override def register(channel: Channel): ChannelFuture = register(channel, channel.newPromise)

  override def register(channel: Channel, promise: ChannelPromise): ChannelFuture = {
    if (channel.isInstanceOf[IoThreadPowered]) {
      val ch = channel.asInstanceOf[IoThreadPowered]
      val loop = new XnioEventLoop(this, ch.ioThread)
      channel.unsafe.register(loop, promise)
      return promise
    }
    next.register(channel, promise)
  }

  override def isShuttingDown: Boolean = worker.isTerminated

  override def shutdownGracefully(quietPeriod: Long, timeout: Long, unit: TimeUnit): Future[_] = {
    shutdown()
    if (isShutdown) ImmediateEventExecutor.INSTANCE.newSucceededFuture(null)
    else ImmediateEventExecutor.INSTANCE.newFailedFuture(new TimeoutException)
  }

  override def terminationFuture = throw new UnsupportedOperationException

  override def iterator = throw new UnsupportedOperationException

  override def isShutdown: Boolean = worker.isShutdown

  override def isTerminated: Boolean = worker.isTerminated

  @throws[InterruptedException]
  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = worker.awaitTermination(timeout, unit)
}

object XnioEventLoopGroup {
  def newIntOptionMap(opt:org.xnio.Option[Integer],value:Integer):OptionMap = {
    OptionMap.create(opt,value)
  }
}