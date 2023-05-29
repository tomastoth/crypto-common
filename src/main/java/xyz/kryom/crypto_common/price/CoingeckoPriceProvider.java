/*
 * Copyright 2023 Tomas Toth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package xyz.kryom.crypto_common.price;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import xyz.kryom.crypto_common.HttpUtils;
import xyz.kryom.crypto_common.exceptions.BadRequestError;
import xyz.kryom.crypto_common.exceptions.TokenNotFoundError;

/**
 * @author Tomas Toth
 */
public class CoingeckoPriceProvider implements PriceProvider {
  public static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3";

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final Map<String, String> symbolToId = new HashMap<>();

  CoingeckoPriceProvider() {}

  public void initializeSymbols() {
    String coinsRequestUrl = String.format("%s/coins/list", COINGECKO_API_URL);
    HttpResponse<String> coinsRequestFuture = HttpUtils.fetchUrl(coinsRequestUrl, new HashMap<>());
    extractCoinSymbols(coinsRequestFuture);
  }

  /**
   * @param symbol token to fetch
   * @return BigDecimal price in $
   * @throws TokenNotFoundError when error is not found
   */
  @Override
  public BigDecimal getPriceBySymbol(String symbol) {
    String symbolLower = symbol.toLowerCase();
    if (!symbolToId.containsKey(symbolLower)) {
      throw new TokenNotFoundError();
    }
    Map<String, BigDecimal> prices = fetchPriceOfSymbols(symbolLower);
    return prices.get(symbolLower);
  }

  @Override
  public void initialize() {
    initializeSymbols();
  }

  private void extractCoinSymbols(HttpResponse<String> coinRequestResponse) {
    try {
      if (coinRequestResponse.statusCode() != 200) {
        // TODO add handling of 429
        throw new BadRequestError(coinRequestResponse.body());
      }
      String coinSymbolsBody = coinRequestResponse.body();
      JsonNode jsonNode = OBJECT_MAPPER.readTree(coinSymbolsBody);
      for (JsonNode symbolMapping : jsonNode) {
        String symbol = symbolMapping.get("symbol").asText();
        String id = symbolMapping.get("id").asText();
        symbolToId.put(symbol, id);
      }
    } catch (JsonProcessingException e) {
      throw new BadRequestError(e);
    }
  }

  private Map<String, BigDecimal> fetchPriceOfSymbols(String... symbols) {
    StringBuilder urlBuilder = new StringBuilder(String.format("%s/simple/price?ids=", COINGECKO_API_URL));
    for (String symbol : symbols) {
      String id = symbolToId.get(symbol.toLowerCase());
      urlBuilder.append(String.format("%s%%2C", id));
    }
    urlBuilder.append("&vs_currencies=usd");
    String url = urlBuilder.toString();
    try {
      HttpResponse<String> priceResponseResponse = HttpUtils.fetchUrl(url, new HashMap<>());
      if (priceResponseResponse.statusCode() != 200) {
        // TODO add handling of 429
        throw new BadRequestError(priceResponseResponse.body());
      }
      String priceResponseBody = priceResponseResponse.body();
      JsonNode jsonNode = OBJECT_MAPPER.readTree(priceResponseBody);
      HashMap<String, BigDecimal> priceMap = new HashMap<>();
      for (String symbol : symbols) {
        String id = symbolToId.get(symbol.toLowerCase());
        priceMap.put(symbol, new BigDecimal(jsonNode.get(id).get("usd").toString()));
      }
      return priceMap;
    } catch (JsonProcessingException e) {
      throw new BadRequestError(e);
    }
  }
}
