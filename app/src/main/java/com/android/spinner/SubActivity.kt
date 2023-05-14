package com.android.spinner

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_sub.*
import kotlin.math.pow
import kotlin.math.sqrt

//리스트의 넘버와 인덱스 넘버가 다름! 이거는 수정할지 어칼지 고민 필요

class SubActivity : AppCompatActivity(), SensorEventListener {
    private var mSensorManager: SensorManager? = null
    // sensors
    private var mSensorLinearAcceleration: Sensor? = null
    private var line = FloatArray(3)

    // 거리 계산용
    private var nowAccX = 0F  //Float 타입임
    private var recentSpeedX:Float = 0F //A
    private var nowSpeedX:Float = 0F  //B
    private var distanceX:Float = 0F //이동거리

    private var nowAccY = 0F  //Float 타입임
    private var recentSpeedY:Float = 0F //A
    private var nowSpeedY:Float = 0F  //B
    private var distanceY:Float = 0F //이동거리

    private var nowAccZ = 0F  //Float 타입임
    private var recentSpeedZ:Float = 0F //A
    private var nowSpeedZ:Float = 0F  //B
    private var distanceZ:Float = 0F //이동거리

    private var last_totalAcc = 0F
    private var totalAcc = 0F
    private var totalD:Float = 0F //총 이동거리
    private var betweenStationDis:Float = 0F

    //시간계산
    private var previousTime:Long = System.currentTimeMillis()

    //정거장 수 확인용
    private var startNum = 0
    private var nowNum = 0
    private var destinationNum = 0

    //출발정거장 확인용 이동거리
    private var disA:Double = 0.0
    private var disB:Double = 0.0

