// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.iris;

import com.hbm.client.render.TracerRibbon;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.lib.Library;
import io.github.douira.glsl_transformer.ast.node.Version;
import io.github.douira.glsl_transformer.ast.node.declaration.TypeAndInitDeclaration;
import io.github.douira.glsl_transformer.ast.node.expression.ReferenceExpression;
import io.github.douira.glsl_transformer.ast.node.expression.binary.AssignmentExpression;
import io.github.douira.glsl_transformer.ast.node.expression.unary.FunctionCallExpression;
import io.github.douira.glsl_transformer.ast.node.type.qualifier.StorageQualifier;
import io.github.douira.glsl_transformer.ast.node.type.specifier.BuiltinNumericTypeSpecifier;
import io.github.douira.glsl_transformer.ast.print.PrintType;
import io.github.douira.glsl_transformer.ast.transform.ASTInjectionPoint;
import io.github.douira.glsl_transformer.ast.transform.SingleASTTransformer;
import io.github.douira.glsl_transformer.util.Type;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class RibbonShaderTransform {
    private static final Set<String> ALBEDO =
            Set.of("tex", "texture", "gtexture", "u_MainSampler", "Sampler0");
    private static final Set<String> SAMPLES =
            Set.of("texture", "textureLod", "textureGrad", "texelFetch");
    private static final Pattern VERSION = Pattern.compile("#version\\s+(\\d+)");
    private static final String DEPTH_DEFINE = "NTM_RIBBON_DEPTH_ZERO_TO_ONE";

    private RibbonShaderTransform() {}

    public static String vertex(String source, boolean fallback) {
        String prefix = fallback ? "" : "iris_";
        var transform = new SingleASTTransformer<>();
        var version = VERSION.matcher(source);
        if (!version.find())
            throw new IllegalArgumentException("Ribbon vertex program has no GLSL version");
        transform.getLexer().version = Version.fromNumber(Integer.parseInt(version.group(1)));
        transform.setPrintType(PrintType.SIMPLE);
        StringBuilder exportColors = new StringBuilder();
        transform.setTransformation(
                (tree, root) -> {
                    var colorReads =
                            root.nodeIndex
                                    .getStream(ReferenceExpression.class)
                                    .filter(
                                            ref ->
                                                    ref.getIdentifier()
                                                            .getName()
                                                            .equals(prefix + "Color"))
                                    .toList();
                    var colorWrites =
                            root.nodeIndex
                                    .getStream(AssignmentExpression.class)
                                    .filter(
                                            assign ->
                                                    assign.getLeft() instanceof ReferenceExpression)
                                    .filter(
                                            assign ->
                                                    colorReads.stream()
                                                            .anyMatch(
                                                                    ref ->
                                                                            ref == assign.getRight()
                                                                                    || ref
                                                                                            .hasAncestor(
                                                                                                    assign
                                                                                                            .getRight())))
                                    .map(
                                            assign ->
                                                    ((ReferenceExpression) assign.getLeft())
                                                            .getIdentifier()
                                                            .getName())
                                    .collect(Collectors.toSet());
                    var colors = new LinkedHashMap<String, String>();

                    for (var declaration :
                            root.nodeIndex.getStream(TypeAndInitDeclaration.class).toList()) {
                        if (!storage(declaration, StorageQualifier.StorageType.OUT)
                                || !(declaration.getType().getTypeSpecifier()
                                        instanceof BuiltinNumericTypeSpecifier numeric)
                                || numeric.type != Type.F32VEC3 && numeric.type != Type.F32VEC4)
                            continue;
                        for (var member : declaration.getMembers()) {
                            String name = member.getName().getName();
                            if (!colorWrites.contains(name)) continue;
                            String type = numeric.type == Type.F32VEC3 ? "vec3" : "vec4";
                            colors.put(name, type);
                            member.getName().setName(carrier(name));
                            tree.parseAndInjectNode(
                                    transform,
                                    ASTInjectionPoint.BEFORE_DECLARATIONS,
                                    type + " " + name + ";");
                            exportColors
                                    .append(carrier(name))
                                    .append(" = ")
                                    .append(name)
                                    .append(";\n");
                        }
                    }
                    RibbonCompiler.colors = colors;
                    root.replaceReferenceExpressions(
                            transform, prefix + "Position", "hbmRibbonPosition");
                    root.replaceReferenceExpressions(transform, prefix + "Color", "vec4(1.0)");
                    root.replaceReferenceExpressions(transform, prefix + "UV0", "vec2(0.5)");
                    root.replaceReferenceExpressions(
                            transform, prefix + "Normal", "hbmRibbonNormal");
                    root.rename("main", "hbmRibbonPackMain");
                    tree.parseAndInjectNodes(
                            transform,
                            ASTInjectionPoint.BEFORE_DECLARATIONS,
                            "vec3 hbmRibbonPosition;",
                            "vec3 hbmRibbonNormal;");
                });
        String modelView = fallback ? "ModelViewMat" : "iris_transforms.ModelViewMat";
        String projection = fallback ? "ProjMat" : "iris_ProjMat";
        String screen = fallback ? "ScreenSize" : "iris_globalInfo.ScreenSize";
        return transform.transform(source)
                + """

                in vec3 OtherPosition;
                in vec4 OtherColor;
                in vec2 Widths;
                vec4 hbmRibbonPackPosition(vec3 position) {
                    hbmRibbonPosition = position;
                    hbmRibbonPackMain();
                    return gl_Position;
                }
                #define NTM_RIBBON_PACK
                #define NTM_RIBBON_PROJECT(position, view) hbmRibbonPackPosition(position)
                """
                + "#define "
                + DEPTH_DEFINE
                + " "
                + WeaponRenderTypes.TRACER_PIPELINE.getShaderDefines().values().get(DEPTH_DEFINE)
                + "\n"
                + "#define NTM_RIBBON_POSITION "
                + prefix
                + "Position\n"
                + "#define NTM_RIBBON_OTHER_POSITION OtherPosition\n"
                + "#define NTM_RIBBON_COLOR "
                + prefix
                + "Color\n"
                + "#define NTM_RIBBON_OTHER_COLOR OtherColor\n"
                + "#define NTM_RIBBON_WIDTHS Widths\n"
                + "#define NTM_RIBBON_SELECTOR "
                + prefix
                + "UV0\n"
                + "#define NTM_RIBBON_MODEL_VIEW "
                + modelView
                + "\n"
                + "#define NTM_RIBBON_PROJECTION "
                + projection
                + "\n"
                + "#define NTM_RIBBON_SCREEN "
                + screen
                + "\n"
                + "#define MIN_RIBBON_WIDTH "
                + TracerRibbon.MIN_WIDTH_PIXELS
                + "\n"
                + "#define RIBBON_FILTER_PADDING "
                + TracerRibbon.FILTER_PADDING_PIXELS
                + "\n"
                + include("tracer_ribbon_vertex.glsl")
                + """

                void main() {
                    vec3 direction = OtherPosition - NTM_RIBBON_POSITION;
                    vec3 facing = cross(direction, cross(-NTM_RIBBON_POSITION, direction));
                    float magnitude = length(facing);
                    if (magnitude == 0.0) {
                        facing = -NTM_RIBBON_POSITION;
                        if (dot(facing, facing) == 0.0) facing = -OtherPosition;
                        magnitude = length(facing);
                    }
                    hbmRibbonNormal = magnitude > 0.0 ? facing / magnitude : vec3(0.0, 0.0, 1.0);
                    hbmRibbonVertex();
                """
                + exportColors
                + "}\n";
    }

    public static String fragment(String source) {
        var transform = new SingleASTTransformer<>();
        var version = VERSION.matcher(source);
        if (!version.find())
            throw new IllegalArgumentException("Ribbon fragment program has no GLSL version");
        transform.getLexer().version = Version.fromNumber(Integer.parseInt(version.group(1)));
        transform.setPrintType(PrintType.SIMPLE);
        StringBuilder importColors = new StringBuilder();
        transform.setTransformation(
                (tree, root) -> {
                    var samples =
                            root.nodeIndex
                                    .getStream(FunctionCallExpression.class)
                                    .filter(
                                            call ->
                                                    call.getFunctionName() != null
                                                            && SAMPLES.contains(
                                                                    call.getFunctionName()
                                                                            .getName()))
                                    .filter(
                                            call ->
                                                    !call.getParameters().isEmpty()
                                                            && call.getParameters().getFirst()
                                                                    instanceof
                                                                    ReferenceExpression sampler
                                                            && ALBEDO.contains(
                                                                    sampler.getIdentifier()
                                                                            .getName()))
                                    .toList();
                    root.replaceExpressions(transform, samples.stream(), "hbmRibbonAlbedo");
                    for (var declaration :
                            root.nodeIndex.getStream(TypeAndInitDeclaration.class).toList()) {
                        if (!storage(declaration, StorageQualifier.StorageType.IN)) continue;
                        for (var member : declaration.getMembers()) {
                            String name = member.getName().getName();
                            String type = RibbonCompiler.colors.get(name);
                            if (type == null) continue;
                            member.getName().setName(carrier(name));
                            tree.parseAndInjectNode(
                                    transform,
                                    ASTInjectionPoint.BEFORE_DECLARATIONS,
                                    type + " " + name + ";");
                            importColors.append(name).append(" = ").append(carrier(name));
                            if (samples.isEmpty())
                                importColors.append(
                                        type.equals("vec3")
                                                ? " * hbmRibbonAlbedo.rgb"
                                                : " * hbmRibbonAlbedo");
                            importColors.append(";\n");
                        }
                    }
                    if (samples.isEmpty() && importColors.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Ribbon program has neither an albedo sample nor a vertex-color input");
                    }
                    root.rename("main", "hbmRibbonPackMain");
                    tree.parseAndInjectNode(
                            transform,
                            ASTInjectionPoint.BEFORE_DECLARATIONS,
                            "vec4 hbmRibbonAlbedo;");
                });
        return transform.transform(source)
                + "\n"
                + include("tracer_ribbon_fragment.glsl")
                + """

                void main() {
                    float position;
                    hbmRibbonAlbedo = hbmRibbonSurface(position);
                """
                + importColors
                + "hbmRibbonPackMain();\n}\n";
    }

    private static String carrier(String name) {
        return "hbmRibbonColor_" + name;
    }

    private static boolean storage(
            TypeAndInitDeclaration declaration, StorageQualifier.StorageType type) {
        var qualifier = declaration.getType().getTypeQualifier();
        return qualifier != null
                && qualifier.getChildren().stream()
                        .anyMatch(
                                part ->
                                        part instanceof StorageQualifier storage
                                                && storage.storageType == type);
    }

    private static String include(String name) {
        return source(Library.id("shaders/include/" + name));
    }

    private static String source(Identifier location) {
        try (var stream = Minecraft.getInstance().getResourceManager().open(location)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
