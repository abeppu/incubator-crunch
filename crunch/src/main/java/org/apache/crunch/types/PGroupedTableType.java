/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.crunch.types;

import java.util.Iterator;
import java.util.List;

import org.apache.crunch.GroupingOptions;
import org.apache.crunch.MapFn;
import org.apache.crunch.PGroupedTable;
import org.apache.crunch.Pair;
import org.apache.crunch.SourceTarget;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

/**
 * The {@code PType} instance for {@link PGroupedTable} instances. Its settings
 * are derived from the {@code PTableType} that was grouped to create the
 * {@code PGroupedTable} instance.
 * 
 */
public abstract class PGroupedTableType<K, V> implements PType<Pair<K, Iterable<V>>> {

  protected static class PTypeIterable<V> implements Iterable<V> {
    private final Iterable<Object> iterable;
    private final MapFn<Object, V> mapFn;

    public PTypeIterable(MapFn<Object, V> mapFn, Iterable<Object> iterable) {
      this.mapFn = mapFn;
      this.iterable = iterable;
    }

    public Iterator<V> iterator() {
      return new Iterator<V>() {
        Iterator<Object> iter = iterable.iterator();

        public boolean hasNext() {
          return iter.hasNext();
        }

        public V next() {
          return mapFn.map(iter.next());
        }

        public void remove() {
          iter.remove();
        }
      };
    }
  }

  public static class PairIterableMapFn<K, V> extends MapFn<Pair<Object, Iterable<Object>>, Pair<K, Iterable<V>>> {
    private final MapFn<Object, K> keys;
    private final MapFn<Object, V> values;

    public PairIterableMapFn(MapFn<Object, K> keys, MapFn<Object, V> values) {
      this.keys = keys;
      this.values = values;
    }

    @Override
    public void initialize() {
      keys.initialize();
      values.initialize();
    }

    @Override
    public Pair<K, Iterable<V>> map(Pair<Object, Iterable<Object>> input) {
      return Pair.<K, Iterable<V>> of(keys.map(input.first()), new PTypeIterable(values, input.second()));
    }
  }

  protected final PTableType<K, V> tableType;

  public PGroupedTableType(PTableType<K, V> tableType) {
    this.tableType = tableType;
  }

  public PTableType<K, V> getTableType() {
    return tableType;
  }

  @Override
  public PTypeFamily getFamily() {
    return tableType.getFamily();
  }

  @Override
  public List<PType> getSubTypes() {
    return tableType.getSubTypes();
  }

  @Override
  public Converter getConverter() {
    return tableType.getConverter();
  }

  public abstract Converter getGroupingConverter();

  public abstract void configureShuffle(Job job, GroupingOptions options);

  @Override
  public SourceTarget<Pair<K, Iterable<V>>> getDefaultFileSource(Path path) {
    throw new UnsupportedOperationException("Grouped tables cannot be written out directly");
  }
}
