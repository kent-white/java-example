package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.types.Identifier;
import com.aptoslabs.japtos.types.ModuleId;
import com.aptoslabs.japtos.types.TransactionArgument;
import com.aptoslabs.japtos.types.TypeTag;
import com.aptoslabs.japtos.types.MoveOption;
import com.aptoslabs.japtos.types.EntryFunctionPayload;
import java.util.Arrays;

import java.io.IOException;
import java.util.List;

/**
 * TransactionExecutable variant for EntryFunction, matching TS SDK's TransactionExecutableEntryFunction.
 */
public class TransactionExecutableEntryFunction implements TransactionExecutable {
    private static final int TRANSACTION_EXECUTABLE_VARIANT_ENTRY_FUNCTION = 1; // matches TS enum

    private final ModuleId moduleId;
    private final Identifier functionName;
    private final List<TypeTag> typeArguments;
    private final List<TransactionArgument> arguments;
    private final EntryFunctionPayload prebuiltPayload; // optional: reuse existing payload serialization

    public TransactionExecutableEntryFunction(
            ModuleId moduleId,
            Identifier functionName,
            List<TypeTag> typeArguments,
            List<TransactionArgument> arguments
    ) {
        this.moduleId = moduleId;
        this.functionName = functionName;
        this.typeArguments = typeArguments;
        this.arguments = arguments;
        this.prebuiltPayload = null;
    }

    private TransactionExecutableEntryFunction(EntryFunctionPayload payload) {
        this.moduleId = null;
        this.functionName = null;
        this.typeArguments = null;
        this.arguments = null;
        this.prebuiltPayload = payload;
    }

    public static TransactionExecutableEntryFunction fromEntryFunctionPayload(EntryFunctionPayload payload) {
        return new TransactionExecutableEntryFunction(payload);
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Write TransactionExecutable variant tag: EntryFunction = 1
        serializer.serializeU32AsUleb128(TRANSACTION_EXECUTABLE_VARIANT_ENTRY_FUNCTION);

        if (prebuiltPayload != null) {
            // Reuse existing EntryFunctionPayload serialization, dropping the TransactionPayload variant tag (EntryFunction = 2)
            byte[] bytes = prebuiltPayload.bcsToBytes();
            if (bytes.length == 0) {
                throw new IOException("Prebuilt payload serialized to empty bytes");
            }
            // Drop leading ULEB for variant=2 (0x02)
            int offset = 1;
            if ((bytes[0] & 0xFF) != 0x02) {
                // Fallback: if not a single-byte 0x02, still attempt to skip one byte (variant should be 2)
                // This keeps us robust if future encoding differs.
                // In current SDK, variant 2 encodes to 0x02.
                offset = 1;
            }
            byte[] inner = Arrays.copyOfRange(bytes, offset, bytes.length);
            serializer.writeBytesDirect(inner);
            return;
        }

        // Serialize the inner EntryFunction (WITHOUT TransactionPayload variant)
        // 1) ModuleId
        moduleId.serialize(serializer);
        // 2) function name
        functionName.serialize(serializer);
        // 3) type args
        serializer.serializeU32AsUleb128(typeArguments.size());
        for (TypeTag typeTag : typeArguments) {
            typeTag.serialize(serializer);
        }
        // 4) arguments - each is length-prefixed bytes of the argument BCS value
        serializer.serializeU32AsUleb128(arguments.size());
        for (TransactionArgument argument : arguments) {
            byte[] argBytes;
            if (argument instanceof TransactionArgument.AccountAddress) {
                argBytes = ((TransactionArgument.AccountAddress) argument).serializeForEntryFunction();
            } else if (argument instanceof TransactionArgument.U64) {
                argBytes = ((TransactionArgument.U64) argument).serializeForEntryFunction();
            } else if (argument instanceof MoveOption) {
                // MoveOption must serialize as entry function bytes
                argBytes = ((MoveOption<?>) argument).serializeForEntryFunction();
            } else {
                // Fallback to provided serialization (mirrors EntryFunctionPayload.java behavior)
                argBytes = argument.bcsToBytes();
            }
            serializer.serializeBytes(argBytes);
        }
    }
}
