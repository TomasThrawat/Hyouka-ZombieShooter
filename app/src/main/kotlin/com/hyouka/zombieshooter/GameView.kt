package com.hyouka.zombieshooter

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

class GameView(context: Context) : GLSurfaceView(context) {
    private val r = Renderer()
    private var left = -1
    private var right = -1
    private var lx = 0f
    private var ly = 0f
    init {
        setEGLContextClientVersion(2)
        setRenderer(r)
        renderMode = RENDERMODE_CONTINUOUSLY
        isFocusable = true
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i=e.actionIndex; val id=e.getPointerId(i); val x=e.getX(i)
                if (x < width*.45f && left<0) left=id else if(right<0){right=id;lx=x;ly=e.getY(i);r.fire=true}
            }
            MotionEvent.ACTION_MOVE -> for(i in 0 until e.pointerCount){
                val id=e.getPointerId(i); val x=e.getX(i); val y=e.getY(i)
                if(id==left){r.mx=((x-width*.2f)/(width*.16f)).coerceIn(-1f,1f);r.my=((y-height*.7f)/(height*.18f)).coerceIn(-1f,1f)}
                if(id==right){r.yaw+=(x-lx)*.22f;r.pitch=(r.pitch-(y-ly)*.12f).coerceIn(-35f,35f);lx=x;ly=y}
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val id=if(e.actionMasked==MotionEvent.ACTION_CANCEL) -2 else e.getPointerId(e.actionIndex)
                if(id==left || e.actionMasked==MotionEvent.ACTION_CANCEL){left=-1;r.mx=0f;r.my=0f}
                if(id==right || e.actionMasked==MotionEvent.ACTION_CANCEL){right=-1;r.fire=false}
            }
        }
        return true
    }
}

private class Renderer : GLSurfaceView.Renderer {
    var mx=0f; var my=0f; var yaw=0f; var pitch=0f; var fire=false
    private val p=FloatArray(16); private val v=FloatArray(16); private val m=FloatArray(16); private val q=FloatArray(16)
    private lateinit var sh: Shader
    private lateinit var cube: Cube
    private var px=0f; private var pz=8f; private var last=System.nanoTime()
    private var shot=0f; private var spawn=1f; private var wave=1
    private val z=ArrayList<Z>()
    private val rnd=java.util.Random(7)
    private data class Z(var x:Float,var zz:Float,var hp:Float,var speed:Float,var type:Int)

