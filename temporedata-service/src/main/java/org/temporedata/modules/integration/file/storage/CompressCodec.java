package org.temporedata.modules.integration.file.storage;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;

/**
 * Pluggable compression codecs (file-compress v1.0 doc).
 * gzip (JDK, level-aware via manual gzip header) and zstd (zstd-jni).
 */
public final class CompressCodec {

    private CompressCodec() {
    }

    /** Compress src -> tgt using the configured algorithm and level. */
    public static void compress(String algorithm, Path src, Path tgt, int level) throws IOException {
        try (InputStream in = Files.newInputStream(src)) {
            if ("zstd".equalsIgnoreCase(algorithm)) {
                ZstdOutputStream zos = new ZstdOutputStream(Files.newOutputStream(tgt));
                if (level > 0) zos.setLevel(level);
                try {
                    pipe(in, zos);
                } finally {
                    zos.close();
                }
            } else { // gzip
                OutputStream raw = Files.newOutputStream(tgt);
                GzipDeflater gz = (GzipDeflater) gzipStream(raw, level);
                try {
                    pipe(in, gz);
                } finally {
                    gz.close(); // writes gzip trailer (CRC32 + ISIZE) then closes raw
                }
            }
        }
    }

    /** Wrap a decompressing stream depending on the stored algorithm. */
    public static InputStream wrapRead(InputStream raw, String algorithm) throws IOException {
        if ("zstd".equalsIgnoreCase(algorithm)) {
            return new ZstdInputStream(raw);
        }
        return new GZIPInputStream(raw);
    }

    /**
     * A level-aware gzip writer: writes the gzip header/trailer around a raw-defalte
     * stream, honoring the requested Deflater level (0-9, or -1 = default).
     */
    private static OutputStream gzipStream(OutputStream out, int level) throws IOException {
        out.write(new byte[]{(byte) 0x1f, (byte) 0x8b, 8, 0, 0, 0, 0, 0, 0, 0});
        int lvl = level < 0 ? Deflater.DEFAULT_COMPRESSION : Math.min(9, Math.max(0, level));
        return new GzipDeflater(out, new Deflater(lvl, true), new CRC32());
    }

    private static void pipe(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        out.flush();
    }

    /** Minimal level-aware gzip writer (header + raw deflate + CRC32/ISIZE trailer). */
    private static final class GzipDeflater extends DeflaterOutputStream {
        private final CRC32 crc;
        private long size;

        GzipDeflater(OutputStream out, Deflater def, CRC32 crc) {
            super(out, def);
            this.crc = crc;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            crc.update(b, off, len);
            size += len;
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            byte[] one = {(byte) b};
            crc.update(one, 0, 1);
            size++;
        }

        @Override
        public void close() throws IOException {
            try {
                finish();
                writeInt((int) crc.getValue());
                writeInt((int) size);
            } finally {
                super.close();
            }
        }

        private void writeInt(int v) throws IOException {
            out.write(new byte[]{(byte) v, (byte) (v >>> 8), (byte) (v >>> 16), (byte) (v >>> 24)});
        }
    }
}