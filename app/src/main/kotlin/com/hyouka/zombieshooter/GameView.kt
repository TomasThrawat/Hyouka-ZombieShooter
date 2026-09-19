package com.hyouka.zombieshooter

import android.content.Context
import android.graphics.*
import android.media.*
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class GameView(c:Context):View(c){
 enum class S{MENU,LOBBY,GAME,PAUSE,OVER}
 data class Z(var x:Float,var y:Float,var hp:Int,var type:Int,var phase:Float,var hit:Float=0f)
 data class B(var x:Float,var y:Float,var vx:Float,var vy:Float,var life:Float=0f)
 data class M(val name:String,val sky:Int,val ground:Int,val accent:Int,val seed:Int)
 val maps=listOf(
  M("ABANDONED CITY",Color.rgb(30,34,38),Color.rgb(48,49,45),Color.rgb(210,65,65),11),
  M("DEAD FOREST",Color.rgb(17,29,22),Color.rgb(34,52,37),Color.rgb(80,210,100),29),
  M("TOXIC LAB",Color.rgb(27,23,36),Color.rgb(52,43,61),Color.rgb(175,90,235),47))
 var s=S.MENU;var map=0;var hp=100f;var ammo=30;var reserve=120;var wave=1;var kills=0;var score=0
 var px=0f;var py=0f;var ax=1f;var ay=0f;var fire=0f;var reload=0f;var spawn=.2f
 var lid=-1;var rid=-1;var lx=0f;var ly=0f;var rx=0f;var ry=0f;var firing=false
 var last=SystemClock.uptimeMillis()
 val z=ArrayList<Z>();val b=ArrayList<B>();val r=java.util.Random()
 val p=Paint(3);val t=Paint(3).apply{typeface=Typeface.DEFAULT_BOLD};var snd=Fx()

 override fun onDraw(c:Canvas){
  val n=SystemClock.uptimeMillis();val dt=min(.033f,(n-last)/1000f);last=n
  when(s){S.MENU->menu(c);S.LOBBY->lobby(c);S.GAME->{update(dt);game(c)};S.PAUSE->{game(c);overlay(c,"PAUSED")};S.OVER->{game(c);over(c)}}
  if(s==S.GAME)postInvalidateOnAnimation() else postInvalidateDelayed(30)
 }
 fun bg(c:Canvas,col:Int){c.drawColor(col)}
 fun txt(c:Canvas,v:String,x:Float,y:Float,size:Float,col:Int,cen:Boolean=false){t.textSize=size;t.color=col;t.textAlign=if(cen)Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(v,x,y,t)}
 fun btn(c:Canvas,q:RectF,v:String,col:Int){p.color=Color.argb(225,7,7,9);p.style=Paint.Style.FILL;c.drawRoundRect(q,18f,18f,p);p.color=col;p.style=Paint.Style.STROKE;p.strokeWidth=3f;c.drawRoundRect(q,18f,18f,p);p.style=Paint.Style.FILL;txt(c,v,q.centerX(),q.centerY()+10,25f,Color.WHITE,true)}

 fun menu(c:Canvas){bg(c,Color.rgb(5,7,8));val w=width.toFloat();val h=height.toFloat();txt(c,"HYOUKA",w/2,h*.25f,58f,Color.WHITE,true);txt(c,"ZOMBIE SHOOTER",w/2,h*.25f+50,29f,Color.rgb(156,255,61),true);btn(c,RectF(w*.35f,h*.47f,w*.65f,h*.57f),"PLAY",Color.rgb(156,255,61));btn(c,RectF(w*.35f,h*.61f,w*.65f,h*.71f),"HOW TO PLAY",Color.rgb(100,180,255));txt(c,"3 MAPS  •  WAVES  •  TOUCH CONTROLS",w/2,h*.88f,16f,Color.LTGRAY,true)}
 fun lobby(c:Canvas){bg(c,Color.rgb(7,9,10));val w=width.toFloat();val h=height.toFloat();txt(c,"MISSION SELECT",w/2,58f,34f,Color.WHITE,true);val cw=w*.27f
  maps.forEachIndexed{i,m->val x=w*.04f+i*(cw+w*.025f);val q=RectF(x,h*.2f,x+cw,h*.72f);p.color=m.ground;c.drawRoundRect(q,20f,20f,p);txt(c,"0"+(i+1),q.left+18,q.top+38,18f,m.accent);txt(c,m.name,q.left+18,q.bottom-45,17f,Color.WHITE);if(i==map){p.color=m.accent;p.style=Paint.Style.STROKE;p.strokeWidth=5f;c.drawRoundRect(q,20f,20f,p);p.style=Paint.Style.FILL}};btn(c,RectF(w*.39f,h*.8f,w*.61f,h*.91f),"START",maps[map].accent)}
 fun start(){s=S.GAME;hp=100f;ammo=30;reserve=120;wave=1;kills=0;score=0;reload=0f;spawn=.2f;z.clear();b.clear();px=width*.5f;py=height*.55f;ax=1f;ay=0f;repeat(4){spawnZ()};snd.beep(660,80)}
 fun spawnZ(){val e=r.nextInt(4);val x=if(e==0)-40f else if(e==1)width+40f else r.nextInt(max(1,width)).toFloat();val y=if(e==2)-40f else if(e==3)height+40f else r.nextInt(max(1,height)).toFloat();val ty=r.nextInt(3);z+=Z(x,y,2+wave/3+if(ty==2)2 else 0,ty,r.nextFloat()*6.28f)}
 fun update(dt:Float){
  if(reload>0){reload-=dt;if(reload<=0){val n=min(30-ammo,reserve);ammo+=n;reserve-=n;reload=0f;snd.beep(520,100)}};fire=max(0f,fire-dt);spawn-=dt
  if(lid>=0){val dx=lx-px;val dy=ly-py;val d=hypot(dx,dy);if(d>8){px+=dx/d*240*dt;py+=dy/d*240*dt}}
  px=px.coerceIn(55f,width-55f);py=py.coerceIn(85f,height-55f)
  if(rid>=0){val dx=rx-px;val dy=ry-py;val d=hypot(dx,dy);if(d>10){ax=dx/d;ay=dy/d};if(firing)shoot()}
  b.forEach{it.x+=it.vx*dt;it.y+=it.vy*dt;it.life+=dt};b.removeAll{it.life>1.2||it.x< -80||it.x>width+80||it.y< -80||it.y>height+80}
  if(spawn<=0&&z.size<min(26,5+wave*2)){spawnZ();spawn=max(.35,1.1-wave*.035)}
  val dead=ArrayList<Z>()
  z.forEach{q->q.phase+=dt*3;q.hit=max(0f,q.hit-dt);val dx=px-q.x;val dy=py-q.y;val d=max(1f,hypot(dx,dy));val sp=if(q.type==1)70f else if(q.type==2)35f else 45f;if(d>48){q.x+=dx/d*sp*dt;q.y+=dy/d*sp*dt}else hp-=(if(q.type==1)14f else if(q.type==2)8f else 10f)*dt}
  b.forEach{bb->z.forEach{q->if(!dead.contains(q)&&hypot(bb.x-q.x,bb.y-q.y)<38){q.hp--;q.hit=.12f;bb.life=2f;if(q.hp<=0){dead+=q;kills++;score+=if(q.type==1)150 else if(q.type==2)125 else 100;snd.beep(180,40)}}}};z.removeAll(dead)
  if(kills>=wave*8){wave++;hp=min(100f,hp+12);reserve+=30;snd.beep(780,120)};if(hp<=0){hp=0f;s= S.OVER;snd.beep(100,450)}
 }
 fun shoot(){if(reload>0||fire>0)return;if(ammo<=0){if(reserve>0)reload=1f;return};ammo--;fire=.13f;b+=B(px+ax*35,py+ay*35,ax*920,ay*920);snd.beep(850,28)}
 fun game(c:Canvas){val m=maps[map];bg(c,m.sky);p.color=m.ground;c.drawRect(0f,height*.22f,width.toFloat(),height.toFloat(),p);details(c,m);z.forEach{drawZ(c,it)};b.forEach{p.color=Color.YELLOW;c.drawCircle(it.x,it.y,5f,p)};drawP(c);hud(c);controls(c)}
 fun details(c:Canvas,m:M){val rr=java.util.Random(m.seed.toLong());repeat(38){val x=rr.nextInt(max(1,width)).toFloat();val y=(height*.24f+rr.nextInt(max(1,(height*.7f).toInt()))).coerceAtMost(height.toFloat());p.color=Color.argb(75,0,0,0);if(map==0)c.drawRect(x,y,x+30+rr.nextInt(55),y+10+rr.nextInt(35),p)else if(map==1){p.color=Color.argb(100,10,35,10);c.drawCircle(x,y,12+rr.nextInt(24).toFloat(),p)}else{p.color=Color.argb(110,m.accent ushr 16,(m.accent ushr 8) and 255,m.accent and 255);c.drawCircle(x,y,4+rr.nextInt(12).toFloat(),p)}}}
 fun drawP(c:Canvas){val bb=sin(SystemClock.uptimeMillis()/110.0).toFloat()*2;p.color=Color.rgb(70,130,220);c.drawCircle(px,py+bb,24f,p);p.color=Color.rgb(220,185,150);c.drawCircle(px,py-22+bb,14f,p);p.color=Color.DKGRAY;c.drawRect(px-10,py-35+bb,px+10,py-8+bb,p);p.color=Color.LTGRAY;c.drawRect(px+ax*10-4,py+ay*10-4,px+ax*42+4,py+ay*42+4,p)}
 fun drawZ(c:Canvas,q:Z){val bb=sin(q.phase).toFloat()*3;p.color=if(q.hit>0)Color.WHITE else if(q.type==1)Color.rgb(155,65,65)else if(q.type==2)Color.rgb(100,70,150)else Color.rgb(75,145,80);c.drawCircle(q.x,q.y+bb,25f+q.type*3,p);p.color=Color.rgb(210,205,180);c.drawCircle(q.x-7,q.y-8+bb,4f,p);c.drawCircle(q.x+7,q.y-8+bb,4f,p);p.color=Color.RED;c.drawRect(q.x-20,q.y-38,q.x+20,q.y-33,p);p.color=Color.GREEN;c.drawRect(q.x-20,q.y-38,q.x-20+40f*q.hp.toFloat()/max(1,3+wave/3+if(q.type==2)2 else 0),q.y-33,p)}
 fun hud(c:Canvas){val w=width.toFloat();p.color=Color.argb(175,0,0,0);c.drawRect(18f,18f,340f,105f,p);txt(c,"HP",34f,48f,18f,Color.WHITE);p.color=Color.DKGRAY;c.drawRect(78f,31f,300f,51f,p);p.color=Color.rgb(75,220,95);c.drawRect(78f,31f,78f+222*hp/100f,51f,p);txt(c,"WAVE $wave",34f,84f,18f,Color.WHITE);txt(c,"KILLS $kills",155f,84f,18f,Color.WHITE);txt(c,"$ammo / $reserve",w-35f,55f,27f,Color.WHITE,true);txt(c,maps[map].name,w/2,35f,17f,Color.WHITE,true)}
 fun controls(c:Canvas){p.color=Color.argb(45,255,255,255);c.drawCircle(110f,height-110f,82f,p);if(lid>=0)c.drawCircle(lx,ly,34f,p);p.color=Color.argb(65,255,255,255);c.drawCircle(width-110f,height-110f,78f,p);txt(c,"FIRE",width-110f,height-102f,20f,Color.WHITE,true);p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=Color.WHITE;c.drawCircle(width-205f,height-58f,30f,p);p.style=Paint.Style.FILL;txt(c,"R",width-205f,height-52f,18f,Color.WHITE,true);txt(c,"II",width-40f,40f,24f,Color.WHITE,true)}
 fun overlay(c:Canvas,v:String){p.color=Color.argb(195,0,0,0);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);txt(c,v,width/2f,height*.35f,48f,Color.WHITE,true);btn(c,RectF(width*.38f,height*.48f,width*.62f,height*.59f),"RESUME",Color.rgb(156,255,61));btn(c,RectF(width*.38f,height*.64f,width*.62f,height*.75f),"QUIT",Color.rgb(255,90,90))}
 fun over(c:Canvas){p.color=Color.argb(205,0,0,0);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);txt(c,"MISSION FAILED",width/2f,height*.30f,46f,Color.rgb(255,90,90),true);txt(c,"SCORE  $score",width/2f,height*.40f,28f,Color.WHITE,true);btn(c,RectF(width*.36f,height*.56f,width*.64f,height*.67f),"RETRY",Color.rgb(156,255,61));btn(c,RectF(width*.36f,height*.71f,width*.64f,height*.82f),"MENU",Color.rgb(100,180,255))}
 override fun onTouchEvent(e:MotionEvent):Boolean{
  val x=e.x;val y=e.y
  when(e.actionMasked){
   MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN->{val i=e.actionIndex;val id=e.getPointerId(i)
    if(s==S.MENU){if(y>height*.43&&y<height*.59)s=S.LOBBY;return true}
    if(s==S.LOBBY){if(y>height*.18&&y<height*.75){val cw=width*.27f;val j=((x-width*.04f)/(cw+width*.025f)).toInt();if(j in maps.indices)map=j};if(y>height*.77)start();return true}
    if(s==S.PAUSE){if(y>height*.45&&y<height*.62)s=S.GAME else if(y>height*.62)s=S.MENU;return true}
    if(s==S.OVER){if(y>height*.53&&y<height*.70)start() else if(y>height*.70)s=S.MENU;return true}
    if(s==S.GAME){if(x>width-75&&y<80){s=S.PAUSE;return true};if(x>width-250&&y>height-100){if(ammo<30&&reserve>0)reload=1f;return true};if(x<width*.45&&y>height*.55){lid=id;lx=x;ly=y;return true};if(x>width*.55&&y>height*.55){rid=id;rx=x;ry=y;firing=true;return true}}
   }
   MotionEvent.ACTION_MOVE->{if(s==S.GAME)for(i in 0 until e.pointerCount){val id=e.getPointerId(i);if(id==lid){lx=e.getX(i);ly=e.getY(i)};if(id==rid){rx=e.getX(i);ry=e.getY(i)}}}
   MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP,MotionEvent.ACTION_CANCEL->{val i=if(e.actionMasked==MotionEvent.ACTION_CANCEL)0 else e.actionIndex;val id=e.getPointerId(i);if(id==lid)lid=-1;if(id==rid){rid=-1;firing=false}}
  };return true
 }
 class Fx{fun beep(f:Int,ms:Int){Thread{try{val sr=22050;val n=sr*ms/1000;val a=ShortArray(n);for(i in a.indices){val env=1.0-i.toDouble()/n;a[i]=(sin(2*Math.PI*f*i/sr)*env*2800).toInt().toShort()};val at=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(a.size*2).build();at.play();at.write(a,0,a.size);at.stop();at.release()}catch(_:Throwable){}}.start()}}
}