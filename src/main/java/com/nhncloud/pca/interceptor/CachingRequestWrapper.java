package com.nhncloud.pca.interceptor;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class CachingRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachingRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);

        // Request body를 한 번 읽어서 캐시
        ServletInputStream inputStream = request.getInputStream();
        this.cachedBody = inputStream.readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachingServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    public String getCachedBody() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    private static class CachingServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachingServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // Not needed for our use case
        }
    }
}