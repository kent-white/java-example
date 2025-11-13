package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * Transaction extra configuration abstraction (for orderless nonce etc.).
 */
public interface TransactionExtraConfig extends com.aptoslabs.japtos.bcs.Serializable {
    @Override
    void serialize(Serializer serializer) throws IOException;
}
