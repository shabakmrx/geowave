/**
 * Copyright (c) 2013-2020 Contributors to the Eclipse Foundation
 *
 * <p> See the NOTICE file distributed with this work for additional information regarding copyright
 * ownership. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Apache License, Version 2.0 which accompanies this distribution and is
 * available at http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package org.locationtech.geowave.core.store.index;

import java.nio.ByteBuffer;
import org.locationtech.geowave.core.index.ByteArrayUtils;
import org.locationtech.geowave.core.index.NumericIndexStrategy;
import org.locationtech.geowave.core.index.VarintUtils;
import org.locationtech.geowave.core.index.persist.PersistenceUtils;
import org.locationtech.geowave.core.store.api.Index;

/**
 * This class fully describes everything necessary to index data within GeoWave. The key components
 * are the indexing strategy and the common index model.
 */
public class IndexImpl implements Index {
  protected NumericIndexStrategy indexStrategy;
  protected CommonIndexModel indexModel;

  public IndexImpl() {}

  public IndexImpl(final NumericIndexStrategy indexStrategy, final CommonIndexModel indexModel) {
    this.indexStrategy = indexStrategy;
    this.indexModel = indexModel;
  }

  @Override
  public NumericIndexStrategy getIndexStrategy() {
    return indexStrategy;
  }

  @Override
  public CommonIndexModel getIndexModel() {
    return indexModel;
  }

  @Override
  public String getName() {
    return indexStrategy.getId() + "_" + indexModel.getId();
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final IndexImpl other = (IndexImpl) obj;
    return getName().equals(other.getName());
  }

  @Override
  public byte[] toBinary() {
    final byte[] indexStrategyBinary = PersistenceUtils.toBinary(indexStrategy);
    final byte[] indexModelBinary = PersistenceUtils.toBinary(indexModel);
    final ByteBuffer buf =
        ByteBuffer.allocate(
            indexStrategyBinary.length
                + indexModelBinary.length
                + VarintUtils.unsignedIntByteLength(indexStrategyBinary.length));
    VarintUtils.writeUnsignedInt(indexStrategyBinary.length, buf);
    buf.put(indexStrategyBinary);
    buf.put(indexModelBinary);
    return buf.array();
  }

  @Override
  public void fromBinary(final byte[] bytes) {
    final ByteBuffer buf = ByteBuffer.wrap(bytes);
    final int indexStrategyLength = VarintUtils.readUnsignedInt(buf);
    final byte[] indexStrategyBinary = ByteArrayUtils.safeRead(buf, indexStrategyLength);

    indexStrategy = (NumericIndexStrategy) PersistenceUtils.fromBinary(indexStrategyBinary);

    final byte[] indexModelBinary = new byte[buf.remaining()];
    buf.get(indexModelBinary);
    indexModel = (CommonIndexModel) PersistenceUtils.fromBinary(indexModelBinary);
  }
}
