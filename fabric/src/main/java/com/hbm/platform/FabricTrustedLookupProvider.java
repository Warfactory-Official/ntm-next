// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2025 Burning_TNT <pangyl08@163.com>
// SPDX-License-Identifier: MIT AND LGPL-3.0-only

package com.hbm.platform;

import com.hbm.platform.services.ITrustedLookupProvider;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public final class FabricTrustedLookupProvider implements ITrustedLookupProvider {
    private static MethodHandles.Lookup lookup;

    static {
        try {
            SequenceLayout VL_JNIInvokeInterface =
                    MemoryLayout.sequenceLayout(8L, ValueLayout.ADDRESS);
            AddressLayout VL_P_JNIInvokeInterface =
                    ValueLayout.ADDRESS.withTargetLayout(VL_JNIInvokeInterface);
            AddressLayout VL_PP_JNIInvokeInterface =
                    ValueLayout.ADDRESS.withTargetLayout(VL_P_JNIInvokeInterface);
            SequenceLayout VL_JNINativeInterface =
                    MemoryLayout.sequenceLayout(233L, ValueLayout.ADDRESS);
            AddressLayout VL_P_JNINativeInterface =
                    ValueLayout.ADDRESS.withTargetLayout(VL_JNINativeInterface);
            AddressLayout VL_PP_JNINativeInterface =
                    ValueLayout.ADDRESS.withTargetLayout(VL_P_JNINativeInterface);
            MemoryLayout VL_JVALUE =
                    MemoryLayout.unionLayout(
                            ValueLayout.JAVA_BOOLEAN.withName("z"),
                            ValueLayout.JAVA_BYTE.withName("b"),
                            ValueLayout.JAVA_CHAR.withName("c"),
                            ValueLayout.JAVA_SHORT.withName("s"),
                            ValueLayout.JAVA_INT.withName("i"),
                            ValueLayout.JAVA_LONG.withName("j"),
                            ValueLayout.JAVA_FLOAT.withName("f"),
                            ValueLayout.JAVA_DOUBLE.withName("d"),
                            ValueLayout.ADDRESS.withName("l"));
            Linker LINKER = Linker.nativeLinker();

            try (Arena ARENA = Arena.ofConfined()) {
                SymbolLookup JVM = SymbolLookup.libraryLookup(System.mapLibraryName("jvm"), ARENA);
                MethodHandle JNI_GetCreatedJavaVMs =
                        LINKER.downcallHandle(
                                JVM.find("JNI_GetCreatedJavaVMs")
                                        .orElseThrow(
                                                () ->
                                                        new IllegalStateException(
                                                                "JNI_GetCreatedJavaVMs must exist.")),
                                FunctionDescriptor.of(
                                        ValueLayout.JAVA_INT,
                                        ValueLayout.ADDRESS,
                                        ValueLayout.JAVA_INT,
                                        ValueLayout.ADDRESS));
                MethodHandle JVM_LatestUserDefinedLoader =
                        LINKER.downcallHandle(
                                JVM.find("JVM_LatestUserDefinedLoader")
                                        .orElseThrow(
                                                () ->
                                                        new IllegalStateException(
                                                                "JVM_LatestUserDefinedLoader must exist.")),
                                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

                MemorySegment pVM = ARENA.allocate(VL_PP_JNIInvokeInterface);
                MemorySegment nVMs = ARENA.allocate(ValueLayout.JAVA_INT);
                int ec = (int) JNI_GetCreatedJavaVMs.invokeExact(pVM, 1, nVMs);
                if (ec != 0) {
                    throw new IllegalStateException(
                            "JNI_GetCreatedJavaVMs returned error code " + ec);
                }
                if (nVMs.get(ValueLayout.JAVA_INT, 0L) != 1) {
                    throw new IllegalStateException("There must be one VM.");
                }

                MethodHandle GetEnv =
                        LINKER.downcallHandle(
                                pVM.get(VL_PP_JNIInvokeInterface, 0L)
                                        .get(VL_P_JNIInvokeInterface, 0L)
                                        .getAtIndex(ValueLayout.ADDRESS, 6L),
                                FunctionDescriptor.of(
                                        ValueLayout.JAVA_INT,
                                        VL_PP_JNIInvokeInterface,
                                        VL_PP_JNINativeInterface,
                                        ValueLayout.JAVA_INT));
                MemorySegment ppEnv = ARENA.allocate(VL_PP_JNINativeInterface);
                ec = (int) GetEnv.invokeExact(pVM, ppEnv, 0x00010008);
                if (ec != 0) {
                    throw new IllegalStateException("GetEnv returned error code " + ec);
                }

                MemorySegment pEnv = ppEnv.get(VL_PP_JNINativeInterface, 0L);
                MemorySegment pJNINativeInterface = pEnv.get(VL_P_JNINativeInterface, 0L);
                MethodHandle FindClass =
                        LINKER.downcallHandle(
                                        pJNINativeInterface.getAtIndex(ValueLayout.ADDRESS, 6L),
                                        FunctionDescriptor.of(
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS))
                                .bindTo(pEnv);
                MethodHandle NewGlobalRef =
                        LINKER.downcallHandle(
                                        pJNINativeInterface.getAtIndex(ValueLayout.ADDRESS, 21L),
                                        FunctionDescriptor.of(
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS))
                                .bindTo(pEnv);
                MethodHandle DeleteGlobalRef =
                        LINKER.downcallHandle(
                                        pJNINativeInterface.getAtIndex(ValueLayout.ADDRESS, 22L),
                                        FunctionDescriptor.ofVoid(
                                                ValueLayout.ADDRESS, ValueLayout.ADDRESS))
                                .bindTo(pEnv);
                MethodHandle GetMethodID =
                        LINKER.downcallHandle(
                                        pJNINativeInterface.getAtIndex(ValueLayout.ADDRESS, 33L),
                                        FunctionDescriptor.of(
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS))
                                .bindTo(pEnv);
                MethodHandle CallObjectMethodA =
                        LINKER.downcallHandle(
                                        pJNINativeInterface.getAtIndex(ValueLayout.ADDRESS, 36L),
                                        FunctionDescriptor.of(
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS))
                                .bindTo(pEnv);
                MethodHandle GetStaticFieldID =
                        LINKER.downcallHandle(
                                        pJNINativeInterface.getAtIndex(ValueLayout.ADDRESS, 144L),
                                        FunctionDescriptor.of(
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS))
                                .bindTo(pEnv);
                MethodHandle GetStaticObjectField =
                        LINKER.downcallHandle(
                                        pJNINativeInterface.getAtIndex(ValueLayout.ADDRESS, 145L),
                                        FunctionDescriptor.of(
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS))
                                .bindTo(pEnv);
                MethodHandle SetStaticObjectField =
                        LINKER.downcallHandle(
                                        pJNINativeInterface.getAtIndex(ValueLayout.ADDRESS, 154L),
                                        FunctionDescriptor.ofVoid(
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS))
                                .bindTo(pEnv);
                MethodHandle NewStringUTF =
                        LINKER.downcallHandle(
                                        pJNINativeInterface.getAtIndex(ValueLayout.ADDRESS, 167L),
                                        FunctionDescriptor.of(
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS,
                                                ValueLayout.ADDRESS))
                                .bindTo(pEnv);

                MemorySegment lookupJClass =
                        (MemorySegment)
                                FindClass.invokeExact(
                                        ARENA.allocateFrom(
                                                "java/lang/invoke/MethodHandles$Lookup"));
                MemorySegment lookupJClassRef =
                        (MemorySegment) NewGlobalRef.invokeExact(lookupJClass);
                MemorySegment lookupJFieldID =
                        (MemorySegment)
                                GetStaticFieldID.invokeExact(
                                        lookupJClassRef,
                                        ARENA.allocateFrom("IMPL_LOOKUP"),
                                        ARENA.allocateFrom(
                                                "Ljava/lang/invoke/MethodHandles$Lookup;"));
                MemorySegment lookupJObject =
                        (MemorySegment)
                                GetStaticObjectField.invokeExact(lookupJClassRef, lookupJFieldID);
                MemorySegment lookupJObjectRef =
                        (MemorySegment) NewGlobalRef.invokeExact(lookupJObject);

                MemorySegment classLoaderJClass =
                        (MemorySegment)
                                FindClass.invokeExact(ARENA.allocateFrom("java/lang/ClassLoader"));
                MemorySegment classLoaderJClassRef =
                        (MemorySegment) NewGlobalRef.invokeExact(classLoaderJClass);
                MemorySegment loadClassJMethodID =
                        (MemorySegment)
                                GetMethodID.invokeExact(
                                        classLoaderJClassRef,
                                        ARENA.allocateFrom("loadClass"),
                                        ARENA.allocateFrom(
                                                "(Ljava/lang/String;)Ljava/lang/Class;"));
                MemorySegment targetJClassArguments = ARENA.allocate(VL_JVALUE, 1L);
                MemorySegment targetJName =
                        (MemorySegment)
                                NewStringUTF.invokeExact(
                                        ARENA.allocateFrom(
                                                FabricTrustedLookupProvider.class.getName()));
                MemorySegment targetJNameRef =
                        (MemorySegment) NewGlobalRef.invokeExact(targetJName);
                targetJClassArguments.set(ValueLayout.ADDRESS, 0L, targetJNameRef);
                MemorySegment targetJClass =
                        (MemorySegment)
                                CallObjectMethodA.invokeExact(
                                        (MemorySegment)
                                                JVM_LatestUserDefinedLoader.invokeExact(pEnv),
                                        loadClassJMethodID,
                                        targetJClassArguments);
                MemorySegment targetJClassRef =
                        (MemorySegment) NewGlobalRef.invokeExact(targetJClass);
                MemorySegment targetJFieldID =
                        (MemorySegment)
                                GetStaticFieldID.invokeExact(
                                        targetJClassRef,
                                        ARENA.allocateFrom("lookup"),
                                        ARENA.allocateFrom(
                                                "Ljava/lang/invoke/MethodHandles$Lookup;"));
                SetStaticObjectField.invokeExact(targetJClassRef, targetJFieldID, lookupJObjectRef);

                DeleteGlobalRef.invoke(lookupJClassRef);
                DeleteGlobalRef.invoke(lookupJObjectRef);
                DeleteGlobalRef.invoke(classLoaderJClassRef);
                DeleteGlobalRef.invoke(targetJNameRef);
                DeleteGlobalRef.invoke(targetJClassRef);
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    @Override
    public MethodHandles.Lookup implLookup() {
        return lookup;
    }
}
