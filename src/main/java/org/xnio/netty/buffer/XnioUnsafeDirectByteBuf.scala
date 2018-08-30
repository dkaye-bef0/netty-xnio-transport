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
package org.xnio.netty.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.UnpooledUnsafeDirectByteBuf
import org.xnio.Pooled
import java.nio.ByteBuffer

/**
  * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
  */
final class XnioUnsafeDirectByteBuf private[buffer](alloc: XnioByteBufAllocator, val initialSize: Int, maxCapacity: Int) extends UnpooledUnsafeDirectByteBuf(alloc, initialSize, maxCapacity) {
  private var pooled:Pooled[ByteBuffer] = null

  override def capacity(newCapacity: Int): ByteBuf = {
    ensureAccessible()
    if (newCapacity < 0 || newCapacity > maxCapacity) throw new IllegalArgumentException("newCapacity: " + newCapacity)
    val oldPooled = this.pooled
    super.capacity(newCapacity)
    if (oldPooled ne pooled) oldPooled.free()
    this
  }

  override protected def allocateDirect(initialCapacity: Int): ByteBuffer = {
    val pooled = XnioByteBufUtil.allocateDirect(alloc.asInstanceOf[XnioByteBufAllocator].pool, initialCapacity)
    this.pooled = pooled
    pooled.getResource
  }

  override protected def freeDirect(buffer: ByteBuffer): Unit = XnioByteBufUtil.freeDirect(buffer, pooled.getResource)

  override protected def deallocate(): Unit = {
    super.deallocate()
    pooled.free()
  }
}