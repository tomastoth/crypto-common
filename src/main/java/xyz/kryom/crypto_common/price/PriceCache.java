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

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tomas Toth
 */
public class PriceCache implements PriceProvider {

  private static final long CACHE_TIME = 5 * 60_000L;
  private final PriceProvider provider;
  private final Clock clock;
  private final Map<String, PriceUpdate> priceMap = new HashMap<>();

  public PriceCache(PriceProvider provider, Clock clock) {
    this.provider = provider;
    this.clock = clock;
  }

  public PriceCache(PriceProvider provider) {
    this.provider = provider;
    this.clock = Clock.systemDefaultZone();
  }

  @Override
  public BigDecimal getPriceBySymbol(String symbol) {
    long timeNow = clock.millis();
    if (priceMap.containsKey(symbol)) {
      PriceUpdate cachedPriceUpdate = priceMap.get(symbol);
      if (isOld(cachedPriceUpdate.timeUpdated(), timeNow)) {
        return cacheNewPrice(symbol, timeNow);
      } else {
        return cachedPriceUpdate.price();
      }
    } else {
      return cacheNewPrice(symbol, timeNow);
    }
  }

  @Override
  public void initialize() {
    provider.initialize();
  }

  private BigDecimal cacheNewPrice(String symbol, long timeNow) {
    BigDecimal newPrice = provider.getPriceBySymbol(symbol);
    priceMap.put(symbol, new PriceUpdate(symbol, timeNow, newPrice));
    return newPrice;
  }

  private boolean isOld(long updatedTime, long timeNow) {
    return timeNow - updatedTime > CACHE_TIME;
  }
}
