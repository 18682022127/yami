package com.kingdom.yami.gateway.filter.crypto;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;
    private String contentTypeOverride;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
        super(request);
        this.cachedBody = body;
    }

    public void setContentType(String contentType) {
        this.contentTypeOverride = contentType;
    }

    @Override
    public String getContentType() {
        return contentTypeOverride != null ? contentTypeOverride : super.getContentType();
    }

    @Override
    public String getHeader(String name) {
        if (contentTypeOverride != null && "Content-Type".equalsIgnoreCase(name)) {
            return contentTypeOverride;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (contentTypeOverride != null && "Content-Type".equalsIgnoreCase(name)) {
            return Collections.enumeration(List.of(contentTypeOverride));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        if (contentTypeOverride == null) {
            return super.getHeaderNames();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) {
            names.add(original.nextElement());
        }
        names.add("Content-Type");
        return Collections.enumeration(names);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(
            new ByteArrayInputStream(cachedBody), StandardCharsets.UTF_8));
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
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
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
