package io.github.xlopec.opengl.edu.ui.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.Matrix
import androidx.compose.ui.graphics.Color
import io.github.xlopec.opengl.edu.model.Editor
import io.github.xlopec.opengl.edu.model.displayCropSelection
import io.github.xlopec.opengl.edu.model.displayTransformations
import io.github.xlopec.opengl.edu.model.geometry.*
import io.github.xlopec.opengl.edu.model.transformation.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

context (GL)
internal class GlRendererDelegate(
    context: Context,
    bitmap: Bitmap,
    windowSize: Size,
) {

    private companion object {
        val VerticesBuffer = floatBufferOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
        )
        val TextureBuffer = floatBufferOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )

        val DisplayFrameBuffer = FrameBuffer(0)
    }

    private val colorTransformations = listOf(
        GrayscaleTransformation(context, VerticesBuffer, TextureBuffer),
        HsvTransformation(context, VerticesBuffer, TextureBuffer),
        ContrastTransformation(context, VerticesBuffer, TextureBuffer),
        TintTransformation(context, VerticesBuffer, TextureBuffer),
        GaussianBlurTransformation(context, VerticesBuffer, TextureBuffer),
    )

    private val projectionMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16).also { matrix ->
        Matrix.setLookAtM(matrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
    }
    private val viewTransformation = ViewTransformation(context, VerticesBuffer, TextureBuffer)
    private val cropTransformation = CropTransformation(context, VerticesBuffer, TextureBuffer)
    private val sourceFbo = Fbo.forBitmap(bitmap)
    private val transformationFbos = Fbo.forPingPong(windowSize, colorTransformations.size + 1)

    fun updateImage(
        bitmap: Bitmap,
    ) = sourceFbo.updateImage(bitmap)

    fun onExportFrame(
        backgroundColor: Color,
        editor: Editor,
        isDebugModeEnabled: Boolean,
    ): Bitmap {
        glClearColor(backgroundColor)
        val transformations = editor.displayTransformations
        val scene = transformations.scene

        transformationFbos.resizeTextures(scene.imageSize)
        GLES31.glViewport(0, 0, scene.imageSize.width, scene.imageSize.height)

        val lastColorTransformTexture = drawColorTransformations(transformations)
        val cropFbo = transformationFbos[colorTransformations.size]

        cropTransformation.clearMarks(
            transformations = transformations,
            frameBuffer = cropFbo.frameBuffer,
            sourceTexture = lastColorTransformTexture,
            isDebugEnabled = isDebugModeEnabled,
        )

        val bitmap = readTextureToBitmap(cropFbo.frameBuffer, scene.imageSize)

        transformationFbos.resizeTextures(scene.windowSize)
        return bitmap
    }

    fun onDrawCropping(
        backgroundColor: Color,
        oldEditor: Editor,
        newEditor: Editor,
        isDebugModeEnabled: Boolean,
    ) {
        glClearColor(backgroundColor)
        val transformations = newEditor.displayTransformations
        val scene = transformations.scene

        GLES31.glViewport(0, 0, scene.windowSize.width, scene.windowSize.height)

        with(oldEditor.displayTransformations.scene) {
            val topLeft = leftTopImageOffset
            val bottomRight = rightBottomImageOffset
            val croppedSize = imageSize - topLeft - bottomRight
            sourceFbo.resize(topLeft, croppedSize)
        }

        val lastColorTransformTexture = drawColorTransformations(transformations)
        val cropFbo = transformationFbos[colorTransformations.size]

        cropTransformation.clearMarks(
            transformations = transformations,
            frameBuffer = cropFbo.frameBuffer,
            sourceTexture = lastColorTransformTexture,
            isDebugEnabled = isDebugModeEnabled,
        )

        drawOnDisplay(scene, cropFbo.texture)
    }

    fun onDrawNormal(
        backgroundColor: Color,
        editor: Editor,
        isDebugModeEnabled: Boolean,
    ) {
        glClearColor(backgroundColor)
        val transformations = editor.displayTransformations
        val scene = transformations.scene

        GLES31.glViewport(0, 0, scene.windowSize.width, scene.windowSize.height)

        val lastColorTransformTexture = drawColorTransformations(transformations)
        val cropFbo = transformationFbos[colorTransformations.size]

        if (editor.displayCropSelection) {
            cropTransformation.drawSelection(
                transformations = transformations,
                frameBuffer = cropFbo.frameBuffer,
                texture = lastColorTransformTexture,
                isDebugEnabled = isDebugModeEnabled,
            )
        } else {
            cropTransformation.drawNormal(
                frameBuffer = cropFbo.frameBuffer,
                texture = lastColorTransformTexture,
                transformations = transformations,
                isDebugEnabled = isDebugModeEnabled,
            )
        }

        drawOnDisplay(scene, cropFbo.texture)
    }

    // we're using previous texture as target for next transformations, render pipeline looks like the following
    // original texture -> grayscale transformation -> texture[1];
    // texture[1] -> hsv transformation -> texture[2];
    // ....
    // texture[last modified texture + 1] -> matrix transformation -> screen
    context (GL)
    private fun drawColorTransformations(
        transformations: Transformations,
    ) = colorTransformations.fastFoldIndexed(sourceFbo.texture) { index, sourceTexture, transformation ->
        transformation.draw(
            transformations = transformations,
            frameBuffer = transformationFbos[index].frameBuffer,
            // fbo index -> txt index mapping: 0 -> 0; 1 -> 1; 2 -> 2; 3 -> 1; 4 -> 2...
            sourceTexture = sourceTexture,
        )
        transformationFbos[index].texture
    }

    context (GL)
    private fun drawOnDisplay(
        scene: Scene,
        sourceTexture: Texture,
    ) = with(scene) {
        val frustumOffsetX = 2 * windowSize.ratio * sceneOffset.x / windowSize.width
        val frustumOffsetY = 2 * sceneOffset.y / windowSize.height

        Matrix.frustumM(
            /* m = */ projectionMatrix,
            /* offset = */ 0,
            /* left = */ -windowSize.ratio - frustumOffsetX,
            /* right = */ windowSize.ratio - frustumOffsetX,
            /* bottom = */ -1f + frustumOffsetY,
            /* top = */ 1f + frustumOffsetY,
            /* near = */ 3f,
            /* far = */ 7f
        )

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        viewTransformation.render(
            vPMatrix = vPMatrix,
            frameBuffer = DisplayFrameBuffer,
            sourceTexture = sourceTexture,
            textureSize = imageSize,
            windowSize = windowSize
        )
    }

}

context (GL)
private fun Iterable<Fbo>.resizeTextures(
    size: Size,
) = forEach { fbo ->
    fbo.texture.size = size
}

context (GL)
private fun glClearColor(
    color: Color,
) = GLES31.glClearColor(color.red, color.green, color.blue, color.alpha)

private fun floatBufferOf(
    vararg data: Float,
): FloatBuffer {
    val buff: ByteBuffer = ByteBuffer.allocateDirect(data.size * Float.SIZE_BYTES)

    buff.order(ByteOrder.nativeOrder())
    val floatBuffer = buff.asFloatBuffer()
    floatBuffer.put(data)
    floatBuffer.position(0)
    return floatBuffer
}