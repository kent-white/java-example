package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;

import java.io.IOException;

/**
 * TransactionExtraConfig V1, matching TS SDK (multisig address option + replayProtectionNonce option).
 */
public class TransactionExtraConfigV1 implements TransactionExtraConfig {
    private static final int TRANSACTION_EXTRA_CONFIG_VARIANT_V1 = 0; // matches TS enum

    private final AccountAddress multisigAddress; // optional
    private final Long replayProtectionNonce; // optional

    public TransactionExtraConfigV1(AccountAddress multisigAddress, Long replayProtectionNonce) {
        this.multisigAddress = multisigAddress;
        this.replayProtectionNonce = replayProtectionNonce;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Write TransactionExtraConfig variant tag: V1 = 0
        serializer.serializeU32AsUleb128(TRANSACTION_EXTRA_CONFIG_VARIANT_V1);

        // Serialize Option<AccountAddress>
        if (multisigAddress == null) {
            serializer.serializeBool(false);
        } else {
            serializer.serializeBool(true);
            serializer.serializeAccountAddress(multisigAddress);
        }

        // Serialize Option<U64> for replayProtectionNonce
        if (replayProtectionNonce == null) {
            serializer.serializeBool(false);
        } else {
            serializer.serializeBool(true);
            serializer.serializeU64(replayProtectionNonce);
        }
    }
}
