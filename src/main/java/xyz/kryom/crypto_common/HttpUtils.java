/*
 * Copyright 2023 Tomas Toth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package xyz.kryom.crypto_common;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Tomas Toth
 */
public class HttpUtils {

  private static final HttpClient client = HttpClient.newHttpClient();

  private HttpUtils() {}

  public static CompletableFuture<String> fetchUrlAsync(String url, Map<String, String> headers) {
    HttpRequest request = createRequest(url, headers);
    return client.sendAsync(request, BodyHandlers.ofString())
        .thenApply(HttpResponse::body);
  }

  public static HttpResponse<String> fetchUrl(String url, Map<String, String> headers) {
    HttpRequest request = createRequest(url, headers);
    try {
      return client.send(request, BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static HttpRequest createRequest(String url, Map<String, String> headers) {
    HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder(URI.create(url));
    headers.forEach(httpRequestBuilder::header);
    return httpRequestBuilder.build();
  }
}
