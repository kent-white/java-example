package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * Transaction executable abstraction for orderless transactions.
 */
public interface TransactionExecutable extends com.aptoslabs.japtos.bcs.Serializable {
    @Override
    void serialize(Serializer serializer) throws IOException;
}
