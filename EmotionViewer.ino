#define START_BYTE 'S'
#define END_BYTE 'E'
#define TX_NUM 3
#define RX_NUM 3

double x,y;
double r;
double degree;
uint8_t emotion;

uint8_t txPacket[TX_NUM], rxPacket[RX_NUM];

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
}

void loop() {
  // put your main code here, to run repeatedly:
  x = analogRead(A0) - 511.0;
  y = analogRead(A1) - 511.0;
  //Serial.print("x = ");Serial.println(x);
  //Serial.print("y = ");Serial.println(y);
  r = sqrt(x * x + y * y);
  degree = atan2(y, x) * 180.0 / PI;
  //Serial.print("r = ");Serial.println(r);
  //Serial.print("degree = ");Serial.println(degree);
  if(r > 255.0) {
    if(-180.0 <= degree && degree < -90.0) {
      emotion = 'D';
    }else if(-90.0 <= degree && degree < 0.0) {
      emotion = 'R';
    }else if(0.0 <= degree && degree < 90.0) {
      emotion = 'H';
    }else {
      emotion = 'A';
    }
  }else {
    emotion = 'N';
  }
  //Serial.println(emotion);
  txPacket[0] = START_BYTE;
  txPacket[1] = emotion;
  txPacket[2] = END_BYTE;
  for(int i = 0; i < TX_NUM; i++) {
    Serial.write(txPacket[i]);
  }
}
