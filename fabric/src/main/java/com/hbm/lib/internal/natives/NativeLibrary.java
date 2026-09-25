// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal.natives;

import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NativeLibrary {

    private static final Logger LOG = LoggerFactory.getLogger("NTM");

    private static final String RESOURCE_DIR = "ntm_natives/";

    public static final String LIB_BASE_NAME = "ntm_next_native";

    private static final int EXPECTED_ABI_VERSION = 33;

    public static final String NORMALIZED_ARCH = normalizeArch(System.getProperty("os.arch", ""));
    public static final String NORMALIZED_OS = normalizeOs(System.getProperty("os.name", ""));

    public static final Linker LINKER = openLinker();

    public static final SymbolLookup LOOKUP = Mapping.LOOKUP;

    public static final int ABI_VERSION = probeAbiVersion();

    public static final int ACTIVE_PATH = ABI_VERSION > 0 ? probePath() : -1;

    public static final boolean AVAILABLE = ABI_VERSION > 0 && ACTIVE_PATH >= 0;

    private NativeLibrary() {}

    static MethodHandle bind(String symbol, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(find(symbol), descriptor);
    }

    static MethodHandle bindLeaf(String symbol, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(find(symbol), descriptor, Linker.Option.critical(false));
    }

    static MethodHandle bindCritical(String symbol, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(find(symbol), descriptor, Linker.Option.critical(true));
    }

    private static MemorySegment find(String symbol) {
        return LOOKUP.find(symbol)
                .orElseThrow(
                        () -> new IllegalStateException(LIB_BASE_NAME + " exports no " + symbol));
    }

    public static String artifactName(String baseName) {
        return String.format(
                "%s-%s-%s", NORMALIZED_OS, NORMALIZED_ARCH, System.mapLibraryName(baseName));
    }

    public static String resourcePath(String baseName) {
        return RESOURCE_DIR + artifactName(baseName);
    }

    public static boolean artifactPresent() {
        return NativeLibrary.class.getClassLoader().getResource(resourcePath(LIB_BASE_NAME))
                != null;
    }

    public static String availability() {
        return AVAILABLE
                ? "available, ABI " + ABI_VERSION + ", path " + pathName(ACTIVE_PATH)
                : "unavailable (" + artifactName(LIB_BASE_NAME) + ")";
    }

    private static String pathName(int path) {
        return switch (path) {
            case 0 -> "scalar";
            case 1 -> "4-wide";
            case 2 -> "8-wide";
            default -> "unknown(" + path + ")";
        };
    }

    private static Linker openLinker() {
        try {
            return Linker.nativeLinker();
        } catch (Throwable t) {
            LOG.info("Native libraries unavailable, no FFM linker ({})", t.toString());
            return null;
        }
    }

    private static int probeAbiVersion() {
        if (LOOKUP == null) return -1;
        try {
            int version =
                    (int)
                            LINKER.downcallHandle(
                                            LOOKUP.find("ntm_natives_abi_version").orElseThrow(),
                                            FunctionDescriptor.of(ValueLayout.JAVA_INT))
                                    .invokeExact();
            if (version != EXPECTED_ABI_VERSION) {
                LOG.warn(
                        "Native library reports ABI {} but these bindings target {}; using Java kernels",
                        version,
                        EXPECTED_ABI_VERSION);
                return -1;
            }
            return version;
        } catch (Throwable t) {
            LOG.warn("Native library failed its ABI probe; using Java kernels", t);
            return -1;
        }
    }

    private static int probePath() {
        if (LOOKUP == null) return -1;
        try {
            int path =
                    (int)
                            LINKER.downcallHandle(
                                            LOOKUP.find("ntm_natives_active_path").orElseThrow(),
                                            FunctionDescriptor.of(ValueLayout.JAVA_INT))
                                    .invokeExact();
            LOG.info(
                    "Native library bound: {}-{}, ABI {}, path {}",
                    NORMALIZED_OS,
                    NORMALIZED_ARCH,
                    ABI_VERSION,
                    pathName(path));
            return path;
        } catch (Throwable t) {
            LOG.warn("Native library unavailable, path probe failed; using Java kernels", t);
            return -1;
        }
    }

    private static final class Mapping {

        private static final Arena ARENA = Arena.ofAuto();

        static final SymbolLookup LOOKUP = open();

        private static SymbolLookup open() {
            if (LINKER == null) return null;
            if (!Boolean.parseBoolean(System.getProperty("hbm.natives", "true"))) {
                LOG.info("Natives disabled by hbm.natives; using the Java path");
                return null;
            }
            String artifact = artifactName(LIB_BASE_NAME);
            try (InputStream in =
                    NativeLibrary.class
                            .getClassLoader()
                            .getResourceAsStream(RESOURCE_DIR + artifact)) {
                if (in == null) {
                    LOG.info("No native build for {}; using the Java path", artifact);
                    return null;
                }
                Path extracted = Files.createTempFile("ntm-", "-" + artifact);
                extracted.toFile().deleteOnExit();
                Files.copy(in, extracted, StandardCopyOption.REPLACE_EXISTING);
                return SymbolLookup.libraryLookup(extracted, ARENA);
            } catch (Throwable t) {
                LOG.warn("Failed to load {}; using the Java path", artifact, t);
                return null;
            }
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private static String normalizeArch(String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) return "x86_64";
        if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) return "x86_32";
        if ("aarch64".equals(value)) return "aarch_64";
        if (value.matches("^(arm|arm32)$")) return "arm_32";
        if ("ppc64le".equals(value)) return "ppcle_64";
        if ("ppc64".equals(value)) return "ppc_64";
        if ("s390x".equals(value)) return "s390_64";
        if ("loongarch64".equals(value)) return "loongarch_64";
        return "unknown";
    }

    private static String normalizeOs(String value) {
        value = normalize(value);
        if (value.startsWith("linux")) return "linux";
        if (value.startsWith("macosx") || value.startsWith("osx") || value.startsWith("darwin"))
            return "osx";
        if (value.startsWith("windows")) return "windows";
        if (value.startsWith("freebsd")) return "freebsd";
        if (value.startsWith("openbsd")) return "openbsd";
        if (value.startsWith("netbsd")) return "netbsd";
        if (value.startsWith("solaris") || value.startsWith("sunos")) return "sunos";
        if (value.startsWith("aix")) return "aix";
        return "unknown";
    }
}
