package com.animania.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.Set;

/** Native ModelPart cube preserving arbitrary legacy eight-vertex cuboids. */
public final class LegacyMeshCube extends ModelPart.Cube {
    private static final int[][] FACE_VERTICES = {
            {5, 1, 2, 6}, {0, 4, 7, 3}, {5, 4, 0, 1},
            {2, 3, 7, 6}, {1, 0, 3, 2}, {4, 5, 6, 7}
    };
    private final float[][] vertices;
    private final int[][] textureRects;
    private final float textureWidth;
    private final float textureHeight;

    public LegacyMeshCube(float[][] vertices, int[][] textureRects, int textureWidth, int textureHeight) {
        super(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false,
                textureWidth, textureHeight, Set.of());
        if (vertices.length != 8 || textureRects.length != 6) {
            throw new IllegalArgumentException("Legacy cuboids require eight vertices and six texture rectangles");
        }
        this.vertices = vertices;
        this.textureRects = textureRects;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                        float red, float green, float blue, float alpha) {
        for (int face = 0; face < FACE_VERTICES.length; face++) {
            int[] indices = FACE_VERTICES[face];
            float[] p0 = vertices[indices[0]], p1 = vertices[indices[1]], p2 = vertices[indices[2]];
            Vector3f edgeA = new Vector3f(p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]);
            Vector3f edgeB = new Vector3f(p2[0] - p1[0], p2[1] - p1[1], p2[2] - p1[2]);
            Vector3f normal = edgeA.cross(edgeB);
            if (normal.lengthSquared() > 1.0e-8F) normal.normalize();
            pose.normal().transform(normal).normalize();
            int[] uv = textureRects[face];
            float[][] mappedUv = {
                    {uv[2] / textureWidth, uv[1] / textureHeight}, {uv[0] / textureWidth, uv[1] / textureHeight},
                    {uv[0] / textureWidth, uv[3] / textureHeight}, {uv[2] / textureWidth, uv[3] / textureHeight}
            };
            for (int corner = 0; corner < 4; corner++) {
                float[] source = vertices[indices[corner]];
                Vector4f position = pose.pose().transform(new Vector4f(source[0] / 16.0F, source[1] / 16.0F,
                        source[2] / 16.0F, 1.0F));
                consumer.vertex(position.x(), position.y(), position.z(), red, green, blue, alpha,
                        mappedUv[corner][0], mappedUv[corner][1], overlay, light,
                        normal.x(), normal.y(), normal.z());
            }
        }
    }
}
