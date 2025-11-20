package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * TransactionInnerPayloadV1 wrapping an executable and extra config.
 * This serializes as a top-level TransactionPayload with variant 4 (Payload),
 * followed by inner payload variant V1 (0), then the executable and extra config.
 */
public class TransactionInnerPayloadV1 implements TransactionPayload {
    private static final int TRANSACTION_PAYLOAD_VARIANT_PAYLOAD = 4; // matches TS enum
    private static final int TRANSACTION_INNER_PAYLOAD_VARIANT_V1 = 0; // matches TS enum

    private final TransactionExecutable executable;
    private final TransactionExtraConfig extraConfig;

    public TransactionInnerPayloadV1(TransactionExecutable executable, TransactionExtraConfig extraConfig) {
        this.executable = executable;
        this.extraConfig = extraConfig;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Top-level TransactionPayload enum variant
        serializer.serializeU32AsUleb128(TRANSACTION_PAYLOAD_VARIANT_PAYLOAD);
        // Inner payload V1
        serializer.serializeU32AsUleb128(TRANSACTION_INNER_PAYLOAD_VARIANT_V1);
        // Executable + Extra config
        executable.serialize(serializer);
        extraConfig.serialize(serializer);
    }
}
