package io.github.ziy1.nexevent.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ziy1.nexevent.util.IdNormalizerUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
@Order(1)
public class RequestIdNormalizationFilter implements Filter {
  private static final Set<String> ID_PARAMETERS =
      Set.of("userId", "eventId", "user_id", "event_id");
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request instanceof HttpServletRequest httpRequest) {
      NormalizedRequestWrapper wrappedRequest = new NormalizedRequestWrapper(httpRequest);
      chain.doFilter(wrappedRequest, response);
    } else {
      chain.doFilter(request, response);
    }
  }

  private class NormalizedRequestWrapper extends HttpServletRequestWrapper {
    private byte[] cachedBody;
    private final Map<String, String[]> normalizedParams;

    public NormalizedRequestWrapper(HttpServletRequest request) throws IOException {
      super(request);
      // Initialize normalized params
      Map<String, String[]> originalParams = super.getParameterMap();
      normalizedParams = new HashMap<>();
      originalParams.forEach(
          (key, values) -> {
            if (ID_PARAMETERS.contains(key)) {
              normalizedParams.put(
                  key,
                  Arrays.stream(values).map(IdNormalizerUtil::normalize).toArray(String[]::new));
            } else {
              normalizedParams.put(key, values);
            }
          });

      // Cache the request body
      cachedBody = StreamUtils.copyToByteArray(request.getInputStream());

      // If it's a JSON request, normalize the body
      String contentType = request.getContentType();
      if (contentType != null && contentType.contains("application/json")) {
        normalizeJsonBody();
      }
    }

    private void normalizeJsonBody() {
      try {
        String bodyString = new String(cachedBody);
        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(bodyString);

        // Normalize ID fields in JSON
        jsonNode
            .fields()
            .forEachRemaining(
                entry -> {
                  String key = entry.getKey();
                  if (ID_PARAMETERS.contains(key)) {
                    jsonNode.put(key, IdNormalizerUtil.normalize(entry.getValue().asText()));
                  }
                });

        cachedBody = objectMapper.writeValueAsBytes(jsonNode);
      } catch (IOException e) {
        // Log error or handle appropriately
      }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public String getParameter(String name) {
      String[] values = getParameterValues(name);
      return values != null && values.length > 0 ? values[0] : null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
      return Collections.unmodifiableMap(normalizedParams);
    }

    @Override
    public Enumeration<String> getParameterNames() {
      return Collections.enumeration(normalizedParams.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
      return normalizedParams.get(name);
    }
  }

  private static class CachedBodyServletInputStream extends ServletInputStream {
    private final ByteArrayInputStream inputStream;

    public CachedBodyServletInputStream(byte[] cachedBody) {
      this.inputStream = new ByteArrayInputStream(cachedBody);
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