    override fun onSurfaceCreated(gl:javax.microedition.khronos.opengles.GL10?,c:javax.microedition.khronos.egl.EGLConfig?){
        GLES20.glClearColor(.01f,.012f,.015f,1f);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_CULL_FACE)
        sh=Shader();cube=Cube();repeat(6){spawnZ()}
    }
    override fun onSurfaceChanged(gl:javax.microedition.khronos.opengles.GL10?,w:Int,h:Int){
        GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(p,0,65f,w.toFloat()/max(1,h),.1f,100f)
    }
    override fun onDrawFrame(gl:javax.microedition.khronos.opengles.GL10?){
        val now=System.nanoTime();val dt=min(.05f,(now-last)/1e9f);last=now;update(dt)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val y=Math.toRadians(yaw.toDouble()).toFloat();val pp=Math.toRadians(pitch.toDouble()).toFloat()
        val fx=sin(y)*cos(pp);val fy=sin(pp);val fz=-cos(y)*cos(pp)
        Matrix.setLookAtM(v,0,px,1.65f,pz,px+fx,1.65f+fy,pz+fz,0f,1f,0f)
        sh.use(); world(); gun()
    }
    private fun update(dt:Float){
        val y=Math.toRadians(yaw.toDouble()).toFloat();val fx=sin(y);val fz=-cos(y);val rx=cos(y);val rz=sin(y)
        px+=(fx*-my+rx*mx)*4.2f*dt;pz+=(fz*-my+rz*mx)*4.2f*dt
        px=px.coerceIn(-18f,18f);pz=pz.coerceIn(-18f,18f)
        spawn-=dt;if(spawn<=0&&z.size<5+wave*2){spawnZ();spawn=max(.35f,1.1f-wave*.03f)}
        for(a in z){val dx=px-a.x;val dz=pz-a.zz;val d=hypot(dx,dz).coerceAtLeast(.001f);if(d>1.4f){a.x+=dx/d*a.speed*dt;a.zz+=dz/d*a.speed*dt}}
        shot-=dt;if(fire&&shot<=0){shoot();shot=.16f}
        if(z.isEmpty()){wave++;repeat(min(3+wave,10)){spawnZ()}}
    }
    private fun spawnZ(){val a=rnd.nextFloat()*16f-8f;val e=rnd.nextInt(4);val x:Float;val zz:Float
        when(e){0->{x=-18f;zz=a};1->{x=18f;zz=a};2->{x=a;zz=-18f};else->{x=a;zz=18f}}
        val t=rnd.nextInt(3);z.add(Z(x,zz,2f+t*1.5f,.8f+t*.35f,t))
    }
    private fun shoot(){
        val y=Math.toRadians(yaw.toDouble()).toFloat();val pp=Math.toRadians(pitch.toDouble()).toFloat()
        val dx=sin(y)*cos(pp);val dy=sin(pp);val dz=-cos(y)*cos(pp);var hit:Z?=null;var bt=999f
        for(a in z){val vx=a.x-px;val vy=1.2f;val vz=a.zz-pz;val t=vx*dx+vy*dy+vz*dz;if(t<=0||t>=bt)continue
            val ax=px+dx*t;val ay=1.65f+dy*t;val az=pz+dz*t;val d=sqrt((a.x-ax).pow(2)+(1.2f-ay).pow(2)+(a.zz-az).pow(2))
            if(d<1.1f){hit=a;bt=t}}
        hit?.let{it.hp-=1f;if(it.hp<=0)z.remove(it)}
    }
    private fun world(){
        box(0f,-.5f,0f,40f,1f,40f,.09f,.1f,.12f)
        box(0f,3f,-20f,40f,6f,1f,.06f,.07f,.08f);box(-20f,3f,0f,1f,6f,40f,.07f,.08f,.09f)
        box(20f,3f,0f,1f,6f,40f,.07f,.08f,.09f);box(0f,3f,20f,40f,6f,1f,.06f,.07f,.08f)
        repeat(7){i->box(-9+i*3f,1f,-5f,1.5f,2f,3f,.22f,.23f,.25f)}
        repeat(6){i->box(-8+i*3.2f,1.1f,7f,2f,2.2f,2f,.14f,.2f,.16f)}
        for(a in z){val c=when(a.type){0->floatArrayOf(.22f,.62f,.25f);1->floatArrayOf(.62f,.23f,.18f);else->floatArrayOf(.52f,.18f,.62f)}
            box(a.x,1.05f,a.zz,1f,2.1f,.8f,c[0],c[1],c[2]);box(a.x,2.35f,a.zz,.72f,.72f,.72f,.7f,.72f,.58f)
            box(a.x-.38f,1.15f,a.zz,.25f,1.3f,.35f,c[0]*.8f,c[1]*.8f,c[2]*.8f);box(a.x+.38f,1.15f,a.zz,.25f,1.3f,.35f,c[0]*.8f,c[1]*.8f,c[2]*.8f)}
    }
    private fun gun(){val y=Math.toRadians(yaw.toDouble()).toFloat();box(px+sin(y)*.65f+cos(y)*.35f,1.25f,pz-cos(y)*.65f+sin(y)*.35f,.32f,.28f,1.35f,.05f,.05f,.06f)}
    private fun box(x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,r:Float,g:Float,b:Float){
        Matrix.setIdentityM(m,0);Matrix.translateM(m,0,x,y,z);Matrix.scaleM(m,0,sx,sy,sz);Matrix.multiplyMM(q,0,v,0,m,0);Matrix.multiplyMM(q,0,p,0,q,0)
        sh.color(r,g,b);sh.matrix(q);cube.draw(sh.pos())
    }
    private class Shader{
        private val prog:Int;private val ap:Int;private val um:Int;private val uc:Int
        init{val vs="uniform mat4 uM;attribute vec3 aP;void main(){gl_Position=uM*vec4(aP,1.0);}"
            val fs="precision mediump float;uniform vec4 uC;void main(){gl_FragColor=uC;}"
            prog=GLES20.glCreateProgram();val a=compile(GLES20.GL_VERTEX_SHADER,vs);val b=compile(GLES20.GL_FRAGMENT_SHADER,fs)
            GLES20.glAttachShader(prog,a);GLES20.glAttachShader(prog,b);GLES20.glLinkProgram(prog);ap=GLES20.glGetAttribLocation(prog,"aP");um=GLES20.glGetUniformLocation(prog,"uM");uc=GLES20.glGetUniformLocation(prog,"uC")}
        private fun compile(t:Int,s:String):Int{val x=GLES20.glCreateShader(t);GLES20.glShaderSource(x,s);GLES20.glCompileShader(x);return x}
        fun use(){GLES20.glUseProgram(prog);GLES20.glEnableVertexAttribArray(ap)}
        fun color(r:Float,g:Float,b:Float){GLES20.glUniform4f(uc,r.coerceIn(0f,1f),g.coerceIn(0f,1f),b.coerceIn(0f,1f),1f)}
        fun matrix(x:FloatArray){GLES20.glUniformMatrix4fv(um,1,false,x,0)}
        fun pos()=ap
    }
    private class Cube{
        private val b:FloatBuffer
        private val v=floatArrayOf(-1f,-1f,1f,1f,-1f,1f,1f,1f,1f,-1f,1f,1f,-1f,-1f,-1f,-1f,1f,-1f,1f,1f,-1f,1f,-1f,-1f,-1f,1f,-1f,-1f,1f,1f,1f,1f,1f,1f,1f,-1f,-1f,-1f,1f,-1f,-1f,1f,-1f,1f,-1f,-1f,1f,1f,-1f,-1f,1f,1f,-1f,1f,1f,1f,1f,-1f,1f,-1f,-1f,-1f,-1f,-1f,1f,-1f,1f,1f,-1f,1f,1f)
        init{b=ByteBuffer.allocateDirect(v.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(v).position(0)}
        fun draw(s:Int){b.position(0);GLES20.glVertexAttribPointer(s,3,GLES20.GL_FLOAT,false,0,b);repeat(6){GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN,it*4,4)}}
    }
}
