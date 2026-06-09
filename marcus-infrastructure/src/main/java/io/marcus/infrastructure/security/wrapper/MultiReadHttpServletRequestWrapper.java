package io.marcus.infrastructure.security.wrapper;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipException;
import java.util.zip.GZIPInputStream;

public class MultiReadHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private final byte[] rawBody;
    private final byte[] requestBody;
    private final boolean gzipEncoded;

    public MultiReadHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        this(request, Integer.MAX_VALUE);
    }

    public MultiReadHttpServletRequestWrapper(HttpServletRequest request, int maxRawBodyBytes) throws IOException {
        super(request);
        this.rawBody = readRequestBody(request.getInputStream(), maxRawBodyBytes);
        this.gzipEncoded = hasGzipEncoding(request);
        this.requestBody = gzipEncoded ? decompress(rawBody) : rawBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(this.requestBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    public String getBody() {
        return new String(this.requestBody, StandardCharsets.UTF_8);
    }

    public byte[] getRawBody() {
        return this.rawBody;
    }

    public byte[] getRequestBodyBytes() {
        return this.requestBody;
    }

    public boolean isGzipEncoded() {
        return gzipEncoded;
    }

    private static boolean hasGzipEncoding(HttpServletRequest request) {
        String encoding = request.getHeader("Content-Encoding");
        return encoding != null && encoding.toLowerCase(Locale.ROOT).contains("gzip");
    }

    private static byte[] readRequestBody(InputStream inputStream, int maxRawBodyBytes) throws IOException {
        try (InputStream stream = inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > maxRawBodyBytes) {
                    throw new RequestBodyTooLargeException(maxRawBodyBytes, totalBytes);
                }
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

    private static byte[] decompress(byte[] compressed) throws IOException {
        try (InputStream byteStream = new ByteArrayInputStream(compressed);
             GZIPInputStream gzipStream = new GZIPInputStream(byteStream)) {
            return StreamUtils.copyToByteArray(gzipStream);
        } catch (ZipException ex) {
            throw new InvalidGzipBodyException("Request body is not a valid gzip stream", ex);
        }
    }

    public static final class InvalidGzipBodyException extends IOException {
        public InvalidGzipBodyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class RequestBodyTooLargeException extends IOException {
        private final int maxAllowedBytes;
        private final int actualBytes;

        public RequestBodyTooLargeException(int maxAllowedBytes, int actualBytes) {
            super("Request body exceeds max compressed size of " + maxAllowedBytes + " bytes");
            this.maxAllowedBytes = maxAllowedBytes;
            this.actualBytes = actualBytes;
        }

        public int getMaxAllowedBytes() {
            return maxAllowedBytes;
        }

        public int getActualBytes() {
            return actualBytes;
        }
    }
}
