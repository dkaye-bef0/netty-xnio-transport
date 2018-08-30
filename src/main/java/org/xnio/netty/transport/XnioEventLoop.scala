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
import io.netty.util.concurrent.AbstractEventExecutor
import io.netty.util.concurrent.DefaultPromise
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.ScheduledFuture
import org.xnio.XnioExecutor
import org.xnio.XnioIoThread
import java.util.concurrent.Callable
import java.util.concurrent.Delayed
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
  * {@link EventLoop} implementation which uses a {@link XnioIoThread}.
  *
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
final class XnioEventLoop extends AbstractEventExecutor with EventLoop with IoThreadPowered {
  final private var executor:XnioIoThread = null
  final private var _parent:EventLoopGroup = null

  def this(parent: EventLoopGroup, executor: XnioIoThread) {
    this()
    this._parent = parent
    this.executor = executor
  }

  def this(executor: XnioIoThread) {
    this()
    this._parent = this
    this.executor = executor
  }

  override def ioThread: XnioIoThread = executor

  override def shutdown(): Unit = {
    // Not supported, just ignore
  }

  override def parent: EventLoopGroup = parent

  override def inEventLoop(thread: Thread): Boolean = thread eq executor

  override def register(channel: Channel): ChannelFuture = register(channel, channel.newPromise)

  override def register(channel: Channel, promise: ChannelPromise): ChannelFuture = {
    if (channel == null) throw new NullPointerException("channel")
    if (promise == null) throw new NullPointerException("promise")
    channel.unsafe.register(this, promise)
    promise
  }

  override def isShuttingDown: Boolean = executor.getWorker.isShutdown

  override def shutdownGracefully(quietPeriod: Long, timeout: Long, unit: TimeUnit): Future[_] = newFailedFuture(new UnsupportedOperationException)

  override def terminationFuture: Future[_] = newFailedFuture(new UnsupportedOperationException)

  override def isShutdown: Boolean = executor.getWorker.isShutdown

  override def isTerminated: Boolean = executor.getWorker.isTerminated

  @throws[InterruptedException]
  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = executor.getWorker.awaitTermination(timeout, unit)

  override def execute(command: Runnable): Unit = executor.execute(command)

  override def schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture[_] = schedule(Executors.callable(command), delay, unit)

  override def schedule[V](callable: Callable[V], delay: Long, unit: TimeUnit): ScheduledFuture[V] = {
    val wrapper = new ScheduledFutureWrapper[V](callable, delay, unit)
    wrapper.key = executor.executeAfter(wrapper, delay, unit)
    wrapper
  }

  override def scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture[_] = {
    val wrapper = new FixedScheduledFuture(command, initialDelay, delay, unit)
    wrapper.key = executor.executeAfter(wrapper, delay, unit)
    wrapper
  }

  override def scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture[_] = {
    val wrapper = new FixedRateScheduledFuture(command, initialDelay, period, unit)
    wrapper.key = executor.executeAfter(wrapper, initialDelay, unit)
    wrapper
  }

  override def next: XnioEventLoop = this

  final class FixedRateScheduledFuture(taskInit: Runnable, initialDelay: Long, val period: Long, val unit: TimeUnit) extends ScheduledFutureWrapper[AnyRef](Executors.callable(taskInit), initialDelay, unit) {
    private var count = 1

    override def run(): Unit = try {
      task.call()
      start = System.nanoTime
      delay = initialDelay + period * {
        count += 1; count
      }
      key = XnioEventLoop.this.executor.executeAfter(this, delay, TimeUnit.NANOSECONDS)
    } catch {
      case cause: Throwable => {
        val p:DefaultPromise[_] = FixedRateScheduledFuture.this
        p.tryFailure(cause)
      }
    }
  }

  final class FixedScheduledFuture(initialTask: Runnable, val initialDelay: Long, initialFurtherDelay: Long, val unit: TimeUnit) extends ScheduledFutureWrapper[AnyRef](Executors.callable(initialTask), initialDelay, unit) {
    private val furtherDelay = unit.toNanos(initialFurtherDelay)

    override def run(): Unit =
      try {
        task.call()
        start = System.nanoTime
        delay = furtherDelay
        key = XnioEventLoop.this.executor.executeAfter(this, furtherDelay, TimeUnit.NANOSECONDS)
      } catch {
        case cause: Throwable =>
          tryFailure(cause)
      }
  }

  class ScheduledFutureWrapper[V](val task: Callable[V], var delay: Long) extends DefaultPromise[V] with ScheduledFuture[V] with Runnable {
    protected[XnioEventLoop] var key: XnioExecutor.Key = null
    protected var start = System.nanoTime

    def this(task: Callable[V], delay: Long, unit: TimeUnit) =
      this(task,unit.toNanos(delay))

    override def getDelay(unit: TimeUnit): Long = {
      val remaining = (start + delay) - System.nanoTime
      if (remaining < 1 || (unit eq TimeUnit.NANOSECONDS)) return remaining
      unit.convert(remaining, TimeUnit.NANOSECONDS)
    }

    override def compareTo(o: Delayed): Int = {
      val d = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS)
      if (d < 0) -1
      else if (d > 0) 1
      else 0
    }

    override def run(): Unit = try trySuccess(task.call)
    catch {
      case t: Throwable =>
        tryFailure(t)
    }

    override def cancel(mayInterruptIfRunning: Boolean): Boolean = setUncancellable && key.remove
  }

}