    //움직임 확인용
    private var isMoving: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val destination = intent.getStringExtra("destination")
        textView.text = resources.getString(R.string.label_end, destination)
        //tvAccX!!.text = resources.getString(R.string.label_accelerometerX, acc[0]) 응용
        val destinationList = stationInfo.filter { it.name==destination }.toList()
        destinationNum = destinationList[0].num //목적 역의 넘버
        S_staion.text = "출발역을 탐색하는 중입니다."

    }

    override fun onStart() { //앱 실행시 시작하는 구간
        super.onStart()
        if (mSensorLinearAcceleration != null) {
            mSensorManager!!.registerListener(
                this,
                mSensorLinearAcceleration,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }
    override fun onStop() {
        super.onStop()
        // Stop listening the sensors
        mSensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Get sensors data when values changed
        val sensorType = event.sensor.type
        when (sensorType) {

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                line = event.values
                /* 아직 레이아웃 적용 안함
                tvLineX!!.text = resources.getString(R.string.label_lineX, line[0])
                tvLineY!!.text = resources.getString(R.string.label_lineY, line[1])
                tvLineZ!!.text = resources.getString(R.string.label_lineZ, line[2])

                nowAccX = round(line[0]*100) /100  //소수점 두번째 까지 끊음
                nowAccY = round(line[1]*100) /100
                nowAccZ = round(line[2]*100) /100


                tvSpeedX!!.text = resources.getString(R.string.label_speedX, nowSpeedX)
                tvDisX!!.text = resources.getString(R.string.label_disX, distanceX)

                tvSpeedY!!.text = resources.getString(R.string.label_speedY, nowSpeedY)
                tvDisY!!.text = resources.getString(R.string.label_disY, distanceY)

                tvSpeedZ!!.text = resources.getString(R.string.label_speedZ, nowSpeedZ)
                tvDisZ!!.text = resources.getString(R.string.label_disZ, distanceZ)

                tvTotD!!.text = resources.getString(R.string.label_totalDis, totalD)
            */
                getDiswT() //이동거리 찾는 함수수

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                last_totalAcc = totalAcc
                totalAcc = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val deltaAcceleration = totalAcc - last_totalAcc

                if (deltaAcceleration > 1.5f) {
                    // 이동 감지
                    isMoving = true
                    MoveingCheck.text = "moving test : yes"
                } else if (isMoving) {
                    Toast.makeText(this, "이동이 멈춤", Toast.LENGTH_LONG).show()
                    isMoving = false
                    MoveingCheck.text = "moving test : no"


                    //순서를 바꿀 수도 있음 (출발역 찾는 구문을 하단에, 남은 정거장 표시를 위에
                    //센서값 바뀔 때 마다 말고 초당... 이나 시간당으로 바꿀까 고려
                    //어차피 감소세 아니면 실행 안되니까 상관없나?
                    if(disA == 0.0 && totalD>500 && startNum ==0) {
                        disA = (betweenStationDis/1000.0)
                    }else if (disB == 0.0 && betweenStationDis>500){
                        disB = (betweenStationDis/1000.0)

                        //출발역 찾기
                        startNum = stationFind(disA, disB)
                        nowNum = startNum+2
                        if (startNum == 0) {
                            S_staion.text = "error"
                        } else {
                            S_staion.text = resources.getString(R.string.label_start, stationInfo[startNum-1].name)//출발역 표시
                        }
                        left_station.text = resources.getString(R.string.label_left, leftStaion(nowNum, destinationNum)) //남은 정거장 수 표시
                    }else {
                        atStop()
                    }
                    betweenStationDis = 0F //역간거리 초기화
                }

                //한정거장 남았을 때 남은 거리 표시...
                if (leftStaion(nowNum, destinationNum) == 1){
                    val leftLastDis = stationInfo[destinationNum-1].dis-betweenStationDis
                //아직 레이아웃 없음
                //left_last_station.text = resources.getString(R.string.last_distance, leftLastDis)
                    if (leftLastDis <= 0){ //도착착
                       Toast.makeText(this, "목적지에 도착했습니다", Toast.LENGTH_LONG).show()
                        onStop()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        TODO("Not yet implemented")
    }

    private fun getDiswT() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - previousTime) / 1000.0

        //X
        nowSpeedX = recentSpeedX+nowAccX*(elapsedTime.toFloat())
        val avgSpeedX = (nowSpeedX+recentSpeedX) / 2
        distanceX += avgSpeedX*elapsedTime.toFloat()

        //Y
        nowSpeedY = recentSpeedY+nowAccY*elapsedTime.toFloat()
        val avgSpeedY = (nowSpeedY+recentSpeedY) / 2
        distanceY += avgSpeedY*elapsedTime.toFloat()

        //Z
        nowSpeedZ = recentSpeedZ+nowAccZ*elapsedTime.toFloat()
        val avgSpeedZ = (nowSpeedZ+recentSpeedZ) / 2
        distanceZ += avgSpeedZ*elapsedTime.toFloat()
        previousTime = currentTime

        //총 거리
        totalD += sqrt((distanceX).pow(2)+(distanceY).pow(2)+(distanceZ).pow(2))

        //역간거리(정지시 초기화)
        betweenStationDis+= sqrt((distanceX).pow(2)+(distanceY).pow(2)+(distanceZ).pow(2))

        //이월
        previousTime = currentTime
        recentSpeedX = nowSpeedX
        recentSpeedY = nowSpeedY
        recentSpeedZ = nowSpeedZ

    }

    private fun atStop() {
        nowNum++
        if (leftStaion(nowNum, destinationNum) == 1) { //한정거장 남았을 때 표시
            Toast.makeText(this, "한 정거장 남았습니다", Toast.LENGTH_LONG).show()
            left_station.text = resources.getString(R.string.label_one)

        } else { //남은 정거장 수 -1
            left_station.text = resources.getString(R.string.label_left, leftStaion(nowNum, destinationNum))
        }
    }

}

data class inFo(val num: Int, val name: String, val dis: Double)
val stationInfo = listOf(
    inFo(1,	"진접",	                0.0),
    inFo(2,	"오남",	                2.1),
    inFo(3,	"별내별가람",	            7.8),
    inFo(4,	"당고개",              	4.4),
    inFo(5,	"상계",	                1.2),
    inFo(6,	"노원",	                1.0),
    inFo(7,	"창동",	                1.4),
    inFo(8,	"쌍문",	                1.3),
    inFo(9,	"수유(강북구청)",	        1.5),
    inFo(10,	"미아(서울사이버대학)",	    1.4),
    inFo(11,	"미아사거리",            	1.5),
    inFo(12,	"길음",	                1.3),
    inFo(13,	"성신여대입구(돈암)",	    1.4),
    inFo(14,	"한성대입구(삼선교)",	    1.0),
    inFo(15,	"혜화",	                0.9),
    inFo(16,	"동대문",	                1.5),
    inFo(17,	"동대문역사문화공원(DDP)",   0.7),
    inFo(18,	"충무로",	                1.3),
    inFo(19,	"명동(정화예술대)",	        0.7),
    inFo(20,	"회현(남대문시장)",	        0.7),
    inFo(21,	"서울역",	                0.9),
    inFo(22,	"숙대입구(갈월)",	        1.0),
    inFo(23,	"삼각지",	                1.2),
    inFo(24,	"신용산(아모레퍼시픽)",	    0.7),
    inFo(25,	"이촌(국립중앙박물관)",	    1.3),
    inFo(26,	"동작(현충원)",	            2.7),
    inFo(27,	"총신대입구(이수)",        	1.8),
    inFo(28,	"사당",	                1.1),
    inFo(29,	"남태령",	                1.6),
    inFo(30,	"선바위",	                2.0),
    inFo(31,	"경마공원",	                1.0),
    inFo(32,	"대공원",	                0.9),
    inFo(33,	"과천",	                1.0),
    inFo(34,	"정부과천청사",	            1.0),
    inFo(35,	"인덕원",	3.0),
    inFo(36,	"평촌",	1.6),
    inFo(37,	"범계",	1.3),
    inFo(38,	"금정",	2.6),
    inFo(39,	"산본",	2.3),
    inFo(40,	"수리산",	1.1),
    inFo(41,	"대야미",	2.6),
    inFo(42,	"반월",	2.0),
    inFo(43,	"상록수",	3.7),
    inFo(44,	"한대앞",	1.5),
    inFo(45,	"중앙",	1.6),
    inFo(46,	"고잔",	1.4),
    inFo(47,	"초지",	1.5),
    inFo(48,	"안산",	1.8),
    inFo(49,	"신길온천",	2.2),
    inFo(50,	"정왕",	2.9),
    inFo(51,	"오이도",	1.4),

    )

//출발점 찾기
fun stationFind(disA: Double, disB: Double): Int { //첫번째 이동거리:A, 두번째 이동거리:B

    val filteredList = stationInfo.filter { it.dis == disA }.toList()
    var start = 0
    //A와 역간거리가 같은 역만 뽑아 다시 리스트로 저장, 내용 동일(넘버,역이름,거리)

    if (filteredList.count() == 1) { //필터된 리스트에 1개 요소만 남으면 실행
        start = (filteredList[0].num - 2)
        //println("A만 사용")
        //println("출발역은 ${stationInfo[start].name}}")
        return start
    } else if (filteredList.isEmpty()) { //일치 없음
        //println("No Station, ERROR")
        return 0
    } else { //둘 이상 중복되면 실행
        for (i in 0 until filteredList.count()) { //필터리스트 요소 갯수만큼 반복
            val indexFor = filteredList[i].num //인덱스 넘버 받아서
            //println("$i, $indexFor") //테스트용(삭제)
            if (stationInfo[indexFor].dis == disB) { //B와 리스트의 다음 역까지의 역간거리와 비교
                start = (indexFor - 2) //출발위치 넘버
                //println("A, B 사용") //테스트용(삭제)
                //println("출발역은 ${stationInfo[start].name}//${stationInfo[start].num}")
                break
            }
        }
        return start
    }
}

//남은 정거장 수
fun leftStaion(nowNum: Int, desNum: Int): Int {

    return if(desNum>nowNum){
        (desNum - nowNum)
    }
    else{
        (nowNum - desNum)
    }
}