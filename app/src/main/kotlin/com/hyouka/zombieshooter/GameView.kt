package com.hyouka.zombieshooter

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GameView(context: Context) : GLSurfaceView(context) {
    private val renderer = Renderer()
    private var leftPointer = -1
    private var rightPointer = -1
    private var leftX = 0f
    private var leftY = 0f
    private var lastLookX = 0f
    private var lastLookY = 0f

    var onMenuRequested: (() -> Unit)? = null

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 24, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        isFocusable = true
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val action = e.actionMasked
        val index = e.actionIndex
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val id = e.getPointerId(index)
                val x = e.getX(index)
                val y = e.getY(index)
                if (x > width * 0.88f && y < height * 0.16f) {
                    onMenuRequested?.invoke()
                    return true
                }
                if (x < width * 0.45f && leftPointer == -1) {
                    leftPointer = id
                    leftX = x
                    leftY = y
                } else if (x >= width * 0.45f && rightPointer == -1) {
                    rightPointer = id
                    lastLookX = x
                    lastLookY = y
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    val x = e.getX(i)
                    val y = e.getY(i)
                    if (id == leftPointer) {
                        val dx = ((x - leftX) / (width * 0.18f)).coerceIn(-1f, 1f)
                        val dy = ((y - leftY) / (height * 0.20f)).coerceIn(-1f, 1f)
                        renderer.moveX = dx
                        renderer.moveZ = dy
                    } else if (id == rightPointer) {
                        renderer.yaw += (x - lastLookX) * 0.16f
                        renderer.pitch = (renderer.pitch - (y - lastLookY) * 0.10f).coerceIn(-45f, 45f)
                        lastLookX = x
                        lastLookY = y
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val id = if (action == MotionEvent.ACTION_POINTER_UP) e.getPointerId(index) else -1
                if (id == leftPointer || action == MotionEvent.ACTION_CANCEL) {
                    leftPointer = -1
                    renderer.moveX = 0f
                    renderer.moveZ = 0f
                }
                if (id == rightPointer || action == MotionEvent.ACTION_CANCEL) {
                    rightPointer = -1
                }
            }
        }
        return true
    }

    private class Renderer : GLSurfaceView.Renderer {
        var moveX = 0f
        var moveZ = 0f
        var yaw = 0f
        var pitch = 0f

        private val projection = FloatArray(16)
        private val view = FloatArray(16)
        private val model = FloatArray(16)
        private val vp = FloatArray(16)
        private val mvp = FloatArray(16)

        private lateinit var cube: Cube
        private lateinit var shader: Shader

        private var px = 0f
        private var pz = 8f
        private var lastNs = System.nanoTime()

        private val zombies = arrayListOf(
            floatArrayOf(-7f, -6f),
            floatArrayOf(7f, -8f),
            floatArrayOf(-10f, -15f),
            floatArrayOf(10f, -18f)
        )

        override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
            GLES20.glClearColor(0.055f, 0.075f, 0.10f, 1f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDepthFunc(GLES20.GL_LEQUAL)
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            shader = Shader()
            cube = Cube()
        }

        override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            val aspect = width.toFloat() / height.coerceAtLeast(1)
            Matrix.perspectiveM(projection, 0, 68f, aspect, 0.1f, 100f)
        }

        override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
            val now = System.nanoTime()
            val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastNs = now

            update(dt)

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

            val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
            val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()
            val fx = (sin(yawRad) * cos(pitchRad))
            val fy = sin(pitchRad)
            val fz = (-cos(yawRad) * cos(pitchRad))

            Matrix.setLookAtM(
                view, 0,
                px, 1.65f, pz,
                px + fx, 1.65f + fy, pz + fz,
                0f, 1f, 0f
            )
            Matrix.multiplyMM(vp, 0, projection, 0, view, 0)

            shader.use()
            drawWorld()
            drawZombies()
        }

        private fun update(dt: Float) {
            val speed = 4.0f
            val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
            val forwardX = sin(yawRad)
            val forwardZ = -cos(yawRad)
            val rightX = cos(yawRad)
            val rightZ = sin(yawRad)

            val dx = (forwardX * -moveZ + rightX * moveX) * speed * dt
            val dz = (forwardZ * -moveZ + rightZ * moveX) * speed * dt
            px = (px + dx).coerceIn(-17.5f, 17.5f)
            pz = (pz + dz).coerceIn(-17.5f, 17.5f)
        }

        private fun drawWorld() {
            // Large floor and four walls, leaving the camera in an open central area.
            box(0f, -0.35f, 0f, 36f, 0.7f, 36f, 0.16f, 0.20f, 0.16f)
            box(0f, 2.5f, -18f, 36f, 5f, 0.5f, 0.12f, 0.16f, 0.14f)
            box(0f, 2.5f, 18f, 36f, 5f, 0.5f, 0.12f, 0.16f, 0.14f)
            box(-18f, 2.5f, 0f, 0.5f, 5f, 36f, 0.12f, 0.16f, 0.14f)
            box(18f, 2.5f, 0f, 0.5f, 5f, 36f, 0.12f, 0.16f, 0.14f)

            box(-6f, 1f, -5f, 3f, 2f, 3f, 0.22f, 0.28f, 0.25f)
            box(7f, 1.25f, -3f, 2.5f, 2.5f, 2.5f, 0.24f, 0.30f, 0.27f)
            box(-9f, 0.8f, 7f, 4f, 1.6f, 2f, 0.20f, 0.26f, 0.23f)
        }

        private fun drawZombies() {
            for (z in zombies) {
                box(z[0], 1.05f, z[1], 1.1f, 2.1f, 0.75f, 0.30f, 0.42f, 0.30f)
                box(z[0], 2.55f, z[1], 0.85f, 0.85f, 0.75f, 0.38f, 0.50f, 0.36f)
            }
        }

        private fun box(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, x, y, z)
            Matrix.scaleM(model, 0, sx, sy, sz)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            shader.color(r, g, b)
            shader.matrix(mvp)
            cube.draw(shader.positionHandle)
        }
    }

    private class Shader {
        private val program: Int
        val positionHandle: Int
        private val matrixHandle: Int
        private val colorHandle: Int

        init {
            val vs = """
                uniform mat4 uMVP;
                attribute vec3 aPosition;
                void main() {
                    gl_Position = uMVP * vec4(aPosition, 1.0);
                }
            """.trimIndent()
            val fs = """
                precision mediump float;
                uniform vec4 uColor;
                void main() {
                    gl_FragColor = uColor;
                }
            """.trimIndent()

            val v = compile(GLES20.GL_VERTEX_SHADER, vs)
            val f = compile(GLES20.GL_FRAGMENT_SHADER, fs)
            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, v)
            GLES20.glAttachShader(program, f)
            GLES20.glLinkProgram(program)

            val status = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
            require(status[0] != 0) { "OpenGL shader link failed" }

            positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            matrixHandle = GLES20.glGetUniformLocation(program, "uMVP")
            colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        }

        fun use() = GLES20.glUseProgram(program)

        fun matrix(m: FloatArray) = GLES20.glUniformMatrix4fv(matrixHandle, 1, false, m, 0)

        fun color(r: Float, g: Float, b: Float) {
            GLES20.glUniform4f(colorHandle, r, g, b, 1f)
        }

        private fun compile(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            require(status[0] != 0) { GLES20.glGetShaderInfoLog(shader) }
            return shader
        }
    }

    private class Cube {
        private val buffer: FloatBuffer
        init {
            val vertices = floatArrayOf(
                -1f,-1f,1f,  1f,-1f,1f,  1f,1f,1f,
                -1f,-1f,1f,  1f,1f,1f, -1f,1f,1f,
                1f,-1f,1f,  1f,-1f,-1f, 1f,1f,-1f,
                1f,-1f,1f,  1f,1f,-1f, 1f,1f,1f,
                1f,-1f,-1f, -1f,-1f,-1f, -1f,1f,-1f,
                1f,-1f,-1f, -1f,1f,-1f, 1f,1f,-1f,
                -1f,-1f,-1f, -1f,-1f,1f, -1f,1f,1f,
                -1f,-1f,-1f, -1f,1f,1f, -1f,1f,-1f,
                -1f,1f,1f, 1f,1f,1f, 1f,1f,-1f,
                -1f,1f,1f, 1f,1f,-1f, -1f,1f,-1f,
                -1f,-1f,-1f, 1f,-1f,-1f, 1f,-1f,1f,
                -1f,-1f,-1f, 1f,-1f,1f, -1f,-1f,1f
            )
            buffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            buffer.put(vertices).position(0)
        }

        fun draw(positionHandle: Int) {
            buffer.position(0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)
            GLES20.glDisableVertexAttribArray(positionHandle)
        }
    }
}
