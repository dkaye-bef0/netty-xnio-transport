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

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.AbstractChannel
import io.netty.channel.ChannelConfig
import io.netty.channel.ChannelException
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelMetadata
import io.netty.channel.ChannelOutboundBuffer
import io.netty.channel.ChannelPipeline
import io.netty.channel.ChannelPromise
import io.netty.channel.EventLoop
import io.netty.channel.FileRegion
import io.netty.channel.RecvByteBufAllocator
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.SocketChannelConfig
import io.netty.util.IllegalReferenceCountException
import io.netty.util.internal.StringUtil
import org.xnio.ChannelListener
import org.xnio.Option
import org.xnio.StreamConnection
import org.xnio.conduits.ConduitStreamSinkChannel
import org.xnio.conduits.ConduitStreamSourceChannel
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel

import scala.util.control.Breaks._

/**
  * {@link SocketChannel} base class for our XNIO transport
  *
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
object AbstractXnioSocketChannel {
  private val META_DATA = new ChannelMetadata(false)

  private def suspend(connection: StreamConnection): Unit = {
    if (connection == null) return
    connection.getSourceChannel.suspendReads()
    connection.getSinkChannel.suspendWrites()
  }
}

abstract class AbstractXnioSocketChannel private[transport](_parent: AbstractXnioServerSocketChannel) extends AbstractChannel(_parent) with SocketChannel {
  def continue = break // FIXME: stub for java continue

  override val config = new XnioSocketChannelConfig(this)
  private var flushTask: Runnable = null
  private var writeListener: ChannelListener[ConduitStreamSinkChannel] = null
  private var closed = false

  override def parent: ServerSocketChannel = super.parent.asInstanceOf[ServerSocketChannel]

  override def remoteAddress: InetSocketAddress = super.remoteAddress.asInstanceOf[InetSocketAddress]

  override def localAddress: InetSocketAddress = super.localAddress.asInstanceOf[InetSocketAddress]

  override protected def newUnsafe: AbstractXnioSocketChannel#AbstractXnioUnsafe

  override protected def isCompatible(loop: EventLoop): Boolean = {
    if (!loop.isInstanceOf[XnioEventLoop]) return false
    val parent = this.parent
    if (parent != null) { // if this channel has a parent we need to ensure that both EventLoopGroups are the same for XNIO
      // to be sure it uses a Thread from the correct Worker.
      if (parent.eventLoop.parent ne loop.parent) return false
    }
    true
  }

  @throws[Exception]
  override protected def doDisconnect(): Unit = doClose()

  private def incompleteWrite(setOpWrite: Boolean) = { // Did not write completely.
    if (setOpWrite) setOpWrite()
    else { // Schedule flush again later so other tasks can be picked up in the meantime
      var flushTask = this.flushTask
      if (flushTask == null) {
        flushTask = new Runnable() {
          override def run(): Unit = flush
        }
        this.flushTask = flushTask
      }
      eventLoop.execute(flushTask)
    }
  }

  private def setOpWrite() = {
    val sink = connection.getSinkChannel
    if (!sink.isWriteResumed) {
      var writeListener = this.writeListener
      if (writeListener == null) {
        writeListener = new WriteListener
        this.writeListener = writeListener
      }
      sink.getWriteSetter.set(writeListener)
      sink.resumeWrites()
    }
  }

  @throws[Exception]
  override protected def doWrite(in: ChannelOutboundBuffer): Unit = {
    var writeSpinCount = -1
    val sink = connection.getSinkChannel
    while ( {
      true
    }) { // Do gathering write for a non-single buffer case.
      val msgCount = in.size
      if (msgCount > 0) { // Ensure the pending writes are made of ByteBufs only.
        val nioBuffers = in.nioBuffers
        if (nioBuffers != null) {
          val nioBufferCnt = in.nioBufferCount
          var expectedWrittenBytes = in.nioBufferSize
          var writtenBytes = 0
          var done = false
          var setOpWrite = false
          var i = config.getWriteSpinCount - 1
          while ( {
            i >= 0
          }) {
            val localWrittenBytes = sink.write(nioBuffers, 0, nioBufferCnt)
            if (localWrittenBytes == 0) {
              setOpWrite = true
              break //todo: break is not supported
            }
            expectedWrittenBytes -= localWrittenBytes
            writtenBytes += localWrittenBytes
            if (expectedWrittenBytes == 0) {
              done = true
              break //todo: break is not supported
            }
            {
              i -= 1; i + 1
            }
          }
          if (done) { // Release all buffers
            var i = msgCount
            while ( {
              i > 0
            }) {
              in.remove()
              {
                i -= 1; i + 1
              }
            }
            // Finish the write loop if no new messages were flushed by in.remove().
            if (in.isEmpty) {
              connection.getSinkChannel.suspendWrites()
              break //todo: break is not supported
            }
          }
          else { // Did not write all buffers completely.
            // Release the fully written buffers and update the indexes of the partially written buffer.
            var i = msgCount
            while ( {
              i > 0
            }) {
              val buf = in.current.asInstanceOf[ByteBuf]
              val readerIndex = buf.readerIndex
              val readableBytes = buf.writerIndex - readerIndex
              if (readableBytes < writtenBytes) {
                in.progress(readableBytes)
                in.remove
                writtenBytes -= readableBytes
              }
              else if (readableBytes > writtenBytes) {
                buf.readerIndex(readerIndex + writtenBytes.toInt)
                in.progress(writtenBytes)
                break //todo: break is not supported
              }
              else { // readableBytes == writtenBytes
                in.progress(readableBytes)
                in.remove
                break //todo: break is not supported
              }
              {
                i -= 1; i + 1
              }
            }
            incompleteWrite(setOpWrite)
            break //todo: break is not supported
          }
          continue //todo: continue is not supported
        }
      }
      val msg = in.current
      if (msg == null) { // Wrote all messages.
        connection.getSinkChannel.suspendWrites()
        break //todo: break is not supported
      }
      if (msg.isInstanceOf[ByteBuf]) {
        var buf = msg.asInstanceOf[ByteBuf]
        val readableBytes = buf.readableBytes
        if (readableBytes == 0) {
          in.remove
          continue //todo: continue is not supported
        }
        if (!buf.isDirect) {
          val alloc = this.alloc
          if (alloc.isDirectBufferPooled) { // Non-direct buffers are copied into JDK's own internal direct buffer on every I/O.
            // We can do a better job by using our pooled allocator. If the current allocator does not
            // pool a direct buffer, we rely on JDK's direct buffer pool.
            buf = alloc.directBuffer(readableBytes).writeBytes(buf)
            in.current(buf)
          }
        }
        var setOpWrite = false
        var done = false
        var flushedAmount = 0
        if (writeSpinCount == -1) writeSpinCount = config.getWriteSpinCount
        var i = writeSpinCount - 1
        while ( {
          i >= 0
        }) {
          val localFlushedAmount = buf.readBytes(sink, buf.readableBytes)
          if (localFlushedAmount == 0) {
            setOpWrite = true
            break //todo: break is not supported
          }
          flushedAmount += localFlushedAmount
          if (!buf.isReadable) {
            done = true
            break //todo: break is not supported
          }
          {
            i -= 1; i + 1
          }
        }
        in.progress(flushedAmount)
        if (done) in.remove
        else {
          incompleteWrite(setOpWrite)
          break //todo: break is not supported
        }
      }
      else if (msg.isInstanceOf[FileRegion]) {
        val region = msg.asInstanceOf[FileRegion]
        var setOpWrite = false
        var done = false
        var flushedAmount = 0
        if (writeSpinCount == -1) writeSpinCount = config.getWriteSpinCount
        var i = writeSpinCount - 1
        while ( {
          i >= 0
        }) {
          val localFlushedAmount = region.transferTo(sink, region.transfered)
          if (localFlushedAmount == 0) {
            setOpWrite = true
            break //todo: break is not supported
          }
          flushedAmount += localFlushedAmount
          if (region.transfered >= region.count) {
            done = true
            break //todo: break is not supported
          }
          {
            i -= 1; i + 1
          }
        }
        in.progress(flushedAmount)
        if (done) in.remove
        else {
          incompleteWrite(setOpWrite)
          break //todo: break is not supported
        }
      }
      else throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg))
    }
  }

  override def shutdownOutput: ChannelFuture = newFailedFuture(new UnsupportedOperationException)

  override def shutdownOutput(future: ChannelPromise): ChannelFuture = newFailedFuture(new UnsupportedOperationException)

  override def isOpen: Boolean = {
    val conn = connection
    (conn == null || conn.isOpen) && !closed
  }

  override def isActive: Boolean = {
    val conn = connection
    conn != null && conn.isOpen && !closed
  }

  override def metadata: ChannelMetadata = AbstractXnioSocketChannel.META_DATA

  override protected def localAddress0: SocketAddress = {
    val conn = connection
    if (conn == null) return null
    conn.getLocalAddress
  }

  override protected def remoteAddress0: SocketAddress = {
    val conn = connection
    if (conn == null) return null
    conn.getPeerAddress
  }

  abstract protected class AbstractXnioUnsafe extends AbstractChannel#AbstractUnsafe {
    private[AbstractXnioSocketChannel] var readPending = false

    override def beginRead(): Unit = { // Channel.read() or ChannelHandlerContext.read() was called
      readPending = true
      super.beginRead()
    }

    override protected def flush0(): Unit = { // Flush immediately only when there's no pending flush.
      // If there's a pending flush operation, event loop will call forceFlush() later,
      // and thus there's no need to call it now.
      if (connection.getSinkChannel.isWriteResumed) return
      super.flush0()
    }

    def forceFlush(): Unit = super.flush0()
  }

  final private[transport] class ReadListener extends ChannelListener[ConduitStreamSourceChannel] {
    private var allocHandle:RecvByteBufAllocator.Handle = null

    private def removeReadOp(channel: ConduitStreamSourceChannel) = if (channel.isReadResumed) channel.suspendReads()

    private def closeOnRead() = {
      val connection = connection
      AbstractXnioSocketChannel.suspend(connection)
      if (isOpen) unsafe.close(unsafe.voidPromise)
    }

    private def handleReadException(pipeline: ChannelPipeline, byteBuf: ByteBuf, cause: Throwable, close: Boolean) = {
      if (byteBuf != null) if (byteBuf.isReadable) pipeline.fireChannelRead(byteBuf)
      else try byteBuf.release
      catch {
        case ignore: IllegalReferenceCountException =>

        // ignore as it may be released already
      }
      pipeline.fireChannelReadComplete
      pipeline.fireExceptionCaught(cause)
      if (close || cause.isInstanceOf[IOException]) closeOnRead()
    }

    override def handleEvent(channel: ConduitStreamSourceChannel): Unit = {
      val config = AbstractXnioSocketChannel.this.config
      val pipeline = AbstractXnioSocketChannel.this.pipeline
      val allocator = config.getAllocator
      val maxMessagesPerRead = config.getMaxMessagesPerRead
      var allocHandle = this.allocHandle
      if (allocHandle == null) {
        allocHandle = config.getRecvByteBufAllocator.newHandle
        this.allocHandle = allocHandle
      }
      var byteBuf:ByteBuf = null
      var messages = 0
      var close = false
      try {
        val byteBufCapacity = allocHandle.guess
        var totalReadAmount = 0
        do {
          byteBuf = allocator.ioBuffer(byteBufCapacity)
          val writable = byteBuf.writableBytes
          val localReadAmount = byteBuf.writeBytes(channel, byteBuf.writableBytes)
          if (localReadAmount <= 0) { // not was read release the buffer
            byteBuf.release
            close = localReadAmount < 0
            break //todo: break is not supported
          }
          unsafe.asInstanceOf[AbstractXnioUnsafe].readPending = false
          pipeline.fireChannelRead(byteBuf)
          byteBuf = null
          if (totalReadAmount >= Integer.MAX_VALUE - localReadAmount) { // Avoid overflow.
            totalReadAmount = Integer.MAX_VALUE
            break //todo: break is not supported
          }
          totalReadAmount += localReadAmount
          // stop reading
          if (!config.isAutoRead) break //todo: break is not supported
          if (localReadAmount < writable) { // Read less than what the buffer can hold,
            // which might mean we drained the recv buffer completely.
            break //todo: break is not supported
          }
        } while ( {
          {
            messages += 1; messages
          } < maxMessagesPerRead
        })
        pipeline.fireChannelReadComplete
        allocHandle.record(totalReadAmount)
        if (close) {
          closeOnRead()
          close = false
        }
      } catch {
        case t: Throwable =>
          handleReadException(pipeline, byteBuf, t, close)
      } finally {
        // Check if there is a readPending which was not processed yet.
        // This could be for two reasons:
        // * The user called Channel.read() or ChannelHandlerContext.read() in channelRead(...) method
        // * The user called Channel.read() or ChannelHandlerContext.read() in channelReadComplete(...) method
        //
        // See https://github.com/netty/netty/issues/2254
        if (!config.isAutoRead && !(unsafe.asInstanceOf[AbstractXnioSocketChannel#AbstractXnioUnsafe]).readPending) removeReadOp(channel)
      }
    }
  }

  private class WriteListener extends ChannelListener[ConduitStreamSinkChannel] {
    override def handleEvent(channel: ConduitStreamSinkChannel): Unit = unsafe.asInstanceOf[AbstractXnioSocketChannel#AbstractXnioUnsafe].forceFlush()
  }

  override def isInputShutdown: Boolean = {
    val conn = connection
    conn == null || conn.isReadShutdown
  }

  override def isOutputShutdown: Boolean = {
    val conn = connection
    conn == null || conn.isWriteShutdown
  }

  @throws[Exception]
  override protected def doBeginRead(): Unit = {
    val conn = connection
    if (conn == null) return
    val source = conn.getSourceChannel
    if (!source.isReadResumed) source.resumeReads()
  }

  @throws[Exception]
  override protected def doClose(): Unit = {
    closed = true
    val conn = connection
    if (conn != null) {
      AbstractXnioSocketChannel.suspend(conn)
      conn.close()
    }
  }

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
    * Returns the underlying {@link StreamConnection} or {@code null} if not created yet.
    */
  protected def connection: StreamConnection
}