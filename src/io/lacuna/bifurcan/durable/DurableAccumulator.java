package io.lacuna.bifurcan.durable;

import io.lacuna.bifurcan.DurableInput;
import io.lacuna.bifurcan.DurableOutput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import static io.lacuna.bifurcan.allocator.SlabAllocator.free;

public class DurableAccumulator implements DurableOutput {

  private final ByteBufferWritableChannel buffer;
  private final ByteChannelDurableOutput channel;

  public DurableAccumulator() {
    this(DurableOutput.DEFAULT_BUFFER_SIZE);
  }

  public DurableAccumulator(int bufferSize) {
    this.buffer = new ByteBufferWritableChannel(bufferSize);
    this.channel = new ByteChannelDurableOutput(buffer, bufferSize);
  }

  public void flushTo(DurableOutput out) {
    close();
    Iterable<ByteBuffer> buffers = buffer.contents();
    out.write(buffers);
    free(buffers);
  }

  Iterable<ByteBuffer> contents() {
    close();
    return buffer.contents();
  }

  public void flushTo(BlockPrefix.BlockType type, boolean checksum, DurableOutput out) {
    close();
    Iterable<ByteBuffer> buffers = buffer.contents();

    long size = 0;
    for (ByteBuffer b : buffers) {
      size += b.remaining();
    }

    try {
      if (checksum) {
        CRC32 crc = new CRC32();
        buffers.forEach(b -> crc.update(b.duplicate()));
        BlockPrefix.write(new BlockPrefix(size, type, (int) crc.getValue()), out);
      } else {
        BlockPrefix.write(new BlockPrefix(size, type), out);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    out.write(buffers);
    free(buffers);
  }

  @Override
  public void close() {
    channel.close();
  }

  @Override
  public void flush() {
    channel.flush();
  }

  @Override
  public long written() {
    return channel.written();
  }

  @Override
  public void write(Iterable<ByteBuffer> buffers) {
    long size = 0;
    for (ByteBuffer b : buffers) {
      size += b.remaining();
    }
    buffer.extend((int) Math.min(size, Integer.MAX_VALUE));
    buffers.forEach(this::write);
  }

  @Override
  public int write(ByteBuffer src) {
    return channel.write(src);
  }

  @Override
  public void transferFrom(DurableInput in, long bytes) {
    channel.transferFrom(in, bytes);
  }

  @Override
  public void writeByte(int v) {
    channel.writeByte(v);
  }

  @Override
  public void writeShort(int v) {
    channel.writeShort(v);
  }

  @Override
  public void writeChar(int v) {
    channel.writeChar(v);
  }

  @Override
  public void writeInt(int v) {
    channel.writeInt(v);
  }

  @Override
  public void writeLong(long v) {
    channel.writeLong(v);
  }

  @Override
  public void writeFloat(float v) {
    channel.writeFloat(v);
  }

  @Override
  public void writeDouble(double v) {
    channel.writeDouble(v);
  }
}